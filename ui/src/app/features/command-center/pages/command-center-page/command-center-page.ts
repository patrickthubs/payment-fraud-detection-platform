import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CommandCenterStore } from '../../store/command-center.store';

@Component({
  selector: 'app-command-center-page',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink],
  providers: [CommandCenterStore],
  template: `
    <section class="page-shell command-center">
      <header class="page-header page-header--split">
        <div>
          <p class="eyebrow">Fraud operations command center</p>
          <h1>Live case command</h1>
        </div>
        <div class="command-center__posture">
          <span>Active policy</span>
          <strong>{{ store.activeProfile()?.profileName ?? 'System default' }}</strong>
          <a routerLink="/rules-studio" class="text-link">Open rules studio</a>
        </div>
      </header>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      <section class="metric-grid metric-grid--five">
        @for (item of headlineMetrics(); track item.label) {
          <article class="metric-tile">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        }
      </section>

      <section class="command-layout">
        <article class="table-panel">
          <div class="section-heading section-heading--split">
            <div>
              <p class="eyebrow">Priority queue</p>
              <h2>Cases that need attention</h2>
            </div>
            <a routerLink="/cases" class="text-link">Full queue</a>
          </div>

          <table class="data-table data-table--interactive">
            <thead>
              <tr>
                <th>Payment</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Decision</th>
                <th>Score</th>
              </tr>
            </thead>
            <tbody>
              @for (item of store.priorityCases(); track item.caseId) {
                <tr
                  [class.is-selected]="item.caseId === store.selectedCase()?.caseId"
                  (click)="store.selectCase(item.caseId)"
                >
                  <td>{{ item.paymentId }}</td>
                  <td>{{ item.customerId }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.decision }}</td>
                  <td>{{ item.riskScore }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <aside class="detail-panel command-center__case">
          @if (store.selectedCase(); as item) {
            <div class="section-heading">
              <p class="eyebrow">Investigation file</p>
              <h2>{{ item.paymentId }}</h2>
            </div>

            <dl class="definition-grid">
              <div><dt>Status</dt><dd>{{ item.status }}</dd></div>
              <div><dt>Score</dt><dd>{{ item.riskScore }}</dd></div>
              <div><dt>Decision</dt><dd>{{ item.decision }}</dd></div>
              <div><dt>Owner</dt><dd>{{ item.currentAssignee || 'Unassigned' }}</dd></div>
            </dl>

            <p class="detail-copy">{{ item.summary }}</p>

            @if (store.actionMessage()) {
              <p class="inline-success">{{ store.actionMessage() }}</p>
            }

            @if (store.actionError()) {
              <p class="inline-error">{{ store.actionError() }}</p>
            }

            <form class="form-grid" [formGroup]="actionForm">
              <label><span>Assignee</span><input formControlName="assignee" type="text" /></label>
              <label><span>Operator note</span><input formControlName="note" type="text" /></label>
              <div class="button-row button-row--inline">
                <button class="button" type="button" (click)="assign()" [disabled]="store.actionBusy() || actionForm.invalid">Assign</button>
                <button class="button button--ghost" type="button" (click)="escalate()" [disabled]="store.actionBusy() || actionForm.invalid">Escalate</button>
              </div>
              <div class="button-row button-row--inline">
                <button class="button" type="button" (click)="release()" [disabled]="store.actionBusy() || actionForm.invalid">Release</button>
                <button class="button button--ghost" type="button" (click)="confirmDecline()" [disabled]="store.actionBusy() || actionForm.invalid">Confirm decline</button>
              </div>
            </form>

            <div class="timeline timeline--compact">
              @for (entry of item.timelineEntries.slice(0, 4); track entry.entryId) {
                <div class="timeline__item">
                  <strong>{{ entry.actionType }}</strong>
                  <span>{{ entry.actor }}</span>
                  <p>{{ entry.detail }}</p>
                </div>
              }
            </div>
          } @else {
            <p class="detail-copy">No open or escalated cases are waiting in the command queue.</p>
          }
        </aside>
      </section>

      <section class="content-grid">
        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Payment holds</p>
            <h2>Payments blocked by fraud controls</h2>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>Payment</th>
                <th>Status</th>
                <th>Decision</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              @for (payment of store.heldPayments(); track payment.paymentId) {
                <tr>
                  <td>{{ payment.paymentId }}</td>
                  <td>{{ payment.paymentStatus }}</td>
                  <td>{{ payment.latestDecision }}</td>
                  <td>{{ payment.amount | number: '1.2-2' }} {{ payment.currency }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Decision quality</p>
            <h2>Ground-truth signal</h2>
          </div>
          @if (store.quality(); as quality) {
            <dl class="definition-grid">
              <div><dt>Precision</dt><dd>{{ percent(quality.precision) }}</dd></div>
              <div><dt>Recall</dt><dd>{{ percent(quality.recall) }}</dd></div>
              <div><dt>False positives</dt><dd>{{ quality.falsePositives }}</dd></div>
              <div><dt>Net loss</dt><dd>{{ quality.netLoss | number: '1.2-2' }}</dd></div>
            </dl>
          }
          <a routerLink="/quality" class="text-link">Record outcomes</a>
        </article>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CommandCenterPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly store = inject(CommandCenterStore);

  protected readonly actionForm = this.formBuilder.nonNullable.group({
    assignee: ['analyst.one', Validators.required],
    note: ['Reviewed from command center priority queue.', Validators.required]
  });

  protected readonly headlineMetrics = computed(() => {
    const summary = this.store.summary();
    const quality = this.store.quality();
    if (!summary) {
      return [];
    }

    return [
      { label: 'Open backlog', value: String(summary.reviewBacklogCount) },
      { label: 'Tracked payments', value: String(summary.totalTrackedPayments) },
      { label: 'Avg risk score', value: String(summary.averageRiskScore ?? 0) },
      { label: 'Precision', value: quality ? this.percent(quality.precision) : '0.0%' },
      { label: 'Net loss', value: quality ? this.money(quality.netLoss) : 'R0.00' }
    ];
  });

  constructor() {
    this.store.load();
  }

  protected assign(): void {
    const value = this.actionForm.getRawValue();
    this.store.assignSelected(value.assignee, value.note);
  }

  protected escalate(): void {
    this.store.escalateSelected(this.actionForm.getRawValue().note);
  }

  protected release(): void {
    this.store.releaseSelected(this.actionForm.getRawValue().note);
  }

  protected confirmDecline(): void {
    this.store.confirmDeclineSelected(this.actionForm.getRawValue().note);
  }

  protected percent(value: number): string {
    return `${(value * 100).toFixed(1)}%`;
  }

  private money(value: number): string {
    return new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' }).format(value);
  }
}
