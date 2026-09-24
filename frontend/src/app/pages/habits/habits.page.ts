import { ChangeDetectionStrategy, Component } from '@angular/core';

// Placeholder page; its story phase fills it in later.
@Component({
  selector: 'app-habits-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1 data-testid="page-title">Habits</h1>`,
})
export default class HabitsPage {}
