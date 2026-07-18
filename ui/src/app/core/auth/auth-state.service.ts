import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, switchMap, tap } from 'rxjs';

import { AuthCredentials, AuthSession } from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly http = inject(HttpClient);

  readonly session = signal<AuthSession | null>(null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly username = computed(() => this.session()?.username ?? '');

  readonly demoAccounts = [
    {
      label: 'Analyst',
      username: 'analyst.one',
      note: 'Queue triage, notes, and read workflows.'
    },
    {
      label: 'Senior Analyst',
      username: 'senior.analyst',
      note: 'Escalation, supervisor controls, and protected actions.'
    },
    {
      label: 'Platform Admin',
      username: 'platform.admin',
      note: 'Full platform access for operations and testing.'
    }
  ] as const;

  login(credentials: AuthCredentials): Observable<void> {
    const request = {
      username: credentials.username.trim(),
      password: credentials.password
    };
    return this.ensureCsrf().pipe(
      switchMap(() => this.http.post<AuthSession>('/api/v1/auth/session', request)),
      tap((session) => this.session.set(session)),
      map(() => void 0)
    );
  }

  ensureSession(): Observable<boolean> {
    if (this.session()) {
      return of(true);
    }
    return this.http.get<AuthSession>('/api/v1/auth/session').pipe(
      tap((session) => this.session.set(session)),
      map(() => true),
      catchError(() => of(false))
    );
  }

  logout(): Observable<void> {
    return this.ensureCsrf().pipe(
      switchMap(() => this.http.post<void>('/api/v1/auth/logout', {})),
      tap(() => this.session.set(null))
    );
  }

  private ensureCsrf(): Observable<unknown> {
    return this.http.get('/api/v1/auth/csrf');
  }
}
