import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthStateService } from '../../../../core/auth/auth-state.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-page">
      <section class="login-page__story">
        <p class="eyebrow">Saturday, July 18, 2026</p>
        <h1>Signal Desk</h1>
      </section>

      <section class="login-page__panel">
        <div class="section-heading">
          <p class="eyebrow">Operator access</p>
          <h2>Sign in</h2>
        </div>

        <div class="operator-list">
          @for (account of authState.demoAccounts; track account.username) {
            <button
              class="operator-list__item"
              type="button"
              (click)="fillDemoAccount(account.username)"
            >
              <strong>{{ account.label }}</strong>
              <span>{{ account.username }}</span>
              <small>{{ account.note }}</small>
            </button>
          }
        </div>

        <form class="form-grid" [formGroup]="form" (ngSubmit)="login()">
          <label>
            <span>Username</span>
            <input formControlName="username" type="text" autocomplete="username" />
          </label>

          <label>
            <span>Password</span>
            <input
              formControlName="password"
              type="password"
              autocomplete="current-password"
            />
          </label>

          @if (error()) {
            <p class="inline-error">{{ error() }}</p>
          }

          <button class="button" type="submit" [disabled]="submitting() || form.invalid">
            {{ submitting() ? 'Signing in...' : 'Enter workspace' }}
          </button>
        </form>
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  protected fillDemoAccount(username: string): void {
    this.form.patchValue({ username });
    this.error.set('');
  }

  protected login(): void {
    if (this.form.invalid) {
      return;
    }

    this.submitting.set(true);
    this.error.set('');

    this.authState
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/command-center');
        },
        error: () => {
          this.error.set('Login failed. Use one of the persisted demo operators and try again.');
        }
      });
  }
}
