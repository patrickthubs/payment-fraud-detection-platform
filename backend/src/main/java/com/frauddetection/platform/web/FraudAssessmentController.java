package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.FraudScoringThresholdsResponse;
import com.frauddetection.platform.dto.FraudScoringProfileActivationRequest;
import com.frauddetection.platform.dto.FraudScoringProfileCreateRequest;
import com.frauddetection.platform.dto.FraudScoringProfileResponse;
import com.frauddetection.platform.dto.FraudSimulationComparisonRequest;
import com.frauddetection.platform.dto.FraudSimulationComparisonResponse;
import com.frauddetection.platform.dto.FraudSimulationOutcomeResponse;
import com.frauddetection.platform.dto.FraudSimulationResponse;
import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.dto.PaymentRiskAssessmentResponse;
import com.frauddetection.platform.dto.RiskFactorView;
import com.frauddetection.platform.service.FraudAssessmentResult;
import com.frauddetection.platform.service.FraudAssessmentService;
import com.frauddetection.platform.service.FraudRiskAssessment;
import com.frauddetection.platform.service.FraudScoringProfile;
import com.frauddetection.platform.service.FraudScoringProfileService;
import com.frauddetection.platform.service.FraudSimulationResult;
import com.frauddetection.platform.service.FraudSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import com.frauddetection.platform.service.AuthenticatedOperatorService;

@RestController
@RequestMapping("/api/v1/fraud-assessments")
@Tag(name = "Fraud Assessments", description = "Real-time fraud scoring and payment decisioning endpoints.")
public class FraudAssessmentController {

    private final FraudAssessmentService fraudAssessmentService;
    private final FraudSimulationService fraudSimulationService;
    private final FraudScoringProfileService fraudScoringProfileService;
    private final AuthenticatedOperatorService authenticatedOperatorService;

    public FraudAssessmentController(
        FraudAssessmentService fraudAssessmentService,
        FraudSimulationService fraudSimulationService,
        FraudScoringProfileService fraudScoringProfileService,
        AuthenticatedOperatorService authenticatedOperatorService
    ) {
        this.fraudAssessmentService = fraudAssessmentService;
        this.fraudSimulationService = fraudSimulationService;
        this.fraudScoringProfileService = fraudScoringProfileService;
        this.authenticatedOperatorService = authenticatedOperatorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Assess a payment for fraud risk",
        description = "Scores the incoming payment, persists the latest payment state, and creates a review case when the outcome requires analyst attention."
    )
    public PaymentRiskAssessmentResponse assess(
        @Valid @RequestBody PaymentRiskAssessmentRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        FraudAssessmentResult result = fraudAssessmentService.assess(request, idempotencyKey);
        FraudRiskAssessment assessment = result.assessment();
        List<RiskFactorView> factors = assessment.triggeredFactors().stream()
            .map(factor -> new RiskFactorView(factor.code(), factor.weight(), factor.detail()))
            .toList();

        return new PaymentRiskAssessmentResponse(
            result.assessmentId(),
            result.reviewCaseId(),
            request.paymentId(),
            request.customerId(),
            assessment.riskScore(),
            assessment.decision(),
            result.paymentStatus(),
            result.velocitySource(),
            assessment.summary(),
            factors
        );
    }

