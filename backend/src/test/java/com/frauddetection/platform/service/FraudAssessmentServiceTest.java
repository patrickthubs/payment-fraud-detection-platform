package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudAssessmentServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    @Mock
    private VelocityFeatureService velocityFeatureService;

    @Mock
    private FraudAssessmentRecordRepository fraudAssessmentRecordRepository;

    @Mock
    private FraudReviewCaseRepository fraudReviewCaseRepository;

    @Mock
    private FraudAssessmentEventPublisher fraudAssessmentEventPublisher;

    @Mock
    private PaymentLifecycleService paymentLifecycleService;

    @Mock
    private FraudNotificationHookService fraudNotificationHookService;

    @Mock
    private CurrentTenantService currentTenantService;

    private FraudAssessmentService fraudAssessmentService;
    private FraudScoringProfileService fraudScoringProfileService;

    @BeforeEach
    void setUp() {
        fraudScoringProfileService = mock(FraudScoringProfileService.class);
        when(fraudScoringProfileService.activeProfile()).thenReturn(new FraudScoringProfile(45, 65, 85));
        when(currentTenantService.organizationId()).thenReturn(ORGANIZATION_ID);
        fraudAssessmentService = new FraudAssessmentService(
            velocityFeatureService,
            new FraudRiskScoringService(fraudScoringProfileService),
            fraudAssessmentRecordRepository,
            fraudReviewCaseRepository,
            fraudAssessmentEventPublisher,
            paymentLifecycleService,
            new PlatformMetricsService(new SimpleMeterRegistry()),
            fraudNotificationHookService,
            currentTenantService
        );

        when(fraudAssessmentRecordRepository.save(any(FraudAssessmentRecordEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsReviewCaseForHighRiskDecision() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-1001",
            "CUST-1001",
            BigDecimal.valueOf(20000),
            "ZAR",
            "CRYPTO",
            "MOBILE_APP",
            BigDecimal.valueOf(1500),
            1,
            BigDecimal.valueOf(1000),
            1,
            true,
            true,
            true,
            true
        );

        when(velocityFeatureService.resolveFeatures(request))
            .thenReturn(new VelocitySnapshot(6, BigDecimal.valueOf(30000), VelocitySource.REDIS));
        when(fraudReviewCaseRepository.save(any(FraudReviewCaseEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentLifecycleService.recordAssessmentOutcome(any(), any(), any()))
            .thenReturn(buildPaymentRecord(request.paymentId(), request.customerId(), PaymentStatus.DECLINED));

        FraudAssessmentResult result = fraudAssessmentService.assess(request);

        assertThat(result.assessment().decision()).isEqualTo(RiskDecision.DECLINE);
        assertThat(result.reviewCaseId()).isNotNull();
        verify(fraudReviewCaseRepository).save(any(FraudReviewCaseEntity.class));
        verify(fraudAssessmentEventPublisher).publish(any(), any(), any(), any());
        verify(fraudNotificationHookService).publishAssessmentNotifications(any(), any(), any(), any());
        verify(velocityFeatureService).recordAssessment(request, new VelocitySnapshot(6, BigDecimal.valueOf(30000), VelocitySource.REDIS));
    }

    @Test
    void skipsReviewCaseForAllowDecision() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-1002",
            "CUST-1002",
            BigDecimal.valueOf(800),
            "ZAR",
            "GROCERY",
            "WEB",
            BigDecimal.valueOf(1200),
            0,
            BigDecimal.valueOf(800),
            72,
            false,
            false,
            false,
            false
        );

        VelocitySnapshot snapshot = new VelocitySnapshot(0, BigDecimal.valueOf(800), VelocitySource.REQUEST_FALLBACK);
        when(velocityFeatureService.resolveFeatures(request)).thenReturn(snapshot);
        when(paymentLifecycleService.recordAssessmentOutcome(any(), any(), any()))
            .thenReturn(buildPaymentRecord(request.paymentId(), request.customerId(), PaymentStatus.APPROVED));

        FraudAssessmentResult result = fraudAssessmentService.assess(request);

        assertThat(result.assessmentId()).isNotNull();
        assertThat(result.reviewCaseId()).isNull();
        assertThat(result.assessment().decision()).isEqualTo(RiskDecision.ALLOW);
        verify(fraudReviewCaseRepository, never()).save(any(FraudReviewCaseEntity.class));
    }

    private PaymentRecordEntity buildPaymentRecord(String paymentId, String customerId, PaymentStatus paymentStatus) {
        return new PaymentRecordEntity(
            UUID.randomUUID(),
            ORGANIZATION_ID,
            paymentId,
            customerId,
            BigDecimal.valueOf(1200),
            "ZAR",
            "WEB",
            "GROCERY",
            UUID.randomUUID(),
            45,
            paymentStatus == PaymentStatus.DECLINED ? RiskDecision.DECLINE : RiskDecision.ALLOW,
            paymentStatus,
            null,
            null,
            null,
            null,
            null,
            Instant.parse("2026-07-16T10:00:00Z"),
            Instant.parse("2026-07-16T10:00:00Z")
        );
    }
}
