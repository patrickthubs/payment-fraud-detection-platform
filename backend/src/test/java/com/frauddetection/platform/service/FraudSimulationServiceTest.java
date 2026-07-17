package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.config.FraudScoringProperties;
import com.frauddetection.platform.dto.FraudScoringOverrideRequest;
import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudSimulationServiceTest {

    @Mock
    private VelocityFeatureService velocityFeatureService;

    private FraudScoringProfileService fraudScoringProfileService;
    private FraudSimulationService fraudSimulationService;

    @BeforeEach
    void setUp() {
        fraudScoringProfileService = mock(FraudScoringProfileService.class);
        when(fraudScoringProfileService.activeProfile()).thenReturn(FraudScoringProfile.from(new FraudScoringProperties(45, 65, 85)));
        fraudSimulationService = new FraudSimulationService(
            velocityFeatureService,
            new FraudRiskScoringService(fraudScoringProfileService),
            new PlatformMetricsService(new SimpleMeterRegistry())
        );
    }

    @Test
    void simulatesDeclineWithoutPersistence() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-SIM-1001",
            "CUST-SIM-1001",
            BigDecimal.valueOf(18000),
            "ZAR",
            "CRYPTO",
            "MOBILE_APP",
            BigDecimal.valueOf(1500),
            5,
            BigDecimal.valueOf(26000),
            1,
            true,
            true,
            true,
            true
        );

        when(velocityFeatureService.resolveFeatures(request))
            .thenReturn(new VelocitySnapshot(5, BigDecimal.valueOf(26000), VelocitySource.REDIS));

        FraudSimulationResult result = fraudSimulationService.simulate(request);

        assertThat(result.assessment().decision()).isEqualTo(RiskDecision.DECLINE);
        assertThat(result.projectedPaymentStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(result.reviewCaseWouldBeCreated()).isTrue();
    }

    @Test
    void simulatesAllowForLowRiskScenario() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-SIM-1002",
            "CUST-SIM-1002",
            BigDecimal.valueOf(900),
            "ZAR",
            "GROCERY",
            "WEB",
            BigDecimal.valueOf(1200),
            0,
            BigDecimal.valueOf(900),
            72,
            false,
            false,
            false,
            false
        );

        when(velocityFeatureService.resolveFeatures(request))
            .thenReturn(new VelocitySnapshot(0, BigDecimal.valueOf(900), VelocitySource.REQUEST_FALLBACK));

        FraudSimulationResult result = fraudSimulationService.simulate(request);

        assertThat(result.assessment().decision()).isEqualTo(RiskDecision.ALLOW);
        assertThat(result.projectedPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.reviewCaseWouldBeCreated()).isFalse();
    }

    @Test
    void mergesThresholdOverridesOntoActiveProfile() {
        FraudScoringProfile mergedProfile = fraudSimulationService.mergeOverrides(
            new FraudScoringOverrideRequest(40, null, 90)
        );

        assertThat(mergedProfile.challengeThreshold()).isEqualTo(40);
        assertThat(mergedProfile.holdThreshold()).isEqualTo(65);
        assertThat(mergedProfile.declineThreshold()).isEqualTo(90);
    }
}
