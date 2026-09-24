import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';

import { Task, TaskCreate, TaskPriority, TaskStatus, TaskUpdate, TasksService } from '../../api';
import { formatDate } from '../../core/date-format';
import { NoticeService } from '../../core/notice.service';
import { readProblem } from '../../core/problem';
import { EmptyStateComponent } from '../../shared/empty-state';
import { PRIORITY_OPTIONS, STATUS_OPTIONS, TASK_FORM_FIELDS, TaskFormComponent } from './task-form';

type SortField = 'createdAt' | 'dueDate';
type SortDirection = 'asc' | 'desc';

interface TaskFilters {
  q: string;
  status: TaskStatus | '';
  priority: TaskPriority | '';
  dueFrom: string;
  dueTo: string;
  archived: boolean;
  sort: SortField;
  direction: SortDirection;
}

// Contract defaults (data-model "Tasks page", FA-14).
const DEFAULT_FILTERS: TaskFilters = {
  q: '',
  status: '',
  priority: '',
  dueFrom: '',
  dueTo: '',
  archived: false,
  sort: 'createdAt',
  direction: 'desc',
};

const STATUS_LABELS = Object.fromEntries(STATUS_OPTIONS.map((o) => [o.value, o.label])) as Record<TaskStatus, string>;
const PRIORITY_LABELS = Object.fromEntries(PRIORITY_OPTIONS.map((o) => [o.value, o.label])) as Record<TaskPriority, string>;

