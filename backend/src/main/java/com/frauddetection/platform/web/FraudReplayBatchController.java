package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.FraudReplayBatchCreateRequest;
import com.frauddetection.platform.dto.FraudReplayBatchResponse;
import com.frauddetection.platform.service.FraudReplayBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.frauddetection.platform.service.AuthenticatedOperatorService;

@RestController
@RequestMapping("/api/v1/fraud-replays")
@Tag(name = "Fraud Replays", description = "Historical fraud scenario replay batches for rule tuning and analyst review.")
public class FraudReplayBatchController {

    private final FraudReplayBatchService fraudReplayBatchService;
    private final AuthenticatedOperatorService authenticatedOperatorService;

    public FraudReplayBatchController(
        FraudReplayBatchService fraudReplayBatchService,
        AuthenticatedOperatorService authenticatedOperatorService
    ) {
        this.fraudReplayBatchService = fraudReplayBatchService;
        this.authenticatedOperatorService = authenticatedOperatorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a fraud replay batch",
        description = "Runs a batch of fraud scenarios through the simulation engine, persists the replay results, and returns the batch summary."
    )
    public FraudReplayBatchResponse create(@Valid @RequestBody FraudReplayBatchCreateRequest request, Authentication authentication) {
        return fraudReplayBatchService.create(request, authenticatedOperatorService.resolveOperator(authentication));
    }

    @GetMapping
    @Operation(
        summary = "List fraud replay batches",
        description = "Returns persisted replay batches ordered from newest to oldest."
    )
    public List<FraudReplayBatchResponse> findAll() {
        return fraudReplayBatchService.findAll();
    }

    @GetMapping("/{batchId}")
    @Operation(
        summary = "Get a fraud replay batch by id",
        description = "Returns one persisted replay batch together with its itemized scenario outcomes."
    )
    public FraudReplayBatchResponse findById(@PathVariable UUID batchId) {
        return fraudReplayBatchService.findById(batchId);
    }
}
