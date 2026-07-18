import { inject, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import {
  FraudScoringProfile,
  FraudScoringProfileCreateRequest,
  FraudSimulationComparisonResponse,
  FraudSimulationResponse,
  PaymentRiskAssessmentRequest,
  PaymentRiskAssessmentResponse
} from '../../../shared/models/api.models';

@Injectable()
export class SimulationsStore {
  private readonly api = inject(FraudApiService);

  readonly running = signal(false);
  readonly error = signal('');
  readonly simulation = signal<FraudSimulationResponse | null>(null);
  readonly comparison = signal<FraudSimulationComparisonResponse | null>(null);
  readonly liveAssessment = signal<PaymentRiskAssessmentResponse | null>(null);
  readonly profiles = signal<FraudScoringProfile[]>([]);
  readonly activeProfile = signal<FraudScoringProfile | null>(null);
  readonly profileBusy = signal(false);
  readonly profileMessage = signal('');
  readonly profileError = signal('');

  loadProfiles(): void {
    forkJoin({
      profiles: this.api.listProfiles(),
      activeProfile: this.api.getActiveProfile()
    }).subscribe({
      next: ({ profiles, activeProfile }) => {
        this.profiles.set(profiles);
        this.activeProfile.set(activeProfile);
      },
      error: () => {
        this.profileError.set('Unable to load scoring profiles right now.');
      }
    });
  }

  runSimulation(payload: PaymentRiskAssessmentRequest): void {
    this.running.set(true);
    this.error.set('');
    this.comparison.set(null);
    this.liveAssessment.set(null);

    this.api.simulateAssessment(payload).subscribe({
      next: (response) => {
        this.simulation.set(response);
        this.running.set(false);
      },
      error: () => {
        this.error.set('Unable to run the simulation with the current inputs.');
        this.running.set(false);
      }
    });
  }

  runComparison(
    payload: PaymentRiskAssessmentRequest,
    challengeThreshold: number,
    holdThreshold: number,
    declineThreshold: number
  ): void {
    this.running.set(true);
    this.error.set('');
    this.liveAssessment.set(null);

    this.api
      .compareAssessment(payload, {
        challengeThreshold,
        holdThreshold,
        declineThreshold
      })
      .subscribe({
        next: (response) => {
          this.comparison.set(response);
          this.running.set(false);
        },
        error: () => {
          this.error.set('Unable to compare override thresholds against the active profile.');
          this.running.set(false);
        }
      });
  }

  createAssessment(payload: PaymentRiskAssessmentRequest): void {
    this.running.set(true);
    this.error.set('');

    this.api.createAssessment(payload).subscribe({
      next: (response) => {
        this.liveAssessment.set(response);
        this.running.set(false);
      },
      error: () => {
        this.error.set('Unable to persist the live fraud assessment.');
        this.running.set(false);
      }
    });
  }

  createProfile(request: FraudScoringProfileCreateRequest): void {
    this.profileBusy.set(true);
    this.profileMessage.set('');
    this.profileError.set('');

    this.api.createProfile(request).subscribe({
      next: () => {
        this.profileMessage.set('Draft scoring profile created successfully.');
        this.profileBusy.set(false);
        this.loadProfiles();
      },
      error: () => {
        this.profileError.set('Unable to create the scoring profile.');
        this.profileBusy.set(false);
      }
    });
  }

  activateProfile(profileId: string): void {
    this.profileBusy.set(true);
    this.profileMessage.set('');
    this.profileError.set('');

    this.api.activateProfile(profileId).subscribe({
      next: () => {
        this.profileMessage.set('Scoring profile activated successfully.');
        this.profileBusy.set(false);
        this.loadProfiles();
      },
      error: () => {
        this.profileError.set('Profile activation failed. Complete step-up verification if required and try again.');
        this.profileBusy.set(false);
      }
    });
  }

  compareSavedProfile(profileId: string, payload: PaymentRiskAssessmentRequest): void {
    this.running.set(true);
    this.error.set('');

    this.api.compareSavedProfile(profileId, payload).subscribe({
      next: (response) => {
        this.comparison.set(response);
        this.running.set(false);
      },
      error: () => {
        this.error.set('Unable to compare the selected saved profile against the live thresholds.');
        this.running.set(false);
      }
    });
  }
}
