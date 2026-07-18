import { computed, inject, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import {
  FraudCase,
  FraudOperationsSummary,
  FraudQualityMetrics,
  FraudScoringProfile,
  PaymentStatus
} from '../../../shared/models/api.models';

@Injectable()
export class CommandCenterStore {
  private readonly api = inject(FraudApiService);

  readonly loading = signal(false);
  readonly actionBusy = signal(false);
  readonly error = signal('');
  readonly actionMessage = signal('');
  readonly actionError = signal('');
  readonly summary = signal<FraudOperationsSummary | null>(null);
  readonly quality = signal<FraudQualityMetrics | null>(null);
  readonly activeProfile = signal<FraudScoringProfile | null>(null);
  readonly cases = signal<FraudCase[]>([]);
  readonly selectedCase = signal<FraudCase | null>(null);
  readonly payments = signal<PaymentStatus[]>([]);

  readonly priorityCases = computed(() =>
    this.cases()
      .filter((item) => item.status !== 'RESOLVED')
      .slice()
      .sort((left, right) => right.riskScore - left.riskScore)
      .slice(0, 8)
  );

  readonly heldPayments = computed(() =>
    this.payments()
      .filter((item) => ['HELD', 'CHALLENGE_REQUIRED', 'DECLINED'].includes(item.paymentStatus))
      .slice(0, 6)
  );

  load(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      summary: this.api.getSummary(),
      quality: this.api.getQualityMetrics(),
      activeProfile: this.api.getActiveProfile(),
      openCases: this.api.listCases({ status: 'OPEN' }),
      escalatedCases: this.api.listCases({ status: 'ESCALATED' }),
      payments: this.api.listPayments()
    }).subscribe({
      next: ({ summary, quality, activeProfile, openCases, escalatedCases, payments }) => {
        const cases = [...escalatedCases, ...openCases];
        this.summary.set(summary);
        this.quality.set(quality);
        this.activeProfile.set(activeProfile);
        this.cases.set(cases);
        this.payments.set(payments);
        this.selectedCase.set(this.selectedCase() ?? cases[0] ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load the command center right now.');
        this.loading.set(false);
      }
    });
  }

  selectCase(caseId: string): void {
    this.api.getCase(caseId).subscribe({
      next: (item) => this.selectedCase.set(item),
      error: () => this.actionError.set('Unable to open the selected case.')
    });
  }

  assignSelected(assignee: string, note: string): void {
    const item = this.selectedCase();
    if (!item) {
      return;
    }
    this.runCaseAction(
      this.api.assignCase(item.caseId, assignee, note),
      'Case assigned and ready for investigator review.'
    );
  }

  escalateSelected(reason: string): void {
    const item = this.selectedCase();
    if (!item) {
      return;
    }
    this.runCaseAction(
      this.api.escalateCase(item.caseId, reason),
      'Case escalated to supervisor review.'
    );
  }

  releaseSelected(summary: string): void {
    const item = this.selectedCase();
    if (!item) {
      return;
    }
    this.runCaseAction(
      this.api.releaseCase(item.caseId, summary),
      'Payment released after review.'
    );
  }

  confirmDeclineSelected(summary: string): void {
    const item = this.selectedCase();
    if (!item) {
      return;
    }
    this.runCaseAction(
      this.api.confirmDecline(item.caseId, summary),
      'Decline confirmed and case resolved.'
    );
  }

  private runCaseAction(request$: { subscribe: Function }, successMessage: string): void {
    this.actionBusy.set(true);
    this.actionMessage.set('');
    this.actionError.set('');

    request$.subscribe({
      next: (item: FraudCase) => {
        this.selectedCase.set(item);
        this.actionMessage.set(successMessage);
        this.actionBusy.set(false);
        this.load();
      },
      error: () => {
        this.actionError.set('The case action could not be completed with the current session.');
        this.actionBusy.set(false);
      }
    });
  }
}