    @PostMapping("/simulations")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Simulate a fraud decision",
        description = "Scores a payment scenario without persisting an assessment, payment record, or fraud case."
    )
    public FraudSimulationResponse simulate(@Valid @RequestBody PaymentRiskAssessmentRequest request) {
        FraudSimulationResult result = fraudSimulationService.simulate(request);
        return toSimulationResponse(request, result);
    }

    @GetMapping("/rules")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Read the active fraud scoring thresholds",
        description = "Returns the currently active fraud decision thresholds used by the scoring engine."
    )
    public FraudScoringThresholdsResponse readRules() {
        return fraudScoringProfileService.readActiveProfile().thresholds();
    }

    @GetMapping("/scoring-profiles/active")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Read the active fraud scoring profile",
        description = "Returns the active scoring profile metadata and thresholds used by the decision engine."
    )
    public FraudScoringProfileResponse readActiveScoringProfile() {
        return fraudScoringProfileService.readActiveProfile();
    }

    @GetMapping("/scoring-profiles")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "List fraud scoring profiles",
        description = "Returns known scoring profiles so analysts can inspect draft and active threshold versions."
    )
    public List<FraudScoringProfileResponse> listScoringProfiles() {
        return fraudScoringProfileService.listProfiles();
    }

    @PostMapping("/scoring-profiles")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a fraud scoring profile",
        description = "Creates a versioned fraud scoring profile without activating it."
    )
    public FraudScoringProfileResponse createScoringProfile(
        @Valid @RequestBody FraudScoringProfileCreateRequest request,
        Authentication authentication
    ) {
        return fraudScoringProfileService.createProfile(request, authenticatedOperatorService.resolveOperator(authentication));
    }

    @PostMapping("/scoring-profiles/{profileId}/activate")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Activate a fraud scoring profile",
        description = "Promotes one stored scoring profile to become the live fraud threshold set used by assessments."
    )
    public FraudScoringProfileResponse activateScoringProfile(
        @PathVariable UUID profileId,
        @Valid @RequestBody FraudScoringProfileActivationRequest request,
        Authentication authentication
    ) {
        return fraudScoringProfileService.activateProfile(profileId, request, authenticatedOperatorService.resolveOperator(authentication));
    }

    @PostMapping("/simulations/compare")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Compare baseline and overridden fraud thresholds",
        description = "Runs the same payment scenario twice: once with the active thresholds and once with caller-supplied threshold overrides, without persisting records."
    )
    public FraudSimulationComparisonResponse compareSimulation(
        @Valid @RequestBody FraudSimulationComparisonRequest request
    ) {
        FraudScoringProfile baselineProfile = fraudSimulationService.activeProfile();
        FraudScoringProfile overrideProfile = fraudSimulationService.mergeOverrides(request.overrides());
        FraudSimulationResult baselineResult = fraudSimulationService.simulate(request.scenario(), baselineProfile);
        FraudSimulationResult overrideResult = fraudSimulationService.simulate(request.scenario(), overrideProfile);
        FraudSimulationOutcomeResponse baselineOutcome = toOutcomeResponse(baselineProfile, baselineResult);
        FraudSimulationOutcomeResponse overrideOutcome = toOutcomeResponse(overrideProfile, overrideResult);

        return new FraudSimulationComparisonResponse(
            request.scenario().paymentId(),
            request.scenario().customerId(),
            baselineOutcome,
            overrideOutcome,
            baselineOutcome.decision() != overrideOutcome.decision(),
            baselineOutcome.projectedPaymentStatus() != overrideOutcome.projectedPaymentStatus(),
            baselineOutcome.reviewCaseWouldBeCreated() != overrideOutcome.reviewCaseWouldBeCreated(),
            buildComparisonSummary(baselineOutcome, overrideOutcome)
        );
    }

    @PostMapping("/simulations/compare-saved-profile/{profileId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Compare the active profile against a saved fraud scoring profile",
        description = "Runs the same payment scenario against the live thresholds and a stored candidate profile without persisting records."
    )
    public FraudSimulationComparisonResponse compareSavedProfileSimulation(
        @PathVariable UUID profileId,
        @Valid @RequestBody PaymentRiskAssessmentRequest request
    ) {
        FraudScoringProfile baselineProfile = fraudSimulationService.activeProfile();
        FraudScoringProfile candidateProfile = fraudScoringProfileService.findProfile(profileId);
        FraudSimulationResult baselineResult = fraudSimulationService.simulate(request, baselineProfile);
        FraudSimulationResult candidateResult = fraudSimulationService.simulate(request, candidateProfile);
        FraudSimulationOutcomeResponse baselineOutcome = toOutcomeResponse(baselineProfile, baselineResult);
        FraudSimulationOutcomeResponse overrideOutcome = toOutcomeResponse(candidateProfile, candidateResult);

        return new FraudSimulationComparisonResponse(
            request.paymentId(),
            request.customerId(),
            baselineOutcome,
            overrideOutcome,
            baselineOutcome.decision() != overrideOutcome.decision(),
            baselineOutcome.projectedPaymentStatus() != overrideOutcome.projectedPaymentStatus(),
            baselineOutcome.reviewCaseWouldBeCreated() != overrideOutcome.reviewCaseWouldBeCreated(),
            buildComparisonSummary(baselineOutcome, overrideOutcome)
        );
    }

    private FraudSimulationResponse toSimulationResponse(
        PaymentRiskAssessmentRequest request,
        FraudSimulationResult result
    ) {
        FraudRiskAssessment assessment = result.assessment();
        List<RiskFactorView> factors = assessment.triggeredFactors().stream()
            .map(factor -> new RiskFactorView(factor.code(), factor.weight(), factor.detail()))
            .toList();

        return new FraudSimulationResponse(
            request.paymentId(),
            request.customerId(),
            assessment.riskScore(),
            assessment.decision(),
            result.projectedPaymentStatus(),
            result.velocitySource(),
            result.reviewCaseWouldBeCreated(),
            assessment.summary(),
            factors
        );
    }

    private FraudSimulationOutcomeResponse toOutcomeResponse(
        FraudScoringProfile profile,
        FraudSimulationResult result
    ) {
        FraudRiskAssessment assessment = result.assessment();
        return new FraudSimulationOutcomeResponse(
            profile.challengeThreshold(),
            profile.holdThreshold(),
            profile.declineThreshold(),
            assessment.riskScore(),
            assessment.decision(),
            result.projectedPaymentStatus(),
            result.velocitySource(),
            result.reviewCaseWouldBeCreated(),
            assessment.summary(),
            assessment.triggeredFactors().stream()
                .map(factor -> new RiskFactorView(factor.code(), factor.weight(), factor.detail()))
                .toList()
        );
    }

    private String buildComparisonSummary(
        FraudSimulationOutcomeResponse baselineOutcome,
        FraudSimulationOutcomeResponse overrideOutcome
    ) {
        if (baselineOutcome.decision() == overrideOutcome.decision()) {
            return "Override thresholds did not change the projected fraud decision for this scenario.";
        }
        return "Projected decision changed from %s to %s under the override thresholds."
            .formatted(baselineOutcome.decision(), overrideOutcome.decision());
    }
}
