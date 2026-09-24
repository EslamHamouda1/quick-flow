import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output } from '@angular/core';

import { LearningCard, LearningCardCreate } from '../../api';
import { FieldErrorComponent } from '../../shared/field-error';

/** Fields the form shows errors for; a 400 on any other field goes to a notice. */
export const CARD_FORM_FIELDS = ['title', 'description', 'status'];

// Create + edit form (AC-US3-1, AC-US3-2). No status input: the page keeps the card's current status on edit (FA-25).
// No client-side length checks: the server's messages are shown (FA-3).
@Component({
  selector: 'app-card-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  template: `
    <form class="card" data-testid="card-form" (submit)="submit($event)" novalidate>
      <h2>{{ isEdit() ? 'Edit Learning Card' : 'Add Learning Card' }}</h2>
      <div class="form-row">
        <label for="card-title">Title</label>
        <input id="card-title" type="text" data-testid="card-title" [value]="title()" (input)="title.set(value($event))" />
        <app-field-error field="title" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="card-description">Description / source</label>
        <textarea id="card-description" data-testid="card-description" [value]="description()" (input)="description.set(value($event))"></textarea>
        <app-field-error field="description" [errors]="errors()" />
      </div>
      <app-field-error field="status" [errors]="errors()" />
      <div class="form-actions">
        <button type="submit" class="primary" data-testid="card-save">Save</button>
        <button type="button" data-testid="card-cancel" (click)="cancel.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class CardFormComponent {
  /** The card being edited; null = create. */
  readonly card = input<LearningCard | null>(null);
  readonly errors = input<Record<string, string>>({});
  readonly save = output<LearningCardCreate>();
  readonly cancel = output<void>();

  protected readonly isEdit = computed(() => this.card() !== null);
  protected readonly title = linkedSignal(() => this.card()?.title ?? '');
  protected readonly description = linkedSignal(() => this.card()?.description ?? '');

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected submit(event: Event): void {
    event.preventDefault();
    // An empty description is sent as null (on edit this clears it).
    this.save.emit({
      title: this.title(),
      description: this.description() === '' ? null : this.description(),
    });
  }
}
