package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.FraudOutcomeRequest;
import com.frauddetection.platform.dto.FraudOutcomeResponse;
import com.frauddetection.platform.dto.FraudQualityMetricsResponse;
import com.frauddetection.platform.service.AuthenticatedOperatorService;
import com.frauddetection.platform.service.FraudOutcomeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-outcomes")
public class FraudOutcomeController {

    private final FraudOutcomeService fraudOutcomeService;
    private final AuthenticatedOperatorService authenticatedOperatorService;

    public FraudOutcomeController(
        FraudOutcomeService fraudOutcomeService,
        AuthenticatedOperatorService authenticatedOperatorService
    ) {
        this.fraudOutcomeService = fraudOutcomeService;
        this.authenticatedOperatorService = authenticatedOperatorService;
    }

    @PostMapping("/assessments/{assessmentId}")
    public FraudOutcomeResponse record(
        @PathVariable UUID assessmentId,
        @Valid @RequestBody FraudOutcomeRequest request,
        Authentication authentication
    ) {
        return fraudOutcomeService.record(
            assessmentId, request, authenticatedOperatorService.resolveOperator(authentication)
        );
    }

    @GetMapping("/assessments/{assessmentId}")
    public FraudOutcomeResponse findByAssessmentId(@PathVariable UUID assessmentId) {
        return fraudOutcomeService.findByAssessmentId(assessmentId);
    }

    @GetMapping("/quality-metrics")
    public FraudQualityMetricsResponse qualityMetrics() {
        return fraudOutcomeService.qualityMetrics();
    }
}
