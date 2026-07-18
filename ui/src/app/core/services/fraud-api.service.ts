import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CompletePaymentChallengeRequest,
  FraudCase,
  FraudCaseFilters,
  FraudOutboundDispatchResponse,
  FraudOperationsSummary,
  FraudOutboundEvent,
  FraudOutboundFilters,
  FraudOutboundIncidentNoteRequest,
  FraudOutboundRetryBatchResponse,
  FraudReplayBatchCreateRequest,
  FraudReplayBatch,
  FraudScoringProfileCreateRequest,
  FraudScoringProfile,
  FraudScoringThresholds,
  FraudSimulationComparisonResponse,
  FraudSimulationResponse,
  FraudOutcome,
  FraudOutcomeRequest,
  FraudQualityMetrics,
  PaymentRiskAssessmentRequest,
  PaymentRiskAssessmentResponse,
  PaymentStatus,
  StepUpDeliveryFilters,
  StepUpDeliveryResponse,
  StepUpRevokeResponse,
  StepUpTokenResponse,
  StepUpVerificationResponse
} from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class FraudApiService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<FraudOperationsSummary> {
    return this.http.get<FraudOperationsSummary>('/api/v1/fraud-operations/summary');
  }

  getQualityMetrics(): Observable<FraudQualityMetrics> {
    return this.http.get<FraudQualityMetrics>('/api/v1/fraud-outcomes/quality-metrics');
  }

  recordOutcome(assessmentId: string, request: FraudOutcomeRequest): Observable<FraudOutcome> {
    return this.http.post<FraudOutcome>(`/api/v1/fraud-outcomes/assessments/${assessmentId}`, request);
  }

  listCases(filters: FraudCaseFilters = {}): Observable<FraudCase[]> {
    return this.http.get<FraudCase[]>('/api/v1/fraud-cases', {
      params: this.toParams(filters as Record<string, unknown>)
    });
  }

  getCase(caseId: string): Observable<FraudCase> {
    return this.http.get<FraudCase>(`/api/v1/fraud-cases/${caseId}`);
  }

  assignCase(caseId: string, assignee: string, note: string): Observable<FraudCase> {
    return this.http.post<FraudCase>(`/api/v1/fraud-cases/${caseId}/assign`, {
      assignee,
      note
    });
  }

  escalateCase(caseId: string, reason: string): Observable<FraudCase> {
    return this.http.post<FraudCase>(`/api/v1/fraud-cases/${caseId}/escalate`, { reason });
  }

  addCaseNote(caseId: string, note: string): Observable<FraudCase> {
    return this.http.post<FraudCase>(`/api/v1/fraud-cases/${caseId}/notes`, { note });
  }

  releaseCase(caseId: string, resolutionSummary: string): Observable<FraudCase> {
    return this.http.post<FraudCase>(`/api/v1/fraud-cases/${caseId}/release`, {
      resolutionSummary
    });
  }

  confirmDecline(caseId: string, resolutionSummary: string): Observable<FraudCase> {
    return this.http.post<FraudCase>(`/api/v1/fraud-cases/${caseId}/confirm-decline`, {
      resolutionSummary
    });
  }

  listPayments(): Observable<PaymentStatus[]> {
    return this.http.get<PaymentStatus[]>('/api/v1/payments');
  }

  getPayment(paymentId: string): Observable<PaymentStatus> {
    return this.http.get<PaymentStatus>(`/api/v1/payments/${paymentId}`);
  }

  completePaymentChallenge(
    paymentId: string,
    request: CompletePaymentChallengeRequest
  ): Observable<PaymentStatus> {
    return this.http.post<PaymentStatus>(
      `/api/v1/payments/${paymentId}/challenge-outcome`,
      request
    );
  }

  simulateAssessment(payload: PaymentRiskAssessmentRequest): Observable<FraudSimulationResponse> {
    return this.http.post<FraudSimulationResponse>('/api/v1/fraud-assessments/simulations', payload);
  }

  compareAssessment(
    payload: PaymentRiskAssessmentRequest,
    thresholds: FraudScoringThresholds
  ): Observable<FraudSimulationComparisonResponse> {
    return this.http.post<FraudSimulationComparisonResponse>(
      '/api/v1/fraud-assessments/simulations/compare',
      {
        scenario: payload,
        overrides: thresholds
      }
    );
  }

  createAssessment(
    payload: PaymentRiskAssessmentRequest
  ): Observable<PaymentRiskAssessmentResponse> {
    return this.http.post<PaymentRiskAssessmentResponse>('/api/v1/fraud-assessments', payload, {
      headers: { 'Idempotency-Key': crypto.randomUUID() }
    });
  }

  listProfiles(): Observable<FraudScoringProfile[]> {
    return this.http.get<FraudScoringProfile[]>('/api/v1/fraud-assessments/scoring-profiles');
  }

  getActiveProfile(): Observable<FraudScoringProfile> {
    return this.http.get<FraudScoringProfile>('/api/v1/fraud-assessments/scoring-profiles/active');
  }

  createProfile(
    request: FraudScoringProfileCreateRequest
  ): Observable<FraudScoringProfile> {
    return this.http.post<FraudScoringProfile>(
      '/api/v1/fraud-assessments/scoring-profiles',
      request
    );
  }

  activateProfile(profileId: string): Observable<FraudScoringProfile> {
    return this.http.post<FraudScoringProfile>(
      `/api/v1/fraud-assessments/scoring-profiles/${profileId}/activate`,
      {}
    );
  }

  compareSavedProfile(
    profileId: string,
    payload: PaymentRiskAssessmentRequest
  ): Observable<FraudSimulationComparisonResponse> {
    return this.http.post<FraudSimulationComparisonResponse>(
      `/api/v1/fraud-assessments/simulations/compare-saved-profile/${profileId}`,
      payload
    );
  }

  listOutboundEvents(filters: FraudOutboundFilters = {}): Observable<FraudOutboundEvent[]> {
    return this.http.get<FraudOutboundEvent[]>('/api/v1/fraud-operations/outbound-events', {
      params: this.toParams(filters as Record<string, unknown>)
    });
  }

  listReplays(): Observable<FraudReplayBatch[]> {
    return this.http.get<FraudReplayBatch[]>('/api/v1/fraud-replays');
  }

  getReplay(batchId: string): Observable<FraudReplayBatch> {
    return this.http.get<FraudReplayBatch>(`/api/v1/fraud-replays/${batchId}`);
  }

  createReplay(request: FraudReplayBatchCreateRequest): Observable<FraudReplayBatch> {
    return this.http.post<FraudReplayBatch>('/api/v1/fraud-replays', request);
  }

  addOutboundIncidentNote(
    eventId: string,
    request: FraudOutboundIncidentNoteRequest
  ): Observable<FraudOutboundEvent> {
    return this.http.post<FraudOutboundEvent>(
      `/api/v1/fraud-operations/outbound-events/${eventId}/incident-note`,
      request
    );
  }

  retryOutboundEvent(eventId: string): Observable<FraudOutboundEvent> {
    return this.http.post<FraudOutboundEvent>(
      `/api/v1/fraud-operations/outbound-events/${eventId}/retry`,
      {}
    );
  }

  retryFailedOutboundEvents(limit: number): Observable<FraudOutboundRetryBatchResponse> {
    return this.http.post<FraudOutboundRetryBatchResponse>(
      `/api/v1/fraud-operations/outbound-events/retry-failed?limit=${limit}`,
      {}
    );
  }

  dispatchOutboundEventsNow(): Observable<FraudOutboundDispatchResponse> {
    return this.http.post<FraudOutboundDispatchResponse>(
      '/api/v1/fraud-operations/outbound-events/dispatch-now',
      {}
    );
  }

  generateStepUpToken(): Observable<StepUpTokenResponse> {
    return this.http.post<StepUpTokenResponse>('/api/v1/security/step-up/token', {});
  }

  resendStepUpToken(): Observable<StepUpTokenResponse> {
    return this.http.post<StepUpTokenResponse>('/api/v1/security/step-up/token/resend', {});
  }

  verifyStepUpToken(token: string): Observable<StepUpVerificationResponse> {
    return this.http.get<StepUpVerificationResponse>(
      `/api/v1/security/step-up/verify`,
      { params: new HttpParams().set('token', token) }
    );
  }

  revokeStepUp(): Observable<StepUpRevokeResponse> {
    return this.http.post<StepUpRevokeResponse>('/api/v1/security/step-up/revoke', {});
  }

  listStepUpDeliveries(filters: StepUpDeliveryFilters = {}): Observable<StepUpDeliveryResponse[]> {
    return this.http.get<StepUpDeliveryResponse[]>('/api/v1/security/step-up/deliveries', {
      params: this.toParams(filters as Record<string, unknown>)
    });
  }

  private toParams(values: Record<string, unknown>): HttpParams {
    let params = new HttpParams();

    for (const [key, value] of Object.entries(values)) {
      if (value === null || value === undefined || value === '') {
        continue;
      }
      params = params.set(key, String(value));
    }

    return params;
  }
}
