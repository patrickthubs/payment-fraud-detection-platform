package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.AssignFraudCaseRequest;
import com.frauddetection.platform.dto.EscalateFraudCaseRequest;
import com.frauddetection.platform.dto.FraudCaseDecisionRequest;
import com.frauddetection.platform.dto.FraudCaseNoteRequest;
import com.frauddetection.platform.dto.FraudCaseResponse;
import com.frauddetection.platform.dto.FraudCaseTimelineEntryResponse;
import com.frauddetection.platform.dto.ResolveFraudCaseRequest;
import com.frauddetection.platform.entity.FraudCaseTimelineEntryEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.exception.FraudCaseActionNotAllowedException;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudCaseTimelineEntryRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudCaseCommandServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    @Mock
    private FraudReviewCaseRepository fraudReviewCaseRepository;

    @Mock
    private FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;

    @Mock
    private FraudCaseQueryService fraudCaseQueryService;

    @Mock
    private PaymentLifecycleService paymentLifecycleService;

    @Mock
    private FraudNotificationHookService fraudNotificationHookService;

    @Mock
    private CurrentTenantService currentTenantService;

    private FraudCaseCommandService fraudCaseCommandService;

    @BeforeEach
    void setUp() {
        when(currentTenantService.organizationId()).thenReturn(ORGANIZATION_ID);
        fraudCaseCommandService = new FraudCaseCommandService(
            fraudReviewCaseRepository,
            fraudCaseTimelineEntryRepository,
            fraudCaseQueryService,
            paymentLifecycleService,
            new PlatformMetricsService(new SimpleMeterRegistry()),
            fraudNotificationHookService,
            currentTenantService
        );
    }

    @Test
    void assignsOpenCase() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.OPEN);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));
        when(fraudCaseTimelineEntryRepository.save(any(FraudCaseTimelineEntryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudCaseQueryService.findById(caseId)).thenReturn(buildResponse(caseId, ReviewCaseStatus.OPEN, "analyst.one", null, null));

        FraudCaseResponse response = fraudCaseCommandService.assign(
            caseId,
            "lead.analyst",
            new AssignFraudCaseRequest("analyst.one", "Taking ownership.")
        );

        assertThat(response.currentAssignee()).isEqualTo("analyst.one");
        verify(fraudCaseTimelineEntryRepository).save(any(FraudCaseTimelineEntryEntity.class));
    }

    @Test
    void escalatesCase() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.OPEN);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));
        when(fraudCaseTimelineEntryRepository.save(any(FraudCaseTimelineEntryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudCaseQueryService.findById(caseId)).thenReturn(buildResponse(caseId, ReviewCaseStatus.ESCALATED, null, null, null));

        FraudCaseResponse response = fraudCaseCommandService.escalate(
            caseId,
            "analyst.one",
            new EscalateFraudCaseRequest("Needs a senior fraud investigator review.")
        );

        assertThat(response.status()).isEqualTo(ReviewCaseStatus.ESCALATED);
        verify(fraudCaseTimelineEntryRepository).save(any(FraudCaseTimelineEntryEntity.class));
    }

    @Test
    void resolvesCase() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.ESCALATED, RiskDecision.HOLD);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));
        when(fraudCaseTimelineEntryRepository.save(any(FraudCaseTimelineEntryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentLifecycleService.transitionFromCaseResolution(any(), any(), any(), any(), any(), any()))
            .thenReturn(buildPaymentRecord(PaymentStatus.APPROVED));
        when(fraudCaseQueryService.findById(caseId)).thenReturn(buildResponse(
            caseId,
            ReviewCaseStatus.RESOLVED,
            "analyst.one",
            "Customer confirmed transaction as genuine.",
            CaseResolutionOutcome.RELEASE_PAYMENT
        ));

        FraudCaseResponse response = fraudCaseCommandService.resolve(
            caseId,
            new ResolveFraudCaseRequest(
                "Customer confirmed transaction as genuine.",
                CaseResolutionOutcome.RELEASE_PAYMENT
            ),
            "analyst.one"
        );

        assertThat(response.status()).isEqualTo(ReviewCaseStatus.RESOLVED);
        assertThat(response.resolutionSummary()).contains("genuine");
        assertThat(response.resolutionOutcome()).isEqualTo(CaseResolutionOutcome.RELEASE_PAYMENT);
        verify(paymentLifecycleService).transitionFromCaseResolution(eq(ORGANIZATION_ID), any(), any(), any(), any(), any());
        verify(fraudCaseTimelineEntryRepository).save(any(FraudCaseTimelineEntryEntity.class));
    }

    @Test
    void releasesPaymentViaDedicatedCommand() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.OPEN, RiskDecision.HOLD);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));
        when(fraudCaseTimelineEntryRepository.save(any(FraudCaseTimelineEntryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentLifecycleService.transitionFromCaseResolution(any(), any(), any(), any(), any(), any()))
            .thenReturn(buildPaymentRecord(PaymentStatus.APPROVED));
        when(fraudCaseQueryService.findById(caseId)).thenReturn(buildResponse(
            caseId,
            ReviewCaseStatus.RESOLVED,
            "analyst.one",
            "Released after step-up verification.",
            CaseResolutionOutcome.RELEASE_PAYMENT
        ));

        FraudCaseResponse response = fraudCaseCommandService.releasePayment(
            caseId,
            "analyst.one",
            new FraudCaseDecisionRequest("Released after step-up verification.")
        );

        assertThat(response.resolutionOutcome()).isEqualTo(CaseResolutionOutcome.RELEASE_PAYMENT);
        verify(paymentLifecycleService).transitionFromCaseResolution(eq(ORGANIZATION_ID), any(), any(), any(), any(), any());
        verify(fraudNotificationHookService).publishResolutionNotification(any(), any(), any(), any());
    }

    @Test
    void confirmsDeclineViaDedicatedCommand() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.OPEN, RiskDecision.DECLINE);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));
        when(fraudCaseTimelineEntryRepository.save(any(FraudCaseTimelineEntryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentLifecycleService.transitionFromCaseResolution(any(), any(), any(), any(), any(), any()))
            .thenReturn(buildPaymentRecord(PaymentStatus.DECLINED));
        when(fraudCaseQueryService.findById(caseId)).thenReturn(buildResponse(
            caseId,
            ReviewCaseStatus.RESOLVED,
            "senior.analyst",
            "Decline confirmed after beneficiary risk review.",
            CaseResolutionOutcome.CONFIRM_DECLINE
        ));

        FraudCaseResponse response = fraudCaseCommandService.confirmDecline(
            caseId,
            "senior.analyst",
            new FraudCaseDecisionRequest("Decline confirmed after beneficiary risk review.")
        );

        assertThat(response.resolutionOutcome()).isEqualTo(CaseResolutionOutcome.CONFIRM_DECLINE);
        assertThat(response.status()).isEqualTo(ReviewCaseStatus.RESOLVED);
    }

    @Test
    void rejectsEscalationOfResolvedCase() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.RESOLVED);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> fraudCaseCommandService.escalate(
            caseId,
            "analyst.one",
            new EscalateFraudCaseRequest("Trying to reopen without review.")
        )).isInstanceOf(FraudCaseActionNotAllowedException.class);

        verify(fraudCaseTimelineEntryRepository, never()).save(any(FraudCaseTimelineEntryEntity.class));
    }

    @Test
    void addsNoteToResolvedCase() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.RESOLVED);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));
        when(fraudCaseTimelineEntryRepository.save(any(FraudCaseTimelineEntryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudCaseQueryService.findById(caseId)).thenReturn(buildResponse(
            caseId,
            ReviewCaseStatus.RESOLVED,
            "analyst.one",
            "Closed after customer callback.",
            CaseResolutionOutcome.CONFIRM_DECLINE
        ));

        FraudCaseResponse response = fraudCaseCommandService.addNote(
            caseId,
            "qa.reviewer",
            new FraudCaseNoteRequest("Post-resolution QA check completed.")
        );

        assertThat(response.status()).isEqualTo(ReviewCaseStatus.RESOLVED);
        verify(fraudCaseTimelineEntryRepository).save(any(FraudCaseTimelineEntryEntity.class));
    }

    @Test
    void rejectsReleaseOutcomeForDeclinedCase() {
        UUID caseId = UUID.randomUUID();
        FraudReviewCaseEntity entity = buildCase(caseId, ReviewCaseStatus.OPEN, RiskDecision.DECLINE);
        when(fraudReviewCaseRepository.findByOrganizationIdAndId(ORGANIZATION_ID, caseId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> fraudCaseCommandService.resolve(
            caseId,
            new ResolveFraudCaseRequest(
                "Attempted release for a previously declined payment.",
                CaseResolutionOutcome.RELEASE_PAYMENT
            ),
            "analyst.one"
        )).isInstanceOf(FraudCaseActionNotAllowedException.class);

        verify(paymentLifecycleService, never()).transitionFromCaseResolution(any(), any(), any(), any(), any(), any());
    }

    private FraudReviewCaseEntity buildCase(UUID caseId, ReviewCaseStatus status) {
        return buildCase(caseId, status, RiskDecision.DECLINE);
    }

    private FraudReviewCaseEntity buildCase(UUID caseId, ReviewCaseStatus status, RiskDecision decision) {
        return new FraudReviewCaseEntity(
            caseId,
            ORGANIZATION_ID,
            UUID.randomUUID(),
            "PAY-1",
            "CUST-1",
            92,
            decision,
            status,
            "High-risk payment routed for manual review.",
            null,
            null,
            null,
            Instant.parse("2026-07-16T10:00:00Z"),
            Instant.parse("2026-07-16T10:00:00Z")
        );
    }

    private FraudCaseResponse buildResponse(
        UUID caseId,
        ReviewCaseStatus status,
        String assignee,
        String resolutionSummary,
        CaseResolutionOutcome resolutionOutcome
    ) {
        return new FraudCaseResponse(
            caseId,
            UUID.randomUUID(),
            "PAY-1",
            "CUST-1",
            92,
            RiskDecision.DECLINE,
            status,
            "High-risk payment routed for manual review.",
            assignee,
            resolutionSummary,
            resolutionOutcome,
            Instant.parse("2026-07-16T10:00:00Z"),
            Instant.parse("2026-07-16T11:00:00Z"),
            List.of(new FraudCaseTimelineEntryResponse(
                UUID.randomUUID(),
                FraudCaseActionType.NOTE_ADDED,
                "seed.user",
                "Timeline placeholder",
                Instant.parse("2026-07-16T10:30:00Z")
            ))
        );
    }

    private com.frauddetection.platform.entity.PaymentRecordEntity buildPaymentRecord(PaymentStatus paymentStatus) {
        return new com.frauddetection.platform.entity.PaymentRecordEntity(
            UUID.randomUUID(),
            "PAY-1",
            "CUST-1",
            BigDecimal.valueOf(9200),
            "ZAR",
            "MOBILE_APP",
            "ELECTRONICS",
            UUID.randomUUID(),
            92,
            RiskDecision.HOLD,
            paymentStatus,
            null,
            null,
            null,
            null,
            null,
            Instant.parse("2026-07-16T10:00:00Z"),
            Instant.parse("2026-07-16T10:05:00Z")
        );
    }
}
