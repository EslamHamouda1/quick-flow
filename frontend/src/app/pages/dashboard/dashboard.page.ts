import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { Dashboard, DashboardService, HabitFrequency, Plan, PlanItem, PlansService, TaskPriority } from '../../api';
import { ClockService } from '../../core/clock.service';
import { formatDate } from '../../core/date-format';
import { NoticeService } from '../../core/notice.service';
import { PlanStartWatcher } from '../../core/plan-start-watcher.service';
import { readProblem } from '../../core/problem';
import { FREQUENCY_OPTIONS } from '../habits/habit-form';
import { PlanCardComponent } from '../plans/plan-card';
import { PRIORITY_OPTIONS } from '../tasks/task-form';

/** A re-read waits this long past a plan end / start / midnight, so the server's clock has passed it too. */
const BOUNDARY_SLACK_MS = 1000;

const PRIORITY_LABELS = Object.fromEntries(PRIORITY_OPTIONS.map((o) => [o.value, o.label])) as Record<TaskPriority, string>;
const FREQUENCY_LABELS = Object.fromEntries(FREQUENCY_OPTIONS.map((o) => [o.value, o.label])) as Record<HabitFrequency, string>;

// Dashboard (US5): every figure comes from one `getDashboard` read (FR-09.1, FR-09.2); in-progress plans as plan
// cards without edit/delete (FA-35); quick-add links (R-7); re-read after a toggle, when a shown plan ends, when a
// plan starts and at app-zone midnight (FA-36).
@Component({
  selector: 'app-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, PlanCardComponent],
  styles: `
    h1 { margin: 0 0 4px; }
    .greeting { margin: 0 0 var(--gap); color: var(--muted); font-size: 1.1em; }
    .quick-add { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: var(--gap); }
    .metrics { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: var(--gap); }
    .metric .label { color: var(--muted); font-size: 0.85em; }
    .metric .value { font-weight: 600; margin-top: 4px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--gap); margin-top: var(--gap); align-items: start; }
    section h2 { margin: 0 0 8px; font-size: 1.1em; }
    .percent { font-size: 1.6em; font-weight: 600; }
    .rows { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 6px; }
    .rows li { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
    .rows .text { flex: 1 1 120px; }
    .muted { color: var(--muted); font-size: 0.85em; }
    .empty { color: var(--muted); margin: 0; }
    .plans { margin-top: var(--gap); }
    .plan-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--gap); align-items: start; }
    .snapshot { display: grid; grid-template-columns: auto 1fr; gap: 4px 12px; margin: 0; }
    .snapshot dt { color: var(--muted); }
    .snapshot dd { margin: 0; font-weight: 600; }
  `,
  template: `
    <h1 data-testid="page-title">Dashboard</h1>
    <p class="greeting" data-testid="dash-greeting">{{ greeting() }}</p>

    <div class="quick-add">
      <button type="button" data-testid="quick-add-task" routerLink="/tasks" [queryParams]="{ add: 1 }">Add Task</button>
      <button type="button" data-testid="quick-add-habit" routerLink="/habits" [queryParams]="{ add: 1 }">Add Habit</button>
      <button type="button" data-testid="quick-add-learning" routerLink="/learning" [queryParams]="{ add: 1 }">Add Learning Card</button>
      <button type="button" data-testid="quick-add-plan" routerLink="/plans" [queryParams]="{ add: 1 }">Create Plan</button>
    </div>

    @if (data(); as d) {
      <div class="metrics">
        <div class="card metric">
          <div class="label">Tasks</div>
          <div class="value" data-testid="metric-tasks">{{ d.taskCounts.done }}/{{ d.taskCounts.total }} tasks done</div>
        </div>
        <div class="card metric">
          <div class="label">Habits</div>
          <div class="value" data-testid="metric-habits">{{ d.habitCounts.completedToday }}/{{ d.habitCounts.active }} habits today</div>
        </div>
        <div class="card metric">
          <div class="label">Todo Plans</div>
          <div class="value" data-testid="metric-plans">
            {{ d.planCounts.inProgress }} in progress · {{ d.planCounts.notStarted }} upcoming · {{ d.planCounts.completed }} completed
          </div>
        </div>
        <div class="card metric">
          <div class="label">Learning</div>
          <div class="value" data-testid="metric-learning">{{ d.learning.inProgress }} in progress · {{ d.learning.completed }} completed</div>
        </div>
      </div>

      <div class="grid">
        <section class="card">
          <h2>Task completion</h2>
          <div class="percent" data-testid="dash-task-percent">{{ d.taskCompletionPercent }}%</div>
        </section>

        <section class="card" data-testid="dash-due-today">
          <h2>Due today</h2>
          @if (d.dueToday.length === 0) {
            <p class="empty">Nothing due today</p>
          } @else {
            <ul class="rows">
              @for (t of d.dueToday; track t.id) {
                <li [attr.data-testid]="'dash-task-' + t.id">
                  <span class="text">{{ t.title }}</span>
                  <span class="badge badge-muted">{{ priorityLabel(t.priority) }}</span>
                  @if (t.dueDate) { <span class="muted">{{ date(t.dueDate) }}</span> }
                </li>
              }
            </ul>
          }
        </section>

        <section class="card" data-testid="dash-overdue">
          <h2>Overdue</h2>
          @if (d.overdue.length === 0) {
            <p class="empty">No overdue tasks</p>
          } @else {
            <ul class="rows">
              @for (t of d.overdue; track t.id) {
                <li [attr.data-testid]="'dash-task-' + t.id">
                  <span class="text">{{ t.title }}</span>
                  <span class="badge badge-muted">{{ priorityLabel(t.priority) }}</span>
                  @if (t.dueDate) { <span class="muted">{{ date(t.dueDate) }}</span> }
                </li>
              }
            </ul>
          }
        </section>

        <section class="card" data-testid="dash-completed-today">
          <h2>Completed today</h2>
          @if (d.completedToday.length === 0) {
            <p class="empty">Nothing completed today</p>
          } @else {
            <ul class="rows">
              @for (t of d.completedToday; track t.id) {
                <li [attr.data-testid]="'dash-task-' + t.id">
                  <span class="text">{{ t.title }}</span>
                  <span class="badge badge-muted">{{ priorityLabel(t.priority) }}</span>
                  @if (t.dueDate) { <span class="muted">{{ date(t.dueDate) }}</span> }
                </li>
              }
            </ul>
          }
        </section>

        <section class="card" data-testid="dash-habits">
          <h2>Today's habits <span class="muted" data-testid="dash-habit-count">{{ d.habitCounts.completedToday }}/{{ d.habitCounts.active }}</span></h2>
          @if (d.habits.length === 0) {
            <p class="empty">No active habits</p>
          } @else {
            <ul class="rows">
              @for (h of d.habits; track h.id) {
                <li [attr.data-testid]="'dash-habit-' + h.id">
                  <span class="text">{{ h.name }}</span>
                  <span class="muted">{{ frequencyLabel(h.frequency) }}</span>
                  @if (h.completedToday) {
                    <span class="badge badge-success" [attr.data-testid]="'dash-habit-done-' + h.id">Done today</span>
                  }
                </li>
              }
            </ul>
          }
        </section>

        <section class="card" data-testid="dash-learning">
          <h2>Learning</h2>
          <dl class="snapshot">
            <dt>Not started</dt><dd data-testid="dash-learning-not-started">{{ d.learning.notStarted }}</dd>
            <dt>In progress</dt><dd data-testid="dash-learning-in-progress">{{ d.learning.inProgress }}</dd>
            <dt>Completed</dt><dd data-testid="dash-learning-completed">{{ d.learning.completed }}</dd>
            <dt>Milestones</dt><dd data-testid="dash-milestones">{{ d.learning.milestonesDone }}/{{ d.learning.milestonesTotal }}</dd>
          </dl>
        </section>
      </div>

      <section class="plans" data-testid="dash-plans">
        <h2>Plans in progress</h2>
        @if (d.activePlans.length === 0) {
          <p class="empty">No plans in progress</p>
        } @else {
          <div class="plan-grid">
            @for (p of d.activePlans; track p.id) {
              <div [attr.data-testid]="'dash-plan-' + p.id">
                <app-plan-card [plan]="p" [actions]="false" [highlighted]="watcher.highlighted().has(p.id)" [busy]="isBusy(p.id)"
                  (toggle)="toggleItem(p, $event.item, $event.done)" />
              </div>
            }
          </div>
        }
      </section>
    }
  `,
})
export default class DashboardPage {
  private readonly api = inject(DashboardService);
  private readonly plansApi = inject(PlansService);
  private readonly clock = inject(ClockService);
  private readonly notice = inject(NoticeService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly watcher = inject(PlanStartWatcher);

  protected readonly data = signal<Dashboard | null>(null);
  private readonly name = signal<string | null>(null);
  protected readonly greeting = computed(() => {
    const name = this.name()?.trim();
    return name ? `Hello, ${name}` : 'Hello';
  });
  /** Ids of plans with an item call running (their card's controls are disabled). */
  private readonly busy = signal<ReadonlySet<number>>(new Set());
  /** When the last dashboard was read; the next re-read is at the first shown plan end after it (R-4). */
  private readonly readAt = signal<number | null>(null);
  /** A re-read asked for at this time (a plan started, the app-zone day changed), or null. */
  private readonly dueAt = signal<number | null>(null);
  private readonly today = computed(() => this.clock.today());

  /** The earliest end of a shown plan still ahead of the last read, or null. */
  private readonly nextBoundary = computed(() => {
    const readAt = this.readAt();
    if (readAt === null) return null;
    let next: number | null = null;
    for (const p of this.data()?.activePlans ?? []) {
      const t = Date.parse(p.endDateTime);
      if (t > readAt && (next === null || t < next)) next = t;
    }
    return next;
  });

  private pending: Subscription | undefined;

  constructor() {
    this.destroyRef.onDestroy(() => this.pending?.unsubscribe());
    effect(() => {
      const now = this.clock.now();
      const boundary = this.nextBoundary();
      const due = this.dueAt();
      if ((boundary !== null && now >= boundary + BOUNDARY_SLACK_MS) || (due !== null && now >= due)) this.reload();
    });
    // A plan just started (the watcher highlighted it): it now belongs in "Plans in progress".
    let seen = untracked(this.watcher.highlighted);
    effect(() => {
      const ids = this.watcher.highlighted();
      const gained = [...ids].some((id) => !seen.has(id));
      seen = ids;
      if (gained) this.requestReload();
    });
    // App-zone midnight: due today, overdue, completed today and today's habits change.
    let day = untracked(this.today);
    effect(() => {
      const d = this.today();
      if (d === day) return;
      day = d;
      this.requestReload();
    });
    this.reload();
  }

  protected priorityLabel(p: TaskPriority): string {
    return PRIORITY_LABELS[p] ?? p;
  }

  protected frequencyLabel(f: HabitFrequency): string {
    return FREQUENCY_LABELS[f] ?? f;
  }

  protected date(isoDate: string): string {
    return formatDate(isoDate);
  }

  protected isBusy(id: number): boolean {
    return this.busy().has(id);
  }

  /** Re-reads the dashboard (and the greeting name); a newer request cancels the pending one. */
  protected reload(): void {
    this.pending?.unsubscribe();
    this.readAt.set(null);
    this.dueAt.set(null);
    void this.displayName().then((name) => this.name.set(name));
    const startedAt = this.clock.now();
    this.pending = this.api.getDashboard().subscribe({
      next: (d) => {
        this.data.set(d);
        // Measured from the request start, so an end passed during the call triggers one more read.
        this.readAt.set(startedAt - BOUNDARY_SLACK_MS);
      },
      // FA-37: the last figures stay.
      error: (err: HttpErrorResponse) => this.notice.error(readProblem(err).message),
    });
  }

  // FR-08.5: the toggle goes to the plan; then the whole dashboard is re-read so every figure is the server's.
  protected toggleItem(plan: Plan, item: PlanItem, done: boolean): void {
    if (this.isBusy(plan.id)) return;
    this.setBusy(plan.id, true);
    this.plansApi.setPlanItemDone(plan.id, item.id, { done }).subscribe({
      next: (updated) => {
        this.setBusy(plan.id, false);
        this.notice.success(done ? 'Item marked done' : 'Item marked not done');
        // The returned plan shows at once; the re-read then brings every other figure.
        this.data.update((d) => d && { ...d, activePlans: d.activePlans.map((p) => (p.id === updated.id ? updated : p)) });
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.setBusy(plan.id, false);
        this.notice.error(readProblem(err).message);
        this.reload();
      },
    });
  }

  /**
   * FA-34: `/api/settings` does not exist before US6, so there is no name and the greeting is "Hello".
   * Phase-08 replaces this with `getSettings().displayName`, read on every Dashboard load.
   */
  private displayName(): Promise<string | null> {
    return Promise.resolve(null);
  }

  private requestReload(): void {
    this.dueAt.set(untracked(this.clock.now) + BOUNDARY_SLACK_MS);
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
