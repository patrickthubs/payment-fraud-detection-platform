package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.AssignFraudCaseRequest;
import com.frauddetection.platform.dto.EscalateFraudCaseRequest;
import com.frauddetection.platform.dto.FraudCaseDecisionRequest;
import com.frauddetection.platform.dto.FraudCaseResponse;
import com.frauddetection.platform.dto.FraudCaseNoteRequest;
import com.frauddetection.platform.dto.ResolveFraudCaseRequest;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.service.FraudCaseCommandService;
import com.frauddetection.platform.service.FraudCaseFilterCriteria;
import com.frauddetection.platform.service.FraudCaseQueryService;
import com.frauddetection.platform.service.AuthenticatedOperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-cases")
@Tag(name = "Fraud Cases", description = "Manual review queue, analyst notes, escalation, and final payment decisions.")
public class FraudCaseController {

    private final FraudCaseQueryService fraudCaseQueryService;
    private final FraudCaseCommandService fraudCaseCommandService;
    private final AuthenticatedOperatorService authenticatedOperatorService;

    public FraudCaseController(
        FraudCaseQueryService fraudCaseQueryService,
        FraudCaseCommandService fraudCaseCommandService,
        AuthenticatedOperatorService authenticatedOperatorService
    ) {
        this.fraudCaseQueryService = fraudCaseQueryService;
        this.fraudCaseCommandService = fraudCaseCommandService;
        this.authenticatedOperatorService = authenticatedOperatorService;
    }

    @GetMapping
    @Operation(summary = "List fraud review cases", description = "Returns the manual review queue ordered by newest cases first with persisted triage filters.")
    public List<FraudCaseResponse> findAll(
        @RequestParam(required = false) ReviewCaseStatus status,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) Integer minRiskScore,
        @RequestParam(required = false) Integer maxRiskScore,
        @RequestParam(required = false) Boolean breachedOnly,
        @RequestParam(required = false) Boolean unassignedOnly,
        @RequestParam(required = false) String paymentId,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size
    ) {
        return fraudCaseQueryService.findAll(buildCriteria(
            status,
            assignee,
            minRiskScore,
            maxRiskScore,
            breachedOnly,
            unassignedOnly,
            paymentId,
            customerId,
            createdFrom,
            createdTo
        ), page, size);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export fraud review cases", description = "Exports the filtered review queue as CSV for supervisor-led operational triage.")
    public ResponseEntity<String> export(
        @RequestParam(required = false) ReviewCaseStatus status,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) Integer minRiskScore,
        @RequestParam(required = false) Integer maxRiskScore,
        @RequestParam(required = false) Boolean breachedOnly,
        @RequestParam(required = false) Boolean unassignedOnly,
        @RequestParam(required = false) String paymentId,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(defaultValue = "1000") @Min(1) @Max(10000) int limit
    ) {
        String csv = fraudCaseQueryService.export(buildCriteria(
            status,
            assignee,
            minRiskScore,
            maxRiskScore,
            breachedOnly,
            unassignedOnly,
            paymentId,
            customerId,
            createdFrom,
            createdTo
        ), limit);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fraud-case-export.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "Get a fraud case by id", description = "Returns one fraud case together with its analyst timeline.")
    public FraudCaseResponse findById(@PathVariable UUID caseId) {
        return fraudCaseQueryService.findById(caseId);
    }

    @PostMapping("/{caseId}/assign")
    @Operation(summary = "Assign a fraud case", description = "Assigns a case to an analyst while keeping the case open.")
    public FraudCaseResponse assign(@PathVariable UUID caseId, @Valid @RequestBody AssignFraudCaseRequest request, Authentication authentication) {
        return fraudCaseCommandService.assign(caseId, authenticatedOperatorService.resolveOperator(authentication), request);
    }

    @PostMapping("/{caseId}/escalate")
    @Operation(summary = "Escalate a fraud case", description = "Escalates a case for a higher-review workflow.")
    public FraudCaseResponse escalate(@PathVariable UUID caseId, @Valid @RequestBody EscalateFraudCaseRequest request, Authentication authentication) {
        return fraudCaseCommandService.escalate(caseId, authenticatedOperatorService.resolveOperator(authentication), request);
    }

    @PostMapping("/{caseId}/notes")
    @Operation(summary = "Add a fraud case note", description = "Appends an auditable analyst note to the case timeline.")
    public FraudCaseResponse addNote(@PathVariable UUID caseId, @Valid @RequestBody FraudCaseNoteRequest request, Authentication authentication) {
        return fraudCaseCommandService.addNote(caseId, authenticatedOperatorService.resolveOperator(authentication), request);
    }

    @PostMapping("/{caseId}/release")
    @Operation(summary = "Release a held payment", description = "Resolves the case in favor of the customer and transitions the payment to APPROVED.")
    public FraudCaseResponse releasePayment(@PathVariable UUID caseId, @Valid @RequestBody FraudCaseDecisionRequest request, Authentication authentication) {
        return fraudCaseCommandService.releasePayment(caseId, authenticatedOperatorService.resolveOperator(authentication), request);
    }

    @PostMapping("/{caseId}/confirm-decline")
    @Operation(summary = "Confirm a declined payment", description = "Resolves the case in favor of keeping the payment declined.")
    public FraudCaseResponse confirmDecline(@PathVariable UUID caseId, @Valid @RequestBody FraudCaseDecisionRequest request, Authentication authentication) {
        return fraudCaseCommandService.confirmDecline(caseId, authenticatedOperatorService.resolveOperator(authentication), request);
    }

    @PostMapping("/{caseId}/resolve")
    @Operation(summary = "Resolve a fraud case", description = "Lower-level resolution endpoint that accepts an explicit outcome for internal integrations.")
    public FraudCaseResponse resolve(@PathVariable UUID caseId, @Valid @RequestBody ResolveFraudCaseRequest request, Authentication authentication) {
        return fraudCaseCommandService.resolve(caseId, request, authenticatedOperatorService.resolveOperator(authentication));
    }

    private FraudCaseFilterCriteria buildCriteria(
        ReviewCaseStatus status,
        String assignee,
        Integer minRiskScore,
        Integer maxRiskScore,
        Boolean breachedOnly,
        Boolean unassignedOnly,
        String paymentId,
        String customerId,
        Instant createdFrom,
        Instant createdTo
    ) {
        return new FraudCaseFilterCriteria(
            status,
            assignee,
            minRiskScore,
            maxRiskScore,
            breachedOnly,
            unassignedOnly,
            paymentId,
            customerId,
            createdFrom,
            createdTo
        );
    }
}
