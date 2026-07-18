import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { RulesStudioStore } from '../../store/rules-studio.store';

@Component({
  selector: 'app-rules-studio-page',
  imports: [ReactiveFormsModule, RouterLink],
  providers: [RulesStudioStore],
  template: `
    <section class="page-shell rules-studio">
      <header class="page-header page-header--split">
        <div>
          <p class="eyebrow">Risk rules studio</p>
          <h1>Policy tuning workbench</h1>
        </div>
        <a routerLink="/simulations" class="text-link">Open simulation lab</a>
      </header>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      @if (store.message()) {
        <p class="inline-success">{{ store.message() }}</p>
      }

      <section class="content-grid content-grid--wide">
        <article class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Active live policy</p>
            <h2>{{ store.activeProfile()?.profileName ?? 'System default' }}</h2>
          </div>

          @if (store.activeProfile(); as profile) {
            <dl class="definition-grid definition-grid--three">
              <div><dt>Challenge</dt><dd>{{ profile.thresholds.challengeThreshold }}</dd></div>
              <div><dt>Hold</dt><dd>{{ profile.thresholds.holdThreshold }}</dd></div>
              <div><dt>Decline</dt><dd>{{ profile.thresholds.declineThreshold }}</dd></div>
              <div><dt>Rule set</dt><dd>{{ profile.rulesetVersion }}</dd></div>
              <div><dt>Version</dt><dd>{{ profile.versionNumber }}</dd></div>
              <div><dt>Status</dt><dd>{{ profile.active ? 'ACTIVE' : 'DRAFT' }}</dd></div>
            </dl>
            <p class="detail-copy">{{ profile.changeSummary }}</p>
          }
        </article>

        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Saved policy versions</p>
            <h2>Threshold and rule history</h2>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>Profile</th>
                <th>Version</th>
                <th>Thresholds</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              @for (profile of store.profiles(); track profile.profileId || profile.profileName) {
                <tr>
                  <td>{{ profile.profileName }}</td>
                  <td>{{ profile.versionNumber }}</td>
                  <td>{{ profile.thresholds.challengeThreshold }}/{{ profile.thresholds.holdThreshold }}/{{ profile.thresholds.declineThreshold }}</td>
                  <td>{{ profile.active ? 'ACTIVE' : 'DRAFT' }}</td>
                  <td>
                    @if (profile.profileId) {
                      <button class="button button--ghost" type="button" (click)="activate(profile.profileId)" [disabled]="profile.active || store.saving()">Activate</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      </section>

      <form class="rules-layout" [formGroup]="form">
        <article class="table-panel">
          <div class="section-heading">
            <p class="eyebrow">Draft policy</p>
            <h2>Create a candidate rule set</h2>
          </div>

          <div class="form-grid form-grid--three">
            <label><span>Profile name</span><input formControlName="profileName" /></label>
            <label><span>Challenge threshold</span><input type="number" formControlName="challengeThreshold" /></label>
            <label><span>Hold threshold</span><input type="number" formControlName="holdThreshold" /></label>
            <label><span>Decline threshold</span><input type="number" formControlName="declineThreshold" /></label>
            <label><span>Amount spike multiplier</span><input type="number" step="0.1" formControlName="amountSpikeMultiplier" /></label>
            <label><span>Amount spike weight</span><input type="number" formControlName="amountSpikeWeight" /></label>
            <label><span>Velocity count threshold</span><input type="number" formControlName="velocityCountThreshold" /></label>
            <label><span>Velocity weight</span><input type="number" formControlName="velocityWeight" /></label>
            <label><span>Spend burst multiplier</span><input type="number" step="0.1" formControlName="spendBurstMultiplier" /></label>
            <label><span>Spend burst weight</span><input type="number" formControlName="spendBurstWeight" /></label>
            <label><span>New device weight</span><input type="number" formControlName="newDeviceWeight" /></label>
            <label><span>Impossible travel weight</span><input type="number" formControlName="impossibleTravelWeight" /></label>
            <label><span>New beneficiary hours</span><input type="number" formControlName="beneficiaryAgeHoursThreshold" /></label>
            <label><span>New beneficiary weight</span><input type="number" formControlName="newBeneficiaryWeight" /></label>
            <label><span>Password reset weight</span><input type="number" formControlName="recentPasswordResetWeight" /></label>
            <label><span>Risky merchants</span><input formControlName="riskyMerchantCategories" /></label>
            <label><span>Merchant weight</span><input type="number" formControlName="riskyMerchantWeight" /></label>
            <label><span>Country weight</span><input type="number" formControlName="highRiskCountryWeight" /></label>
          </div>

          <label class="form-grid__wide">
            <span>Change summary</span>
            <textarea formControlName="changeSummary"></textarea>
          </label>

          <div class="button-row button-row--inline">
            <button class="button" type="button" (click)="compare()" [disabled]="form.invalid || store.running()">Compare policy</button>
            <button class="button button--ghost" type="button" (click)="save()" [disabled]="form.invalid || store.saving()">Save draft</button>
          </div>
        </article>

        <aside class="detail-panel">
          <div class="section-heading">
            <p class="eyebrow">Test scenario</p>
            <h2>Policy blast-radius check</h2>
          </div>

          <div class="form-grid">
            <label><span>Amount</span><input type="number" formControlName="amount" /></label>
            <label><span>Average ticket</span><input type="number" formControlName="customerAverageTicket" /></label>
            <label><span>Transactions in 5 min</span><input type="number" formControlName="transactionCountLastFiveMinutes" /></label>
            <label><span>Spend last hour</span><input type="number" formControlName="spendLastHour" /></label>
            <label><span>Beneficiary age hours</span><input type="number" formControlName="beneficiaryAgeHours" /></label>
            <label class="toggle"><input type="checkbox" formControlName="newDevice" /><span>New device</span></label>
            <label class="toggle"><input type="checkbox" formControlName="impossibleTravel" /><span>Impossible travel</span></label>
            <label class="toggle"><input type="checkbox" formControlName="recentPasswordReset" /><span>Password reset</span></label>
          </div>

          @if (store.comparison(); as comparison) {
            <div class="comparison-strip">
              <div>
                <span>Active</span>
                <strong>{{ comparison.baselineOutcome.decision }}</strong>
                <small>{{ comparison.baselineOutcome.projectedPaymentStatus }}</small>
              </div>
              <div>
                <span>Draft</span>
                <strong>{{ comparison.overrideOutcome.decision }}</strong>
                <small>{{ comparison.overrideOutcome.projectedPaymentStatus }}</small>
              </div>
            </div>
            <p class="detail-copy">{{ comparison.comparisonSummary }}</p>
          }
        </aside>
      </form>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RulesStudioPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly store = inject(RulesStudioStore);

  protected readonly form = this.formBuilder.nonNullable.group({
    profileName: ['Command center policy candidate', Validators.required],
    challengeThreshold: [42, [Validators.required, Validators.min(1), Validators.max(100)]],
    holdThreshold: [63, [Validators.required, Validators.min(1), Validators.max(100)]],
    declineThreshold: [84, [Validators.required, Validators.min(1), Validators.max(100)]],
    amountSpikeMultiplier: [3, [Validators.required, Validators.min(0.1)]],
    amountSpikeWeight: [25, [Validators.required, Validators.min(0), Validators.max(100)]],
    velocityCountThreshold: [4, [Validators.required, Validators.min(1)]],
    velocityWeight: [20, [Validators.required, Validators.min(0), Validators.max(100)]],
    spendBurstMultiplier: [8, [Validators.required, Validators.min(0.1)]],
    spendBurstWeight: [15, [Validators.required, Validators.min(0), Validators.max(100)]],
    newDeviceWeight: [10, [Validators.required, Validators.min(0), Validators.max(100)]],
    impossibleTravelWeight: [25, [Validators.required, Validators.min(0), Validators.max(100)]],
    beneficiaryAgeHoursThreshold: [24, [Validators.required, Validators.min(0)]],
    newBeneficiaryWeight: [15, [Validators.required, Validators.min(0), Validators.max(100)]],
    recentPasswordResetWeight: [15, [Validators.required, Validators.min(0), Validators.max(100)]],
    riskyMerchantCategories: ['ELECTRONICS, CRYPTO, GIFT_CARD, MONEY_TRANSFER', Validators.required],
    riskyMerchantWeight: [12, [Validators.required, Validators.min(0), Validators.max(100)]],
    highRiskCountryWeight: [18, [Validators.required, Validators.min(0), Validators.max(100)]],
    changeSummary: ['Tune rule weights from analyst review patterns while preserving the decline boundary.', Validators.required],
    amount: [12500, Validators.required],
    customerAverageTicket: [1800, Validators.required],
    transactionCountLastFiveMinutes: [5, Validators.required],
    spendLastHour: [24000, Validators.required],
    beneficiaryAgeHours: [1, Validators.required],
    newDevice: true,
    impossibleTravel: true,
    recentPasswordReset: true
  });

  constructor() {
    this.store.load();
  }

  protected compare(): void {
    const value = this.form.getRawValue();
    this.store.compareScenario(
      this.scenario(),
      value.challengeThreshold,
      value.holdThreshold,
      value.declineThreshold,
      this.rules()
    );
  }

  protected save(): void {
    const value = this.form.getRawValue();
    this.store.createProfile({
      profileName: value.profileName,
      challengeThreshold: value.challengeThreshold,
      holdThreshold: value.holdThreshold,
      declineThreshold: value.declineThreshold,
      changeSummary: value.changeSummary,
      rules: this.rules()
    });
  }

  protected activate(profileId: string): void {
    this.store.activateProfile(profileId);
  }

  private rules() {
    const value = this.form.getRawValue();
    return {
      amountSpikeMultiplier: value.amountSpikeMultiplier,
      amountSpikeWeight: value.amountSpikeWeight,
      velocityCountThreshold: value.velocityCountThreshold,
      velocityWeight: value.velocityWeight,
      spendBurstMultiplier: value.spendBurstMultiplier,
      spendBurstWeight: value.spendBurstWeight,
      newDeviceWeight: value.newDeviceWeight,
      impossibleTravelWeight: value.impossibleTravelWeight,
      beneficiaryAgeHoursThreshold: value.beneficiaryAgeHoursThreshold,
      newBeneficiaryWeight: value.newBeneficiaryWeight,
      recentPasswordResetWeight: value.recentPasswordResetWeight,
      riskyMerchantCategories: value.riskyMerchantCategories
        .split(',')
        .map((item) => item.trim().toUpperCase())
        .filter(Boolean),
      riskyMerchantWeight: value.riskyMerchantWeight,
      highRiskCountryWeight: value.highRiskCountryWeight
    };
  }

  private scenario() {
    const value = this.form.getRawValue();
    return {
      paymentId: 'PAY-RULES-STUDIO-1001',
      customerId: 'CUST-RULES-STUDIO-1001',
      amount: value.amount,
      currency: 'ZAR',
      merchantCategory: 'ELECTRONICS',
      paymentChannel: 'MOBILE_APP',
      customerAverageTicket: value.customerAverageTicket,
      transactionCountLastFiveMinutes: value.transactionCountLastFiveMinutes,
      spendLastHour: value.spendLastHour,
      beneficiaryAgeHours: value.beneficiaryAgeHours,
      newDevice: value.newDevice,
      highRiskCountry: false,
      impossibleTravel: value.impossibleTravel,
      recentPasswordReset: value.recentPasswordReset
    };
  }
}
