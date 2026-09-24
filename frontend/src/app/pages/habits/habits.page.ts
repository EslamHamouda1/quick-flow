import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';

import { Habit, HabitFrequency, HabitWrite, HabitsService } from '../../api';
import { ClockService } from '../../core/clock.service';
import { NoticeService } from '../../core/notice.service';
import { readProblem } from '../../core/problem';
import { EmptyStateComponent } from '../../shared/empty-state';
import { FREQUENCY_OPTIONS, HABIT_FORM_FIELDS, HabitFormComponent } from './habit-form';

const FREQUENCY_LABELS = Object.fromEntries(FREQUENCY_OPTIONS.map((o) => [o.value, o.label])) as Record<HabitFrequency, string>;

// Habits page (US2): cards with period status and streak, create/edit form, today toggle and card actions.
@Component({
  selector: 'app-habits-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [HabitFormComponent, EmptyStateComponent],
  styles: `
    .habit-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
    .habit-card { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
    .habit-card .toggle { display: flex; align-items: center; }
    .habit-card .main { flex: 1 1 200px; display: flex; flex-direction: column; gap: 2px; }
    .habit-card .name { font-weight: 600; }
    .habit-card .description { color: var(--muted); font-size: 0.9em; }
    .habit-card .streak { color: var(--muted); font-size: 0.9em; }
    .habit-card .actions { display: flex; gap: 6px; }
    .header { display: flex; align-items: center; gap: var(--gap); margin-bottom: var(--gap); }
    .header h1 { margin: 0; flex: 1 1 auto; }
  `,
  template: `
    <div class="header">
      <h1 data-testid="page-title">Habits</h1>
      <button type="button" class="primary" data-testid="habit-add" (click)="openCreate()">Add Habit</button>
    </div>

    @if (editing(); as current) {
      <app-habit-form
        [habit]="current === 'new' ? null : current"
        [errors]="formErrors()"
        (save)="save($event)"
        (cancel)="closeForm()"
      />
    }

    @if (loaded()) {
      @if (habits().length === 0) {
        <app-empty-state message="No habits yet" actionLabel="Add Habit" (add)="openCreate()" />
      } @else {
        <ul class="habit-list" data-testid="habit-list">
          @for (h of habits(); track h.id) {
            <li class="card habit-card" [attr.data-testid]="'habit-card-' + h.id">
              <label class="toggle">
                <input type="checkbox" [attr.data-testid]="'habit-toggle-' + h.id" [attr.aria-label]="'Done today: ' + h.name"
                  [checked]="h.completedToday" [disabled]="!h.active || isBusy(h.id)" (change)="toggle(h, $event)" />
              </label>
              <span class="main">
                <span class="name" data-testid="habit-name-text">{{ h.name }}</span>
                @if (h.description) {
                  <span class="description">{{ h.description }}</span>
                }
              </span>
              <span class="badge badge-muted" data-testid="habit-frequency-label">{{ frequencyLabel(h.frequency) }}</span>
              <span class="badge" data-testid="habit-period-done">{{ periodDone(h) }}</span>
              <span class="streak">Streak <strong data-testid="habit-streak">{{ h.currentStreak }}</strong></span>
              @if (!h.active) {
                <span class="badge badge-danger" data-testid="habit-inactive">Inactive</span>
              }
              <span class="actions">
                <button type="button" [attr.data-testid]="'habit-edit-' + h.id" [disabled]="isBusy(h.id)" (click)="openEdit(h)">Edit</button>
                @if (h.active) {
                  <button type="button" [attr.data-testid]="'habit-deactivate-' + h.id" [disabled]="isBusy(h.id)" (click)="deactivate(h)">Deactivate</button>
                } @else {
                  <button type="button" [attr.data-testid]="'habit-activate-' + h.id" [disabled]="isBusy(h.id)" (click)="activate(h)">Activate</button>
                }
                <button type="button" class="danger" [attr.data-testid]="'habit-delete-' + h.id" [disabled]="isBusy(h.id)" (click)="remove(h)">Delete</button>
              </span>
            </li>
          }
        </ul>
      }
    }
  `,
})
export default class HabitsPage {
  private readonly api = inject(HabitsService);
  private readonly clock = inject(ClockService);
  private readonly notice = inject(NoticeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly habits = signal<Habit[]>([]);
  protected readonly loaded = signal(false);
  protected readonly editing = signal<Habit | 'new' | null>(null);
  protected readonly formErrors = signal<Record<string, string>>({});
  /** Ids of cards with a call running (FA-22: no double submit). */
  private readonly busy = signal<ReadonlySet<number>>(new Set());

  private pending: Subscription | undefined;
  private openedFromQuery = false;

  constructor() {
    this.destroyRef.onDestroy(() => this.pending?.unsubscribe());
    // `?add=1` opens the create form, also when the page is already open (FA-23).
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      if (params.get('add') === '1') {
        this.openedFromQuery = true;
        this.openCreate();
      }
    });
    this.reload();
  }

  protected frequencyLabel(f: HabitFrequency): string {
    return FREQUENCY_LABELS[f] ?? f;
  }

  // FA-21: the period text comes only from the server's doneForCurrentPeriod.
  protected periodDone(h: Habit): string {
    if (!h.doneForCurrentPeriod) return 'Not done';
    return h.frequency === HabitFrequency.Weekly ? 'Done this week' : 'Done today';
  }

  protected isBusy(id: number): boolean {
    return this.busy().has(id);
  }

  /** Re-reads all habits (active and inactive); a newer request cancels the pending one. */
  private reload(): void {
    this.pending?.unsubscribe();
    this.pending = this.api.listHabits().subscribe({
      next: (list) => {
        this.habits.set(list);
        this.loaded.set(true);
      },
      error: (err: HttpErrorResponse) => {
        this.loaded.set(true);
        this.notice.error(readProblem(err).message);
      },
    });
  }

  protected openCreate(): void {
    this.formErrors.set({});
    this.editing.set('new');
  }

  protected openEdit(habit: Habit): void {
    this.formErrors.set({});
    this.editing.set(habit);
  }

  protected closeForm(): void {
    this.editing.set(null);
    this.formErrors.set({});
    if (this.openedFromQuery) {
      this.openedFromQuery = false;
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { add: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  protected save(body: HabitWrite): void {
    const current = this.editing();
    if (current === null) return;
    const request = current === 'new' ? this.api.createHabit(body) : this.api.updateHabit(current.id, body);
    request.subscribe({
      next: () => {
        this.notice.success(current === 'new' ? 'Habit created' : 'Habit updated');
        this.closeForm();
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        const problem = readProblem(err);
        if (err.status === 400) {
          const onForm: Record<string, string> = {};
          const other: string[] = [];
          for (const [field, message] of Object.entries(problem.fieldErrors)) {
            if (HABIT_FORM_FIELDS.includes(field)) onForm[field] = message;
            else other.push(message);
          }
          this.formErrors.set(onForm);
          if (other.length > 0 || Object.keys(onForm).length === 0) {
            this.notice.error(other.length > 0 ? other.join('; ') : problem.message);
          }
          return;
        }
        this.notice.error(problem.message);
      },
    });
  }

  // FA-22 / R-9: checked = complete for today (no body), unchecked = undo today's (app-zone) completion.
  protected toggle(habit: Habit, event: Event): void {
    const box = event.target as HTMLInputElement;
    const complete = box.checked;
    // The browser has already flipped the box; show the server state until the list is re-read.
    box.checked = habit.completedToday;
    if (complete) {
      this.act(habit, this.api.completeHabit(habit.id), 'Habit completed');
    } else {
      this.act(habit, this.api.undoHabitCompletion(habit.id, this.clock.today()), 'Habit completion undone');
    }
  }

  protected deactivate(habit: Habit): void {
    this.act(habit, this.api.deactivateHabit(habit.id), 'Habit deactivated');
  }

  protected activate(habit: Habit): void {
    this.act(habit, this.api.activateHabit(habit.id), 'Habit activated');
  }

  // FA-7: deletes without a confirmation dialog.
  protected remove(habit: Habit): void {
    this.act(habit, this.api.deleteHabit(habit.id), 'Habit deleted');
  }

  /** Runs a card call; the list is re-read after success or error (a 409 `detail` goes to the notice). */
  private act(habit: Habit, request: Observable<unknown>, success: string): void {
    if (this.isBusy(habit.id)) return;
    this.setBusy(habit.id, true);
    request.subscribe({
      next: () => {
        this.setBusy(habit.id, false);
        this.notice.success(success);
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.setBusy(habit.id, false);
        this.notice.error(readProblem(err).message);
        this.reload();
      },
    });
  }

  private setBusy(id: number, on: boolean): void {
    this.busy.update((s) => {
      const next = new Set(s);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });
  }
}
