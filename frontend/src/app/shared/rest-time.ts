import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ClockService } from '../core/clock.service';
import { formatDuration } from '../core/date-format';

// Rest time left until endDateTime, updated every clock tick (FR-08.2, NFR-2, R-4). The parent decides when to show it.
@Component({
  selector: 'app-rest-time',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="rest-time" data-testid="plan-rest-time" title="Time left" [attr.aria-label]="'Time left ' + text()">{{ text() }}</span>`,
})
export class RestTimeComponent {
  private readonly clock = inject(ClockService);

  readonly endDateTime = input.required<string>();
  protected readonly text = computed(() =>
    formatDuration(Date.parse(this.endDateTime()) - this.clock.now()),
  );
}
