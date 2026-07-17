package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;

public interface VelocityFeatureService {

    VelocitySnapshot resolveFeatures(PaymentRiskAssessmentRequest request);

    void recordAssessment(PaymentRiskAssessmentRequest request, VelocitySnapshot velocitySnapshot);
}
