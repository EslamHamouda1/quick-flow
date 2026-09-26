import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { DefaultPage, Settings, SettingsService } from '../../api';
import { ClockService } from '../../core/clock.service';
import { NoticeService } from '../../core/notice.service';
import { readProblem } from '../../core/problem';
import { FieldErrorComponent } from '../../shared/field-error';

/** Nav order and nav labels (FA-41). */
const DEFAULT_PAGE_OPTIONS: { value: DefaultPage; label: string }[] = [
  { value: DefaultPage.Dashboard, label: 'Dashboard' },
  { value: DefaultPage.Tasks, label: 'Tasks' },
  { value: DefaultPage.Habits, label: 'Habits' },
  { value: DefaultPage.Learning, label: 'Learning Resources' },
  { value: DefaultPage.Plans, label: 'Todo Plans' },
  { value: DefaultPage.Settings, label: 'Settings' },
];

/** Fields the form has an input for; a 400 on any other field goes to a notice. */
const SETTINGS_FORM_FIELDS = ['displayName', 'planStartNotifications', 'defaultPage'];

// Settings page (US6, FR-10.2, AC-US6-1, AC-US6-2): profile and preferences from `getSettings`, saved together with
// `updateSettings`; the time zone is the app zone, read-only. No client-side length checks: the server's messages are
// shown (FA-3). The form shows only after a successful read (FA-41).
@Component({
  selector: 'app-settings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldErrorComponent],
  template: `
    <h1 data-testid="page-title">Settings</h1>

    @if (loaded()) {
      <form class="card" data-testid="settings-form" (submit)="submit($event)" novalidate>
        <div class="form-row">
          <label for="settings-display-name">Display name</label>
          <input id="settings-display-name" type="text" data-testid="settings-display-name" [value]="displayName()"
            (input)="displayName.set(value($event))" />
          <app-field-error field="displayName" [errors]="errors()" />
        </div>
        <div class="form-row">
          <label>
            <input type="checkbox" data-testid="settings-notifications" [checked]="notifications()"
              (change)="notifications.set(checked($event))" />
            Plan-start notifications
          </label>
          <app-field-error field="planStartNotifications" [errors]="errors()" />
        </div>
        <div class="form-row">
          <label for="settings-default-page">Default page</label>
          <select id="settings-default-page" data-testid="settings-default-page" (change)="defaultPage.set($any(value($event)))">
            @for (o of defaultPageOptions; track o.value) {
              <option [value]="o.value" [selected]="o.value === defaultPage()">{{ o.label }}</option>
            }
          </select>
          <app-field-error field="defaultPage" [errors]="errors()" />
        </div>
        <div class="form-row">
          <span>Time zone</span>
          <span data-testid="settings-time-zone">{{ clock.timeZone() }}</span>
        </div>
        <div class="form-actions">
          <button type="submit" class="primary" data-testid="settings-save" [disabled]="saving()">Save</button>
        </div>
      </form>
    }
  `,
})
export default class SettingsPage {
  private readonly api = inject(SettingsService);
  private readonly notice = inject(NoticeService);
  protected readonly clock = inject(ClockService);

  protected readonly defaultPageOptions = DEFAULT_PAGE_OPTIONS;

  protected readonly loaded = signal(false);
  protected readonly saving = signal(false);
  protected readonly errors = signal<Record<string, string>>({});
  protected readonly displayName = signal('');
  protected readonly notifications = signal(true);
  protected readonly defaultPage = signal<DefaultPage>(DefaultPage.Dashboard);

  constructor() {
    this.api.getSettings().subscribe({
      next: (s) => {
        this.show(s);
        this.loaded.set(true);
      },
      error: (err: HttpErrorResponse) => this.notice.error(readProblem(err).message),
    });
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement | HTMLSelectElement).value;
  }

  protected checked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  protected submit(event: Event): void {
    event.preventDefault();
    if (this.saving()) return;
    this.saving.set(true);
    // FA-41: all three fields are sent together; an empty name is sent as null.
    this.api
      .updateSettings({
        displayName: this.displayName() === '' ? null : this.displayName(),
        planStartNotifications: this.notifications(),
        defaultPage: this.defaultPage(),
      })
      .subscribe({
        next: (s) => {
          this.saving.set(false);
          this.errors.set({});
          this.show(s);
          this.notice.success('Settings saved');
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          const problem = readProblem(err);
          if (err.status === 400) {
            const onForm: Record<string, string> = {};
            const other: string[] = [];
            for (const [field, message] of Object.entries(problem.fieldErrors)) {
              if (SETTINGS_FORM_FIELDS.includes(field)) onForm[field] = message;
              else other.push(message);
            }
            this.errors.set(onForm);
            if (other.length > 0 || Object.keys(onForm).length === 0) {
              this.notice.error(other.length > 0 ? other.join('; ') : problem.message);
            }
            return;
          }
          this.errors.set({});
          this.notice.error(problem.message);
        },
      });
  }

  /** Shows the server's values (after a save: the trimmed name, a blank one as empty). */
  private show(s: Settings): void {
    this.displayName.set(s.displayName ?? '');
    this.notifications.set(s.planStartNotifications);
    this.defaultPage.set(s.defaultPage);
  }
}
