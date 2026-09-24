import { Injectable, signal } from '@angular/core';

export type NoticeKind = 'success' | 'error' | 'info';

export interface Notice {
  id: number;
  kind: NoticeKind;
  text: string;
}

/** App-wide transient notices; each auto-dismisses after 5 s. */
@Injectable({ providedIn: 'root' })
export class NoticeService {
  private static readonly AUTO_DISMISS_MS = 5000;

  private readonly _notices = signal<Notice[]>([]);
  private nextId = 1;

  /** Read-only view of the current notices. */
  readonly notices = this._notices.asReadonly();

  success(text: string): void {
    this.push('success', text);
  }

  error(text: string): void {
    this.push('error', text);
  }

  info(text: string): void {
    this.push('info', text);
  }

  dismiss(id: number): void {
    this._notices.update((list) => list.filter((n) => n.id !== id));
  }

  private push(kind: NoticeKind, text: string): void {
    const id = this.nextId++;
    this._notices.update((list) => [...list, { id, kind, text }]);
    setTimeout(() => this.dismiss(id), NoticeService.AUTO_DISMISS_MS);
  }
}
