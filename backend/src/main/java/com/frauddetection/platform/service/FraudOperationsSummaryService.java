package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudChallengeOutcomeSummaryResponse;
import com.frauddetection.platform.dto.FraudBacklogAgingBucketResponse;
import com.frauddetection.platform.dto.FraudCaseSlaSummaryResponse;
import com.frauddetection.platform.dto.FraudCasePriorityRoutingHintResponse;
import com.frauddetection.platform.dto.FraudCaseTurnaroundSummaryResponse;
import com.frauddetection.platform.dto.FraudMetricCountResponse;
import com.frauddetection.platform.dto.FraudOutboundDeliverySummaryResponse;
import com.frauddetection.platform.dto.FraudOperationsSummaryResponse;
import com.frauddetection.platform.dto.FraudReviewerAnalyticsSummaryResponse;
import com.frauddetection.platform.dto.FraudReviewerDrilldownResponse;
import com.frauddetection.platform.dto.FraudReviewerProductivityResponse;
import com.frauddetection.platform.dto.FraudSupervisorInterventionResponse;
import com.frauddetection.platform.dto.FraudWorkloadRecommendationResponse;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudCaseTimelineEntryRepository;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import com.frauddetection.platform.repository.PaymentRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudOperationsSummaryService {

    private static final Duration REVIEW_BACKLOG_SLA = Duration.ofHours(24);

    private final FraudAssessmentRecordRepository fraudAssessmentRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final FraudReviewCaseRepository fraudReviewCaseRepository;
    private final FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;
    private final FraudOutboundEventRepository fraudOutboundEventRepository;
    private final Clock clock;

    public FraudOperationsSummaryService(
        FraudAssessmentRecordRepository fraudAssessmentRecordRepository,
        PaymentRecordRepository paymentRecordRepository,
        FraudReviewCaseRepository fraudReviewCaseRepository,
        FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository,
        FraudOutboundEventRepository fraudOutboundEventRepository,
        Clock clock
    ) {
        this.fraudAssessmentRecordRepository = fraudAssessmentRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.fraudReviewCaseRepository = fraudReviewCaseRepository;
        this.fraudCaseTimelineEntryRepository = fraudCaseTimelineEntryRepository;
        this.fraudOutboundEventRepository = fraudOutboundEventRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FraudOperationsSummaryResponse getSummary() {
        long totalAssessments = fraudAssessmentRecordRepository.count();
        long totalTrackedPayments = paymentRecordRepository.count();
        long totalReviewCases = fraudReviewCaseRepository.count();
        long distinctCustomersAssessed = fraudAssessmentRecordRepository.countDistinctCustomerIds();
        BigDecimal averageRiskScore = BigDecimal.valueOf(fraudAssessmentRecordRepository.averageRiskScore())
            .setScale(2, RoundingMode.HALF_UP);

        long reviewBacklogCount = fraudReviewCaseRepository.countByStatusIn(
            List.of(ReviewCaseStatus.OPEN, ReviewCaseStatus.ESCALATED)
        );
        FraudChallengeOutcomeSummaryResponse challengeOutcomeSummary = buildChallengeOutcomeSummary();
        FraudReviewerAnalyticsSummaryResponse reviewerAnalytics = buildReviewerAnalytics();

        return new FraudOperationsSummaryResponse(
            totalAssessments,
            totalTrackedPayments,
            totalReviewCases,
            distinctCustomersAssessed,
            averageRiskScore,
            reviewBacklogCount,
            challengeOutcomeSummary,
            reviewerAnalytics,
            new FraudOutboundDeliverySummaryResponse(
                fraudOutboundEventRepository.countByStatus(FraudOutboundEventStatus.PENDING),
                fraudOutboundEventRepository.countByStatus(FraudOutboundEventStatus.DELIVERED),
                fraudOutboundEventRepository.countByStatus(FraudOutboundEventStatus.FAILED)
            ),
            fraudAssessmentRecordRepository.countGroupedByDecision().stream()
                .map(row -> new FraudMetricCountResponse(row.getDecision().name(), row.getTotal()))
                .toList(),
            fraudAssessmentRecordRepository.countGroupedByVelocitySource().stream()
                .map(row -> new FraudMetricCountResponse(row.getVelocitySource().name(), row.getTotal()))
                .toList(),
            paymentRecordRepository.countGroupedByPaymentStatus().stream()
                .map(row -> new FraudMetricCountResponse(row.getPaymentStatus().name(), row.getTotal()))
                .toList(),
            fraudReviewCaseRepository.countGroupedByStatus().stream()
                .map(row -> new FraudMetricCountResponse(row.getStatus().name(), row.getTotal()))
                .toList(),
            fraudReviewCaseRepository.countGroupedByResolutionOutcome().stream()
                .map(row -> new FraudMetricCountResponse(row.getResolutionOutcome().name(), row.getTotal()))
                .toList()
        );
    }

    private FraudChallengeOutcomeSummaryResponse buildChallengeOutcomeSummary() {
        long challengedPayments = paymentRecordRepository.countByLatestDecision(RiskDecision.CHALLENGE);
        long pendingChallenges = paymentRecordRepository.countByPaymentStatus(PaymentStatus.CHALLENGED);
        long passedChallenges = paymentRecordRepository.countByChallengeOutcome(ChallengeOutcome.PASSED);
        long failedChallenges = paymentRecordRepository.countByChallengeOutcome(ChallengeOutcome.FAILED);
        long abandonedChallenges = paymentRecordRepository.countByChallengeOutcome(ChallengeOutcome.ABANDONED);
        long completedChallenges = passedChallenges + failedChallenges + abandonedChallenges;

        return new FraudChallengeOutcomeSummaryResponse(
            challengedPayments,
            pendingChallenges,
            completedChallenges,
            passedChallenges,
            failedChallenges,
            abandonedChallenges,
            rate(completedChallenges, challengedPayments),
            rate(passedChallenges, completedChallenges),
            rate(abandonedChallenges, completedChallenges),
            averageRiskScore(ChallengeOutcome.PASSED),
            averageRiskScore(ChallengeOutcome.ABANDONED),
            paymentRecordRepository.countGroupedByChallengeOutcome().stream()
                .map(row -> new FraudMetricCountResponse(row.getChallengeOutcome().name(), row.getTotal()))
                .toList()
        );
    }

    private BigDecimal averageRiskScore(ChallengeOutcome challengeOutcome) {
        return BigDecimal.valueOf(paymentRecordRepository.averageRiskScoreByChallengeOutcome(challengeOutcome))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private FraudReviewerAnalyticsSummaryResponse buildReviewerAnalytics() {
        List<FraudReviewCaseEntity> cases = fraudReviewCaseRepository.findAll();
        Instant now = Instant.now(clock);
        List<FraudReviewCaseEntity> backlogCases = cases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.OPEN || caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
            .toList();

        FraudCaseTurnaroundSummaryResponse turnaround = buildTurnaroundSummary(cases, now);
        List<FraudReviewerProductivityResponse> reviewers = buildReviewerProductivity(cases);
        List<FraudReviewerDrilldownResponse> drilldown = buildReviewerDrilldown(cases, backlogCases, now);
        List<FraudWorkloadRecommendationResponse> workloadRecommendations = buildWorkloadRecommendations(drilldown, backlogCases);
        List<FraudCasePriorityRoutingHintResponse> priorityRoutingHints = buildPriorityRoutingHints(backlogCases, now);
        List<FraudSupervisorInterventionResponse> supervisorInterventions = buildSupervisorInterventions(
            backlogCases,
            drilldown,
            priorityRoutingHints,
            now
        );
        return new FraudReviewerAnalyticsSummaryResponse(
            turnaround,
            reviewers,
            drilldown,
            workloadRecommendations,
            priorityRoutingHints,
            supervisorInterventions
        );
    }

    private FraudCaseTurnaroundSummaryResponse buildTurnaroundSummary(List<FraudReviewCaseEntity> cases, Instant now) {
        List<FraudReviewCaseEntity> openCases = cases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.OPEN)
            .toList();
        List<FraudReviewCaseEntity> escalatedCases = cases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
            .toList();
        List<FraudReviewCaseEntity> backlogCases = cases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.OPEN || caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
            .toList();
        List<FraudReviewCaseEntity> resolvedCases = cases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.RESOLVED)
            .toList();

        long resolvedWithin24Hours = resolvedCases.stream()
            .filter(caseEntity -> hoursBetween(caseEntity.getCreatedAt(), caseEntity.getUpdatedAt()).compareTo(BigDecimal.valueOf(24)) <= 0)
            .count();

        return new FraudCaseTurnaroundSummaryResponse(
            openCases.size(),
            escalatedCases.size(),
            resolvedCases.size(),
            averageHours(resolvedCases.stream()
                .map(caseEntity -> Duration.between(caseEntity.getCreatedAt(), caseEntity.getUpdatedAt()))
                .toList()),
            averageHours(backlogCases.stream()
                .map(caseEntity -> Duration.between(caseEntity.getCreatedAt(), now))
                .toList()),
            maxHours(backlogCases.stream()
                .map(caseEntity -> Duration.between(caseEntity.getCreatedAt(), now))
                .toList()),
            rate(resolvedWithin24Hours, resolvedCases.size()),
            buildSlaSummary(backlogCases, now)
        );
    }

    private FraudCaseSlaSummaryResponse buildSlaSummary(List<FraudReviewCaseEntity> backlogCases, Instant now) {
        long breachedBacklogCount = backlogCases.stream()
            .filter(caseEntity -> isSlaBreached(caseEntity, now))
            .count();
        long breachedOpenCaseCount = backlogCases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.OPEN)
            .filter(caseEntity -> isSlaBreached(caseEntity, now))
            .count();
        long breachedEscalatedCaseCount = backlogCases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
            .filter(caseEntity -> isSlaBreached(caseEntity, now))
            .count();

        return new FraudCaseSlaSummaryResponse(
            now,
            BigDecimal.valueOf(REVIEW_BACKLOG_SLA.toHours()).setScale(2, RoundingMode.HALF_UP),
            breachedBacklogCount,
            breachedOpenCaseCount,
            breachedEscalatedCaseCount,
            rate(breachedBacklogCount, backlogCases.size()),
            List.of(
                toBacklogBucket("0-4h", Duration.ZERO, Duration.ofHours(4), backlogCases, now),
                toBacklogBucket("4-12h", Duration.ofHours(4), Duration.ofHours(12), backlogCases, now),
                toBacklogBucket("12-24h", Duration.ofHours(12), REVIEW_BACKLOG_SLA, backlogCases, now),
                toBacklogBucket("24-48h", REVIEW_BACKLOG_SLA, Duration.ofHours(48), backlogCases, now),
                toBacklogBucket("48h+", Duration.ofHours(48), null, backlogCases, now)
            )
        );
    }

    private List<FraudReviewerProductivityResponse> buildReviewerProductivity(List<FraudReviewCaseEntity> cases) {
        Map<String, ReviewerAggregation> aggregations = new HashMap<>();

        cases.stream()
            .filter(caseEntity -> caseEntity.getCurrentAssignee() != null && !caseEntity.getCurrentAssignee().isBlank())
            .forEach(caseEntity -> {
                ReviewerAggregation aggregation = aggregations.computeIfAbsent(
                    caseEntity.getCurrentAssignee(),
                    ignored -> new ReviewerAggregation()
                );
                switch (caseEntity.getStatus()) {
                    case OPEN -> aggregation.assignedOpenCases++;
                    case ESCALATED -> aggregation.assignedEscalatedCases++;
                    case RESOLVED -> {
                        aggregation.resolvedCases++;
                        aggregation.totalResolutionMinutes += Duration.between(caseEntity.getCreatedAt(), caseEntity.getUpdatedAt()).toMinutes();
                        if (caseEntity.getResolutionOutcome() == CaseResolutionOutcome.RELEASE_PAYMENT) {
                            aggregation.releasedPayments++;
                        }
                        if (caseEntity.getResolutionOutcome() == CaseResolutionOutcome.CONFIRM_DECLINE) {
                            aggregation.confirmedDeclines++;
                        }
                    }
                }
            });

        fraudCaseTimelineEntryRepository.countGroupedByActorAndActionType().forEach(view -> {
            ReviewerAggregation aggregation = aggregations.computeIfAbsent(view.getActor(), ignored -> new ReviewerAggregation());
            aggregation.actionCounts.merge(view.getActionType(), view.getTotal(), Long::sum);
        });

        return aggregations.entrySet().stream()
            .map(entry -> toReviewerResponse(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(FraudReviewerProductivityResponse::resolvedCases).reversed()
                .thenComparing(FraudReviewerProductivityResponse::reviewer))
            .toList();
    }

    private List<FraudReviewerDrilldownResponse> buildReviewerDrilldown(
        List<FraudReviewCaseEntity> cases,
        List<FraudReviewCaseEntity> backlogCases,
        Instant now
    ) {
        Map<String, ReviewerBacklogAggregation> aggregations = new HashMap<>();

        cases.stream()
            .filter(caseEntity -> hasAssignee(caseEntity.getCurrentAssignee()))
            .forEach(caseEntity -> {
                ReviewerBacklogAggregation aggregation = aggregations.computeIfAbsent(
                    caseEntity.getCurrentAssignee(),
                    ignored -> new ReviewerBacklogAggregation()
                );
                if (caseEntity.getStatus() == ReviewCaseStatus.RESOLVED) {
                    aggregation.assignedResolvedCases++;
                }
            });

        backlogCases.stream()
            .filter(caseEntity -> hasAssignee(caseEntity.getCurrentAssignee()))
            .forEach(caseEntity -> {
                ReviewerBacklogAggregation aggregation = aggregations.computeIfAbsent(
                    caseEntity.getCurrentAssignee(),
                    ignored -> new ReviewerBacklogAggregation()
                );
                Duration backlogAge = Duration.between(caseEntity.getCreatedAt(), now);
                aggregation.assignedBacklogCount++;
                aggregation.totalBacklogMinutes += backlogAge.toMinutes();
                aggregation.oldestBacklogMinutes = Math.max(aggregation.oldestBacklogMinutes, backlogAge.toMinutes());
                if (caseEntity.getStatus() == ReviewCaseStatus.OPEN) {
                    aggregation.assignedOpenCases++;
                }
                if (caseEntity.getStatus() == ReviewCaseStatus.ESCALATED) {
                    aggregation.assignedEscalatedCases++;
                }
                if (isSlaBreached(caseEntity, now)) {
                    aggregation.breachedBacklogCount++;
                }
            });

        return aggregations.entrySet().stream()
            .map(entry -> new FraudReviewerDrilldownResponse(
                entry.getKey(),
                entry.getValue().assignedBacklogCount,
                entry.getValue().assignedOpenCases,
                entry.getValue().assignedEscalatedCases,
                entry.getValue().assignedResolvedCases,
                entry.getValue().breachedBacklogCount,
                averageHours(entry.getValue().totalBacklogMinutes, entry.getValue().assignedBacklogCount),
                averageHours(entry.getValue().oldestBacklogMinutes, entry.getValue().oldestBacklogMinutes == 0 ? 0 : 1),
                rate(entry.getValue().assignedBacklogCount, backlogCases.size())
            ))
            .sorted(Comparator.comparing(FraudReviewerDrilldownResponse::assignedBacklogCount).reversed()
                .thenComparing(FraudReviewerDrilldownResponse::breachedBacklogCount).reversed()
                .thenComparing(FraudReviewerDrilldownResponse::reviewer))
            .toList();
    }

    private List<FraudWorkloadRecommendationResponse> buildWorkloadRecommendations(
        List<FraudReviewerDrilldownResponse> drilldown,
        List<FraudReviewCaseEntity> backlogCases
    ) {
        List<FraudWorkloadRecommendationResponse> recommendations = new java.util.ArrayList<>();

        long unassignedBacklogCount = backlogCases.stream()
            .filter(caseEntity -> !hasAssignee(caseEntity.getCurrentAssignee()))
            .count();
        if (unassignedBacklogCount > 0) {
            recommendations.add(new FraudWorkloadRecommendationResponse(
                "HIGH",
                "ASSIGN_UNOWNED_BACKLOG",
                "Assign unowned backlog cases so they stop aging without a reviewer.",
                null,
                null,
                unassignedBacklogCount
            ));
        }

        drilldown.stream()
            .filter(item -> item.breachedBacklogCount() > 0)
            .forEach(item -> recommendations.add(new FraudWorkloadRecommendationResponse(
                "HIGH",
                "REBALANCE_BREACHED_QUEUE",
                "Reviewer has SLA-breached backlog that should be redistributed or escalated.",
                item.reviewer(),
                null,
                item.breachedBacklogCount()
            )));

        List<FraudReviewerDrilldownResponse> activeReviewers = drilldown.stream()
            .filter(item -> item.assignedBacklogCount() > 0)
            .toList();
        if (activeReviewers.size() >= 2) {
            FraudReviewerDrilldownResponse busiest = activeReviewers.stream()
                .max(Comparator.comparing(FraudReviewerDrilldownResponse::assignedBacklogCount)
                    .thenComparing(FraudReviewerDrilldownResponse::breachedBacklogCount))
                .orElse(null);
            FraudReviewerDrilldownResponse lightest = activeReviewers.stream()
                .min(Comparator.comparing(FraudReviewerDrilldownResponse::assignedBacklogCount)
                    .thenComparing(FraudReviewerDrilldownResponse::breachedBacklogCount))
                .orElse(null);

            if (busiest != null
                && lightest != null
                && !Objects.equals(busiest.reviewer(), lightest.reviewer())
                && busiest.assignedBacklogCount() - lightest.assignedBacklogCount() >= 2) {
                recommendations.add(new FraudWorkloadRecommendationResponse(
                    "MEDIUM",
                    "SHIFT_CAPACITY",
                    "Move part of the queue from the busiest reviewer to the lightest active reviewer.",
                    busiest.reviewer(),
                    lightest.reviewer(),
                    (busiest.assignedBacklogCount() - lightest.assignedBacklogCount()) / 2
                ));
            }
        }

        return recommendations;
    }

    private List<FraudCasePriorityRoutingHintResponse> buildPriorityRoutingHints(
        List<FraudReviewCaseEntity> backlogCases,
        Instant now
    ) {
        List<FraudCasePriorityRoutingHintResponse> hints = List.of(
            toPriorityRoutingHint(
                "CRITICAL",
                "SENIOR_REVIEW_NOW",
                "Escalated cases already past SLA need immediate senior reviewer attention.",
                backlogCases.stream()
                    .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
                    .filter(caseEntity -> isSlaBreached(caseEntity, now))
                    .toList(),
                now
            ),
            toPriorityRoutingHint(
                "HIGH",
                "CLAIM_AND_ASSIGN",
                "Unassigned high-risk or breached cases should be claimed before more work is pulled.",
                backlogCases.stream()
                    .filter(caseEntity -> !hasAssignee(caseEntity.getCurrentAssignee()))
                    .filter(caseEntity -> caseEntity.getRiskScore() >= 85 || isSlaBreached(caseEntity, now))
                    .toList(),
                now
            ),
            toPriorityRoutingHint(
                "HIGH",
                "NEXT_AVAILABLE_ANALYST",
                "Open high-risk cases should move ahead of standard queue work.",
                backlogCases.stream()
                    .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.OPEN)
                    .filter(caseEntity -> caseEntity.getRiskScore() >= 85)
                    .toList(),
                now
            ),
            toPriorityRoutingHint(
                "MEDIUM",
                "CLEAR_BEFORE_BREACH",
                "Standard-risk cases approaching SLA should be cleared before they tip into breach.",
                backlogCases.stream()
                    .filter(caseEntity -> !isSlaBreached(caseEntity, now))
                    .filter(caseEntity -> {
                        Duration age = Duration.between(caseEntity.getCreatedAt(), now);
                        return !age.minus(Duration.ofHours(12)).isNegative() && age.minus(REVIEW_BACKLOG_SLA).isNegative();
                    })
                    .toList(),
                now
            )
        );

        return hints.stream()
            .filter(hint -> hint.affectedCaseCount() > 0)
            .toList();
    }

    private FraudCasePriorityRoutingHintResponse toPriorityRoutingHint(
        String priorityBand,
        String recommendedQueue,
        String rationale,
        List<FraudReviewCaseEntity> matchingCases,
        Instant now
    ) {
        long breachedCaseCount = matchingCases.stream()
            .filter(caseEntity -> isSlaBreached(caseEntity, now))
            .count();
        long unassignedCaseCount = matchingCases.stream()
            .filter(caseEntity -> !hasAssignee(caseEntity.getCurrentAssignee()))
            .count();
        long escalatedCaseCount = matchingCases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
            .count();
        BigDecimal averageRiskScore = matchingCases.isEmpty()
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(matchingCases.stream().mapToInt(FraudReviewCaseEntity::getRiskScore).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);

        return new FraudCasePriorityRoutingHintResponse(
            priorityBand,
            recommendedQueue,
            rationale,
            matchingCases.size(),
            breachedCaseCount,
            unassignedCaseCount,
            escalatedCaseCount,
            averageRiskScore
        );
    }

    private List<FraudSupervisorInterventionResponse> buildSupervisorInterventions(
        List<FraudReviewCaseEntity> backlogCases,
        List<FraudReviewerDrilldownResponse> drilldown,
        List<FraudCasePriorityRoutingHintResponse> priorityRoutingHints,
        Instant now
    ) {
        List<FraudSupervisorInterventionResponse> interventions = new java.util.ArrayList<>();

        long unassignedBreachedCases = backlogCases.stream()
            .filter(caseEntity -> !hasAssignee(caseEntity.getCurrentAssignee()))
            .filter(caseEntity -> isSlaBreached(caseEntity, now))
            .count();
        if (unassignedBreachedCases > 0) {
            interventions.add(new FraudSupervisorInterventionResponse(
                "CRITICAL",
                "CLAIM_BREACHED_UNOWNED_CASES",
                "Breached cases are sitting without ownership and need direct supervisor assignment.",
                null,
                unassignedBreachedCases
            ));
        }

        priorityRoutingHints.stream()
            .filter(hint -> "SENIOR_REVIEW_NOW".equals(hint.recommendedQueue()))
            .findFirst()
            .ifPresent(hint -> interventions.add(new FraudSupervisorInterventionResponse(
                "CRITICAL",
                "RUN_SENIOR_ESCALATION_SWEEP",
                "Escalated breached cases require an immediate supervisor-led sweep.",
                null,
                hint.affectedCaseCount()
            )));

        drilldown.stream()
            .filter(item -> item.breachedBacklogCount() > 0)
            .forEach(item -> interventions.add(new FraudSupervisorInterventionResponse(
                "HIGH",
                "INTERVENE_ON_REVIEWER_BACKLOG",
                "Reviewer has breached backlog that needs reassignment or direct supervisor support.",
                item.reviewer(),
                item.breachedBacklogCount()
            )));

        return interventions;
    }

    private FraudReviewerProductivityResponse toReviewerResponse(String reviewer, ReviewerAggregation aggregation) {
        return new FraudReviewerProductivityResponse(
            reviewer,
            aggregation.assignedOpenCases,
            aggregation.assignedEscalatedCases,
            aggregation.resolvedCases,
            aggregation.releasedPayments,
            aggregation.confirmedDeclines,
            averageHours(aggregation.totalResolutionMinutes, aggregation.resolvedCases),
            aggregation.actionCounts.getOrDefault(FraudCaseActionType.ASSIGNED, 0L),
            aggregation.actionCounts.getOrDefault(FraudCaseActionType.ESCALATED, 0L),
            aggregation.actionCounts.getOrDefault(FraudCaseActionType.NOTE_ADDED, 0L),
            aggregation.actionCounts.getOrDefault(FraudCaseActionType.RESOLVED, 0L)
        );
    }

    private BigDecimal averageHours(List<Duration> durations) {
        if (durations.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long totalMinutes = durations.stream()
            .mapToLong(Duration::toMinutes)
            .sum();
        return averageHours(totalMinutes, durations.size());
    }

    private BigDecimal maxHours(List<Duration> durations) {
        if (durations.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long maxMinutes = durations.stream()
            .mapToLong(Duration::toMinutes)
            .max()
            .orElse(0);
        return BigDecimal.valueOf(maxMinutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageHours(long totalMinutes, long count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(totalMinutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
            .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal hoursBetween(Instant start, Instant end) {
        return BigDecimal.valueOf(Duration.between(start, end).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private boolean isSlaBreached(FraudReviewCaseEntity caseEntity, Instant now) {
        return !Duration.between(caseEntity.getCreatedAt(), now).minus(REVIEW_BACKLOG_SLA).isNegative();
    }

    private boolean hasAssignee(String assignee) {
        return assignee != null && !assignee.isBlank();
    }

    private FraudBacklogAgingBucketResponse toBacklogBucket(
        String label,
        Duration minInclusive,
        Duration maxExclusive,
        List<FraudReviewCaseEntity> backlogCases,
        Instant now
    ) {
        List<FraudReviewCaseEntity> bucketCases = backlogCases.stream()
            .filter(caseEntity -> isWithinBucket(Duration.between(caseEntity.getCreatedAt(), now), minInclusive, maxExclusive))
            .toList();
        long openCount = bucketCases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.OPEN)
            .count();
        long escalatedCount = bucketCases.stream()
            .filter(caseEntity -> caseEntity.getStatus() == ReviewCaseStatus.ESCALATED)
            .count();

        return new FraudBacklogAgingBucketResponse(
            label,
            bucketCases.size(),
            openCount,
            escalatedCount,
            !minInclusive.minus(REVIEW_BACKLOG_SLA).isNegative()
        );
    }

    private boolean isWithinBucket(Duration age, Duration minInclusive, Duration maxExclusive) {
        boolean meetsMinimum = !age.minus(minInclusive).isNegative();
        boolean belowMaximum = maxExclusive == null || age.minus(maxExclusive).isNegative();
        return meetsMinimum && belowMaximum;
    }

    private static final class ReviewerAggregation {
        private long assignedOpenCases;
        private long assignedEscalatedCases;
        private long resolvedCases;
        private long releasedPayments;
        private long confirmedDeclines;
        private long totalResolutionMinutes;
        private final Map<FraudCaseActionType, Long> actionCounts = new EnumMap<>(FraudCaseActionType.class);
    }

    private static final class ReviewerBacklogAggregation {
        private long assignedBacklogCount;
        private long assignedOpenCases;
        private long assignedEscalatedCases;
        private long assignedResolvedCases;
        private long breachedBacklogCount;
        private long totalBacklogMinutes;
        private long oldestBacklogMinutes;
    }
}
