import { HttpClient, HttpHeaders } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';

import { AuthSession, FraudOperationsSummary } from '../../shared/models/api.models';

const STORAGE_KEY = 'payment-fraud-ui.session';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly http = inject(HttpClient);

  readonly session = signal<AuthSession | null>(this.readSession());
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly username = computed(() => this.session()?.username ?? '');

  readonly demoAccounts = [
    {
      label: 'Analyst',
      username: 'analyst.one',
      password: 'local-analyst-2026',
      note: 'Queue triage, notes, and read workflows.'
    },
    {
      label: 'Senior Analyst',
      username: 'senior.analyst',
      password: 'local-senior-2026',
      note: 'Escalation, supervisor controls, and protected actions.'
    },
    {
      label: 'Platform Admin',
      username: 'platform.admin',
      password: 'local-admin-2026',
      note: 'Full platform access for operations and testing.'
    }
  ] as const;

  login(credentials: AuthSession): Observable<void> {
    const session = {
      username: credentials.username.trim(),
      password: credentials.password
    };
    const authorization = this.buildAuthHeader(session);
    if (!authorization) {
      throw new Error('Missing credentials for login.');
    }
    const headers = new HttpHeaders({
      Authorization: authorization
    });

    return this.http
      .get<FraudOperationsSummary>('/api/v1/fraud-operations/summary', { headers })
      .pipe(
        tap(() => {
          this.session.set(session);
          localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        }),
        map(() => void 0)
      );
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  buildAuthHeader(session = this.session()): string | null {
    if (!session) {
      return null;
    }

    return `Basic ${btoa(`${session.username}:${session.password}`)}`;
  }

  private readSession(): AuthSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
