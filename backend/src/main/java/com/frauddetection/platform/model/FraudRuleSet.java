package com.frauddetection.platform.model;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.IntStream;

public record FraudRuleSet(
    BigDecimal amountSpikeMultiplier,
    int amountSpikeWeight,
    int velocityCountThreshold,
    int velocityWeight,
    BigDecimal spendBurstMultiplier,
    int spendBurstWeight,
    int newDeviceWeight,
    int impossibleTravelWeight,
    int beneficiaryAgeHoursThreshold,
    int newBeneficiaryWeight,
    int recentPasswordResetWeight,
    Set<String> riskyMerchantCategories,
    int riskyMerchantWeight,
    int highRiskCountryWeight
) {
    public FraudRuleSet {
        riskyMerchantCategories = Set.copyOf(riskyMerchantCategories);
        if (amountSpikeMultiplier.signum() <= 0 || spendBurstMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Rule multipliers must be greater than zero.");
        }
        if (velocityCountThreshold <= 0 || beneficiaryAgeHoursThreshold < 0) {
            throw new IllegalArgumentException("Rule thresholds must be valid positive values.");
        }
        if (IntStream.of(
            amountSpikeWeight,
            velocityWeight,
            spendBurstWeight,
            newDeviceWeight,
            impossibleTravelWeight,
            newBeneficiaryWeight,
            recentPasswordResetWeight,
            riskyMerchantWeight,
            highRiskCountryWeight
        ).anyMatch(weight -> weight < 0 || weight > 100)) {
            throw new IllegalArgumentException("Rule weights must be between zero and 100.");
        }
    }

    public static FraudRuleSet defaults() {
        return new FraudRuleSet(
            BigDecimal.valueOf(3), 25,
            4, 20,
            BigDecimal.valueOf(8), 15,
            10, 25,
            24, 15,
            15,
            Set.of("ELECTRONICS", "CRYPTO", "GIFT_CARD", "MONEY_TRANSFER"),
            12,
            18
        );
    }
}
