import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { DefaultPage, SettingsService } from '../api';
import { NoticeService } from './notice.service';

const DEFAULT_PAGE_ROUTES: Record<DefaultPage, string> = {
  [DefaultPage.Dashboard]: '/dashboard',
  [DefaultPage.Tasks]: '/tasks',
  [DefaultPage.Habits]: '/habits',
  [DefaultPage.Learning]: '/learning',
  [DefaultPage.Plans]: '/plans',
  [DefaultPage.Settings]: '/settings',
};

// Route '' (research R-8, FA-38): reads the settings on every visit and redirects to the default page (FR-10.2).
// Deep links never pass through here (FA-5). A failed read or an unknown value opens the Dashboard (FA-39).
export const defaultPageGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const settingsApi = inject(SettingsService);
  const notice = inject(NoticeService);
  try {
    const settings = await firstValueFrom(settingsApi.getSettings());
    return router.parseUrl(DEFAULT_PAGE_ROUTES[settings.defaultPage] ?? '/dashboard');
  } catch {
    notice.error('Could not load settings');
    return router.parseUrl('/dashboard');
  }
};
