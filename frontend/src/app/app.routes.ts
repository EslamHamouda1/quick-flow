import { Routes } from '@angular/router';

import { defaultPageGuard } from './core/default-page.guard';

export const routes: Routes = [
  // '' opens the default page from the settings (research R-8); `children: []` only makes it a valid route,
  // the guard always redirects.
  { path: '', pathMatch: 'full', canActivate: [defaultPageGuard], children: [] },
  { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.page') },
  { path: 'tasks', loadComponent: () => import('./pages/tasks/tasks.page') },
  { path: 'habits', loadComponent: () => import('./pages/habits/habits.page') },
  { path: 'learning', loadComponent: () => import('./pages/learning/learning.page') },
  { path: 'plans', loadComponent: () => import('./pages/plans/plans.page') },
  { path: 'settings', loadComponent: () => import('./pages/settings/settings.page') },
  // Absolute '/' (the '' route): a relative redirect disables further redirects, so '' → dashboard would not apply.
  { path: '**', redirectTo: '/' },
];
