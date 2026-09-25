import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { LearningCard, LearningCardCreate, LearningService } from '../../api';
import { NoticeService } from '../../core/notice.service';
import { readProblem } from '../../core/problem';
import { EmptyStateComponent } from '../../shared/empty-state';
import { CARD_FORM_FIELDS, CardFormComponent } from './card-form';
import { LearningCardComponent, SubFormErrors } from './learning-card';

const NO_ERRORS: Record<string, string> = {};

// Learning Resources page (US3): card grid in the server's order, create/edit form (also `?add=1`), card delete,
// re-read after every change, empty state (AC-US3-1, AC-US3-7, AC-US3-9).
@Component({
  selector: 'app-learning-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CardFormComponent, LearningCardComponent, EmptyStateComponent],
  styles: `
    .card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--gap); align-items: start; }
    .header { display: flex; align-items: center; gap: var(--gap); margin-bottom: var(--gap); }
    .header h1 { margin: 0; flex: 1 1 auto; }
  `,
  template: `
    <div class="header">
      <h1 data-testid="page-title">Learning Resources</h1>
      <button type="button" class="primary" data-testid="card-add" (click)="openCreate()">Add Learning Card</button>
    </div>

    @if (editing(); as current) {
      <app-card-form
        [card]="current === 'new' ? null : current"
        [errors]="formErrors()"
        (save)="save($event)"
        (cancel)="closeForm()"
      />
    }

    @if (loaded()) {
      @if (cards().length === 0) {
        <app-empty-state message="No learning cards yet" actionLabel="Add Learning Card" (add)="openCreate()" />
      } @else {
        <div class="card-grid" data-testid="card-grid">
          @for (c of cards(); track c.id) {
            <app-learning-card
              [card]="c"
              [expanded]="expanded().has(c.id)"
              [busy]="isBusy(c.id)"
              [milestoneErrors]="subErrorsFor(c.id, 'milestone')"
              [noteErrors]="subErrorsFor(c.id, 'note')"
              (changed)="reload()"
              (toggle)="toggle(c.id)"
              (edit)="openEdit(c)"
              (remove)="remove(c)"
              (subFormErrors)="setSubErrors(c.id, $event)"
            />
          }
        </div>
      }
    }
  `,
})
export default class LearningPage {
  private readonly api = inject(LearningService);
  private readonly notice = inject(NoticeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly cards = signal<LearningCard[]>([]);
  protected readonly loaded = signal(false);
  protected readonly editing = signal<LearningCard | 'new' | null>(null);
  protected readonly formErrors = signal<Record<string, string>>({});
  /** Open cards; kept across re-reads (a new card starts collapsed, FA-26). */
  protected readonly expanded = signal<ReadonlySet<number>>(new Set());
  /** The one sub-form (of one card) that shows errors; submitting any form clears the others (FA-28). */
  private readonly subErrors = signal<({ cardId: number } & SubFormErrors) | null>(null);
  /** Ids of cards with a page-level call (delete) running. */
  private readonly busy = signal<ReadonlySet<number>>(new Set());

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
    this.reload();
  }

  protected isBusy(id: number): boolean {
    return this.busy().has(id);
  }

  protected subErrorsFor(cardId: number, form: SubFormErrors['form']): Record<string, string> {
    const s = this.subErrors();
    return s && s.cardId === cardId && s.form === form ? s.errors : NO_ERRORS;
  }

  protected setSubErrors(cardId: number, e: SubFormErrors): void {
    this.formErrors.set({});
    this.subErrors.set(Object.keys(e.errors).length === 0 ? null : { cardId, ...e });
  }

  /** Re-reads all cards (with milestones and notes); a newer request cancels the pending one. */
  protected reload(): void {
    this.pending?.unsubscribe();
    this.pending = this.api.listLearningCards().subscribe({
      next: (list) => {
        this.cards.set(list);
        this.loaded.set(true);
        // Drop ids of cards that are gone.
        const ids = new Set(list.map((c) => c.id));
        this.expanded.update((s) => new Set([...s].filter((id) => ids.has(id))));
      },
      error: (err: HttpErrorResponse) => {
        this.loaded.set(true);
        this.notice.error(readProblem(err).message);
      },
    });
  }

  protected toggle(id: number): void {
    this.expanded.update((s) => {
      const next = new Set(s);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  protected openCreate(): void {
    this.formErrors.set({});
    this.editing.set('new');
  }

  protected openEdit(card: LearningCard): void {
    this.formErrors.set({});
    this.editing.set(card);
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

  // FA-25: edit keeps the card's current status; status is changed only by the card's select.
  protected save(body: LearningCardCreate): void {
    const current = this.editing();
    if (current === null) return;
    this.subErrors.set(null);
    const request =
      current === 'new'
        ? this.api.createLearningCard(body)
        : this.api.updateLearningCard(current.id, { ...body, status: current.status });
    request.subscribe({
      next: () => {
        this.notice.success(current === 'new' ? 'Learning card created' : 'Learning card updated');
        this.closeForm();
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        const problem = readProblem(err);
        if (err.status === 400) {
          const onForm: Record<string, string> = {};
          const other: string[] = [];
          for (const [field, message] of Object.entries(problem.fieldErrors)) {
            if (CARD_FORM_FIELDS.includes(field)) onForm[field] = message;
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

  // FA-7: deletes without a confirmation dialog; the list is re-read after success or error.
  protected remove(card: LearningCard): void {
    if (this.isBusy(card.id)) return;
    this.setBusy(card.id, true);
    this.api.deleteLearningCard(card.id).subscribe({
      next: () => {
        this.setBusy(card.id, false);
        const current = this.editing();
        if (current !== null && current !== 'new' && current.id === card.id) this.closeForm();
        this.notice.success('Learning card deleted');
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.setBusy(card.id, false);
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
