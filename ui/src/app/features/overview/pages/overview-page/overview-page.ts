import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { OverviewStore } from '../../store/overview.store';

@Component({
  selector: 'app-overview-page',
  imports: [DecimalPipe, RouterLink],
  providers: [OverviewStore],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <p class="eyebrow">Operations overview</p>
          <h1>Keep the platform readable while the queue keeps moving.</h1>
        </div>
        <p class="page-copy">
          This page compresses queue pressure, scoring posture, and the newest movement across
          cases and payments into one clean sweep.
        </p>
      </header>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      <section class="metric-grid">
        @for (item of headlineMetrics(); track item.label) {
          <article class="metric-tile">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.note }}</small>
          </article>
        }
      </section>

      <section class="content-grid">
        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Decision mix</p>
            <h2>Current scoring posture</h2>
          </div>

          <table class="data-table">
            <thead>
              <tr>
                <th>Decision</th>
                <th>Count</th>
              </tr>
            </thead>
            <tbody>
              @for (metric of store.decisionSnapshot(); track metric.key) {
                <tr>
                  <td>{{ metric.key }}</td>
                  <td>{{ metric.count }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Active thresholds</p>
            <h2>{{ store.activeProfile()?.profileName ?? 'System default thresholds' }}</h2>
          </div>

          @if (store.activeProfile(); as profile) {
            <dl class="definition-grid">
              <div>
                <dt>Challenge</dt>
                <dd>{{ profile.thresholds.challengeThreshold }}</dd>
              </div>
              <div>
                <dt>Hold</dt>
                <dd>{{ profile.thresholds.holdThreshold }}</dd>
              </div>
              <div>
                <dt>Decline</dt>
                <dd>{{ profile.thresholds.declineThreshold }}</dd>
              </div>
              <div>
                <dt>Version</dt>
                <dd>{{ profile.versionNumber }}</dd>
              </div>
            </dl>
            <p class="detail-copy">{{ profile.changeSummary }}</p>
          }
        </article>
      </section>

      <section class="content-grid">
        <article class="table-panel">
          <div class="section-heading section-heading--split">
            <div>
              <p class="eyebrow">Open review queue</p>
              <h2>Newest open cases</h2>
            </div>
            <a routerLink="/cases" class="text-link">Open full queue</a>
          </div>

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
              @for (item of store.recentCases(); track item.caseId) {
                <tr>
                  <td>{{ item.paymentId }}</td>
                  <td>{{ item.decision }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.riskScore }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <article class="table-panel">
          <div class="section-heading section-heading--split">
            <div>
              <p class="eyebrow">Payment stream</p>
              <h2>Latest tracked payments</h2>
            </div>
            <a routerLink="/payments" class="text-link">Open payments</a>
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
              @for (item of store.recentPayments(); track item.paymentId) {
                <tr>
                  <td>{{ item.paymentId }}</td>
                  <td>{{ item.paymentStatus }}</td>
                  <td>{{ item.latestDecision }}</td>
                  <td>{{ item.amount | number: '1.2-2' }} {{ item.currency }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OverviewPage {
  protected readonly store = inject(OverviewStore);

  protected readonly headlineMetrics = computed(() => {
    const summary = this.store.summary();
    if (!summary) {
      return [];
    }

    return [
      {
        label: 'Assessments',
        value: String(summary.totalAssessments),
        note: 'All fraud decisions persisted so far.'
      },
      {
        label: 'Tracked payments',
        value: String(summary.totalTrackedPayments),
        note: 'Payment records currently in the platform.'
      },
      {
        label: 'Review backlog',
        value: String(summary.reviewBacklogCount),
        note: 'Open plus escalated analyst work.'
      },
      {
        label: 'Average risk score',
        value: `${summary.averageRiskScore ?? 0}`,
        note: 'Current platform-wide scoring average.'
      }
    ];
  });

  constructor() {
    this.store.load();
  }
}
