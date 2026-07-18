import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { OperationsStore } from '../../store/operations.store';

@Component({
  selector: 'app-operations-page',
  imports: [DatePipe, ReactiveFormsModule],
  providers: [OperationsStore],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <p class="eyebrow">Outbound operations</p>
          <h1>Watch delivery pressure, failed events, and replay posture from one control page.</h1>
        </div>
        <p class="page-copy">
          This page keeps the operational lens narrow: event delivery health first, historical
          replay posture second.
        </p>
      </header>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      @if (store.actionMessage()) {
        <p class="inline-success">{{ store.actionMessage() }}</p>
      }

      @if (store.actionError()) {
        <p class="inline-error">{{ store.actionError() }}</p>
      }

      <section class="metric-grid">
        <article class="metric-tile">
          <span>Pending outbound</span>
          <strong>{{ store.summary()?.outboundDeliverySummary?.pendingCount ?? 0 }}</strong>
          <small>Waiting for dispatch or retry.</small>
        </article>
        <article class="metric-tile">
          <span>Delivered outbound</span>
          <strong>{{ store.summary()?.outboundDeliverySummary?.deliveredCount ?? 0 }}</strong>
          <small>Successfully dispatched platform events.</small>
        </article>
        <article class="metric-tile">
          <span>Failed outbound</span>
          <strong>{{ store.summary()?.outboundDeliverySummary?.failedCount ?? 0 }}</strong>
          <small>Failures needing operator attention.</small>
        </article>
      </section>

      <section class="content-grid">
        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Failed events</p>
            <h2>Current outbound failures</h2>
          </div>

          <table class="data-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Topic</th>
                <th>Key</th>
                <th>Attempts</th>
              </tr>
            </thead>
            <tbody>
              @for (item of store.outboundEvents(); track item.eventId) {
                <tr [class.is-selected]="item.eventId === store.selectedOutboundEvent()?.eventId" (click)="selectOutboundEvent(item.eventId)">
                  <td>{{ item.eventType }}</td>
                  <td>{{ item.topicName }}</td>
                  <td>{{ item.messageKey }}</td>
                  <td>{{ item.attemptCount }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Replay history</p>
            <h2>Latest replay batches</h2>
          </div>

          <table class="data-table">
            <thead>
              <tr>
                <th>Batch</th>
                <th>Scenarios</th>
                <th>Created by</th>
                <th>Review cases</th>
              </tr>
            </thead>
            <tbody>
              @for (item of store.replayBatches(); track item.batchId) {
                <tr [class.is-selected]="item.batchId === store.selectedReplay()?.batchId" (click)="openReplay(item.batchId)">
                  <td>{{ item.batchName }}</td>
                  <td>{{ item.scenarioCount }}</td>
                  <td>{{ item.createdBy }}</td>
                  <td>{{ item.reviewCaseWouldBeCreatedCount }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      </section>

      <section class="content-grid content-grid--wide">
        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Selected failed event</p>
            <h2>{{ store.selectedOutboundEvent()?.eventType || 'Choose a failed outbound event' }}</h2>
          </div>

          @if (store.selectedOutboundEvent(); as event) {
            <dl class="definition-grid">
              <div><dt>Topic</dt><dd>{{ event.topicName }}</dd></div>
              <div><dt>Message key</dt><dd>{{ event.messageKey }}</dd></div>
              <div><dt>Status</dt><dd>{{ event.status }}</dd></div>
              <div><dt>Attempts</dt><dd>{{ event.attemptCount }}</dd></div>
            </dl>

            <form class="form-grid" [formGroup]="eventNoteForm" (ngSubmit)="addIncidentNote()">
              <label><span>Incident note</span><input formControlName="note" type="text" /></label>
              <div class="button-row button-row--inline">
                <button class="button" type="submit" [disabled]="store.actionBusy()">Save note</button>
                <button class="button button--ghost" type="button" (click)="retrySelectedEvent()" [disabled]="store.actionBusy()">Retry event</button>
              </div>
            </form>

            <p class="detail-copy">{{ event.lastError || 'No error detail was recorded on this event.' }}</p>
          }
        </article>

        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Supervisor controls</p>
            <h2>Step-up and recovery actions</h2>
          </div>

          <div class="button-row">
            <button class="button" type="button" (click)="generateStepUpToken()" [disabled]="store.actionBusy()">Generate step-up token</button>
            <button class="button button--ghost" type="button" (click)="resendStepUpToken()" [disabled]="store.actionBusy()">Resend token</button>
            <button class="button button--ghost" type="button" (click)="retryFailedBatch()" [disabled]="store.actionBusy()">Retry failed batch</button>
            <button class="button button--ghost" type="button" (click)="dispatchNow()" [disabled]="store.actionBusy()">Dispatch now</button>
          </div>

          <form class="form-grid" [formGroup]="stepUpForm" (ngSubmit)="verifyStepUpToken()">
            <label><span>Verification token</span><input formControlName="token" type="text" /></label>
            <div class="button-row button-row--inline">
              <button class="button" type="submit" [disabled]="store.actionBusy()">Verify token</button>
              <button class="button button--ghost" type="button" (click)="revokeStepUp()" [disabled]="store.actionBusy()">Revoke outstanding tokens</button>
            </div>
          </form>

          @if (store.stepUpToken(); as token) {
            <p class="detail-copy">
              Latest token delivery: {{ token.deliveryChannel }} to {{ token.destinationMasked }} expiring at
              {{ token.expiresAt | date: 'medium' }}.
            </p>
          }

          @if (store.stepUpVerification(); as verification) {
            <p class="detail-copy">
              Verified operator {{ verification.operator }} until {{ verification.validUntil | date: 'medium' }}.
            </p>
          }
        </article>
      </section>

      <section class="content-grid content-grid--wide">
        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Replay builder</p>
            <h2>Create a persisted replay batch</h2>
          </div>

          <form class="form-grid" [formGroup]="replayForm" (ngSubmit)="createReplay()">
            <label><span>Batch name</span><input formControlName="batchName" type="text" /></label>
            <label><span>Challenge threshold</span><input formControlName="challengeThreshold" type="number" /></label>
            <label><span>Hold threshold</span><input formControlName="holdThreshold" type="number" /></label>
            <label><span>Decline threshold</span><input formControlName="declineThreshold" type="number" /></label>
            <button class="button" type="submit" [disabled]="store.actionBusy()">Create replay batch</button>
          </form>
        </article>

        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Selected replay</p>
            <h2>{{ store.selectedReplay()?.batchName || 'Choose a replay batch' }}</h2>
          </div>

          @if (store.selectedReplay(); as replay) {
            <dl class="definition-grid">
              <div><dt>Scenarios</dt><dd>{{ replay.scenarioCount }}</dd></div>
              <div><dt>Created by</dt><dd>{{ replay.createdBy }}</dd></div>
              <div><dt>Review cases</dt><dd>{{ replay.reviewCaseWouldBeCreatedCount }}</dd></div>
              <div><dt>Created at</dt><dd>{{ replay.createdAt | date: 'medium' }}</dd></div>
            </dl>

            <table class="data-table">
              <thead>
                <tr>
                  <th>Payment</th>
                  <th>Decision</th>
                  <th>Status</th>
                  <th>Score</th>
                </tr>
              </thead>
              <tbody>
                @for (item of replay.items; track item.id) {
                  <tr>
                    <td>{{ item.paymentId }}</td>
                    <td>{{ item.decision }}</td>
                    <td>{{ item.projectedPaymentStatus }}</td>
                    <td>{{ item.riskScore }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </article>
      </section>

      <section class="table-panel">
        <div class="section-heading">
          <p class="eyebrow">Step-up delivery audit</p>
          <h2>Recent operator deliveries</h2>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th>Operator</th>
              <th>Status</th>
              <th>Channel</th>
              <th>Expires</th>
            </tr>
          </thead>
          <tbody>
            @for (item of store.stepUpDeliveries(); track item.deliveryId) {
              <tr>
                <td>{{ item.operator }}</td>
                <td>{{ item.status }}</td>
                <td>{{ item.deliveryChannel }}</td>
                <td>{{ item.expiresAt | date: 'medium' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationsPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly store = inject(OperationsStore);

  protected readonly eventNoteForm = this.formBuilder.nonNullable.group({
    note: ['Kafka broker recovered, but this event still needs replay after downstream schema validation.', Validators.required]
  });

  protected readonly replayForm = this.formBuilder.nonNullable.group({
    batchName: ['Friday July 17 replay batch', Validators.required],
    challengeThreshold: [40, Validators.required],
    holdThreshold: [60, Validators.required],
    declineThreshold: [80, Validators.required]
  });

  protected readonly stepUpForm = this.formBuilder.nonNullable.group({
    token: ['', Validators.required]
  });

  constructor() {
    this.store.load();
  }

  protected selectOutboundEvent(eventId: string): void {
    this.store.selectOutboundEvent(eventId);
  }

  protected openReplay(batchId: string): void {
    this.store.loadReplay(batchId);
  }

  protected addIncidentNote(): void {
    const event = this.store.selectedOutboundEvent();
    if (!event) {
      return;
    }
    this.store.addIncidentNote(event.eventId, this.eventNoteForm.getRawValue().note);
  }

  protected retrySelectedEvent(): void {
    const event = this.store.selectedOutboundEvent();
    if (!event) {
      return;
    }
    this.store.retryEvent(event.eventId);
  }

  protected retryFailedBatch(): void {
    this.store.retryFailedBatch(25);
  }

  protected dispatchNow(): void {
    this.store.dispatchNow();
  }

  protected createReplay(): void {
    const value = this.replayForm.getRawValue();
    this.store.createReplay({
      batchName: value.batchName,
      overrides: {
        challengeThreshold: value.challengeThreshold,
        holdThreshold: value.holdThreshold,
        declineThreshold: value.declineThreshold
      },
      scenarios: [
        {
          paymentId: 'PAY-REPLAY-UI-1001',
          customerId: 'CUST-REPLAY-UI-1001',
          amount: 12500,
          currency: 'ZAR',
          merchantCategory: 'ELECTRONICS',
          paymentChannel: 'MOBILE_APP',
          customerAverageTicket: 1800,
          transactionCountLastFiveMinutes: 5,
          spendLastHour: 24000,
          beneficiaryAgeHours: 1,
          newDevice: true,
          highRiskCountry: false,
          impossibleTravel: true,
          recentPasswordReset: true
        }
      ]
    });
  }

  protected generateStepUpToken(): void {
    this.store.generateStepUpToken();
  }

  protected resendStepUpToken(): void {
    this.store.resendStepUpToken();
  }

  protected verifyStepUpToken(): void {
    this.store.verifyStepUpToken(this.stepUpForm.getRawValue().token);
  }

  protected revokeStepUp(): void {
    this.store.revokeStepUp();
  }
}
