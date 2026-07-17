package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.RiskFactor;
import com.frauddetection.platform.model.RiskFactorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FraudRiskScoringService {

    private final FraudScoringProfileService fraudScoringProfileService;

    public FraudRiskScoringService(FraudScoringProfileService fraudScoringProfileService) {
        this.fraudScoringProfileService = fraudScoringProfileService;
    }

    public FraudRiskAssessment assess(PaymentRiskAssessmentRequest request, VelocitySnapshot velocitySnapshot) {
        return assess(request, velocitySnapshot, activeProfile());
    }

    public FraudRiskAssessment assess(
        PaymentRiskAssessmentRequest request,
        VelocitySnapshot velocitySnapshot,
        FraudScoringProfile profile
    ) {
        List<RiskFactor> factors = new ArrayList<>();

        BigDecimal amountRatio = request.amount()
            .divide(request.customerAverageTicket(), 2, RoundingMode.HALF_UP);

        if (amountRatio.compareTo(BigDecimal.valueOf(3.0)) >= 0) {
            factors.add(new RiskFactor(
                RiskFactorCode.AMOUNT_SPIKE,
                25,
                "Payment amount is at least 3x the customer's average ticket."
            ));
        }

        if (velocitySnapshot.transactionCountLastFiveMinutes() >= 4) {
            factors.add(new RiskFactor(
                RiskFactorCode.HIGH_VELOCITY,
                20,
                "Customer has at least 4 transactions in the last 5 minutes."
            ));
        }

        if (velocitySnapshot.spendLastHour().compareTo(request.customerAverageTicket().multiply(BigDecimal.valueOf(8))) >= 0) {
            factors.add(new RiskFactor(
                RiskFactorCode.SPEND_BURST,
                15,
                "Spend in the last hour is materially above the customer's baseline."
            ));
        }

        if (request.newDevice()) {
            factors.add(new RiskFactor(
                RiskFactorCode.NEW_DEVICE,
                10,
                "Payment was initiated from a device the customer has not used recently."
            ));
        }

        if (request.impossibleTravel()) {
            factors.add(new RiskFactor(
                RiskFactorCode.IMPOSSIBLE_TRAVEL,
                25,
                "Location behavior indicates impossible travel or conflicting session geography."
            ));
        }

        if (request.beneficiaryAgeHours() <= 24) {
            factors.add(new RiskFactor(
                RiskFactorCode.NEW_BENEFICIARY,
                15,
                "Beneficiary relationship is less than 24 hours old."
            ));
        }

        if (request.recentPasswordReset()) {
            factors.add(new RiskFactor(
                RiskFactorCode.RECENT_PASSWORD_RESET,
                15,
                "A recent account security change increases account-takeover risk."
            ));
        }

        if (isHighRiskMerchantCategory(request.merchantCategory())) {
            factors.add(new RiskFactor(
                RiskFactorCode.HIGH_RISK_MERCHANT,
                12,
                "Merchant category is historically associated with elevated fraud pressure."
            ));
        }

        if (request.highRiskCountry()) {
            factors.add(new RiskFactor(
                RiskFactorCode.HIGH_RISK_COUNTRY,
                18,
                "The payment is linked to a geography with elevated fraud risk."
            ));
        }

        int rawScore = factors.stream()
            .mapToInt(RiskFactor::weight)
            .sum();
        int cappedScore = Math.min(rawScore, 100);

        RiskDecision decision = toDecision(cappedScore, profile);
        String summary = buildSummary(decision, factors);

        return new FraudRiskAssessment(cappedScore, decision, summary, List.copyOf(factors));
    }

    public FraudScoringProfile activeProfile() {
        return fraudScoringProfileService.activeProfile();
    }

    private RiskDecision toDecision(int riskScore, FraudScoringProfile profile) {
        if (riskScore >= profile.declineThreshold()) {
            return RiskDecision.DECLINE;
        }
        if (riskScore >= profile.holdThreshold()) {
            return RiskDecision.HOLD;
        }
        if (riskScore >= profile.challengeThreshold()) {
            return RiskDecision.CHALLENGE;
        }
        return RiskDecision.ALLOW;
    }

    private boolean isHighRiskMerchantCategory(String merchantCategory) {
        return switch (merchantCategory.trim().toUpperCase()) {
            case "ELECTRONICS", "CRYPTO", "GIFT_CARD", "MONEY_TRANSFER" -> true;
            default -> false;
        };
    }

    private String buildSummary(RiskDecision decision, List<RiskFactor> factors) {
        if (factors.isEmpty()) {
            return "No elevated fraud signals were triggered by the current rule set.";
        }
        return "%s because %d risk factors were triggered.".formatted(decision, factors.size());
    }
}
