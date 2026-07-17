package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlatformMetricsServiceTest {

    private SimpleMeterRegistry meterRegistry;
    private PlatformMetricsService platformMetricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        platformMetricsService = new PlatformMetricsService(meterRegistry);
    }

    @Test
    void recordsAssessmentMetrics() {
        Timer.Sample sample = platformMetricsService.startAssessmentSample();

        platformMetricsService.recordAssessment(
            sample,
            RiskDecision.HOLD,
            PaymentStatus.HELD,
            VelocitySource.REDIS,
            true
        );

        assertThat(meterRegistry.get("fraud.assessments.total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("fraud.assessments.duration").timer().count()).isEqualTo(1L);
    }

    @Test
    void recordsCaseAndPaymentMetrics() {
        platformMetricsService.recordCaseAction(FraudCaseActionType.RESOLVED, "RESOLVED");
        platformMetricsService.recordCaseResolution(CaseResolutionOutcome.CONFIRM_DECLINE, PaymentStatus.DECLINED);
        platformMetricsService.recordPaymentTransition(PaymentStatus.HELD, PaymentStatus.DECLINED, "CASE_RESOLUTION");

        assertThat(meterRegistry.get("fraud.case.actions.total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("fraud.case.resolutions.total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("fraud.payment.transitions.total").counter().count()).isEqualTo(1.0d);
    }
}
