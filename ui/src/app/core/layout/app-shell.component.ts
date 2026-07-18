import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthStateService } from '../auth/auth-state.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      <header class="shell__header">
        <a class="brand" routerLink="/overview">
          <span class="brand__eyebrow">Payment fraud operations</span>
          <span class="brand__title">Signal Desk</span>
        </a>

        <nav class="shell__nav" aria-label="Primary">
          @for (item of navItems; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="is-active"
              class="shell__nav-link"
            >
              {{ item.label }}
            </a>
          }
        </nav>

        <div class="shell__operator">
          <span class="shell__operator-label">Signed in</span>
          <strong>{{ authState.username() }}</strong>
          <button class="button button--ghost" type="button" (click)="logout()">
            Log out
          </button>
        </div>
      </header>

      <main class="shell__content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShellComponent {
  protected readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  protected readonly navItems = [
    { label: 'Overview', path: '/overview' },
    { label: 'Cases', path: '/cases' },
    { label: 'Payments', path: '/payments' },
    { label: 'Simulation Lab', path: '/simulations' },
    { label: 'Decision Quality', path: '/quality' },
    { label: 'Operations', path: '/operations' }
  ] as const;

  protected logout(): void {
    this.authState.logout().subscribe(() => void this.router.navigateByUrl('/login'));
  }
}
