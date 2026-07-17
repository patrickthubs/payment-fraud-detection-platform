package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.FraudOperationsSummaryResponse;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudCaseTimelineEntryRepository;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import com.frauddetection.platform.repository.PaymentRecordRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudOperationsSummaryServiceTest {

    private FraudAssessmentRecordRepository fraudAssessmentRecordRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private FraudReviewCaseRepository fraudReviewCaseRepository;
    private FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;
    private FraudOutboundEventRepository fraudOutboundEventRepository;
    private FraudOperationsSummaryService fraudOperationsSummaryService;

    @BeforeEach
    void setUp() {
        fraudAssessmentRecordRepository = mock(FraudAssessmentRecordRepository.class);
        paymentRecordRepository = mock(PaymentRecordRepository.class);
        fraudReviewCaseRepository = mock(FraudReviewCaseRepository.class);
        fraudCaseTimelineEntryRepository = mock(FraudCaseTimelineEntryRepository.class);
        fraudOutboundEventRepository = mock(FraudOutboundEventRepository.class);
        fraudOperationsSummaryService = new FraudOperationsSummaryService(
            fraudAssessmentRecordRepository,
            paymentRecordRepository,
            fraudReviewCaseRepository,
            fraudCaseTimelineEntryRepository,
            fraudOutboundEventRepository,
            Clock.fixed(Instant.parse("2026-07-17T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void buildsOperationsSummaryFromRepositoryAggregates() {
        when(fraudAssessmentRecordRepository.count()).thenReturn(12L);
        when(paymentRecordRepository.count()).thenReturn(10L);
        when(fraudReviewCaseRepository.count()).thenReturn(6L);
        when(fraudAssessmentRecordRepository.countDistinctCustomerIds()).thenReturn(8L);
        when(fraudAssessmentRecordRepository.averageRiskScore()).thenReturn(63.333d);
        when(fraudReviewCaseRepository.countByStatusIn(List.of(ReviewCaseStatus.OPEN, ReviewCaseStatus.ESCALATED)))
            .thenReturn(4L);
        when(fraudReviewCaseRepository.findAll()).thenReturn(List.of(
            buildCase("analyst.one", ReviewCaseStatus.OPEN, null, "2026-07-17T08:00:00Z", "2026-07-17T08:30:00Z"),
            buildCase("analyst.one", ReviewCaseStatus.OPEN, null, "2026-07-16T04:00:00Z", "2026-07-16T04:30:00Z"),
            buildCase("analyst.one", ReviewCaseStatus.RESOLVED, CaseResolutionOutcome.RELEASE_PAYMENT, "2026-07-16T08:00:00Z", "2026-07-16T18:00:00Z"),
            buildCase("senior.analyst", ReviewCaseStatus.ESCALATED, null, "2026-07-17T05:00:00Z", "2026-07-17T06:00:00Z"),
            buildCase("senior.analyst", ReviewCaseStatus.RESOLVED, CaseResolutionOutcome.CONFIRM_DECLINE, "2026-07-15T10:00:00Z", "2026-07-16T16:00:00Z"),
            buildCase(null, ReviewCaseStatus.OPEN, null, "2026-07-16T02:00:00Z", "2026-07-16T03:00:00Z")
        ));
        when(fraudOutboundEventRepository.countByStatus(FraudOutboundEventStatus.PENDING)).thenReturn(6L);
        when(fraudOutboundEventRepository.countByStatus(FraudOutboundEventStatus.DELIVERED)).thenReturn(19L);
        when(fraudOutboundEventRepository.countByStatus(FraudOutboundEventStatus.FAILED)).thenReturn(1L);

        FraudAssessmentRecordRepository.DecisionCountView decisionCount = mock(FraudAssessmentRecordRepository.DecisionCountView.class);
        when(decisionCount.getDecision()).thenReturn(RiskDecision.HOLD);
        when(decisionCount.getTotal()).thenReturn(5L);
        when(fraudAssessmentRecordRepository.countGroupedByDecision()).thenReturn(List.of(decisionCount));

        FraudAssessmentRecordRepository.VelocitySourceCountView velocityCount = mock(FraudAssessmentRecordRepository.VelocitySourceCountView.class);
        when(velocityCount.getVelocitySource()).thenReturn(VelocitySource.REDIS);
        when(velocityCount.getTotal()).thenReturn(7L);
        when(fraudAssessmentRecordRepository.countGroupedByVelocitySource()).thenReturn(List.of(velocityCount));

        PaymentRecordRepository.PaymentStatusCountView paymentStatusCount = mock(PaymentRecordRepository.PaymentStatusCountView.class);
        when(paymentStatusCount.getPaymentStatus()).thenReturn(PaymentStatus.HELD);
        when(paymentStatusCount.getTotal()).thenReturn(2L);
        when(paymentRecordRepository.countGroupedByPaymentStatus()).thenReturn(List.of(paymentStatusCount));
        when(paymentRecordRepository.countByLatestDecision(RiskDecision.CHALLENGE)).thenReturn(5L);
        when(paymentRecordRepository.countByPaymentStatus(PaymentStatus.CHALLENGED)).thenReturn(1L);
        when(paymentRecordRepository.countByChallengeOutcome(ChallengeOutcome.PASSED)).thenReturn(2L);
        when(paymentRecordRepository.countByChallengeOutcome(ChallengeOutcome.FAILED)).thenReturn(1L);
        when(paymentRecordRepository.countByChallengeOutcome(ChallengeOutcome.ABANDONED)).thenReturn(1L);
        when(paymentRecordRepository.averageRiskScoreByChallengeOutcome(ChallengeOutcome.PASSED)).thenReturn(44d);
        when(paymentRecordRepository.averageRiskScoreByChallengeOutcome(ChallengeOutcome.ABANDONED)).thenReturn(61d);

        PaymentRecordRepository.ChallengeOutcomeCountView challengeOutcomeCount = mock(PaymentRecordRepository.ChallengeOutcomeCountView.class);
        when(challengeOutcomeCount.getChallengeOutcome()).thenReturn(ChallengeOutcome.PASSED);
        when(challengeOutcomeCount.getTotal()).thenReturn(2L);
        when(paymentRecordRepository.countGroupedByChallengeOutcome()).thenReturn(List.of(challengeOutcomeCount));

        FraudCaseTimelineEntryRepository.ActorActionCountView assignmentActionCount = mock(FraudCaseTimelineEntryRepository.ActorActionCountView.class);
        when(assignmentActionCount.getActor()).thenReturn("lead.analyst");
        when(assignmentActionCount.getActionType()).thenReturn(FraudCaseActionType.ASSIGNED);
        when(assignmentActionCount.getTotal()).thenReturn(3L);

        FraudCaseTimelineEntryRepository.ActorActionCountView noteActionCount = mock(FraudCaseTimelineEntryRepository.ActorActionCountView.class);
        when(noteActionCount.getActor()).thenReturn("analyst.one");
        when(noteActionCount.getActionType()).thenReturn(FraudCaseActionType.NOTE_ADDED);
        when(noteActionCount.getTotal()).thenReturn(4L);

        FraudCaseTimelineEntryRepository.ActorActionCountView resolutionActionCount = mock(FraudCaseTimelineEntryRepository.ActorActionCountView.class);
        when(resolutionActionCount.getActor()).thenReturn("senior.analyst");
        when(resolutionActionCount.getActionType()).thenReturn(FraudCaseActionType.RESOLVED);
        when(resolutionActionCount.getTotal()).thenReturn(2L);

        when(fraudCaseTimelineEntryRepository.countGroupedByActorAndActionType()).thenReturn(
            List.of(assignmentActionCount, noteActionCount, resolutionActionCount)
        );

        FraudReviewCaseRepository.CaseStatusCountView caseStatusCount = mock(FraudReviewCaseRepository.CaseStatusCountView.class);
        when(caseStatusCount.getStatus()).thenReturn(ReviewCaseStatus.ESCALATED);
        when(caseStatusCount.getTotal()).thenReturn(1L);
        when(fraudReviewCaseRepository.countGroupedByStatus()).thenReturn(List.of(caseStatusCount));

        FraudReviewCaseRepository.ResolutionOutcomeCountView resolutionCount = mock(FraudReviewCaseRepository.ResolutionOutcomeCountView.class);
        when(resolutionCount.getResolutionOutcome()).thenReturn(CaseResolutionOutcome.CONFIRM_DECLINE);
        when(resolutionCount.getTotal()).thenReturn(2L);
        when(fraudReviewCaseRepository.countGroupedByResolutionOutcome()).thenReturn(List.of(resolutionCount));

        FraudOperationsSummaryResponse response = fraudOperationsSummaryService.getSummary();

        assertThat(response.totalAssessments()).isEqualTo(12);
        assertThat(response.totalTrackedPayments()).isEqualTo(10);
        assertThat(response.totalReviewCases()).isEqualTo(6);
        assertThat(response.distinctCustomersAssessed()).isEqualTo(8);
        assertThat(response.reviewBacklogCount()).isEqualTo(4);
        assertThat(response.averageRiskScore()).isEqualByComparingTo(new BigDecimal("63.33"));
        assertThat(response.challengeOutcomeSummary().challengedPayments()).isEqualTo(5);
        assertThat(response.challengeOutcomeSummary().pendingChallenges()).isEqualTo(1);
        assertThat(response.challengeOutcomeSummary().completedChallenges()).isEqualTo(4);
        assertThat(response.challengeOutcomeSummary().completionRate()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.challengeOutcomeSummary().passRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.challengeOutcomeSummary().abandonmentRate()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.reviewerAnalytics().turnaround().openCaseCount()).isEqualTo(3);
        assertThat(response.reviewerAnalytics().turnaround().escalatedCaseCount()).isEqualTo(1);
        assertThat(response.reviewerAnalytics().turnaround().resolvedCaseCount()).isEqualTo(2);
        assertThat(response.reviewerAnalytics().turnaround().averageResolutionHours()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(response.reviewerAnalytics().turnaround().resolvedWithin24HoursRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.reviewerAnalytics().turnaround().sla().targetHours()).isEqualByComparingTo(new BigDecimal("24.00"));
        assertThat(response.reviewerAnalytics().turnaround().sla().breachedBacklogCount()).isEqualTo(2);
        assertThat(response.reviewerAnalytics().turnaround().sla().breachRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.reviewerAnalytics().turnaround().sla().backlogAgingBuckets()).anySatisfy(bucket -> {
            assertThat(bucket.bucket()).isEqualTo("0-4h");
            assertThat(bucket.totalCount()).isEqualTo(1);
            assertThat(bucket.openCount()).isEqualTo(1);
            assertThat(bucket.escalatedCount()).isEqualTo(0);
            assertThat(bucket.breached()).isFalse();
        });
        assertThat(response.reviewerAnalytics().turnaround().sla().backlogAgingBuckets()).anySatisfy(bucket -> {
            assertThat(bucket.bucket()).isEqualTo("4-12h");
            assertThat(bucket.totalCount()).isEqualTo(1);
            assertThat(bucket.openCount()).isEqualTo(0);
            assertThat(bucket.escalatedCount()).isEqualTo(1);
            assertThat(bucket.breached()).isFalse();
        });
        assertThat(response.reviewerAnalytics().turnaround().sla().backlogAgingBuckets()).anySatisfy(bucket -> {
            assertThat(bucket.bucket()).isEqualTo("24-48h");
            assertThat(bucket.totalCount()).isEqualTo(2);
            assertThat(bucket.breached()).isTrue();
        });
        assertThat(response.reviewerAnalytics().reviewers()).anySatisfy(item -> {
            assertThat(item.reviewer()).isEqualTo("analyst.one");
            assertThat(item.assignedOpenCases()).isEqualTo(2);
            assertThat(item.resolvedCases()).isEqualTo(1);
            assertThat(item.releasedPayments()).isEqualTo(1);
            assertThat(item.noteActions()).isEqualTo(4);
        });
        assertThat(response.reviewerAnalytics().reviewers()).anySatisfy(item -> {
            assertThat(item.reviewer()).isEqualTo("senior.analyst");
            assertThat(item.assignedEscalatedCases()).isEqualTo(1);
            assertThat(item.confirmedDeclines()).isEqualTo(1);
            assertThat(item.resolutionActions()).isEqualTo(2);
        });
        assertThat(response.reviewerAnalytics().drilldown()).anySatisfy(item -> {
            assertThat(item.reviewer()).isEqualTo("analyst.one");
            assertThat(item.assignedBacklogCount()).isEqualTo(2);
            assertThat(item.breachedBacklogCount()).isEqualTo(1);
            assertThat(item.oldestAssignedBacklogAgeHours()).isEqualByComparingTo(new BigDecimal("30.00"));
        });
        assertThat(response.reviewerAnalytics().drilldown()).anySatisfy(item -> {
            assertThat(item.reviewer()).isEqualTo("senior.analyst");
            assertThat(item.assignedBacklogCount()).isEqualTo(1);
            assertThat(item.assignedEscalatedCases()).isEqualTo(1);
        });
        assertThat(response.reviewerAnalytics().workloadRecommendations()).anySatisfy(item -> {
            assertThat(item.recommendationType()).isEqualTo("ASSIGN_UNOWNED_BACKLOG");
            assertThat(item.priority()).isEqualTo("HIGH");
            assertThat(item.affectedCaseCount()).isEqualTo(1);
        });
        assertThat(response.reviewerAnalytics().workloadRecommendations()).anySatisfy(item -> {
            assertThat(item.recommendationType()).isEqualTo("REBALANCE_BREACHED_QUEUE");
            assertThat(item.sourceReviewer()).isEqualTo("analyst.one");
            assertThat(item.affectedCaseCount()).isEqualTo(1);
        });
        assertThat(response.reviewerAnalytics().priorityRoutingHints()).anySatisfy(item -> {
            assertThat(item.recommendedQueue()).isEqualTo("CLAIM_AND_ASSIGN");
            assertThat(item.priorityBand()).isEqualTo("HIGH");
            assertThat(item.affectedCaseCount()).isEqualTo(1);
            assertThat(item.unassignedCaseCount()).isEqualTo(1);
        });
        assertThat(response.reviewerAnalytics().priorityRoutingHints()).anySatisfy(item -> {
            assertThat(item.recommendedQueue()).isEqualTo("CLAIM_AND_ASSIGN");
            assertThat(item.breachedCaseCount()).isEqualTo(1);
            assertThat(item.affectedCaseCount()).isEqualTo(1);
        });
        assertThat(response.reviewerAnalytics().supervisorInterventions()).anySatisfy(item -> {
            assertThat(item.interventionType()).isEqualTo("CLAIM_BREACHED_UNOWNED_CASES");
            assertThat(item.severity()).isEqualTo("CRITICAL");
            assertThat(item.affectedCaseCount()).isEqualTo(1);
        });
        assertThat(response.reviewerAnalytics().supervisorInterventions()).anySatisfy(item -> {
            assertThat(item.interventionType()).isEqualTo("INTERVENE_ON_REVIEWER_BACKLOG");
            assertThat(item.reviewer()).isEqualTo("analyst.one");
            assertThat(item.affectedCaseCount()).isEqualTo(1);
        });
        assertThat(response.outboundDeliverySummary().pendingCount()).isEqualTo(6);
        assertThat(response.outboundDeliverySummary().deliveredCount()).isEqualTo(19);
        assertThat(response.outboundDeliverySummary().failedCount()).isEqualTo(1);
        assertThat(response.assessmentsByDecision()).singleElement().satisfies(item -> {
            assertThat(item.label()).isEqualTo("HOLD");
            assertThat(item.total()).isEqualTo(5);
        });
        assertThat(response.assessmentsByVelocitySource()).singleElement().satisfies(item -> {
            assertThat(item.label()).isEqualTo("REDIS");
            assertThat(item.total()).isEqualTo(7);
        });
        assertThat(response.paymentsByStatus()).singleElement().satisfies(item -> {
            assertThat(item.label()).isEqualTo("HELD");
            assertThat(item.total()).isEqualTo(2);
        });
        assertThat(response.casesByStatus()).singleElement().satisfies(item -> {
            assertThat(item.label()).isEqualTo("ESCALATED");
            assertThat(item.total()).isEqualTo(1);
        });
        assertThat(response.resolutionsByOutcome()).singleElement().satisfies(item -> {
            assertThat(item.label()).isEqualTo("CONFIRM_DECLINE");
            assertThat(item.total()).isEqualTo(2);
        });
    }

    private FraudReviewCaseEntity buildCase(
        String assignee,
        ReviewCaseStatus status,
        CaseResolutionOutcome resolutionOutcome,
        String createdAt,
        String updatedAt
    ) {
        return new FraudReviewCaseEntity(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID(),
            "PAY-" + assignee + "-" + status.name(),
            "CUST-" + assignee,
            75,
            RiskDecision.HOLD,
            status,
            "Case summary",
            assignee,
            resolutionOutcome == null ? null : "Resolved",
            resolutionOutcome,
            Instant.parse(createdAt),
            Instant.parse(updatedAt)
        );
    }
}
