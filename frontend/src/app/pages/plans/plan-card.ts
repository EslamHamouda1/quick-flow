import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';

import { Plan, PlanItem, PlanSourceType, PlanStatus } from '../../api';
import { ClockService } from '../../core/clock.service';
import { formatDateTime } from '../../core/date-format';
import { RestTimeComponent } from '../../shared/rest-time';

export const STATUS_LABELS: Record<PlanStatus, string> = {
  [PlanStatus.NotStarted]: 'Not Started',
  [PlanStatus.InProgress]: 'In Progress',
  [PlanStatus.Completed]: 'Completed',
};

export const SOURCE_TYPE_LABELS: Record<PlanSourceType, string> = {
  [PlanSourceType.Task]: 'Task',
  [PlanSourceType.Habit]: 'Habit',
  [PlanSourceType.LearningResource]: 'Learning',
};

const STATUS_BADGES: Record<PlanStatus, string> = {
  [PlanStatus.NotStarted]: 'badge badge-muted',
  [PlanStatus.InProgress]: 'badge badge-warning',
  [PlanStatus.Completed]: 'badge badge-success',
};

// One Todo Plan (AC-US4-4..7, AC-US4-10, FR-07.6): status, progress, rest time while In Progress, items with done toggles.
// Presentational: the page (and later the Dashboard) makes the calls.
@Component({
  selector: 'app-plan-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RestTimeComponent],
  styles: `
    :host { display: block; }
    article.started { outline: 2px solid var(--success); }
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
    .title { font-weight: 600; }
    .meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 8px; }
    .rest { display: inline-flex; align-items: center; gap: 4px; }
    .label, .when, .type { color: var(--muted); font-size: 0.85em; }
    .rows { list-style: none; margin: 8px 0 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .rows li { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
    .rows .text { flex: 1 1 120px; }
    .actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
  `,
  template: `
    @let p = plan();
    <article class="card" [attr.data-testid]="'plan-' + p.id" [class.started]="highlighted()">
      <div class="head">
        <span class="title" data-testid="plan-title-text">{{ p.title }}</span>
        @if (highlighted()) {
          <span class="badge badge-success" [attr.data-testid]="'plan-started-' + p.id">Started</span>
        }
      </div>
      <div class="meta">
        <span [class]="statusBadges[p.status]" data-testid="plan-status">{{ statusLabels[p.status] }}</span>
        <span data-testid="plan-progress">{{ p.progressPercent }}% · {{ p.doneItems }}/{{ p.totalItems }} items</span>
        @if (p.status === inProgress && p.restSeconds != null) {
          <span class="rest"><span class="label">Time left</span> <app-rest-time [endDateTime]="p.endDateTime" /></span>
        }
      </div>
      <div class="meta">
        <span class="when" data-testid="plan-window">{{ dateTime(p.startDateTime) }} – {{ dateTime(p.endDateTime) }}</span>
        <span data-testid="plan-priority-order">Priority {{ p.priorityOrder }}</span>
        <span class="when" data-testid="plan-estimated-duration">{{ p.estimatedDurationMinutes }} min</span>
      </div>
      <ul class="rows">
        @for (item of p.items; track item.id) {
          <li [attr.data-testid]="'plan-item-' + item.id">
            <input type="checkbox" [attr.data-testid]="'plan-item-done-' + item.id" [attr.aria-label]="'Done: ' + item.sourceTitle"
              [checked]="item.done" [disabled]="busy()" (change)="toggleItem(item, $event)" />
            <span class="type">{{ sourceTypeLabels[item.sourceType] }}</span>
            <span class="text">{{ item.sourceTitle }}</span>
            @if (item.sourceRemoved) {
              <span class="badge badge-muted" [attr.data-testid]="'plan-item-removed-' + item.id">Removed</span>
            }
          </li>
        }
      </ul>
      <div class="actions">
        <button type="button" [attr.data-testid]="'plan-edit-' + p.id" [disabled]="busy()" (click)="edit.emit()">Edit</button>
        <button type="button" class="danger" [attr.data-testid]="'plan-delete-' + p.id" [disabled]="busy()" (click)="remove.emit()">Delete</button>
      </div>
    </article>
  `,
})
export class PlanCardComponent {
  private readonly clock = inject(ClockService);

  readonly plan = input.required<Plan>();
  /** The plan's start was reached while the app was open (AC-US4-9, FA-32). */
  readonly highlighted = input(false);
  /** A call for this plan is running: all controls are disabled. */
  readonly busy = input(false);

  readonly toggle = output<{ item: PlanItem; done: boolean }>();
  readonly edit = output<void>();
  readonly remove = output<void>();

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusBadges = STATUS_BADGES;
  protected readonly sourceTypeLabels = SOURCE_TYPE_LABELS;
  protected readonly inProgress = PlanStatus.InProgress;

  protected dateTime(iso: string): string {
    return formatDateTime(iso, this.clock.timeZone());
  }

  protected toggleItem(item: PlanItem, event: Event): void {
    const box = event.target as HTMLInputElement;
    const done = box.checked;
    // The browser has already changed the box; show the server state until the page replaces the plan.
    box.checked = item.done;
    this.toggle.emit({ item, done });
  }
}
