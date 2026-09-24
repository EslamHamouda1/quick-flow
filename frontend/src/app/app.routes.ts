import { Routes } from '@angular/router';

export const routes: Routes = [
  // '' is replaced by the default-page guard in US6 (research R-8).
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.page') },
  { path: 'tasks', loadComponent: () => import('./pages/tasks/tasks.page') },
  { path: 'habits', loadComponent: () => import('./pages/habits/habits.page') },
  { path: 'learning', loadComponent: () => import('./pages/learning/learning.page') },
  { path: 'plans', loadComponent: () => import('./pages/plans/plans.page') },
  { path: 'settings', loadComponent: () => import('./pages/settings/settings.page') },
  // Absolute '/' (the '' route): a relative redirect disables further redirects, so '' → dashboard would not apply.
  { path: '**', redirectTo: '/' },
];
