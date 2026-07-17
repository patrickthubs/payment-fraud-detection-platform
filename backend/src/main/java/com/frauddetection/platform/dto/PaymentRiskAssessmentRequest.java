package com.frauddetection.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentRiskAssessmentRequest(
    @NotBlank String paymentId,
    @NotBlank String customerId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String merchantCategory,
    @NotBlank String paymentChannel,
    @NotNull @Positive BigDecimal customerAverageTicket,
    @Min(0) int transactionCountLastFiveMinutes,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal spendLastHour,
    @Min(0) int beneficiaryAgeHours,
    boolean newDevice,
    boolean highRiskCountry,
    boolean impossibleTravel,
    boolean recentPasswordReset
) {
}
