package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.CompletePaymentChallengeRequest;
import com.frauddetection.platform.dto.PaymentStatusResponse;
import com.frauddetection.platform.service.PaymentLifecycleService;
import com.frauddetection.platform.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.frauddetection.platform.service.AuthenticatedOperatorService;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Tracked payment records and lifecycle transitions.")
public class PaymentController {

    private final PaymentQueryService paymentQueryService;
    private final PaymentLifecycleService paymentLifecycleService;
    private final AuthenticatedOperatorService authenticatedOperatorService;
    private final Clock clock;

    public PaymentController(
        PaymentQueryService paymentQueryService,
        PaymentLifecycleService paymentLifecycleService,
        AuthenticatedOperatorService authenticatedOperatorService,
        Clock clock
    ) {
        this.paymentQueryService = paymentQueryService;
        this.paymentLifecycleService = paymentLifecycleService;
        this.authenticatedOperatorService = authenticatedOperatorService;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "List tracked payments", description = "Returns the currently persisted payment records without expanding their full transition history.")
    public List<PaymentStatusResponse> findAll() {
        return paymentQueryService.findAll();
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment by payment id", description = "Returns the payment record and its lifecycle transition history.")
    public PaymentStatusResponse findByPaymentId(@PathVariable String paymentId) {
        return paymentQueryService.findByPaymentId(paymentId);
    }

    @PostMapping("/{paymentId}/challenge-outcome")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Complete a challenged payment",
        description = "Records whether a challenged payment passed, failed, or was abandoned, then applies the resulting payment status transition."
    )
    public PaymentStatusResponse completeChallenge(
        @PathVariable String paymentId,
        @Valid @RequestBody CompletePaymentChallengeRequest request,
        Authentication authentication
    ) {
        Instant now = Instant.now(clock);
        paymentLifecycleService.completeChallenge(
            paymentId,
            authenticatedOperatorService.resolveOperator(authentication),
            request,
            null,
            now
        );
        return paymentQueryService.findByPaymentId(paymentId);
    }
}
