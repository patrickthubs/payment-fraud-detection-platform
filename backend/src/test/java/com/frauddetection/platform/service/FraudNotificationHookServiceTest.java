package com.frauddetection.platform.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.frauddetection.platform.config.FraudEventProperties;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class FraudNotificationHookServiceTest {

    @Mock
    private FraudOutboundEventService fraudOutboundEventService;

    private FraudNotificationHookService fraudNotificationHookService;

    @BeforeEach
    void setUp() {
        fraudNotificationHookService = new FraudNotificationHookService(
            fraudOutboundEventService,
            new FraudEventProperties("fraud-assessment-completed", "payment-status-changed", "fraud-notifications")
        );
    }

    @Test
    void publishesHeldAndReviewCaseNotifications() {
        fraudNotificationHookService.publishAssessmentNotifications(
            paymentRecord(PaymentStatus.HELD),
            reviewCase(),
            new FraudRiskAssessment(70, RiskDecision.HOLD, "Held for review.", List.of()),
            Instant.parse("2026-07-17T10:00:00Z")
        );

        verify(fraudOutboundEventService, times(2)).enqueue(
            any(),
            eq("fraud-notifications"),
            eq("PAY-1"),
            any(FraudNotificationEvent.class),
            eq(Instant.parse("2026-07-17T10:00:00Z"))
        );
    }

    @Test
    void publishesResolutionNotification() {
        fraudNotificationHookService.publishResolutionNotification(
            reviewCase(),
            paymentRecord(PaymentStatus.APPROVED),
            CaseResolutionOutcome.RELEASE_PAYMENT,
            Instant.parse("2026-07-17T10:15:00Z")
        );

        verify(fraudOutboundEventService).enqueue(
            eq("PAYMENT_RELEASED"),
            eq("fraud-notifications"),
            eq("PAY-1"),
            any(FraudNotificationEvent.class),
            eq(Instant.parse("2026-07-17T10:15:00Z"))
        );
    }

    private PaymentRecordEntity paymentRecord(PaymentStatus paymentStatus) {
        return new PaymentRecordEntity(
            UUID.randomUUID(),
            "PAY-1",
            "CUST-1",
            BigDecimal.valueOf(9000),
            "ZAR",
            "MOBILE_APP",
            "ELECTRONICS",
            UUID.randomUUID(),
            70,
            RiskDecision.HOLD,
            paymentStatus,
            null,
            null,
            null,
            null,
            null,
            Instant.parse("2026-07-17T10:00:00Z"),
            Instant.parse("2026-07-17T10:00:00Z")
        );
    }

    private FraudReviewCaseEntity reviewCase() {
        return new FraudReviewCaseEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "PAY-1",
            "CUST-1",
            70,
            RiskDecision.HOLD,
            ReviewCaseStatus.OPEN,
            "Held for review.",
            null,
            null,
            null,
            Instant.parse("2026-07-17T10:00:00Z"),
            Instant.parse("2026-07-17T10:00:00Z")
        );
    }
}
