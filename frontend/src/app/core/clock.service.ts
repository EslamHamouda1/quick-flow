import { Injectable, Signal, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { InfoService } from '../api';
import { NoticeService } from './notice.service';

/** Server-aligned clock in the app time zone (loaded once at startup). */
@Injectable({ providedIn: 'root' })
export class ClockService {
  private readonly info = inject(InfoService);
  private readonly notice = inject(NoticeService);

  private readonly nowSig = signal<number>(Date.now());
  private readonly zoneSig = signal<string>(Intl.DateTimeFormat().resolvedOptions().timeZone);
  private offset = 0;
  private timer: ReturnType<typeof setInterval> | undefined;

  /** Current epoch ms, server-aligned, ticking every second. */
  readonly now: Signal<number> = this.nowSig.asReadonly();
  /** IANA app time zone. */
  readonly timeZone: Signal<string> = this.zoneSig.asReadonly();

  /** Server time minus browser time, in ms. */
  get offsetMs(): number {
    return this.offset;
  }

  async load(): Promise<void> {
    try {
      const info = await firstValueFrom(this.info.getAppInfo());
      const offset = Date.parse(info.now) - Date.now();
      this.offset = Number.isFinite(offset) ? offset : 0;
      this.zoneSig.set(info.timeZone);
    } catch {
      // FA-12: fall back to the browser zone and no offset.
      this.offset = 0;
      this.zoneSig.set(Intl.DateTimeFormat().resolvedOptions().timeZone);
      this.notice.error('Could not load app time zone from the server');
    }
    this.tick();
    if (this.timer === undefined) {
      this.timer = setInterval(() => this.tick(), 1000);
    }
  }

  /** App-zone calendar date of now() as YYYY-MM-DD. */
  today(): string {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: this.zoneSig(),
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(this.nowSig());
    const part = (type: Intl.DateTimeFormatPartTypes) =>
      parts.find((p) => p.type === type)?.value ?? '';
    return `${part('year')}-${part('month')}-${part('day')}`;
  }

  private tick(): void {
    this.nowSig.set(Date.now() + this.offset);
  }
}
