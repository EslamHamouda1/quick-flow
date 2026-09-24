/** Date/time display and conversion helpers built only on the Intl API. */

const LOCAL_RE = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/;

function pad(n: number, width = 2): string {
  return String(n).padStart(width, '0');
}

/** Offset of `zone` at `epochMs`, in minutes east of UTC. */
function offsetMinutes(epochMs: number, zone: string): number {
  const name =
    new Intl.DateTimeFormat('en-US', { timeZone: zone, timeZoneName: 'longOffset' })
      .formatToParts(new Date(epochMs))
      .find((p) => p.type === 'timeZoneName')?.value ?? 'GMT';
  const m = /^GMT(?:([+-])(\d{1,2})(?::?(\d{2}))?)?$/.exec(name);
  if (!m || !m[1]) return 0;
  const minutes = Number(m[2]) * 60 + Number(m[3] ?? 0);
  return m[1] === '-' ? -minutes : minutes;
}

function formatOffset(minutes: number): string {
  const sign = minutes < 0 ? '-' : '+';
  const abs = Math.abs(minutes);
  return `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`;
}

/** ISO calendar date `YYYY-MM-DD` as a readable date; never shifts by time zone. */
export function formatDate(isoDate: string): string {
  if (!isoDate) return '';
  const d = new Date(`${isoDate}T00:00:00Z`);
  if (isNaN(d.getTime())) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeZone: 'UTC' }).format(d);
}

/** ISO date-time (with offset) shown in the given IANA zone. */
export function formatDateTime(iso: string, zone: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: zone,
  }).format(d);
}

/** `datetime-local` value (wall time in `zone`) to `YYYY-MM-DDTHH:mm:ss±HH:MM`. */
export function toZonedIso(datetimeLocal: string, zone: string): string {
  if (!datetimeLocal) return '';
  const m = LOCAL_RE.exec(datetimeLocal);
  if (!m) return '';
  const [y, mo, d, h, mi, s] = m.slice(1).map((v) => Number(v ?? 0));
  const guess = Date.UTC(y, mo - 1, d, h, mi, s);
  if (isNaN(guess)) return '';

  const off1 = offsetMinutes(guess, zone);
  let epoch = guess - off1 * 60_000;
  const off2 = offsetMinutes(epoch, zone);
  if (off2 !== off1) epoch = guess - off2 * 60_000;

  const finalOffset = offsetMinutes(epoch, zone);
  // Wall time of the resolved instant (differs from input only inside a DST gap).
  const w = new Date(epoch + finalOffset * 60_000);
  return (
    `${pad(w.getUTCFullYear(), 4)}-${pad(w.getUTCMonth() + 1)}-${pad(w.getUTCDate())}` +
    `T${pad(w.getUTCHours())}:${pad(w.getUTCMinutes())}:${pad(w.getUTCSeconds())}` +
    formatOffset(finalOffset)
  );
}

/** Instant to its wall time in `zone` as a `datetime-local` value `YYYY-MM-DDTHH:mm`. */
export function toDatetimeLocal(iso: string, zone: string): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (isNaN(date.getTime())) return '';
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: zone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const get = (type: Intl.DateTimeFormatPartTypes): string =>
    parts.find((p) => p.type === type)?.value ?? '';
  const hour = get('hour') === '24' ? '00' : get('hour');
  return `${get('year')}-${get('month')}-${get('day')}T${hour}:${get('minute')}`;
}

/** Milliseconds as `H:MM:SS` (hours unpadded, clamped at 0, floored to seconds). */
export function formatDuration(ms: number): string {
  const total = Number.isFinite(ms) && ms > 0 ? Math.floor(ms / 1000) : 0;
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return `${h}:${pad(m)}:${pad(s)}`;
}
