import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';

import { Plan, PlanCreate, PlanItem, PlanStatus, PlanUpdate, PlansService } from '../../api';
import { ClockService } from '../../core/clock.service';
import { NoticeService } from '../../core/notice.service';
import { PlanStartWatcher } from '../../core/plan-start-watcher.service';
import { readProblem } from '../../core/problem';
import { EmptyStateComponent } from '../../shared/empty-state';
import { PlanBuilderComponent, planFormErrors } from './plan-builder';
import { PlanCardComponent } from './plan-card';

/** A re-read waits this long past a start/end, so the server's clock has passed it too (clock skew). */
const BOUNDARY_SLACK_MS = 1000;

// Todo Plans page (US4): active/upcoming and completed groups in the server's order, create/edit builder (also
// `?add=1`), item toggles, delete, empty state, re-read when `now` passes a shown plan's start or end
// (AC-US4-4, AC-US4-8, AC-US4-10..13, research R-4, FA-33).
@Component({
  selector: 'app-plans-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PlanBuilderComponent, PlanCardComponent, EmptyStateComponent],
  styles: `
    .header { display: flex; align-items: center; gap: var(--gap); margin-bottom: var(--gap); }
    .header h1 { margin: 0; flex: 1 1 auto; }
    .group { margin-top: var(--gap); }
    .group h2 { margin: 0 0 8px; font-size: 1.1em; }
    .plan-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--gap); align-items: start; }
    .empty { color: var(--muted); margin: 0; }
  `,
  template: `
    <div class="header">
      <h1 data-testid="page-title">Todo Plans</h1>
      <button type="button" class="primary" data-testid="plan-create" (click)="openCreate()">Create Plan</button>
    </div>

    @if (editing(); as current) {
      <app-plan-builder
        [plan]="current === 'new' ? null : current"
        [errors]="formErrors()"
        (save)="save($event)"
        (cancel)="closeForm()"
      />
    }

    @if (loaded()) {
      @if (active().length === 0 && completed().length === 0) {
        <app-empty-state message="No plans yet" actionLabel="Create Plan" (add)="openCreate()" />
      } @else {
        <section class="group" data-testid="plans-active">
          <h2>Active &amp; upcoming</h2>
          @if (active().length === 0) {
            <p class="empty">No active or upcoming plans</p>
          } @else {
            <div class="plan-grid">
              @for (p of active(); track p.id) {
                <app-plan-card [plan]="p" [highlighted]="watcher.highlighted().has(p.id)" [busy]="isBusy(p.id)"
                  (toggle)="toggleItem(p, $event.item, $event.done)" (edit)="openEdit(p)" (remove)="remove(p)" />
              }
            </div>
          }
        </section>
        <section class="group" data-testid="plans-completed">
          <h2>History</h2>
          @if (completed().length === 0) {
            <p class="empty">No completed plans yet</p>
          } @else {
            <div class="plan-grid">
              @for (p of completed(); track p.id) {
                <app-plan-card [plan]="p" [highlighted]="watcher.highlighted().has(p.id)" [busy]="isBusy(p.id)"
                  (toggle)="toggleItem(p, $event.item, $event.done)" (edit)="openEdit(p)" (remove)="remove(p)" />
              }
            </div>
          }
        </section>
      }
    }
  `,
})
export default class PlansPage {
  private readonly api = inject(PlansService);
  private readonly clock = inject(ClockService);
  private readonly notice = inject(NoticeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly watcher = inject(PlanStartWatcher);

  protected readonly active = signal<Plan[]>([]);
  protected readonly completed = signal<Plan[]>([]);
  protected readonly loaded = signal(false);
  protected readonly editing = signal<Plan | 'new' | null>(null);
  protected readonly formErrors = signal<Record<string, string>>({});
  /** Ids of plans with a call running (FA-33: that card's controls are disabled). */
  private readonly busy = signal<ReadonlySet<number>>(new Set());
  /** When the last list was read; the next re-read is at the first shown start/end after it (R-4). */
  private readonly readAt = signal<number | null>(null);

  /** The earliest start or end of a shown plan still ahead of the last read, or null. */
  private readonly nextBoundary = computed(() => {
    const readAt = this.readAt();
    if (readAt === null) return null;
    let next: number | null = null;
    for (const p of [...this.active(), ...this.completed()]) {
      for (const t of [Date.parse(p.startDateTime), Date.parse(p.endDateTime)]) {
        if (t > readAt && (next === null || t < next)) next = t;
      }
    }
    return next;
  });

  private pending: Subscription | undefined;
  private openedFromQuery = false;

  constructor() {
    this.destroyRef.onDestroy(() => this.pending?.unsubscribe());
    // `?add=1` opens the create form, also when the page is already open.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      if (params.get('add') === '1') {
        this.openedFromQuery = true;
        this.openCreate();
      }
    });
    // Status is the server's (FR-08.4): re-read once `now` passes a shown plan's start or end.
    effect(() => {
      const boundary = this.nextBoundary();
      if (boundary !== null && this.clock.now() >= boundary + BOUNDARY_SLACK_MS) this.reload();
    });
    this.reload();
  }

  protected isBusy(id: number): boolean {
    return this.busy().has(id);
  }

  /** Re-reads both groups; a newer request cancels the pending one. */
  protected reload(): void {
    this.pending?.unsubscribe();
    // No boundary re-read is scheduled while a read runs.
    this.readAt.set(null);
    const startedAt = this.clock.now();
    this.pending = forkJoin([this.api.listPlans('active'), this.api.listPlans('completed')]).subscribe({
      next: ([active, completed]) => {
        this.active.set(active);
        this.completed.set(completed);
        this.loaded.set(true);
        // Measured from the request start, so a boundary passed during the call triggers one more read.
        this.readAt.set(startedAt - BOUNDARY_SLACK_MS);
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

  protected openEdit(plan: Plan): void {
    this.formErrors.set({});
    this.editing.set(plan);
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

  // FA-31: the form stays open with its values after an error; items errors show as `error-items`.
  protected save(body: PlanCreate | PlanUpdate): void {
    const current = this.editing();
    if (current === null) return;
    const request =
      current === 'new' ? this.api.createPlan(body as PlanCreate) : this.api.updatePlan(current.id, body as PlanUpdate);
    request.subscribe({
      next: () => {
        this.notice.success(current === 'new' ? 'Plan created' : 'Plan updated');
        this.closeForm();
        this.reload();
        this.watcher.refresh();
      },
      error: (err: HttpErrorResponse) => {
        const problem = readProblem(err);
        if (err.status === 400) {
          const { onForm, other } = planFormErrors(problem.fieldErrors);
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

  // AC-US4-4: the returned plan replaces the shown one; if its status puts it in the other group, both groups
  // are re-read so the server's order holds. After an error the list is re-read (the checkbox snaps back).
  protected toggleItem(plan: Plan, item: PlanItem, done: boolean): void {
    if (this.isBusy(plan.id)) return;
    this.setBusy(plan.id, true);
    this.api.setPlanItemDone(plan.id, item.id, { done }).subscribe({
      next: (updated) => {
        this.setBusy(plan.id, false);
        this.notice.success(done ? 'Item marked done' : 'Item marked not done');
        const wasActive = this.active().some((p) => p.id === plan.id);
        const isActive = updated.status !== PlanStatus.Completed;
        if (wasActive !== isActive) {
          this.reload();
          return;
        }
        const replace = (list: Plan[]) => list.map((p) => (p.id === updated.id ? updated : p));
        if (isActive) this.active.update(replace);
        else this.completed.update(replace);
      },
      error: (err: HttpErrorResponse) => {
        this.setBusy(plan.id, false);
        this.notice.error(readProblem(err).message);
        this.reload();
      },
    });
  }

  // FA-7: deletes without a confirmation dialog; the list is re-read after success or error.
  protected remove(plan: Plan): void {
    if (this.isBusy(plan.id)) return;
    this.setBusy(plan.id, true);
    this.api.deletePlan(plan.id).subscribe({
      next: () => {
        this.setBusy(plan.id, false);
        const current = this.editing();
        if (current !== null && current !== 'new' && current.id === plan.id) this.closeForm();
        this.notice.success('Plan deleted');
        this.reload();
        this.watcher.refresh();
      },
      error: (err: HttpErrorResponse) => {
        this.setBusy(plan.id, false);
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
