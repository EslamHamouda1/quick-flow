import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output } from '@angular/core';

import { Habit, HabitFrequency, HabitWrite } from '../../api';
import { FieldErrorComponent } from '../../shared/field-error';

export const FREQUENCY_OPTIONS: { value: HabitFrequency; label: string }[] = [
  { value: HabitFrequency.Daily, label: 'Daily' },
  { value: HabitFrequency.Weekly, label: 'Weekly' },
];

/** Fields the form has an input for; a 400 on any other field goes to a notice (FA-23). */
export const HABIT_FORM_FIELDS = ['name', 'description', 'frequency'];

// Create + edit form (AC-US2-1, AC-US2-2, AC-US2-6). No client-side length checks: the server's messages are shown (FA-3).
@Component({
  selector: 'app-habit-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  template: `
    <form class="card" data-testid="habit-form" (submit)="submit($event)" novalidate>
      <h2>{{ isEdit() ? 'Edit Habit' : 'Add Habit' }}</h2>
      <div class="form-row">
        <label for="habit-name">Name</label>
        <input id="habit-name" type="text" data-testid="habit-name" [value]="name()" (input)="name.set(value($event))" />
        <app-field-error field="name" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="habit-description">Description</label>
        <textarea id="habit-description" data-testid="habit-description" [value]="description()" (input)="description.set(value($event))"></textarea>
        <app-field-error field="description" [errors]="errors()" />
      </div>
      <div class="form-row">
        <label for="habit-frequency">Frequency</label>
        <select id="habit-frequency" data-testid="habit-frequency" (change)="frequency.set($any(value($event)))">
          @for (o of frequencyOptions; track o.value) {
            <option [value]="o.value" [selected]="o.value === frequency()">{{ o.label }}</option>
          }
        </select>
        <app-field-error field="frequency" [errors]="errors()" />
      </div>
      <div class="form-actions">
        <button type="submit" class="primary" data-testid="habit-save">Save</button>
        <button type="button" data-testid="habit-cancel" (click)="cancel.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class HabitFormComponent {
  /** The habit being edited; null = create. */
  readonly habit = input<Habit | null>(null);
  readonly errors = input<Record<string, string>>({});
  readonly save = output<HabitWrite>();
  readonly cancel = output<void>();

  protected readonly frequencyOptions = FREQUENCY_OPTIONS;

  protected readonly isEdit = computed(() => this.habit() !== null);
  protected readonly name = linkedSignal(() => this.habit()?.name ?? '');
  protected readonly description = linkedSignal(() => this.habit()?.description ?? '');
  // FA-20: the create form starts with Daily.
  protected readonly frequency = linkedSignal<HabitFrequency>(() => this.habit()?.frequency ?? HabitFrequency.Daily);

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
  }

  protected submit(event: Event): void {
    event.preventDefault();
    // FA-20: an empty description is sent as null (on edit this clears it).
    this.save.emit({
      name: this.name(),
      description: this.description() === '' ? null : this.description(),
      frequency: this.frequency(),
    });
  }
}
