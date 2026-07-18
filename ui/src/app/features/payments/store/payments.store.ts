import { inject, Injectable, signal } from '@angular/core';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import { CompletePaymentChallengeRequest, PaymentStatus } from '../../../shared/models/api.models';

@Injectable()
export class PaymentsStore {
  private readonly api = inject(FraudApiService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly actionBusy = signal(false);
  readonly actionMessage = signal('');
  readonly actionError = signal('');
  readonly payments = signal<PaymentStatus[]>([]);
  readonly selectedPayment = signal<PaymentStatus | null>(null);

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.api.listPayments().subscribe({
      next: (payments) => {
        this.payments.set(payments);
        this.loading.set(false);
        const current = this.selectedPayment()?.paymentId;
        const preserved = payments.find((item) => item.paymentId === current);
        this.selectedPayment.set(preserved ?? payments[0] ?? null);
        if (this.selectedPayment()) {
          this.loadDetail(this.selectedPayment()!.paymentId);
        }
      },
      error: () => {
        this.error.set('Unable to load tracked payments.');
        this.loading.set(false);
      }
    });
  }

  loadDetail(paymentId: string): void {
    this.api.getPayment(paymentId).subscribe({
      next: (payment) => this.selectedPayment.set(payment)
    });
  }

  completeChallenge(paymentId: string, request: CompletePaymentChallengeRequest): void {
    this.actionBusy.set(true);
    this.actionMessage.set('');
    this.actionError.set('');

    this.api.completePaymentChallenge(paymentId, request).subscribe({
      next: (payment) => {
        this.selectedPayment.set(payment);
        this.actionMessage.set('Challenge outcome recorded successfully.');
        this.actionBusy.set(false);
        this.load();
      },
      error: () => {
        this.actionError.set('Unable to complete the challenge outcome for this payment.');
        this.actionBusy.set(false);
      }
    });
  }
}
