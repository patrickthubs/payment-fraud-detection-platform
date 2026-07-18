import { computed, inject, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import { FraudCase, FraudOperationsSummary, FraudScoringProfile, PaymentStatus } from '../../../shared/models/api.models';

@Injectable()
export class OverviewStore {
  private readonly api = inject(FraudApiService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly summary = signal<FraudOperationsSummary | null>(null);
  readonly activeProfile = signal<FraudScoringProfile | null>(null);
  readonly recentCases = signal<FraudCase[]>([]);
  readonly recentPayments = signal<PaymentStatus[]>([]);

  readonly decisionSnapshot = computed(() => this.summary()?.assessmentsByDecision ?? []);

  load(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      summary: this.api.getSummary(),
      activeProfile: this.api.getActiveProfile(),
      cases: this.api.listCases({ status: 'OPEN' }),
      payments: this.api.listPayments()
    }).subscribe({
      next: ({ summary, activeProfile, cases, payments }) => {
        this.summary.set(summary);
        this.activeProfile.set(activeProfile);
        this.recentCases.set(cases.slice(0, 6));
        this.recentPayments.set(payments.slice(0, 6));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load the fraud operations overview yet.');
        this.loading.set(false);
      }
    });
  }
}
