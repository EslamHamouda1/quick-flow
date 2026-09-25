import { Injectable, Signal, effect, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';

import { Plan, PlanStatus, PlansService } from '../api';
import { ClockService } from './clock.service';
import { NoticeService } from './notice.service';

const STORAGE_KEY = 'quickflow.notifiedPlanIds';

// App-wide plan-start notice (FR-08.1, AC-US4-9, A-10, research R-6, FA-2, FA-32): when `now` reaches a NOT_STARTED
// plan's start, shows "Plan “<title>” has started" once per plan and highlights it until the app is reloaded.
@Injectable({ providedIn: 'root' })
export class PlanStartWatcher {
  private readonly api = inject(PlansService);
  private readonly clock = inject(ClockService);
  private readonly notice = inject(NoticeService);

  private readonly plans = signal<Plan[]>([]);
  private readonly highlightedSig = signal<ReadonlySet<number>>(new Set());
  /** Ids of plans notified in this app session (their cards show `plan-started-<id>`). */
  readonly highlighted: Signal<ReadonlySet<number>> = this.highlightedSig.asReadonly();

  private started = false;
  /** No load has succeeded yet: IN_PROGRESS plans not yet notified are notified once (A-10). */
  private firstLoad = true;
  private pending: Subscription | undefined;
  /** Notified ids kept in memory too, so a plan is notified once per session even when storage can't be written. */
  private readonly sessionIds = new Set<number>();

  constructor() {
    effect(() => {
      const now = this.clock.now();
      let reached = false;
      for (const p of this.plans()) {
        if (p.status !== PlanStatus.NotStarted || Date.parse(p.startDateTime) > now) continue;
        if (this.isStored(p.id)) continue;
        this.notify(p);
        reached = true;
      }
      // Re-read at a reached start so the watcher and the page agree on the status.
      if (reached) this.refresh();
    });
  }

  /** Starts watching; called once by the shell. */
  start(): void {
    if (this.started) return;
    this.started = true;
    this.refresh();
  }

  /** Re-reads the active plans (the page calls it after create/edit/delete). Load errors are silent. */
  refresh(): void {
    if (!this.started) return;
    this.pending?.unsubscribe();
    this.pending = this.api.listPlans('active').subscribe({
      next: (list) => {
        const first = this.firstLoad;
        this.firstLoad = false;
        for (const p of list) {
          if (p.status !== PlanStatus.InProgress || this.isStored(p.id)) continue;
          // On app open: notify once. Later (the user just created/edited it with a past start): store silently.
          if (first) this.notify(p);
          else this.store(p.id);
        }
        this.plans.set(list);
      },
      error: () => undefined,
    });
  }

  private notify(plan: Plan): void {
    // Stored first, so a later tick or load never notifies it again (at most once per plan).
    this.store(plan.id);
    void this.notificationsEnabled().then((on) => {
      if (!on) return;
      this.highlightedSig.update((s) => new Set(s).add(plan.id));
      this.notice.info(`Plan “${plan.title}” has started`);
    });
  }

  /**
   * FA-30: `/api/settings` does not exist before US6, so the FR-10.2 default (on) applies.
   * Phase-08 replaces this with `getSettings().planStartNotifications`, read right before each notice.
   */
  private notificationsEnabled(): Promise<boolean> {
    return Promise.resolve(true);
  }

  private isStored(id: number): boolean {
    return this.readStored().includes(id);
  }

  private store(id: number): void {
    const ids = this.readStored();
    if (ids.includes(id)) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify([...ids, id]));
    } catch {
      // Storage unavailable (private mode): nothing is remembered across reloads.
    }
    this.sessionIds.add(id);
  }

  /** Ids stored now or earlier; unreadable storage counts as empty, plus the ids stored in this session. */
  private readStored(): number[] {
    let ids: number[] = [];
    try {
      const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
      if (Array.isArray(parsed)) ids = parsed.filter((v): v is number => typeof v === 'number');
    } catch {
      ids = [];
    }
    for (const id of this.sessionIds) if (!ids.includes(id)) ids.push(id);
    return ids;
  }
}
