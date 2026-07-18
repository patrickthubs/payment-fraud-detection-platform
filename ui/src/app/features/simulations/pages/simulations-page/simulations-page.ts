import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { SimulationsStore } from '../../store/simulations.store';

@Component({
  selector: 'app-simulations-page',
  imports: [ReactiveFormsModule],
  providers: [SimulationsStore],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <p class="eyebrow">Simulation lab</p>
          <h1>Simulation lab</h1>
        </div>
      </header>

      <form class="lab-grid" [formGroup]="form">
        <div class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Scenario inputs</p>
            <h2>Payment scenario</h2>
          </div>

          <div class="form-grid form-grid--three">
            <label><span>Payment id</span><input formControlName="paymentId" type="text" /></label>
            <label><span>Customer id</span><input formControlName="customerId" type="text" /></label>
            <label><span>Amount</span><input formControlName="amount" type="number" step="0.01" /></label>
            <label><span>Currency</span><input formControlName="currency" type="text" /></label>
            <label><span>Merchant category</span><input formControlName="merchantCategory" type="text" /></label>
            <label><span>Channel</span><input formControlName="paymentChannel" type="text" /></label>
            <label><span>Average ticket</span><input formControlName="customerAverageTicket" type="number" step="0.01" /></label>
            <label><span>Tx in last five minutes</span><input formControlName="transactionCountLastFiveMinutes" type="number" /></label>
            <label><span>Spend last hour</span><input formControlName="spendLastHour" type="number" step="0.01" /></label>
            <label><span>Beneficiary age hours</span><input formControlName="beneficiaryAgeHours" type="number" /></label>
            <label class="toggle"><input formControlName="newDevice" type="checkbox" /><span>New device</span></label>
            <label class="toggle"><input formControlName="highRiskCountry" type="checkbox" /><span>High-risk country</span></label>
            <label class="toggle"><input formControlName="impossibleTravel" type="checkbox" /><span>Impossible travel</span></label>
            <label class="toggle"><input formControlName="recentPasswordReset" type="checkbox" /><span>Recent password reset</span></label>
          </div>
        </div>

        <div class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Override thresholds</p>
            <h2>Comparison inputs</h2>
          </div>

          <div class="form-grid">
            <label><span>Challenge threshold</span><input formControlName="challengeThreshold" type="number" /></label>
            <label><span>Hold threshold</span><input formControlName="holdThreshold" type="number" /></label>
            <label><span>Decline threshold</span><input formControlName="declineThreshold" type="number" /></label>
          </div>

          <div class="button-row">
            <button class="button" type="button" (click)="simulate()" [disabled]="form.invalid || store.running()">
              Run simulation
            </button>
            <button class="button button--ghost" type="button" (click)="compare()" [disabled]="form.invalid || store.running()">
              Compare thresholds
            </button>
            <button class="button button--ghost" type="button" (click)="assess()" [disabled]="form.invalid || store.running()">
              Persist live assessment
            </button>
          </div>
        </div>
      </form>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      <section class="content-grid">
        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Simulation result</p>
            <h2>{{ store.simulation()?.decision || 'Awaiting run' }}</h2>
          </div>
          @if (store.simulation(); as result) {
            <dl class="definition-grid">
              <div><dt>Risk score</dt><dd>{{ result.riskScore }}</dd></div>
              <div><dt>Projected status</dt><dd>{{ result.projectedPaymentStatus }}</dd></div>
              <div><dt>Velocity source</dt><dd>{{ result.velocitySource }}</dd></div>
              <div><dt>Would create case</dt><dd>{{ result.reviewCaseWouldBeCreated ? 'Yes' : 'No' }}</dd></div>
            </dl>
            <p class="detail-copy">{{ result.summary }}</p>
          }
        </article>

        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Live assessment</p>
            <h2>{{ store.liveAssessment()?.decision || 'No persisted run yet' }}</h2>
          </div>
          @if (store.liveAssessment(); as assessment) {
            <dl class="definition-grid">
              <div><dt>Assessment id</dt><dd>{{ assessment.assessmentId }}</dd></div>
              <div><dt>Review case</dt><dd>{{ assessment.reviewCaseId || 'None' }}</dd></div>
              <div><dt>Status</dt><dd>{{ assessment.paymentStatus }}</dd></div>
              <div><dt>Velocity source</dt><dd>{{ assessment.velocitySource }}</dd></div>
            </dl>
            <p class="detail-copy">{{ assessment.summary }}</p>
          }
        </article>
      </section>

      @if (store.comparison(); as comparison) {
        <section class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Comparison result</p>
            <h2>{{ comparison.comparisonSummary }}</h2>
          </div>

          <table class="data-table">
            <thead>
              <tr>
                <th>Mode</th>
                <th>Decision</th>
                <th>Projected status</th>
                <th>Score</th>
                <th>Review case</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Baseline</td>
                <td>{{ comparison.baselineOutcome.decision }}</td>
                <td>{{ comparison.baselineOutcome.projectedPaymentStatus }}</td>
                <td>{{ comparison.baselineOutcome.riskScore }}</td>
                <td>{{ comparison.baselineOutcome.reviewCaseWouldBeCreated ? 'Yes' : 'No' }}</td>
              </tr>
              <tr>
                <td>Override</td>
                <td>{{ comparison.overrideOutcome.decision }}</td>
                <td>{{ comparison.overrideOutcome.projectedPaymentStatus }}</td>
                <td>{{ comparison.overrideOutcome.riskScore }}</td>
                <td>{{ comparison.overrideOutcome.reviewCaseWouldBeCreated ? 'Yes' : 'No' }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      }

      <section class="content-grid content-grid--wide">
        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Scoring profiles</p>
            <h2>Drafts and active threshold sets</h2>
          </div>

          @if (store.profileMessage()) {
            <p class="inline-success">{{ store.profileMessage() }}</p>
          }

          @if (store.profileError()) {
            <p class="inline-error">{{ store.profileError() }}</p>
          }

          <table class="data-table">
            <thead>
              <tr>
                <th>Profile</th>
                <th>Version</th>
                <th>Status</th>
                <th>Thresholds</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (profile of store.profiles(); track profile.profileId) {
                <tr>
                  <td>{{ profile.profileName }}</td>
                  <td>{{ profile.versionNumber }}</td>
                  <td>{{ profile.active ? 'ACTIVE' : 'DRAFT' }}</td>
                  <td>
                    {{ profile.thresholds.challengeThreshold }}/{{ profile.thresholds.holdThreshold }}/{{ profile.thresholds.declineThreshold }}
                  </td>
                  <td>
                    <div class="button-row button-row--inline">
                      <button class="button button--ghost" type="button" (click)="compareSavedProfile(profile.profileId)" [disabled]="store.running()">Compare</button>
                      <button class="button" type="button" (click)="activateProfile(profile.profileId)" [disabled]="store.profileBusy() || profile.active">Activate</button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">New profile</p>
            <h2>Create threshold profile</h2>
          </div>

          @if (store.activeProfile(); as activeProfile) {
            <p class="detail-copy">
              Active profile: <strong>{{ activeProfile.profileName }}</strong> with thresholds
              {{ activeProfile.thresholds.challengeThreshold }}/{{ activeProfile.thresholds.holdThreshold }}/{{ activeProfile.thresholds.declineThreshold }}.
            </p>
          }

          <form class="form-grid" [formGroup]="profileForm" (ngSubmit)="createProfile()">
            <label><span>Profile name</span><input formControlName="profileName" type="text" /></label>
            <label><span>Challenge threshold</span><input formControlName="challengeThreshold" type="number" /></label>
            <label><span>Hold threshold</span><input formControlName="holdThreshold" type="number" /></label>
            <label><span>Decline threshold</span><input formControlName="declineThreshold" type="number" /></label>
            <label><span>Change summary</span><input formControlName="changeSummary" type="text" /></label>
            <button class="button" type="submit" [disabled]="store.profileBusy()">Create profile</button>
          </form>
        </article>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimulationsPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly store = inject(SimulationsStore);

  protected readonly form = this.formBuilder.nonNullable.group({
    paymentId: ['PAY-UI-1001', Validators.required],
    customerId: ['CUST-UI-1001', Validators.required],
    amount: [12500, Validators.required],
    currency: ['ZAR', Validators.required],
    merchantCategory: ['ELECTRONICS', Validators.required],
    paymentChannel: ['MOBILE_APP', Validators.required],
    customerAverageTicket: [1800, Validators.required],
    transactionCountLastFiveMinutes: [5, Validators.required],
    spendLastHour: [24000, Validators.required],
    beneficiaryAgeHours: [1, Validators.required],
    newDevice: true,
    highRiskCountry: false,
    impossibleTravel: true,
    recentPasswordReset: true,
    challengeThreshold: [42, Validators.required],
    holdThreshold: [63, Validators.required],
    declineThreshold: [84, Validators.required]
  });

  protected readonly profileForm = this.formBuilder.nonNullable.group({
    profileName: ['July 17 analyst candidate', Validators.required],
    challengeThreshold: [42, Validators.required],
    holdThreshold: [63, Validators.required],
    declineThreshold: [84, Validators.required],
    changeSummary: ['Lower early friction while keeping the decline boundary conservative.', Validators.required]
  });

  constructor() {
    this.store.loadProfiles();
  }

  protected simulate(): void {
    this.store.runSimulation(this.scenarioPayload());
  }

  protected compare(): void {
    const values = this.form.getRawValue();
    this.store.runComparison(
      this.scenarioPayload(),
      values.challengeThreshold,
      values.holdThreshold,
      values.declineThreshold
    );
  }

  protected assess(): void {
    this.store.createAssessment(this.scenarioPayload());
  }

  protected createProfile(): void {
    this.store.createProfile(this.profileForm.getRawValue());
  }

  protected activateProfile(profileId: string): void {
    this.store.activateProfile(profileId);
  }

  protected compareSavedProfile(profileId: string): void {
    this.store.compareSavedProfile(profileId, this.scenarioPayload());
  }

  private scenarioPayload() {
    const value = this.form.getRawValue();
    return {
      paymentId: value.paymentId,
      customerId: value.customerId,
      amount: value.amount,
      currency: value.currency,
      merchantCategory: value.merchantCategory,
      paymentChannel: value.paymentChannel,
      customerAverageTicket: value.customerAverageTicket,
      transactionCountLastFiveMinutes: value.transactionCountLastFiveMinutes,
      spendLastHour: value.spendLastHour,
      beneficiaryAgeHours: value.beneficiaryAgeHours,
      newDevice: value.newDevice,
      highRiskCountry: value.highRiskCountry,
      impossibleTravel: value.impossibleTravel,
      recentPasswordReset: value.recentPasswordReset
    };
  }
}
