package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.AssignFraudCaseRequest;
import com.frauddetection.platform.dto.EscalateFraudCaseRequest;
import com.frauddetection.platform.dto.FraudCaseDecisionRequest;
import com.frauddetection.platform.dto.FraudCaseNoteRequest;
import com.frauddetection.platform.dto.FraudCaseResponse;
import com.frauddetection.platform.dto.ResolveFraudCaseRequest;
import com.frauddetection.platform.entity.FraudCaseTimelineEntryEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.exception.FraudCaseActionNotAllowedException;
import com.frauddetection.platform.exception.FraudCaseNotFoundException;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudCaseTimelineEntryRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudCaseCommandService {

    private final FraudReviewCaseRepository fraudReviewCaseRepository;
    private final FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;
    private final FraudCaseQueryService fraudCaseQueryService;
    private final PaymentLifecycleService paymentLifecycleService;
    private final PlatformMetricsService platformMetricsService;
    private final FraudNotificationHookService fraudNotificationHookService;
    private final CurrentTenantService currentTenantService;

    @Autowired
    public FraudCaseCommandService(
        FraudReviewCaseRepository fraudReviewCaseRepository,
        FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository,
        FraudCaseQueryService fraudCaseQueryService,
        PaymentLifecycleService paymentLifecycleService,
        PlatformMetricsService platformMetricsService,
        FraudNotificationHookService fraudNotificationHookService,
        CurrentTenantService currentTenantService
    ) {
        this.fraudReviewCaseRepository = fraudReviewCaseRepository;
        this.fraudCaseTimelineEntryRepository = fraudCaseTimelineEntryRepository;
        this.fraudCaseQueryService = fraudCaseQueryService;
        this.paymentLifecycleService = paymentLifecycleService;
        this.platformMetricsService = platformMetricsService;
        this.fraudNotificationHookService = fraudNotificationHookService;
        this.currentTenantService = currentTenantService;
    }

    @Transactional
    public FraudCaseResponse assign(UUID caseId, String operator, AssignFraudCaseRequest request) {
        FraudReviewCaseEntity entity = loadCase(caseId);
        rejectIfResolved(entity, "assigned");

        Instant now = Instant.now();
        entity.assign(request.assignee(), now);
        appendTimeline(entity.getId(), FraudCaseActionType.ASSIGNED, operator,
            "Assigned case to %s. %s".formatted(request.assignee(), request.note()), now);
        platformMetricsService.recordCaseAction(FraudCaseActionType.ASSIGNED, entity.getStatus().name());
        return fraudCaseQueryService.findById(entity.getId());
    }

    @Transactional
    public FraudCaseResponse escalate(UUID caseId, String operator, EscalateFraudCaseRequest request) {
        FraudReviewCaseEntity entity = loadCase(caseId);
        rejectIfResolved(entity, "escalated");

        Instant now = Instant.now();
        entity.escalate(now);
        appendTimeline(entity.getId(), FraudCaseActionType.ESCALATED, operator, request.reason(), now);
        platformMetricsService.recordCaseAction(FraudCaseActionType.ESCALATED, entity.getStatus().name());
        return fraudCaseQueryService.findById(entity.getId());
    }

    @Transactional
    public FraudCaseResponse addNote(UUID caseId, String operator, FraudCaseNoteRequest request) {
        FraudReviewCaseEntity entity = loadCase(caseId);

        Instant now = Instant.now();
        appendTimeline(entity.getId(), FraudCaseActionType.NOTE_ADDED, operator, request.note(), now);
        platformMetricsService.recordCaseAction(FraudCaseActionType.NOTE_ADDED, entity.getStatus().name());
        return fraudCaseQueryService.findById(entity.getId());
    }

    @Transactional
    public FraudCaseResponse releasePayment(UUID caseId, String operator, FraudCaseDecisionRequest request) {
        return resolve(caseId, new ResolveFraudCaseRequest(
            request.resolutionSummary(),
            CaseResolutionOutcome.RELEASE_PAYMENT
        ), operator);
    }

    @Transactional
    public FraudCaseResponse confirmDecline(UUID caseId, String operator, FraudCaseDecisionRequest request) {
        return resolve(caseId, new ResolveFraudCaseRequest(
            request.resolutionSummary(),
            CaseResolutionOutcome.CONFIRM_DECLINE
        ), operator);
    }

    @Transactional
    public FraudCaseResponse resolve(UUID caseId, ResolveFraudCaseRequest request, String operator) {
        FraudReviewCaseEntity entity = loadCase(caseId);
        if (entity.getStatus() == ReviewCaseStatus.RESOLVED) {
            throw new FraudCaseActionNotAllowedException(caseId, "resolved again");
        }

        rejectUnsupportedResolutionOutcome(entity, request.outcome());
        Instant now = Instant.now();
        var paymentRecord = paymentLifecycleService.transitionFromCaseResolution(
            entity.getOrganizationId(),
            entity.getPaymentId(),
            toPaymentStatus(request.outcome()),
            request.resolutionSummary(),
            entity.getAssessmentId(),
            now
        );
        entity.resolve(request.resolutionSummary(), request.outcome(), now);
        fraudNotificationHookService.publishResolutionNotification(entity, paymentRecord, request.outcome(), now);
        appendTimeline(
            entity.getId(),
            FraudCaseActionType.RESOLVED,
            operator,
            "%s Outcome: %s.".formatted(request.resolutionSummary(), request.outcome().name()),
            now
        );
        platformMetricsService.recordCaseAction(FraudCaseActionType.RESOLVED, entity.getStatus().name());
        platformMetricsService.recordCaseResolution(request.outcome(), toPaymentStatus(request.outcome()));
        return fraudCaseQueryService.findById(entity.getId());
    }

    private FraudReviewCaseEntity loadCase(UUID caseId) {
        UUID organizationId = currentTenantService.organizationId();
        return fraudReviewCaseRepository.findByOrganizationIdAndId(organizationId, caseId)
            .orElseThrow(() -> new FraudCaseNotFoundException(caseId));
    }

    private void rejectIfResolved(FraudReviewCaseEntity entity, String action) {
        if (entity.getStatus() == ReviewCaseStatus.RESOLVED) {
            throw new FraudCaseActionNotAllowedException(entity.getId(), action);
        }
    }

    private void rejectUnsupportedResolutionOutcome(FraudReviewCaseEntity entity, CaseResolutionOutcome outcome) {
        if (entity.getDecision() == RiskDecision.DECLINE && outcome == CaseResolutionOutcome.RELEASE_PAYMENT) {
            throw new FraudCaseActionNotAllowedException(entity.getId(), "resolved with release outcome");
        }
    }

    private PaymentStatus toPaymentStatus(CaseResolutionOutcome outcome) {
        return switch (outcome) {
            case RELEASE_PAYMENT -> PaymentStatus.APPROVED;
            case CONFIRM_DECLINE -> PaymentStatus.DECLINED;
        };
    }

    private void appendTimeline(UUID caseId, FraudCaseActionType actionType, String actor, String detail, Instant createdAt) {
        fraudCaseTimelineEntryRepository.save(new FraudCaseTimelineEntryEntity(
            UUID.randomUUID(),
            currentTenantService.organizationId(),
            caseId,
            actionType,
            actor,
            detail,
            createdAt
        ));
    }
}
