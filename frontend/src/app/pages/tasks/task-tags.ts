import { ChangeDetectionStrategy, Component, input, linkedSignal, output } from '@angular/core';

import { Task } from '../../api';
import { FieldErrorComponent } from '../../shared/field-error';

// Tags of one task row (US1): chips with remove buttons, an add input and the server's `tags` message.
@Component({
  selector: 'app-task-tags',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  styles: `
    :host { display: flex; flex-direction: column; gap: 4px; flex: 1 1 100%; }
    .tags { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
    .tag { display: inline-flex; align-items: center; gap: 2px; }
    .tag button { padding: 0 4px; line-height: 1; }
    .add { display: flex; gap: 6px; align-items: center; }
  `,
  template: `
    <span class="tags" data-testid="task-row-tags">
      @for (tag of task().tags; track tag) {
        <span class="badge badge-muted tag" data-testid="task-row-tag">{{ tag }}<button type="button"
            data-testid="task-tag-remove" [attr.aria-label]="'Remove tag ' + tag" (click)="remove.emit(tag)">×</button></span>
      }
    </span>
    <span class="add">
      <input type="text" [attr.data-testid]="'task-tag-input-' + task().id" placeholder="Add tags, comma-separated"
        [value]="draft()" (input)="draft.set(value($event))" (keydown.enter)="submit()" />
      <button type="button" [attr.data-testid]="'task-tag-add-' + task().id" (click)="submit()">Add tag</button>
    </span>
    <app-field-error field="tags" [errors]="errors()" />
  `,
})
export class TaskTagsComponent {
  readonly task = input.required<Task>();
  readonly errors = input<Record<string, string>>({});
  readonly add = output<string[]>();
  readonly remove = output<string>();

  // Research R-5: reset whenever the page passes a new task object (after a re-read), kept after a 400.
  protected readonly draft = linkedSignal(() => {
    this.task();
    return '';
  });

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  // Research R-3: parts sent as typed; the server trims and validates (an empty input sends [""]).
  protected submit(): void {
    this.add.emit(this.draft().split(','));
  }
}
