package com.frauddetection.platform.service;

import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.RiskFactor;
import java.util.List;

public record FraudRiskAssessment(
    int riskScore,
    RiskDecision decision,
    String summary,
    List<RiskFactor> triggeredFactors
) {
}
