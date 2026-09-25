import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  Habit,
  HabitsService,
  LearningCard,
  LearningService,
  Plan,
  PlanCreate,
  PlanItemRef,
  PlanSourceType,
  PlanUpdate,
  Task,
  TasksService,
} from '../../api';
import { ClockService } from '../../core/clock.service';
import { toDatetimeLocal, toZonedIso } from '../../core/date-format';
import { NoticeService } from '../../core/notice.service';
import { readProblem } from '../../core/problem';
import { FieldErrorComponent } from '../../shared/field-error';

/** Fields the form has an input for; `items*` errors go to the item section, any other field to a notice (FA-31). */
export const PLAN_FORM_FIELDS = ['title', 'estimatedDurationMinutes', 'startDateTime', 'endDateTime', 'priorityOrder'];

/** Splits a 400's field errors into the ones the builder shows and the rest (messages for a notice). */
export function planFormErrors(fieldErrors: Record<string, string>): { onForm: Record<string, string>; other: string[] } {
  const onForm: Record<string, string> = {};
  const other: string[] = [];
  for (const [field, message] of Object.entries(fieldErrors)) {
    if (PLAN_FORM_FIELDS.includes(field)) onForm[field] = message;
    else if (field === 'items' || field.startsWith('items[') || field.startsWith('items.')) {
      // Keep the first item error only.
      if (!('items' in onForm)) onForm['items'] = message;
    } else other.push(message);
  }
  return { onForm, other };
}

function sourceKey(type: PlanSourceType, id: number): string {
  return `${type}:${id}`;
}

