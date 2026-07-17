package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudRiskScoringServiceTest {

    private FraudRiskScoringService fraudRiskScoringService;
    private FraudScoringProfileService fraudScoringProfileService;

    @BeforeEach
    void setUp() {
        fraudScoringProfileService = mock(FraudScoringProfileService.class);
        when(fraudScoringProfileService.activeProfile()).thenReturn(new FraudScoringProfile(45, 65, 85));
        fraudRiskScoringService = new FraudRiskScoringService(fraudScoringProfileService);
    }

    @Test
    void allowsLowRiskPayment() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-001",
            "CUST-001",
            BigDecimal.valueOf(950),
            "ZAR",
            "GROCERY",
            "MOBILE_APP",
            BigDecimal.valueOf(1200),
            1,
            BigDecimal.valueOf(1800),
            240,
            false,
            false,
            false,
            false
        );

        FraudRiskAssessment assessment = fraudRiskScoringService.assess(
            request,
            new VelocitySnapshot(1, BigDecimal.valueOf(1800), VelocitySource.REQUEST_FALLBACK)
        );

        assertThat(assessment.decision()).isEqualTo(RiskDecision.ALLOW);
        assertThat(assessment.riskScore()).isZero();
        assertThat(assessment.triggeredFactors()).isEmpty();
    }

    @Test
    void challengesModeratelyRiskyPayment() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-002",
            "CUST-002",
            BigDecimal.valueOf(5000),
            "ZAR",
            "ELECTRONICS",
            "WEB",
            BigDecimal.valueOf(1500),
            2,
            BigDecimal.valueOf(4000),
            8,
            true,
            false,
            false,
            false
        );

        FraudRiskAssessment assessment = fraudRiskScoringService.assess(
            request,
            new VelocitySnapshot(2, BigDecimal.valueOf(4000), VelocitySource.REQUEST_FALLBACK)
        );

        assertThat(assessment.decision()).isEqualTo(RiskDecision.CHALLENGE);
        assertThat(assessment.riskScore()).isGreaterThanOrEqualTo(45);
    }

    @Test
    void declinesStronglySuspiciousPayment() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-003",
            "CUST-003",
            BigDecimal.valueOf(18000),
            "ZAR",
            "CRYPTO",
            "MOBILE_APP",
            BigDecimal.valueOf(2000),
            6,
            BigDecimal.valueOf(30000),
            1,
            true,
            true,
            true,
            true
        );

        FraudRiskAssessment assessment = fraudRiskScoringService.assess(
            request,
            new VelocitySnapshot(6, BigDecimal.valueOf(30000), VelocitySource.REQUEST_FALLBACK)
        );

        assertThat(assessment.decision()).isEqualTo(RiskDecision.DECLINE);
        assertThat(assessment.riskScore()).isEqualTo(100);
        assertThat(assessment.triggeredFactors()).isNotEmpty();
    }

    @Test
    void usesResolvedVelocitySnapshotForDecisioning() {
        PaymentRiskAssessmentRequest request = new PaymentRiskAssessmentRequest(
            "PAY-004",
            "CUST-004",
            BigDecimal.valueOf(3500),
            "ZAR",
            "GROCERY",
            "WEB",
            BigDecimal.valueOf(1000),
            0,
            BigDecimal.ZERO,
            120,
            false,
            false,
            false,
            false
        );

        FraudRiskAssessment assessment = fraudRiskScoringService.assess(
            request,
            new VelocitySnapshot(5, BigDecimal.valueOf(9000), VelocitySource.REDIS)
        );

        assertThat(assessment.decision()).isEqualTo(RiskDecision.CHALLENGE);
        assertThat(assessment.triggeredFactors())
            .extracting(factor -> factor.code().name())
            .contains("HIGH_VELOCITY", "SPEND_BURST");
    }
}
