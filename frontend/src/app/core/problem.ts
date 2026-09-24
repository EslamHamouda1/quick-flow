import { HttpErrorResponse } from '@angular/common/http';

// RFC 7807 problem details (application/problem+json), per the backend contract.
export interface FieldError {
  field: string;
  message: string;
}

export interface Problem {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  errors?: FieldError[];
}

export interface ReadProblem {
  message: string;
  fieldErrors: Record<string, string>;
}

// Turns any HTTP error into a display message plus per-field messages.
export function readProblem(err: HttpErrorResponse): ReadProblem {
  let body: unknown = err.error;
  if (typeof body === 'string') {
    try {
      body = JSON.parse(body);
    } catch {
      // not JSON; fall through to the generic message
    }
  }

  if (body && typeof body === 'object' && ('title' in body || 'detail' in body)) {
    const problem = body as Partial<Problem>;
    const fieldErrors: Record<string, string> = {};
    if (Array.isArray(problem.errors)) {
      for (const e of problem.errors) {
        // keep the first message per field; skip malformed entries
        if (e && typeof e.field === 'string' && !(e.field in fieldErrors)) {
          fieldErrors[e.field] = String(e.message ?? '');
        }
      }
    }
    return { message: problem.detail ?? problem.title ?? '', fieldErrors };
  }

  const message =
    err.status === 0 ? 'Could not reach the server' : err.message || `Request failed (${err.status})`;
  return { message, fieldErrors: {} };
}
