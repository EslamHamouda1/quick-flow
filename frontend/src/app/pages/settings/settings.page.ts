import { ChangeDetectionStrategy, Component } from '@angular/core';

// Placeholder page; its story phase fills it in later.
@Component({
  selector: 'app-settings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1 data-testid="page-title">Settings</h1>`,
})
export default class SettingsPage {}
