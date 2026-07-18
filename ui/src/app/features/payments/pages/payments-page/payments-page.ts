import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { PaymentsStore } from '../../store/payments.store';

@Component({
  selector: 'app-payments-page',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule],
  providers: [PaymentsStore],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <p class="eyebrow">Payment lifecycle</p>
          <h1>Tracked payments</h1>
        </div>
      </header>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      <section class="content-grid content-grid--wide">
        <article class="table-panel">
          <table class="data-table data-table--interactive">
            <thead>
              <tr>
                <th>Payment</th>
                <th>Status</th>
                <th>Decision</th>
                <th>Amount</th>
                <th>Channel</th>
              </tr>
            </thead>
            <tbody>
              @for (item of store.payments(); track item.paymentId) {
                <tr
                  [class.is-selected]="item.paymentId === store.selectedPayment()?.paymentId"
                  (click)="selectPayment(item.paymentId)"
                >
                  <td>{{ item.paymentId }}</td>
                  <td>{{ item.paymentStatus }}</td>
                  <td>{{ item.latestDecision }}</td>
                  <td>{{ item.amount | number: '1.2-2' }} {{ item.currency }}</td>
                  <td>{{ item.paymentChannel }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <aside class="detail-panel">
          @if (store.selectedPayment(); as item) {
            <div class="section-heading">
              <p class="eyebrow">Selected payment</p>
              <h2>{{ item.paymentId }}</h2>
            </div>

            <dl class="definition-grid">
              <div>
                <dt>Customer</dt>
                <dd>{{ item.customerId }}</dd>
              </div>
              <div>
                <dt>Latest status</dt>
                <dd>{{ item.paymentStatus }}</dd>
              </div>
              <div>
                <dt>Latest score</dt>
                <dd>{{ item.latestRiskScore }}</dd>
              </div>
              <div>
                <dt>Merchant category</dt>
                <dd>{{ item.merchantCategory }}</dd>
              </div>
            </dl>

            <div class="timeline">
              <h3>Transition history</h3>
              @for (entry of item.transitions; track entry.id) {
                <div class="timeline__item">
                  <strong>{{ entry.fromStatus || 'NEW' }} -> {{ entry.toStatus }}</strong>
                  <span>{{ entry.createdAt | date: 'medium' }}</span>
                  <p>{{ entry.reason }}</p>
                </div>
              }
            </div>

            @if (item.paymentStatus === 'CHALLENGED') {
              <div class="action-stack">
                <div class="section-heading">
                  <p class="eyebrow">Challenge completion</p>
                  <h3>Record the customer outcome</h3>
                </div>

                @if (store.actionMessage()) {
                  <p class="inline-success">{{ store.actionMessage() }}</p>
                }

                @if (store.actionError()) {
                  <p class="inline-error">{{ store.actionError() }}</p>
                }

                <form class="form-grid" [formGroup]="challengeForm" (ngSubmit)="completeChallenge()">
                  <label>
                    <span>Outcome</span>
                    <select formControlName="outcome">
                      <option value="PASSED">PASSED</option>
                      <option value="FAILED">FAILED</option>
                      <option value="ABANDONED">ABANDONED</option>
                    </select>
                  </label>
                  <label>
                    <span>Operator note</span>
                    <input formControlName="note" type="text" />
                  </label>
                  <button class="button" type="submit" [disabled]="store.actionBusy()">
                    Record challenge outcome
                  </button>
                </form>
              </div>
          }
          } @else {
            <p class="detail-copy">Select a payment to inspect it.</p>
          }
        </aside>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentsPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly store = inject(PaymentsStore);

  protected readonly challengeForm = this.formBuilder.nonNullable.group({
    outcome: 'PASSED' as 'PASSED' | 'FAILED' | 'ABANDONED',
    note: 'Customer completed the verification flow successfully.'
  });

  constructor() {
    this.store.load();
  }

  protected selectPayment(paymentId: string): void {
    this.store.loadDetail(paymentId);
  }

  protected completeChallenge(): void {
    const selectedPayment = this.store.selectedPayment();
    if (!selectedPayment) {
      return;
    }

    this.store.completeChallenge(
      selectedPayment.paymentId,
      this.challengeForm.getRawValue()
    );
  }
}
