package com.frauddetection.platform.service;

import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlatformMetricsService {

    private final MeterRegistry meterRegistry;

    public PlatformMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startAssessmentSample() {
        return Timer.start(meterRegistry);
    }

    public void recordAssessment(
        Timer.Sample sample,
        RiskDecision decision,
        PaymentStatus paymentStatus,
        VelocitySource velocitySource,
        boolean reviewCaseCreated
    ) {
        meterRegistry.counter("fraud.assessments.total", tags(
            "decision", decision.name(),
            "payment_status", paymentStatus.name(),
            "velocity_source", velocitySource.name(),
            "review_case_created", Boolean.toString(reviewCaseCreated)
        )).increment();

        sample.stop(Timer.builder("fraud.assessments.duration")
            .tags(tags(
                "decision", decision.name(),
                "payment_status", paymentStatus.name()
            ))
            .register(meterRegistry));
    }

    public void recordCaseAction(FraudCaseActionType actionType, String caseStatus) {
        meterRegistry.counter("fraud.case.actions.total", tags(
            "action", actionType.name(),
            "case_status", caseStatus
        )).increment();
    }

    public void recordCaseResolution(CaseResolutionOutcome outcome, PaymentStatus paymentStatus) {
        meterRegistry.counter("fraud.case.resolutions.total", tags(
            "outcome", outcome.name(),
            "payment_status", paymentStatus.name()
        )).increment();
    }

    public void recordPaymentTransition(PaymentStatus previousStatus, PaymentStatus targetStatus, String source) {
        meterRegistry.counter("fraud.payment.transitions.total", tags(
            "from_status", previousStatus == null ? "NONE" : previousStatus.name(),
            "to_status", targetStatus.name(),
            "source", source
        )).increment();
    }

    private Iterable<Tag> tags(String... keyValues) {
        List<Tag> tags = new ArrayList<>(keyValues.length / 2);
        for (int index = 0; index < keyValues.length; index += 2) {
            tags.add(Tag.of(keyValues[index], keyValues[index + 1]));
        }
        return tags;
    }
}
