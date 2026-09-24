import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="card" data-testid="empty-state"><p>{{ message() }}</p><button type="button" class="primary" data-testid="empty-state-action" (click)="add.emit()">{{ actionLabel() }}</button></div>`,
})
export class EmptyStateComponent {
  readonly message = input.required<string>();
  readonly actionLabel = input.required<string>();
  readonly add = output<void>();
}
