package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.FraudOutboundDispatchResponse;
import com.frauddetection.platform.dto.FraudOutboundAnalyticsResponse;
import com.frauddetection.platform.dto.FraudOutboundEventResponse;
import com.frauddetection.platform.dto.FraudOutboundIncidentNoteRequest;
import com.frauddetection.platform.dto.FraudOutboundRetryBatchResponse;
import com.frauddetection.platform.dto.FraudOperationsSummaryResponse;
import com.frauddetection.platform.service.FraudOutboundAnalyticsService;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.service.FraudOutboundEventOperationsService;
import com.frauddetection.platform.service.FraudOperationsSummaryService;
import com.frauddetection.platform.service.AuthenticatedOperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-operations")
@Tag(name = "Fraud Operations", description = "Operational summary data for fraud analysts and supervisors.")
public class FraudOperationsController {

    private final FraudOperationsSummaryService fraudOperationsSummaryService;
    private final FraudOutboundEventOperationsService fraudOutboundEventOperationsService;
    private final FraudOutboundAnalyticsService fraudOutboundAnalyticsService;
    private final AuthenticatedOperatorService authenticatedOperatorService;

    public FraudOperationsController(
        FraudOperationsSummaryService fraudOperationsSummaryService,
        FraudOutboundEventOperationsService fraudOutboundEventOperationsService,
        FraudOutboundAnalyticsService fraudOutboundAnalyticsService,
        AuthenticatedOperatorService authenticatedOperatorService
    ) {
        this.fraudOperationsSummaryService = fraudOperationsSummaryService;
        this.fraudOutboundEventOperationsService = fraudOutboundEventOperationsService;
        this.fraudOutboundAnalyticsService = fraudOutboundAnalyticsService;
        this.authenticatedOperatorService = authenticatedOperatorService;
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get fraud operations summary",
        description = "Returns aggregate fraud metrics across assessments, payment states, challenge outcomes, reviewer workload, case turnaround, and resolution outcomes."
    )
    public FraudOperationsSummaryResponse getSummary() {
        return fraudOperationsSummaryService.getSummary();
    }

    @GetMapping("/outbound-events")
    @Operation(
        summary = "List outbound fraud events",
        description = "Returns recent outbound fraud events so operators can inspect pending, delivered, or failed delivery records."
    )
    public List<FraudOutboundEventResponse> getOutboundEvents(
        @RequestParam(required = false) FraudOutboundEventStatus status,
        @RequestParam(required = false) String topicName,
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String messageKey,
        @RequestParam(required = false) Boolean onlyWithNotes,
        @RequestParam(required = false) Integer limit
    ) {
        return fraudOutboundEventOperationsService.findEvents(status, topicName, eventType, messageKey, onlyWithNotes, limit);
    }

    @GetMapping("/outbound-events/analytics")
    @Operation(
        summary = "Read outbound fraud event analytics",
        description = "Returns retry-health metrics, daily delivery trends, and failed-incident aging views for outbound fraud events."
    )
    public FraudOutboundAnalyticsResponse getOutboundEventAnalytics(
        @RequestParam(required = false) Integer days
    ) {
        return fraudOutboundAnalyticsService.getAnalytics(days);
    }

    @GetMapping(value = "/outbound-events/failed-export", produces = "text/csv")
    @Operation(
        summary = "Export failed outbound fraud events",
        description = "Exports recent failed outbound fraud events as CSV for incident review or downstream dead-letter handling."
    )
    public ResponseEntity<String> exportFailedOutboundEvents(
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(fraudOutboundEventOperationsService.exportFailedEvents(limit));
    }

    @PostMapping("/outbound-events/{eventId}/retry")
    @Operation(
        summary = "Retry one outbound fraud event",
        description = "Moves a failed outbound event back to pending delivery so the dispatcher can publish it again."
    )
    public FraudOutboundEventResponse retryOutboundEvent(@PathVariable UUID eventId) {
        return fraudOutboundEventOperationsService.retryEvent(eventId);
    }

    @PostMapping("/outbound-events/{eventId}/incident-note")
    @Operation(
        summary = "Attach an incident note to one outbound fraud event",
        description = "Stores an operator note on an outbound event so incident context stays attached to the delivery record."
    )
    public FraudOutboundEventResponse addOutboundEventIncidentNote(
        @PathVariable UUID eventId,
        @Valid @RequestBody FraudOutboundIncidentNoteRequest request,
        Authentication authentication
    ) {
        return fraudOutboundEventOperationsService.addIncidentNote(
            eventId,
            authenticatedOperatorService.resolveOperator(authentication),
            request
        );
    }

    @PostMapping("/outbound-events/retry-failed")
    @Operation(
        summary = "Retry failed outbound fraud events",
        description = "Moves a batch of failed outbound events back to pending delivery for replay by the outbox dispatcher."
    )
    public FraudOutboundRetryBatchResponse retryFailedOutboundEvents(
        @RequestParam(required = false) Integer limit
    ) {
        return fraudOutboundEventOperationsService.retryFailedEvents(limit);
    }

    @PostMapping("/outbound-events/dispatch-now")
    @Operation(
        summary = "Dispatch pending outbound fraud events now",
        description = "Triggers an immediate outbox dispatch sweep so operators can accelerate recovery after an outage."
    )
    public FraudOutboundDispatchResponse dispatchOutboundEventsNow() {
        return fraudOutboundEventOperationsService.dispatchNow();
    }
}
