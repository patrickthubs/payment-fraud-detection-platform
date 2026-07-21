package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.CompletePaymentChallengeRequest;
import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.entity.PaymentStateTransitionEntity;
import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import com.frauddetection.platform.repository.PaymentRecordRepository;
import com.frauddetection.platform.repository.PaymentStateTransitionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private PaymentStateTransitionRepository paymentStateTransitionRepository;

    @Mock
    private PaymentOrchestrationEventPublisher paymentOrchestrationEventPublisher;

    private PaymentLifecycleService paymentLifecycleService;

    @BeforeEach
    void setUp() {
        paymentLifecycleService = new PaymentLifecycleService(
            paymentRecordRepository,
            paymentStateTransitionRepository,
            new PlatformMetricsService(new SimpleMeterRegistry()),
            paymentOrchestrationEventPublisher
        );
        when(paymentRecordRepository.save(any(PaymentRecordEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentStateTransitionRepository.save(any(PaymentStateTransitionEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsDeclinedPaymentForDeclineDecision() {
        PaymentRiskAssessmentRequest request = request("PAY-9001", "CUST-9");
        FraudAssessmentRecordEntity assessmentRecord = assessment("PAY-9001", "CUST-9", RiskDecision.DECLINE);
        when(paymentRecordRepository.findByOrganizationIdAndPaymentId(ORGANIZATION_ID, "PAY-9001")).thenReturn(Optional.empty());

        PaymentRecordEntity result = paymentLifecycleService.recordAssessmentOutcome(
            request,
            assessmentRecord,
            Instant.parse("2026-07-16T12:00:00Z")
        );

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(paymentStateTransitionRepository).save(any(PaymentStateTransitionEntity.class));
        verify(paymentOrchestrationEventPublisher).publish(any(PaymentStatusChangedEvent.class));
    }

    @Test
    void mapsHoldDecisionToHeldPaymentStatus() {
        PaymentRiskAssessmentRequest request = request("PAY-9002", "CUST-10");
        FraudAssessmentRecordEntity assessmentRecord = assessment("PAY-9002", "CUST-10", RiskDecision.HOLD);
        when(paymentRecordRepository.findByOrganizationIdAndPaymentId(ORGANIZATION_ID, "PAY-9002")).thenReturn(Optional.empty());

        PaymentRecordEntity result = paymentLifecycleService.recordAssessmentOutcome(
            request,
            assessmentRecord,
            Instant.parse("2026-07-16T12:05:00Z")
        );

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.HELD);
    }

    @Test
    void transitionsHeldPaymentToApprovedAfterCaseResolution() {
        PaymentRecordEntity paymentRecord = new PaymentRecordEntity(
            UUID.randomUUID(),
            ORGANIZATION_ID,
            "PAY-9003",
            "CUST-11",
            BigDecimal.valueOf(9100),
            "ZAR",
            "MOBILE_APP",
            "ELECTRONICS",
            UUID.randomUUID(),
            88,
            RiskDecision.HOLD,
            PaymentStatus.HELD,
            null,
            null,
            null,
            null,
            null,
            Instant.parse("2026-07-16T12:00:00Z"),
            Instant.parse("2026-07-16T12:00:00Z")
        );
        when(paymentRecordRepository.findByOrganizationIdAndPaymentId(ORGANIZATION_ID, "PAY-9003")).thenReturn(Optional.of(paymentRecord));

        PaymentRecordEntity result = paymentLifecycleService.transitionFromCaseResolution(
            ORGANIZATION_ID,
            "PAY-9003",
            PaymentStatus.APPROVED,
            "Analyst released the payment after customer verification.",
            UUID.randomUUID(),
            Instant.parse("2026-07-16T12:10:00Z")
        );

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentStateTransitionRepository).save(any(PaymentStateTransitionEntity.class));
        verify(paymentOrchestrationEventPublisher).publish(any(PaymentStatusChangedEvent.class));
    }

    @Test
    void completesChallengeAndMarksPaymentApprovedWhenChallengePasses() {
        UUID assessmentId = UUID.randomUUID();
        PaymentRecordEntity paymentRecord = new PaymentRecordEntity(
            UUID.randomUUID(),
            ORGANIZATION_ID,
            "PAY-9004",
            "CUST-12",
            BigDecimal.valueOf(5100),
            "ZAR",
            "WEB",
            "TRAVEL",
            assessmentId,
            57,
            RiskDecision.CHALLENGE,
            PaymentStatus.CHALLENGED,
            null,
            Instant.parse("2026-07-16T12:00:00Z"),
            null,
            null,
            null,
            Instant.parse("2026-07-16T12:00:00Z"),
            Instant.parse("2026-07-16T12:00:00Z")
        );
        when(paymentRecordRepository.findByOrganizationIdAndPaymentId(ORGANIZATION_ID, "PAY-9004")).thenReturn(Optional.of(paymentRecord));

        PaymentRecordEntity result = paymentLifecycleService.completeChallenge(
            ORGANIZATION_ID,
            "PAY-9004",
            "analyst.one",
            new CompletePaymentChallengeRequest(ChallengeOutcome.PASSED, "Customer completed OTP challenge."),
            assessmentId,
            Instant.parse("2026-07-16T12:08:00Z")
        );

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getChallengeOutcome()).isEqualTo(ChallengeOutcome.PASSED);
        assertThat(result.getChallengeCompletedBy()).isEqualTo("analyst.one");
        verify(paymentStateTransitionRepository).save(any(PaymentStateTransitionEntity.class));
        verify(paymentOrchestrationEventPublisher).publish(any(PaymentStatusChangedEvent.class));
    }

    private PaymentRiskAssessmentRequest request(String paymentId, String customerId) {
        return new PaymentRiskAssessmentRequest(
            paymentId,
            customerId,
            BigDecimal.valueOf(7000),
            "ZAR",
            "ELECTRONICS",
            "MOBILE_APP",
            BigDecimal.valueOf(1300),
            2,
            BigDecimal.valueOf(8000),
            5,
            true,
            false,
            false,
            false
        );
    }

    private FraudAssessmentRecordEntity assessment(String paymentId, String customerId, RiskDecision decision) {
        return new FraudAssessmentRecordEntity(
            UUID.randomUUID(),
            ORGANIZATION_ID,
            paymentId,
            customerId,
            88,
            decision,
            VelocitySource.REDIS,
            "Fraud decision summary",
            "[HIGH_VELOCITY]",
            null,
            0,
            "rules-v1",
            "{}",
            "[]",
            null,
            null,
            Instant.parse("2026-07-16T12:00:00Z")
        );
    }
}
