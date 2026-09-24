import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-field-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (message(); as msg) {
    <div class="field-error" [attr.data-testid]="'error-' + field()">{{ msg }}</div>
  }`,
})
export class FieldErrorComponent {
  readonly field = input.required<string>();
  readonly errors = input<Record<string, string>>({});
  protected readonly message = computed(() => this.errors()[this.field()]);
}
