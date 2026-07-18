package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.RiskFactor;
import com.frauddetection.platform.model.RiskFactorCode;
import com.frauddetection.platform.model.FraudRuleSet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.frauddetection.platform.dto.FraudScoringProfileResponse;

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
        FraudRuleSet rules = profile.rules();

        BigDecimal amountRatio = request.amount()
            .divide(request.customerAverageTicket(), 2, RoundingMode.HALF_UP);

        if (amountRatio.compareTo(rules.amountSpikeMultiplier()) >= 0) {
            factors.add(new RiskFactor(
                RiskFactorCode.AMOUNT_SPIKE,
                rules.amountSpikeWeight(),
                "Payment amount is at least %sx the customer's average ticket."
                    .formatted(rules.amountSpikeMultiplier().stripTrailingZeros().toPlainString())
            ));
        }

        if (velocitySnapshot.transactionCountLastFiveMinutes() >= rules.velocityCountThreshold()) {
            factors.add(new RiskFactor(
                RiskFactorCode.HIGH_VELOCITY,
                rules.velocityWeight(),
                "Customer has at least %d transactions in the last 5 minutes."
                    .formatted(rules.velocityCountThreshold())
            ));
        }

        if (velocitySnapshot.spendLastHour().compareTo(request.customerAverageTicket().multiply(rules.spendBurstMultiplier())) >= 0) {
            factors.add(new RiskFactor(
                RiskFactorCode.SPEND_BURST,
                rules.spendBurstWeight(),
                "Spend in the last hour is materially above the customer's baseline."
            ));
        }

        if (request.newDevice()) {
            factors.add(new RiskFactor(
                RiskFactorCode.NEW_DEVICE,
                rules.newDeviceWeight(),
                "Payment was initiated from a device the customer has not used recently."
            ));
        }

        if (request.impossibleTravel()) {
            factors.add(new RiskFactor(
                RiskFactorCode.IMPOSSIBLE_TRAVEL,
                rules.impossibleTravelWeight(),
                "Location behavior indicates impossible travel or conflicting session geography."
            ));
        }

        if (request.beneficiaryAgeHours() <= rules.beneficiaryAgeHoursThreshold()) {
            factors.add(new RiskFactor(
                RiskFactorCode.NEW_BENEFICIARY,
                rules.newBeneficiaryWeight(),
                "Beneficiary relationship is at most %d hours old."
                    .formatted(rules.beneficiaryAgeHoursThreshold())
            ));
        }

        if (request.recentPasswordReset()) {
            factors.add(new RiskFactor(
                RiskFactorCode.RECENT_PASSWORD_RESET,
                rules.recentPasswordResetWeight(),
                "A recent account security change increases account-takeover risk."
            ));
        }

        if (isHighRiskMerchantCategory(request.merchantCategory(), rules)) {
            factors.add(new RiskFactor(
                RiskFactorCode.HIGH_RISK_MERCHANT,
                rules.riskyMerchantWeight(),
                "Merchant category is historically associated with elevated fraud pressure."
            ));
        }

        if (request.highRiskCountry()) {
            factors.add(new RiskFactor(
                RiskFactorCode.HIGH_RISK_COUNTRY,
                rules.highRiskCountryWeight(),
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

    public FraudScoringProfileResponse activeProfileDetails() {
        return fraudScoringProfileService.readActiveProfile();
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

    private boolean isHighRiskMerchantCategory(String merchantCategory, FraudRuleSet rules) {
        return rules.riskyMerchantCategories().contains(merchantCategory.trim().toUpperCase());
    }

    private String buildSummary(RiskDecision decision, List<RiskFactor> factors) {
        if (factors.isEmpty()) {
            return "No elevated fraud signals were triggered by the current rule set.";
        }
        return "%s because %d risk factors were triggered.".formatted(decision, factors.size());
    }
}
