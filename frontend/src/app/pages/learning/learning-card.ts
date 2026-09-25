import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { LearningCard, LearningService, LearningStatus, Milestone } from '../../api';
import { ClockService } from '../../core/clock.service';
import { formatDate, formatDateTime } from '../../core/date-format';
import { NoticeService } from '../../core/notice.service';
import { readProblem } from '../../core/problem';
import { FieldErrorComponent } from '../../shared/field-error';

// FA-11: only the three valid values are offered.
export const STATUS_OPTIONS: { value: LearningStatus; label: string }[] = [
  { value: LearningStatus.NotStarted, label: 'Not Started' },
  { value: LearningStatus.InProgress, label: 'In Progress' },
  { value: LearningStatus.Completed, label: 'Completed' },
];

/** Fields each sub-form has an input for; a 400 on any other field goes to a notice (FA-28). */
const SUB_FORM_FIELDS = { milestone: ['title', 'targetDate'], note: ['text'] } as const;

export type SubForm = keyof typeof SUB_FORM_FIELDS;

/** Field errors of one sub-form of one card; `errors: {}` clears them. */
export interface SubFormErrors {
  form: SubForm;
  errors: Record<string, string>;
}

// One learning card (AC-US3-3..6, AC-US3-8): status select, milestone count, expandable milestones and notes.
// The card calls LearningService itself and emits `changed` after every call, success or error (FA-26).
@Component({
  selector: 'app-learning-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  styles: `
    :host { display: block; }
    .head { display: flex; flex-direction: column; gap: 4px; }
    .title { font-weight: 600; }
    .description { color: var(--muted); font-size: 0.9em; white-space: pre-wrap; }
    .meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 8px; }
    .actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
    .details { margin-top: 12px; display: flex; flex-direction: column; gap: 12px; }
    .details h3 { margin: 0 0 6px; font-size: 1em; }
    .rows { list-style: none; margin: 0 0 6px; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .rows li { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
    .rows .text { flex: 1 1 120px; white-space: pre-wrap; }
    .rows .when { color: var(--muted); font-size: 0.85em; }
    .empty { color: var(--muted); font-size: 0.9em; margin: 0 0 6px; }
    .add-row { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 6px; }
    .add-row input[type='text'], .add-row textarea { flex: 1 1 140px; }
  `,
  template: `
    @let c = card();
    <article class="card" [attr.data-testid]="'card-' + c.id">
      <div class="head">
        <span class="title" data-testid="card-title-text">{{ c.title }}</span>
        @if (c.description) {
          <span class="description" data-testid="card-description-text">{{ c.description }}</span>
        }
      </div>
      <div class="meta">
        <select data-testid="card-status" [attr.aria-label]="'Status: ' + c.title" [disabled]="disabled()" (change)="changeStatus($event)">
          @for (o of statusOptions; track o.value) {
            <option [value]="o.value" [selected]="o.value === c.status">{{ o.label }}</option>
          }
        </select>
        <span class="badge badge-muted" data-testid="card-milestone-count">{{ c.milestonesDone }}/{{ c.milestonesTotal }}</span>
      </div>
      <div class="actions">
        <button type="button" [attr.data-testid]="'card-expand-' + c.id" [attr.aria-expanded]="expanded()" (click)="toggle.emit()">
          {{ expanded() ? 'Hide details' : 'Show details' }}
        </button>
        <button type="button" [attr.data-testid]="'card-edit-' + c.id" [disabled]="disabled()" (click)="edit.emit()">Edit</button>
        <button type="button" class="danger" [attr.data-testid]="'card-delete-' + c.id" [disabled]="disabled()" (click)="remove.emit()">Delete</button>
      </div>

      @if (expanded()) {
        <div class="details" [attr.data-testid]="'card-details-' + c.id">
          <section>
            <h3>Milestones</h3>
            @if (c.milestones.length === 0) {
              <p class="empty">No milestones yet</p>
            } @else {
              <ul class="rows">
                @for (m of c.milestones; track m.id) {
                  <li [attr.data-testid]="'milestone-' + m.id">
                    <input type="checkbox" [attr.data-testid]="'milestone-done-' + m.id" [attr.aria-label]="'Done: ' + m.title"
                      [checked]="m.done" [disabled]="disabled()" (change)="toggleMilestone(m, $event)" />
                    <span class="text">{{ m.title }}</span>
                    @if (m.targetDate) {
                      <span class="when">{{ date(m.targetDate) }}</span>
                    }
                    <button type="button" class="danger" [attr.data-testid]="'milestone-delete-' + m.id" [disabled]="disabled()" (click)="removeMilestone(m)">Remove</button>
                  </li>
                }
              </ul>
            }
            <form class="add-row" (submit)="addMilestone($event)" novalidate>
              <input type="text" [attr.data-testid]="'milestone-title-input-' + c.id" aria-label="Milestone title" placeholder="Milestone title"
                [value]="milestoneTitle()" (input)="milestoneTitle.set(value($event))" />
              <input type="date" [attr.data-testid]="'milestone-date-input-' + c.id" aria-label="Target date"
                [value]="milestoneDate()" (input)="milestoneDate.set(value($event))" />
              <button type="submit" [attr.data-testid]="'milestone-add-' + c.id" [disabled]="disabled()">Add milestone</button>
            </form>
            <app-field-error field="title" [errors]="milestoneErrors()" />
            <app-field-error field="targetDate" [errors]="milestoneErrors()" />
          </section>

          <section>
            <h3>Notes</h3>
            @if (c.notes.length === 0) {
              <p class="empty">No notes yet</p>
            } @else {
              <ul class="rows">
                @for (n of c.notes; track n.id) {
                  <li [attr.data-testid]="'note-' + n.id">
                    <span class="text">{{ n.text }}</span>
                    <span class="when">{{ dateTime(n.createdAt) }}</span>
                    <button type="button" class="danger" [attr.data-testid]="'note-delete-' + n.id" [disabled]="disabled()" (click)="removeNote(n.id)">Remove</button>
                  </li>
                }
              </ul>
            }
            <form class="add-row" (submit)="addNote($event)" novalidate>
              <textarea [attr.data-testid]="'note-text-input-' + c.id" aria-label="Note text" placeholder="Note"
                [value]="noteText()" (input)="noteText.set(value($event))"></textarea>
              <button type="submit" [attr.data-testid]="'note-add-' + c.id" [disabled]="disabled()">Add note</button>
            </form>
            <app-field-error field="text" [errors]="noteErrors()" />
          </section>
        </div>
      }
    </article>
  `,
})
export class LearningCardComponent {
  private readonly api = inject(LearningService);
  private readonly clock = inject(ClockService);
  private readonly notice = inject(NoticeService);

  readonly card = input.required<LearningCard>();
  readonly expanded = input(false);
  /** A page-level call (delete) is running for this card. */
  readonly busy = input(false);
  readonly milestoneErrors = input<Record<string, string>>({});
  readonly noteErrors = input<Record<string, string>>({});

  /** A call finished (success or error): the page re-reads the list. */
  readonly changed = output<void>();
  readonly edit = output<void>();
  readonly remove = output<void>();
  readonly toggle = output<void>();
  /** Sub-form errors for the page to keep (it clears the other forms' errors). */
  readonly subFormErrors = output<SubFormErrors>();

  protected readonly statusOptions = STATUS_OPTIONS;

  protected readonly milestoneTitle = signal('');
  protected readonly milestoneDate = signal('');
  protected readonly noteText = signal('');

  /** A call of this card is running (FA-26: no double submit). */
  private readonly working = signal(false);
  protected readonly disabled = computed(() => this.busy() || this.working());

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
  }

  protected date(isoDate: string): string {
    return formatDate(isoDate);
  }

  protected dateTime(iso: string): string {
    return formatDateTime(iso, this.clock.timeZone());
  }

  // FA-26: PUT replaces, so the card's current title and description are sent with the new status.
  protected changeStatus(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const c = this.card();
    const status = select.value as LearningStatus;
    // The browser has already changed the select; show the server state until the list is re-read.
    select.value = c.status;
    if (status === c.status) return;
    this.run(this.api.updateLearningCard(c.id, { title: c.title, description: c.description ?? null, status }), 'Status updated');
  }

  // FA-26: the milestone's current title and target date are sent with the new done flag.
  protected toggleMilestone(m: Milestone, event: Event): void {
    const box = event.target as HTMLInputElement;
    const done = box.checked;
    box.checked = m.done;
    this.run(
      this.api.updateMilestone(this.card().id, m.id, { title: m.title, targetDate: m.targetDate ?? null, done }),
      'Milestone updated',
    );
  }

  protected removeMilestone(m: Milestone): void {
    this.run(this.api.deleteMilestone(this.card().id, m.id), 'Milestone removed');
  }

  protected addMilestone(event: Event): void {
    event.preventDefault();
    const body = { title: this.milestoneTitle(), targetDate: this.milestoneDate() === '' ? null : this.milestoneDate() };
    this.run(this.api.addMilestone(this.card().id, body), 'Milestone added', 'milestone', () => {
      this.milestoneTitle.set('');
      this.milestoneDate.set('');
    });
  }

  protected removeNote(noteId: number): void {
    this.run(this.api.deleteNote(this.card().id, noteId), 'Note removed');
  }

  protected addNote(event: Event): void {
    event.preventDefault();
    this.run(this.api.addNote(this.card().id, { text: this.noteText() }), 'Note added', 'note', () => this.noteText.set(''));
  }

  /**
   * Runs one card call. With `form`, a 400 puts the fields that form has under it (FA-28); every other error
   * goes to a notice with the server's message. `changed` is emitted after success and after error.
   */
  private run(request: Observable<unknown>, success: string, form?: SubForm, onSuccess?: () => void): void {
    if (this.disabled()) return;
    this.working.set(true);
    if (form) this.subFormErrors.emit({ form, errors: {} });
    request.subscribe({
      next: () => {
        this.working.set(false);
        onSuccess?.();
        this.notice.success(success);
        this.changed.emit();
      },
      error: (err: HttpErrorResponse) => {
        this.working.set(false);
        const problem = readProblem(err);
        if (form && err.status === 400) {
          const fields: readonly string[] = SUB_FORM_FIELDS[form];
          const onForm: Record<string, string> = {};
          const other: string[] = [];
          for (const [field, message] of Object.entries(problem.fieldErrors)) {
            if (fields.includes(field)) onForm[field] = message;
            else other.push(message);
          }
          this.subFormErrors.emit({ form, errors: onForm });
          if (other.length > 0 || Object.keys(onForm).length === 0) {
            this.notice.error(other.length > 0 ? other.join('; ') : problem.message);
          }
        } else {
          this.notice.error(problem.message);
        }
        this.changed.emit();
      },
    });
  }
}
