export interface AuthCredentials {
  username: string;
  password: string;
}

export interface AuthSession {
  username: string;
  authorities: string[];
  organization: {
    organizationId: string;
    slug: string;
    displayName: string;
    planCode: string;
    status: string;
  } | null;
}

export interface FraudOutcomeRequest {
  outcomeLabel: 'CONFIRMED_FRAUD' | 'ACCOUNT_TAKEOVER' | 'CHARGEBACK' | 'GENUINE' | 'CUSTOMER_AUTHORIZED' | 'INCONCLUSIVE';
  source: string;
  actualLoss: number;
  recoveredAmount: number;
  notes: string;
  occurredAt: string | null;
}

export interface FraudOutcome {
  outcomeId: string;
  assessmentId: string;
  outcomeLabel: string;
  source: string;
  actualLoss: number;
  recoveredAmount: number;
  notes: string | null;
  labelledBy: string;
  occurredAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FraudQualityMetrics {
  totalLabelled: number;
  conclusiveLabels: number;
  truePositives: number;
  falsePositives: number;
  falseNegatives: number;
  trueNegatives: number;
  precision: number;
  recall: number;
  falsePositiveRate: number;
  actualLoss: number;
  recoveredAmount: number;
  netLoss: number;
}

export interface RiskFactorView {
  code: string;
  weight: number;
  detail: string;
}

export interface FraudMetricCount {
  key: string;
  count: number;
}

export interface FraudScoringThresholds {
  challengeThreshold: number;
  holdThreshold: number;
  declineThreshold: number;
}

export interface FraudRuleSet {
  amountSpikeMultiplier: number;
  amountSpikeWeight: number;
  velocityCountThreshold: number;
  velocityWeight: number;
  spendBurstMultiplier: number;
  spendBurstWeight: number;
  newDeviceWeight: number;
  impossibleTravelWeight: number;
  beneficiaryAgeHoursThreshold: number;
  newBeneficiaryWeight: number;
  recentPasswordResetWeight: number;
  riskyMerchantCategories: string[];
  riskyMerchantWeight: number;
  highRiskCountryWeight: number;
}

export interface FraudScoringProfileCreateRequest {
  profileName: string;
  challengeThreshold: number;
  holdThreshold: number;
  declineThreshold: number;
  changeSummary: string;
  rules?: FraudRuleSet;
}

export interface FraudScoringProfile {
  profileId: string;
  versionNumber: number;
  profileName: string;
  active: boolean;
  systemDefault: boolean;
  thresholds: FraudScoringThresholds;
  rulesetVersion: string;
  rules: FraudRuleSet;
  changeSummary: string;
  createdBy: string;
  createdAt: string;
  activatedBy: string | null;
  activatedAt: string | null;
}

export interface FraudCaseTimelineEntry {
  entryId: string;
  actionType: string;
  actor: string;
  detail: string;
  createdAt: string;
}

export interface FraudCase {
  caseId: string;
  assessmentId: string;
  paymentId: string;
  customerId: string;
  riskScore: number;
  decision: string;
  status: string;
  summary: string;
  currentAssignee: string | null;
  resolutionSummary: string | null;
  resolutionOutcome: string | null;
  createdAt: string;
  updatedAt: string;
  timelineEntries: FraudCaseTimelineEntry[];
}

export interface PaymentTransition {
  id: string;
  fromStatus: string | null;
  toStatus: string;
  reason: string;
  assessmentId: string | null;
  createdAt: string;
}

export interface PaymentStatus {
  id: string;
  paymentId: string;
  customerId: string;
  amount: number;
  currency: string;
  paymentChannel: string;
  merchantCategory: string;
  latestAssessmentId: string | null;
  latestRiskScore: number;
  latestDecision: string;
  paymentStatus: string;
  challengeOutcome: string | null;
  challengedAt: string | null;
  challengeCompletedAt: string | null;
  challengeCompletedBy: string | null;
  challengeOutcomeNote: string | null;
  createdAt: string;
  updatedAt: string;
  transitions: PaymentTransition[];
}

export interface CompletePaymentChallengeRequest {
  outcome: 'PASSED' | 'FAILED' | 'ABANDONED';
  note: string;
}

export interface PaymentRiskAssessmentRequest {
  paymentId: string;
  customerId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  paymentChannel: string;
  customerAverageTicket: number;
  transactionCountLastFiveMinutes: number;
  spendLastHour: number;
  beneficiaryAgeHours: number;
  newDevice: boolean;
  highRiskCountry: boolean;
  impossibleTravel: boolean;
  recentPasswordReset: boolean;
}

export interface PaymentRiskAssessmentResponse {
  assessmentId: string;
  reviewCaseId: string | null;
  paymentId: string;
  customerId: string;
  riskScore: number;
  decision: string;
  paymentStatus: string;
  velocitySource: string;
  summary: string;
  triggeredFactors: RiskFactorView[];
}

export interface FraudSimulationResponse {
  paymentId: string;
  customerId: string;
  riskScore: number;
  decision: string;
  projectedPaymentStatus: string;
  velocitySource: string;
  reviewCaseWouldBeCreated: boolean;
  summary: string;
  triggeredFactors: RiskFactorView[];
}

export interface FraudSimulationOutcome {
  challengeThreshold: number;
  holdThreshold: number;
  declineThreshold: number;
  riskScore: number;
  decision: string;
  projectedPaymentStatus: string;
  velocitySource: string;
  reviewCaseWouldBeCreated: boolean;
  summary: string;
  triggeredFactors: RiskFactorView[];
}

export interface FraudSimulationComparisonResponse {
  paymentId: string;
  customerId: string;
  baselineOutcome: FraudSimulationOutcome;
  overrideOutcome: FraudSimulationOutcome;
  decisionChanged: boolean;
  projectedPaymentStatusChanged: boolean;
  reviewCaseCreationChanged: boolean;
  comparisonSummary: string;
}

export interface FraudReplayBatchItem {
  id: string;
  scenarioIndex: number;
  paymentId: string;
  customerId: string;
  riskScore: number;
  decision: string;
  projectedPaymentStatus: string;
  reviewCaseWouldBeCreated: boolean;
  summary: string;
  triggeredFactorCodes: string[];
  velocitySource: string;
  createdAt: string;
}

export interface FraudReplayBatch {
  batchId: string;
  batchName: string;
  scenarioCount: number;
  thresholds: FraudScoringThresholds;
  createdBy: string;
  createdAt: string;
  decisions: FraudMetricCount[];
  projectedPaymentStatuses: FraudMetricCount[];
  reviewCaseWouldBeCreatedCount: number;
  items: FraudReplayBatchItem[];
}

export interface FraudReplayBatchCreateRequest {
  batchName: string;
  overrides: FraudScoringThresholds;
  scenarios: PaymentRiskAssessmentRequest[];
}

export interface FraudOutboundEvent {
  eventId: string;
  eventType: string;
  topicName: string;
  messageKey: string;
  status: string;
  attemptCount: number;
  nextAttemptAt: string | null;
  lastAttemptedAt: string | null;
  publishedAt: string | null;
  lastError: string | null;
  operatorNote: string | null;
  notedBy: string | null;
  notedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FraudOutboundIncidentNoteRequest {
  note: string;
}

export interface FraudOutboundRetryBatchResponse {
  requested: number;
  queuedForRetry: number;
}

export interface FraudOutboundDispatchResponse {
  processedCount: number;
}

export interface StepUpTokenResponse {
  deliveryId: string;
  expiresAt: string;
  deliveryChannel: string;
  destinationMasked: string;
  verificationUrl: string | null;
}

export interface StepUpVerificationResponse {
  operator: string;
  verifiedAt: string;
  validUntil: string;
}

export interface StepUpDeliveryResponse {
  deliveryId: string;
  operator: string;
  deliveryChannel: string;
  destinationMasked: string;
  status: string;
  resendSequence: number;
  attemptCount: number;
  failureReason: string | null;
  createdAt: string;
  expiresAt: string;
  deliveredAt: string | null;
  consumedAt: string | null;
  revokedAt: string | null;
}

export interface StepUpRevokeResponse {
  revokedCount: number;
  revokedAt: string;
}

export interface FraudChallengeOutcomeSummary {
  pendingCount?: number;
  passedCount?: number;
  failedCount?: number;
  abandonedCount?: number;
}

export interface FraudOutboundDeliverySummary {
  pendingCount?: number;
  deliveredCount?: number;
  failedCount?: number;
}

export interface FraudOperationsSummary {
  totalAssessments: number;
  totalTrackedPayments: number;
  totalReviewCases: number;
  distinctCustomersAssessed: number;
  averageRiskScore: number;
  reviewBacklogCount: number;
  challengeOutcomeSummary?: FraudChallengeOutcomeSummary;
  reviewerAnalytics?: Record<string, unknown>;
  outboundDeliverySummary?: FraudOutboundDeliverySummary;
  assessmentsByDecision: FraudMetricCount[];
  assessmentsByVelocitySource: FraudMetricCount[];
  paymentsByStatus: FraudMetricCount[];
  casesByStatus: FraudMetricCount[];
  resolutionsByOutcome: FraudMetricCount[];
}

export interface FraudCaseFilters {
  status?: string;
  assignee?: string;
  minRiskScore?: number | null;
  maxRiskScore?: number | null;
  breachedOnly?: boolean;
  unassignedOnly?: boolean;
  paymentId?: string;
  customerId?: string;
}

export interface FraudOutboundFilters {
  status?: string;
  topicName?: string;
  eventType?: string;
  messageKey?: string;
  onlyWithNotes?: boolean;
  limit?: number;
}

export interface StepUpDeliveryFilters {
  operator?: string;
  status?: string;
  limit?: number;
}
