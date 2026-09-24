import { ChangeDetectionStrategy, Component } from '@angular/core';

// Placeholder page; its story phase fills it in later.
@Component({
  selector: 'app-plans-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1 data-testid="page-title">Todo Plans</h1>`,
})
export default class PlansPage {}
