import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NoticeService } from './core/notice.service';
import { PlanStartWatcher } from './core/plan-start-watcher.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly notices = inject(NoticeService);

  // Persistent navigation (FR-10.1, ui-contract Shell).
  protected readonly nav = [
    { id: 'dashboard', path: '/dashboard', label: 'Dashboard' },
    { id: 'tasks', path: '/tasks', label: 'Tasks' },
    { id: 'habits', path: '/habits', label: 'Habits' },
    { id: 'learning', path: '/learning', label: 'Learning Resources' },
    { id: 'plans', path: '/plans', label: 'Todo Plans' },
    { id: 'settings', path: '/settings', label: 'Settings' },
  ];

  constructor() {
    // App-wide plan-start notices, on every page (research R-6, FA-2).
    inject(PlanStartWatcher).start();
  }
}
