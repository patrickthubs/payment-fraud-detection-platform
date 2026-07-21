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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudCaseQueryService {

    private static final Duration BACKLOG_SLA = Duration.ofHours(24);
    static final int DEFAULT_EXPORT_LIMIT = 1_000;
    static final int MAX_EXPORT_LIMIT = 10_000;

    private final FraudReviewCaseRepository fraudReviewCaseRepository;
    private final FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;
    private final CurrentTenantService currentTenantService;
    private final Clock clock;

    @Autowired
    public FraudCaseQueryService(
        FraudReviewCaseRepository fraudReviewCaseRepository,
        FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository,
        CurrentTenantService currentTenantService,
        Clock clock
    ) {
        this.fraudReviewCaseRepository = fraudReviewCaseRepository;
        this.fraudCaseTimelineEntryRepository = fraudCaseTimelineEntryRepository;
        this.currentTenantService = currentTenantService;
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
    public List<FraudCaseResponse> findAll(FraudCaseFilterCriteria criteria, int page, int size) {
        validateCriteria(criteria);
        return findMatchingCases(criteria, page, size).stream()
            .map(entity -> toResponse(entity, List.of()))
            .toList();
    }

    @Transactional(readOnly = true)
    public String export(FraudCaseFilterCriteria criteria) {
        return export(criteria, DEFAULT_EXPORT_LIMIT);
    }

    @Transactional(readOnly = true)
    public String export(FraudCaseFilterCriteria criteria, int limit) {
        validateCriteria(criteria);
        Instant breachThreshold = breachThreshold();
        int boundedLimit = Math.min(Math.max(1, limit), MAX_EXPORT_LIMIT);
        List<FraudReviewCaseEntity> cases = findMatchingCasesForExport(criteria, boundedLimit);
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
        UUID organizationId = currentTenantService.organizationId();
        FraudReviewCaseEntity entity = fraudReviewCaseRepository.findByOrganizationIdAndId(organizationId, caseId)
            .orElseThrow(() -> new FraudCaseNotFoundException(caseId));
        List<FraudCaseTimelineEntryResponse> timelineEntries = fraudCaseTimelineEntryRepository
            .findAllByOrganizationIdAndCaseIdOrderByCreatedAtAsc(organizationId, caseId)
            .stream()
            .map(this::toTimelineResponse)
            .toList();
        return toResponse(entity, timelineEntries);
    }

    private List<FraudReviewCaseEntity> findMatchingCases(FraudCaseFilterCriteria criteria, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 200);
        return fraudReviewCaseRepository.findAll(
            FraudReviewCaseSpecifications.forCriteria(currentTenantService.organizationId(), criteria, breachThreshold()),
            PageRequest.of(boundedPage, boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    private List<FraudReviewCaseEntity> findMatchingCases(FraudCaseFilterCriteria criteria) {
        return fraudReviewCaseRepository.findAll(
            FraudReviewCaseSpecifications.forCriteria(currentTenantService.organizationId(), criteria, breachThreshold()),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private List<FraudReviewCaseEntity> findMatchingCasesForExport(FraudCaseFilterCriteria criteria, int limit) {
        return fraudReviewCaseRepository.findAll(
            FraudReviewCaseSpecifications.forCriteria(currentTenantService.organizationId(), criteria, breachThreshold()),
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
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
        String rawValue = neutralizeSpreadsheetFormula(value.toString()).replace("\"", "\"\"");
        return '"' + rawValue + '"';
    }

    private String neutralizeSpreadsheetFormula(String value) {
        String leadingTrimmed = value.stripLeading();
        if (!leadingTrimmed.isEmpty() && "=+-@".indexOf(leadingTrimmed.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
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
