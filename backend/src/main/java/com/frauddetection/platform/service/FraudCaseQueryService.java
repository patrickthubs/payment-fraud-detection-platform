package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudCaseResponse;
import com.frauddetection.platform.dto.FraudCaseTimelineEntryResponse;
import com.frauddetection.platform.entity.FraudCaseTimelineEntryEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.exception.FraudCaseNotFoundException;
import com.frauddetection.platform.exception.InvalidFraudCaseFilterException;
import com.frauddetection.platform.repository.FraudCaseTimelineEntryRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import com.frauddetection.platform.repository.FraudReviewCaseSpecifications;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudCaseQueryService {

    private static final Duration BACKLOG_SLA = Duration.ofHours(24);

    private final FraudReviewCaseRepository fraudReviewCaseRepository;
    private final FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;
    private final Clock clock;

    public FraudCaseQueryService(
        FraudReviewCaseRepository fraudReviewCaseRepository,
        FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository,
        Clock clock
    ) {
        this.fraudReviewCaseRepository = fraudReviewCaseRepository;
        this.fraudCaseTimelineEntryRepository = fraudCaseTimelineEntryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FraudCaseResponse> findAll(FraudCaseFilterCriteria criteria) {
        validateCriteria(criteria);
        return findMatchingCases(criteria).stream()
            .map(entity -> toResponse(entity, List.of()))
            .toList();
    }

    @Transactional(readOnly = true)
    public String export(FraudCaseFilterCriteria criteria) {
        validateCriteria(criteria);
        Instant breachThreshold = breachThreshold();
        List<FraudReviewCaseEntity> cases = findMatchingCases(criteria);
        StringBuilder csv = new StringBuilder()
            .append("case_id,assessment_id,payment_id,customer_id,status,decision,risk_score,current_assignee,created_at,updated_at,breached_sla,resolution_outcome,resolution_summary,summary")
            .append(System.lineSeparator());

        for (FraudReviewCaseEntity entity : cases) {
            csv.append(csvValue(entity.getId()))
                .append(',').append(csvValue(entity.getAssessmentId()))
                .append(',').append(csvValue(entity.getPaymentId()))
                .append(',').append(csvValue(entity.getCustomerId()))
                .append(',').append(csvValue(entity.getStatus()))
                .append(',').append(csvValue(entity.getDecision()))
                .append(',').append(csvValue(entity.getRiskScore()))
                .append(',').append(csvValue(entity.getCurrentAssignee()))
                .append(',').append(csvValue(entity.getCreatedAt()))
                .append(',').append(csvValue(entity.getUpdatedAt()))
                .append(',').append(csvValue(isBreached(entity, breachThreshold)))
                .append(',').append(csvValue(entity.getResolutionOutcome()))
                .append(',').append(csvValue(entity.getResolutionSummary()))
                .append(',').append(csvValue(entity.getSummary()))
                .append(System.lineSeparator());
        }

        return csv.toString();
    }

    @Transactional(readOnly = true)
    public FraudCaseResponse findById(UUID caseId) {
        FraudReviewCaseEntity entity = fraudReviewCaseRepository.findById(caseId)
            .orElseThrow(() -> new FraudCaseNotFoundException(caseId));
        List<FraudCaseTimelineEntryResponse> timelineEntries = fraudCaseTimelineEntryRepository
            .findAllByCaseIdOrderByCreatedAtAsc(caseId)
            .stream()
            .map(this::toTimelineResponse)
            .toList();
        return toResponse(entity, timelineEntries);
    }

    private List<FraudReviewCaseEntity> findMatchingCases(FraudCaseFilterCriteria criteria) {
        return fraudReviewCaseRepository.findAll(
            FraudReviewCaseSpecifications.forCriteria(criteria, breachThreshold()),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private Instant breachThreshold() {
        return clock.instant().minus(BACKLOG_SLA);
    }

    private void validateCriteria(FraudCaseFilterCriteria criteria) {
        if (criteria.minRiskScore() != null && criteria.maxRiskScore() != null
            && criteria.minRiskScore() > criteria.maxRiskScore()) {
            throw new InvalidFraudCaseFilterException("Minimum risk score cannot be greater than maximum risk score.");
        }
        if (criteria.createdFrom() != null && criteria.createdTo() != null
            && criteria.createdFrom().isAfter(criteria.createdTo())) {
            throw new InvalidFraudCaseFilterException("createdFrom must be earlier than or equal to createdTo.");
        }
    }

    private boolean isBreached(FraudReviewCaseEntity entity, Instant breachThreshold) {
        return switch (entity.getStatus()) {
            case OPEN, ESCALATED -> !entity.getCreatedAt().isAfter(breachThreshold);
            case RESOLVED -> false;
        };
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String rawValue = value.toString().replace("\"", "\"\"");
        return '"' + rawValue + '"';
    }

    private FraudCaseResponse toResponse(FraudReviewCaseEntity entity, List<FraudCaseTimelineEntryResponse> timelineEntries) {
        return new FraudCaseResponse(
            entity.getId(),
            entity.getAssessmentId(),
            entity.getPaymentId(),
            entity.getCustomerId(),
            entity.getRiskScore(),
            entity.getDecision(),
            entity.getStatus(),
            entity.getSummary(),
            entity.getCurrentAssignee(),
            entity.getResolutionSummary(),
            entity.getResolutionOutcome(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            timelineEntries
        );
    }

    private FraudCaseTimelineEntryResponse toTimelineResponse(FraudCaseTimelineEntryEntity entity) {
        return new FraudCaseTimelineEntryResponse(
            entity.getId(),
            entity.getActionType(),
            entity.getActor(),
            entity.getDetail(),
            entity.getCreatedAt()
        );
    }
}