// Tasks page (US1): list with search/filter/sort, overdue view, create/edit form and row actions.
@Component({
  selector: 'app-tasks-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TaskFormComponent, EmptyStateComponent],
  styles: `
    .toolbar { display: flex; flex-wrap: wrap; gap: 8px; align-items: flex-end; margin-bottom: var(--gap); }
    .toolbar label { display: flex; flex-direction: column; gap: 2px; font-size: 0.85em; color: var(--muted); }
    .toolbar label.inline { flex-direction: row; align-items: center; gap: 6px; }
    .task-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
    .task-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
    .task-row .title { flex: 1 1 200px; font-weight: 600; }
    .task-row .due { color: var(--muted); font-size: 0.9em; }
    .task-row .actions { display: flex; gap: 6px; }
    .header { display: flex; align-items: center; gap: var(--gap); margin-bottom: var(--gap); }
    .header h1 { margin: 0; flex: 1 1 auto; }
  `,
  template: `
    <div class="header">
      <h1 data-testid="page-title">Tasks</h1>
      <button type="button" class="primary" data-testid="task-add" (click)="openCreate()">Add Task</button>
    </div>

    @if (editing(); as current) {
      <app-task-form
        [task]="current === 'new' ? null : current"
        [errors]="formErrors()"
        (save)="save($event)"
        (cancel)="closeForm()"
      />
    }

    <div class="toolbar">
      <label>Search
        <input type="search" data-testid="task-search" placeholder="Search titles" [disabled]="overdueView()"
          [value]="filters().q" (input)="setFilter('q', value($event))" />
      </label>
      <label>Status
        <select data-testid="filter-status" [disabled]="overdueView()" (change)="setFilter('status', $any(value($event)))">
          <option value="" [selected]="filters().status === ''">Any</option>
          @for (o of statusOptions; track o.value) {
            <option [value]="o.value" [selected]="filters().status === o.value">{{ o.label }}</option>
          }
        </select>
      </label>
      <label>Priority
        <select data-testid="filter-priority" [disabled]="overdueView()" (change)="setFilter('priority', $any(value($event)))">
          <option value="" [selected]="filters().priority === ''">Any</option>
          @for (o of priorityOptions; track o.value) {
            <option [value]="o.value" [selected]="filters().priority === o.value">{{ o.label }}</option>
          }
        </select>
      </label>
      <label>Due from
        <input type="date" data-testid="filter-due-from" [disabled]="overdueView()"
          [value]="filters().dueFrom" (input)="setFilter('dueFrom', value($event))" />
      </label>
      <label>Due to
        <input type="date" data-testid="filter-due-to" [disabled]="overdueView()"
          [value]="filters().dueTo" (input)="setFilter('dueTo', value($event))" />
      </label>
      <label class="inline">
        <input type="checkbox" data-testid="filter-archived" [disabled]="overdueView()"
          [checked]="filters().archived" (change)="setFilter('archived', checked($event))" />
        Archived
      </label>
      <label>Sort by
        <select data-testid="sort-field" [disabled]="overdueView()" (change)="setFilter('sort', $any(value($event)))">
          <option value="createdAt" [selected]="filters().sort === 'createdAt'">Creation date</option>
          <option value="dueDate" [selected]="filters().sort === 'dueDate'">Due date</option>
        </select>
      </label>
      <label>Direction
        <select data-testid="sort-direction" [disabled]="overdueView()" (change)="setFilter('direction', $any(value($event)))">
          <option value="asc" [selected]="filters().direction === 'asc'">Ascending</option>
          <option value="desc" [selected]="filters().direction === 'desc'">Descending</option>
        </select>
      </label>
      <button type="button" data-testid="task-view-overdue" [attr.aria-pressed]="overdueView()"
        [class.primary]="overdueView()" (click)="toggleOverdue()">Overdue</button>
    </div>

    @if (loaded()) {
      @if (tasks().length === 0) {
        <app-empty-state [message]="emptyMessage()" actionLabel="Add Task" (add)="openCreate()" />
      } @else {
        <ul class="task-list" data-testid="task-list">
          @for (t of tasks(); track t.id) {
            <li class="card task-row" [attr.data-testid]="'task-row-' + t.id">
              <span class="title" data-testid="task-row-title">{{ t.title }}</span>
              <span class="badge" data-testid="task-row-status">{{ statusLabel(t.status) }}</span>
              <span class="badge badge-muted" data-testid="task-row-priority">{{ priorityLabel(t.priority) }}</span>
              <span class="due" data-testid="task-row-due">{{ t.dueDate ? formatDate(t.dueDate) : 'No due date' }}</span>
              @if (t.overdue) {
                <span class="badge badge-danger" data-testid="task-row-overdue">Overdue</span>
              }
              <span class="actions">
                <button type="button" [attr.data-testid]="'task-edit-' + t.id" (click)="openEdit(t)">Edit</button>
                @if (t.status !== 'DONE' && !t.archived) {
                  <button type="button" [attr.data-testid]="'task-complete-' + t.id" (click)="complete(t)">Complete</button>
                }
                @if (t.archived) {
                  <button type="button" [attr.data-testid]="'task-restore-' + t.id" (click)="restore(t)">Restore</button>
                } @else {
                  <button type="button" [attr.data-testid]="'task-archive-' + t.id" (click)="archive(t)">Archive</button>
                }
                <button type="button" class="danger" [attr.data-testid]="'task-delete-' + t.id" (click)="remove(t)">Delete</button>
              </span>
            </li>
          }
        </ul>
      }
    }
  `,
})
export default class TasksPage {
  private readonly api = inject(TasksService);
  private readonly notice = inject(NoticeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly priorityOptions = PRIORITY_OPTIONS;
  protected readonly formatDate = formatDate;

  protected readonly filters = signal<TaskFilters>({ ...DEFAULT_FILTERS });
  protected readonly overdueView = signal(false);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly loaded = signal(false);
  protected readonly editing = signal<Task | 'new' | null>(null);
  protected readonly formErrors = signal<Record<string, string>>({});

  // FA-18: "No tasks yet" only for the unfiltered default list.
  protected readonly emptyMessage = computed(() => {
    const f = this.filters();
    const filtered =
      this.overdueView() || f.q !== '' || f.status !== '' || f.priority !== '' || f.dueFrom !== '' || f.dueTo !== '' || f.archived;
    return filtered ? 'No tasks match' : 'No tasks yet';
  });

  private pending: Subscription | undefined;
  private openedFromQuery = false;

  constructor() {
    this.destroyRef.onDestroy(() => this.pending?.unsubscribe());
    // `?add=1` opens the create form, also when the page is already open (FA-17).
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      if (params.get('add') === '1') {
        this.openedFromQuery = true;
        this.openCreate();
      }
    });
    this.reload();
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLSelectElement).value;
  }

  protected checked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected statusLabel(s: TaskStatus): string {
    return STATUS_LABELS[s] ?? s;
  }

  protected priorityLabel(p: TaskPriority): string {
    return PRIORITY_LABELS[p] ?? p;
  }

  protected setFilter<K extends keyof TaskFilters>(key: K, value: TaskFilters[K]): void {
    this.filters.update((f) => ({ ...f, [key]: value }));
    this.reload();
  }

  protected toggleOverdue(): void {
    this.overdueView.update((v) => !v);
    this.reload();
  }

  /** Re-reads the shown view; a newer request cancels the pending one (FA-14). */
  private reload(): void {
    this.pending?.unsubscribe();
    const f = this.filters();
    const request: Observable<Task[]> = this.overdueView()
      ? this.api.listOverdueTasks()
      : this.api.listTasks(
          f.q || undefined,
          f.status || undefined,
          f.priority || undefined,
          f.dueFrom || undefined,
          f.dueTo || undefined,
          f.archived,
          f.sort,
          f.direction,
        );
    this.pending = request.subscribe({
      next: (list) => {
        this.tasks.set(list);
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

  protected openEdit(task: Task): void {
    this.formErrors.set({});
    this.editing.set(task);
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

  protected save(body: TaskCreate | TaskUpdate): void {
    const current = this.editing();
    if (current === null) return;
    const request = current === 'new' ? this.api.createTask(body as TaskCreate) : this.api.updateTask(current.id, body as TaskUpdate);
    request.subscribe({
      next: () => {
        this.notice.success(current === 'new' ? 'Task created' : 'Task updated');
        this.closeForm();
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        const problem = readProblem(err);
        if (err.status === 400) {
          const onForm: Record<string, string> = {};
          const other: string[] = [];
          for (const [field, message] of Object.entries(problem.fieldErrors)) {
            if (TASK_FORM_FIELDS.includes(field)) onForm[field] = message;
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

  protected complete(task: Task): void {
    this.act(this.api.completeTask(task.id), 'Task completed');
  }

  protected archive(task: Task): void {
    this.act(this.api.archiveTask(task.id), 'Task archived');
  }

  protected restore(task: Task): void {
    this.act(this.api.restoreTask(task.id), 'Task restored');
  }

  // FA-7: deletes without a confirmation dialog.
  protected remove(task: Task): void {
    this.act(this.api.deleteTask(task.id), 'Task deleted');
  }

  private act(request: Observable<unknown>, success: string): void {
    request.subscribe({
      next: () => {
        this.notice.success(success);
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.notice.error(readProblem(err).message);
        this.reload();
      },
    });
  }
}
