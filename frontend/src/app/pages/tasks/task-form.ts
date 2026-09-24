import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output } from '@angular/core';

import { Task, TaskCreate, TaskPriority, TaskStatus, TaskUpdate } from '../../api';
import { FieldErrorComponent } from '../../shared/field-error';

export const STATUS_OPTIONS: { value: TaskStatus; label: string }[] = [
  { value: TaskStatus.Todo, label: 'Todo' },
  { value: TaskStatus.InProgress, label: 'In Progress' },
  { value: TaskStatus.Done, label: 'Done' },
];

export const PRIORITY_OPTIONS: { value: TaskPriority; label: string }[] = [
  { value: TaskPriority.Low, label: 'Low' },
  { value: TaskPriority.Medium, label: 'Medium' },
  { value: TaskPriority.High, label: 'High' },
];

/** Fields the form has an input for; a 400 on any other field goes to a notice (FA-17). */
export const TASK_FORM_FIELDS = ['title', 'description', 'status', 'priority', 'dueDate'];

// Create + edit form (AC-US1-1..5). No client-side length checks: the server's messages are shown (FA-3).
@Component({
  selector: 'app-task-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  template: `
    <form class="card" data-testid="task-form" (submit)="submit($event)" novalidate>
      <h2>{{ isEdit() ? 'Edit Task' : 'Add Task' }}</h2>
      <div class="form-row">
        <label for="task-title">Title</label>
        <input id="task-title" type="text" data-testid="task-title" [value]="title()" (input)="title.set(value($event))" />
        <app-field-error field="title" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="task-description">Description</label>
        <textarea id="task-description" data-testid="task-description" [value]="description()" (input)="description.set(value($event))"></textarea>
        <app-field-error field="description" [errors]="errors()" />
      </div>
      @if (isEdit()) {
        <div class="form-row">
          <label for="task-status">Status</label>
          <select id="task-status" data-testid="task-status" (change)="status.set($any(value($event)))">
            @for (o of statusOptions; track o.value) {
              <option [value]="o.value" [selected]="o.value === status()">{{ o.label }}</option>
            }
          </select>
          <app-field-error field="status" [errors]="errors()" />
        </div>
      }
      <div class="form-row">
        <label for="task-priority">Priority</label>
        <select id="task-priority" data-testid="task-priority" (change)="priority.set($any(value($event)))">
          @for (o of priorityOptions; track o.value) {
            <option [value]="o.value" [selected]="o.value === priority()">{{ o.label }}</option>
          }
        </select>
        <app-field-error field="priority" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="task-due-date">Due date</label>
        <input id="task-due-date" type="date" data-testid="task-due-date" [value]="dueDate()" (input)="dueDate.set(value($event))" />
        <app-field-error field="dueDate" [errors]="errors()" />
      </div>
      <div class="form-actions">
        <button type="submit" class="primary" data-testid="task-save">Save</button>
        <button type="button" data-testid="task-cancel" (click)="cancel.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class TaskFormComponent {
  /** The task being edited; null = create. */
  readonly task = input<Task | null>(null);
  readonly errors = input<Record<string, string>>({});
  readonly save = output<TaskCreate | TaskUpdate>();
  readonly cancel = output<void>();

  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly priorityOptions = PRIORITY_OPTIONS;

  protected readonly isEdit = computed(() => this.task() !== null);
  protected readonly title = linkedSignal(() => this.task()?.title ?? '');
  protected readonly description = linkedSignal(() => this.task()?.description ?? '');
  protected readonly status = linkedSignal<TaskStatus>(() => this.task()?.status ?? TaskStatus.Todo);
  protected readonly priority = linkedSignal<TaskPriority>(() => this.task()?.priority ?? TaskPriority.Medium);
  protected readonly dueDate = linkedSignal(() => this.task()?.dueDate ?? '');

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
  }

  protected submit(event: Event): void {
    event.preventDefault();
    // FA-13: empty optional fields are sent as null; create has no status (server default Todo).
    const base = {
      title: this.title(),
      description: this.description() === '' ? null : this.description(),
      priority: this.priority(),
      dueDate: this.dueDate() === '' ? null : this.dueDate(),
    };
    this.save.emit(this.isEdit() ? ({ ...base, status: this.status() } as TaskUpdate) : (base as TaskCreate));
  }
}
