package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.FraudOutcomeLabel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record FraudOutcomeRequest(
    @NotNull FraudOutcomeLabel outcomeLabel,
    @NotBlank @Size(max = 60) String source,
    @NotNull @DecimalMin("0.0") BigDecimal actualLoss,
    @NotNull @DecimalMin("0.0") BigDecimal recoveredAmount,
    @Size(max = 1000) String notes,
    Instant occurredAt
) {
    public FraudOutcomeRequest {
        if (actualLoss != null && recoveredAmount != null && recoveredAmount.compareTo(actualLoss) > 0) {
            throw new IllegalArgumentException("Recovered amount cannot exceed actual loss.");
        }
    }
}
