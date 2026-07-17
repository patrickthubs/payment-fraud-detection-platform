package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudEventProperties;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudNotificationType;
import com.frauddetection.platform.model.PaymentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FraudNotificationHookService {

    private final FraudOutboundEventService fraudOutboundEventService;
    private final FraudEventProperties fraudEventProperties;

    public FraudNotificationHookService(
        FraudOutboundEventService fraudOutboundEventService,
        FraudEventProperties fraudEventProperties
    ) {
        this.fraudOutboundEventService = fraudOutboundEventService;
        this.fraudEventProperties = fraudEventProperties;
    }

    public void publishAssessmentNotifications(
        PaymentRecordEntity paymentRecord,
        FraudReviewCaseEntity reviewCase,
        FraudRiskAssessment assessment,
        Instant createdAt
    ) {
        List<FraudNotificationEvent> events = new ArrayList<>();

        if (paymentRecord.getPaymentStatus() == PaymentStatus.CHALLENGED) {
            events.add(event(
                FraudNotificationType.PAYMENT_CHALLENGED,
                reviewCase,
                paymentRecord,
                "Payment challenge required",
                "Payment %s requires additional customer verification."
                    .formatted(paymentRecord.getPaymentId()),
                createdAt
            ));
        }

        if (paymentRecord.getPaymentStatus() == PaymentStatus.HELD) {
            events.add(event(
                FraudNotificationType.PAYMENT_HELD_FOR_REVIEW,
                reviewCase,
                paymentRecord,
                "Payment held for manual review",
                "Payment %s was held because %s"
                    .formatted(paymentRecord.getPaymentId(), assessment.summary()),
                createdAt
            ));
        }

        if (reviewCase != null) {
            events.add(event(
                FraudNotificationType.REVIEW_CASE_CREATED,
                reviewCase,
                paymentRecord,
                "Fraud review case created",
                "Fraud review case %s was created for payment %s."
                    .formatted(reviewCase.getId(), paymentRecord.getPaymentId()),
                createdAt
            ));
        }

        if (paymentRecord.getPaymentStatus() == PaymentStatus.DECLINED) {
            events.add(event(
                FraudNotificationType.PAYMENT_DECLINED,
                reviewCase,
                paymentRecord,
                "Payment declined",
                "Payment %s was declined by the fraud decision engine."
                    .formatted(paymentRecord.getPaymentId()),
                createdAt
            ));
        }

        events.forEach(this::publish);
    }

    public void publishResolutionNotification(
        FraudReviewCaseEntity reviewCase,
        PaymentRecordEntity paymentRecord,
        CaseResolutionOutcome outcome,
        Instant createdAt
    ) {
        FraudNotificationType notificationType = switch (outcome) {
            case RELEASE_PAYMENT -> FraudNotificationType.PAYMENT_RELEASED;
            case CONFIRM_DECLINE -> FraudNotificationType.DECLINE_CONFIRMED;
        };

        String subject = switch (outcome) {
            case RELEASE_PAYMENT -> "Payment released after review";
            case CONFIRM_DECLINE -> "Payment decline confirmed";
        };

        String message = switch (outcome) {
            case RELEASE_PAYMENT -> "Fraud review case %s released payment %s."
                .formatted(reviewCase.getId(), paymentRecord.getPaymentId());
            case CONFIRM_DECLINE -> "Fraud review case %s confirmed the decline for payment %s."
                .formatted(reviewCase.getId(), paymentRecord.getPaymentId());
        };

        publish(event(notificationType, reviewCase, paymentRecord, subject, message, createdAt));
    }

    private FraudNotificationEvent event(
        FraudNotificationType notificationType,
        FraudReviewCaseEntity reviewCase,
        PaymentRecordEntity paymentRecord,
        String subject,
        String message,
        Instant createdAt
    ) {
        return new FraudNotificationEvent(
            notificationType,
            reviewCase == null ? null : reviewCase.getId(),
            paymentRecord.getPaymentId(),
            paymentRecord.getCustomerId(),
            paymentRecord.getPaymentStatus(),
            subject,
            message,
            createdAt
        );
    }

    private void publish(FraudNotificationEvent event) {
        fraudOutboundEventService.enqueue(
            event.notificationType().name(),
            fraudEventProperties.notificationTopic(),
            event.paymentId(),
            event,
            event.createdAt()
        );
    }
}