// Create + edit form for Todo Plans (AC-US4-1..3, A-7, FA-31). Items are picked only on create; edit changes
// title, duration, window and priority. No client-side checks: empty values are sent as null so the server names the field (FA-3).
@Component({
  selector: 'app-plan-builder',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  styles: `
    .sources { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: var(--gap); }
    fieldset { border: 1px solid var(--border); border-radius: var(--radius); padding: var(--gap); margin: 0; }
    legend { font-weight: 600; }
    .source { display: flex; align-items: center; gap: 8px; }
    .empty { color: var(--muted); }
  `,
  template: `
    <form class="card" data-testid="plan-builder" (submit)="submit($event)" novalidate>
      <h2>{{ isEdit() ? 'Edit Plan' : 'Create Plan' }}</h2>
      <div class="form-row">
        <label for="plan-title">Title</label>
        <input id="plan-title" type="text" data-testid="plan-title" [value]="title()" (input)="title.set(value($event))" />
        <app-field-error field="title" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="plan-duration">Estimated duration (minutes)</label>
        <input id="plan-duration" type="number" data-testid="plan-duration" [value]="duration()" (input)="duration.set(value($event))" />
        <app-field-error field="estimatedDurationMinutes" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="plan-start">Start</label>
        <input id="plan-start" type="datetime-local" data-testid="plan-start" [value]="start()" (input)="start.set(value($event))" />
        <app-field-error field="startDateTime" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="plan-end">End</label>
        <input id="plan-end" type="datetime-local" data-testid="plan-end" [value]="end()" (input)="end.set(value($event))" />
        <app-field-error field="endDateTime" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="plan-priority">Priority order (1 = highest)</label>
        <input id="plan-priority" type="number" data-testid="plan-priority" [value]="priority()" (input)="priority.set(value($event))" />
        <app-field-error field="priorityOrder" [errors]="errors()" />
      </div>

      @if (!isEdit()) {
        <section data-testid="plan-items">
          <h3>Items</h3>
          <app-field-error field="items" [errors]="errors()" />
          <div class="sources">
            <fieldset>
              <legend>Tasks</legend>
              @for (t of tasks(); track t.id) {
                <label class="source">
                  <input type="checkbox" [attr.data-testid]="'plan-source-task-' + t.id"
                    [checked]="isSelected(sourceType.Task, t.id)" (change)="toggle(sourceType.Task, t.id, $event)" />
                  {{ t.title }}
                </label>
              } @empty {
                <p class="empty">No tasks</p>
              }
            </fieldset>
            <fieldset>
              <legend>Habits</legend>
              @for (h of habits(); track h.id) {
                <label class="source">
                  <input type="checkbox" [attr.data-testid]="'plan-source-habit-' + h.id"
                    [checked]="isSelected(sourceType.Habit, h.id)" (change)="toggle(sourceType.Habit, h.id, $event)" />
                  {{ h.name }}
                </label>
              } @empty {
                <p class="empty">No active habits</p>
              }
            </fieldset>
            <fieldset>
              <legend>Learning</legend>
              @for (c of cards(); track c.id) {
                <label class="source">
                  <input type="checkbox" [attr.data-testid]="'plan-source-learning-' + c.id"
                    [checked]="isSelected(sourceType.LearningResource, c.id)" (change)="toggle(sourceType.LearningResource, c.id, $event)" />
                  {{ c.title }}
                </label>
              } @empty {
                <p class="empty">No learning cards</p>
              }
            </fieldset>
          </div>
        </section>
      }

      <div class="form-actions">
        <button type="submit" class="primary" data-testid="plan-save">Save</button>
        <button type="button" data-testid="plan-cancel" (click)="cancel.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class PlanBuilderComponent implements OnInit {
  private readonly tasksApi = inject(TasksService);
  private readonly habitsApi = inject(HabitsService);
  private readonly learningApi = inject(LearningService);
  private readonly notice = inject(NoticeService);
  private readonly clock = inject(ClockService);
  private readonly destroyRef = inject(DestroyRef);

  /** The plan being edited; null = create. */
  readonly plan = input<Plan | null>(null);
  readonly errors = input<Record<string, string>>({});
  /** A PlanCreate in create mode, a PlanUpdate in edit mode. */
  readonly save = output<PlanCreate | PlanUpdate>();
  readonly cancel = output<void>();

  protected readonly sourceType = PlanSourceType;

  protected readonly isEdit = computed(() => this.plan() !== null);
  protected readonly title = linkedSignal(() => this.plan()?.title ?? '');
  protected readonly duration = linkedSignal(() => this.numberText(this.plan()?.estimatedDurationMinutes));
  protected readonly start = linkedSignal(() => this.localText(this.plan()?.startDateTime));
  protected readonly end = linkedSignal(() => this.localText(this.plan()?.endDateTime));
  protected readonly priority = linkedSignal(() => this.numberText(this.plan()?.priorityOrder));

  protected readonly tasks = signal<Task[]>([]);
  protected readonly habits = signal<Habit[]>([]);
  protected readonly cards = signal<LearningCard[]>([]);
  /** Ticked sources as `TYPE:id`, in the order they were ticked. */
  private readonly selected = signal<readonly string[]>([]);

  ngOnInit(): void {
    if (this.plan() !== null) return;
    const onError = (err: HttpErrorResponse) => this.notice.error(readProblem(err).message);
    this.tasksApi.listTasks().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: (l) => this.tasks.set(l), error: onError });
    this.habitsApi.listHabits(true).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: (l) => this.habits.set(l), error: onError });
    this.learningApi.listLearningCards().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: (l) => this.cards.set(l), error: onError });
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected isSelected(type: PlanSourceType, id: number): boolean {
    return this.selected().includes(sourceKey(type, id));
  }

  protected toggle(type: PlanSourceType, id: number, event: Event): void {
    const key = sourceKey(type, id);
    const on = (event.target as HTMLInputElement).checked;
    this.selected.update((keys) => (on ? (keys.includes(key) ? keys : [...keys, key]) : keys.filter((k) => k !== key)));
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const zone = this.clock.timeZone();
    const fields = {
      title: this.title(),
      estimatedDurationMinutes: this.toNumber(this.duration()),
      startDateTime: this.toIso(this.start(), zone),
      endDateTime: this.toIso(this.end(), zone),
      priorityOrder: this.toNumber(this.priority()),
    };
    // Empty values go out as null so the server names the field (FA-31); the generated types are non-nullable.
    if (this.isEdit()) {
      this.save.emit(fields as unknown as PlanUpdate);
      return;
    }
    const items: PlanItemRef[] = this.selected().map((key) => {
      const i = key.lastIndexOf(':');
      return { sourceType: key.slice(0, i) as PlanSourceType, sourceId: Number(key.slice(i + 1)) };
    });
    this.save.emit({ ...fields, items } as unknown as PlanCreate);
  }

  private numberText(n: number | undefined): string {
    return n === undefined || n === null ? '' : String(n);
  }

  private localText(iso: string | undefined): string {
    return iso ? toDatetimeLocal(iso, this.clock.timeZone()) : '';
  }

  private toNumber(text: string): number | null {
    return text === '' ? null : Number(text);
  }

  private toIso(local: string, zone: string): string | null {
    if (local === '') return null;
    const iso = toZonedIso(local, zone);
    return iso === '' ? null : iso;
  }
}
