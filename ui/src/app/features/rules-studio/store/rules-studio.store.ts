import { inject, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import {
  FraudRuleSet,
  FraudScoringProfile,
  FraudScoringProfileCreateRequest,
  FraudSimulationComparisonResponse,
  PaymentRiskAssessmentRequest
} from '../../../shared/models/api.models';

@Injectable()
export class RulesStudioStore {
  private readonly api = inject(FraudApiService);

  readonly loading = signal(false);
  readonly running = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly profiles = signal<FraudScoringProfile[]>([]);
  readonly activeProfile = signal<FraudScoringProfile | null>(null);
  readonly comparison = signal<FraudSimulationComparisonResponse | null>(null);

  load(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      profiles: this.api.listProfiles(),
      activeProfile: this.api.getActiveProfile()
    }).subscribe({
      next: ({ profiles, activeProfile }) => {
        this.profiles.set(profiles);
        this.activeProfile.set(activeProfile);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load scoring policy profiles.');
        this.loading.set(false);
      }
    });
  }

  compareScenario(
    scenario: PaymentRiskAssessmentRequest,
    challengeThreshold: number,
    holdThreshold: number,
    declineThreshold: number,
    rules: FraudRuleSet
  ): void {
    this.running.set(true);
    this.error.set('');

    this.api.compareAssessment(scenario, {
        challengeThreshold,
        holdThreshold,
        declineThreshold
      }, rules).subscribe({
      next: (response) => {
        this.comparison.set(response);
        this.running.set(false);
      },
      error: () => {
        this.error.set('Unable to compare this policy against the active profile.');
        this.running.set(false);
      }
    });
  }

  createProfile(request: FraudScoringProfileCreateRequest): void {
    this.saving.set(true);
    this.message.set('');
    this.error.set('');

    this.api.createProfile(request).subscribe({
      next: () => {
        this.message.set('Draft risk policy saved.');
        this.saving.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Unable to save the risk policy draft.');
        this.saving.set(false);
      }
    });
  }

  activateProfile(profileId: string): void {
    this.saving.set(true);
    this.message.set('');
    this.error.set('');

    this.api.activateProfile(profileId).subscribe({
      next: () => {
        this.message.set('Risk policy activated.');
        this.saving.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Policy activation failed. Step-up verification may be required.');
        this.saving.set(false);
      }
    });
  }

  rulesOrDefaults(): FraudRuleSet {
    return this.activeProfile()?.rules ?? {
      amountSpikeMultiplier: 3,
      amountSpikeWeight: 25,
      velocityCountThreshold: 4,
      velocityWeight: 20,
      spendBurstMultiplier: 8,
      spendBurstWeight: 15,
      newDeviceWeight: 10,
      impossibleTravelWeight: 25,
      beneficiaryAgeHoursThreshold: 24,
      newBeneficiaryWeight: 15,
      recentPasswordResetWeight: 15,
      riskyMerchantCategories: ['ELECTRONICS', 'CRYPTO', 'GIFT_CARD', 'MONEY_TRANSFER'],
      riskyMerchantWeight: 12,
      highRiskCountryWeight: 18
    };
  }
}
