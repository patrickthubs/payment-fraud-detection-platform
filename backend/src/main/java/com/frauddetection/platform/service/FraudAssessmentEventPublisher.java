package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudEventProperties;
import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class FraudAssessmentEventPublisher {

    private final FraudOutboundEventService fraudOutboundEventService;
    private final FraudEventProperties fraudEventProperties;

    public FraudAssessmentEventPublisher(
        FraudOutboundEventService fraudOutboundEventService,
        FraudEventProperties fraudEventProperties
    ) {
        this.fraudOutboundEventService = fraudOutboundEventService;
        this.fraudEventProperties = fraudEventProperties;
    }

    public void publish(
        FraudAssessmentRecordEntity assessmentRecord,
        FraudRiskAssessment assessment,
        FraudReviewCaseEntity reviewCase,
        Instant assessedAt
    ) {
        FraudAssessmentCompletedEvent event = new FraudAssessmentCompletedEvent(
            assessmentRecord.getId(),
            reviewCase == null ? null : reviewCase.getId(),
            assessmentRecord.getPaymentId(),
            assessmentRecord.getCustomerId(),
            assessmentRecord.getRiskScore(),
            assessmentRecord.getDecision(),
            assessmentRecord.getVelocitySource(),
            assessment.triggeredFactors().stream().map(factor -> factor.code().name()).toList(),
            assessmentRecord.getSummary(),
            assessedAt
        );

        fraudOutboundEventService.enqueue(
            FraudAssessmentCompletedEvent.class.getSimpleName(),
            fraudEventProperties.assessmentTopic(),
            assessmentRecord.getPaymentId(),
            event,
            assessedAt
        );
    }
}
