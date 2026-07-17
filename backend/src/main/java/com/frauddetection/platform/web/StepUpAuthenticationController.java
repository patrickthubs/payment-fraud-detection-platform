package com.frauddetection.platform.web;

import com.frauddetection.platform.dto.StepUpDeliveryResponse;
import com.frauddetection.platform.dto.StepUpRevokeResponse;
import com.frauddetection.platform.dto.StepUpTokenResponse;
import com.frauddetection.platform.dto.StepUpVerificationResponse;
import com.frauddetection.platform.model.StepUpDeliveryStatus;
import com.frauddetection.platform.service.AuthenticatedOperatorService;
import com.frauddetection.platform.service.StepUpAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/security/step-up")
@Tag(name = "Step-Up Security", description = "One-time verification workflow for high-risk supervisor actions.")
public class StepUpAuthenticationController {

    private final StepUpAuthenticationService stepUpAuthenticationService;
    private final AuthenticatedOperatorService authenticatedOperatorService;

    public StepUpAuthenticationController(
        StepUpAuthenticationService stepUpAuthenticationService,
        AuthenticatedOperatorService authenticatedOperatorService
    ) {
        this.stepUpAuthenticationService = stepUpAuthenticationService;
        this.authenticatedOperatorService = authenticatedOperatorService;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Generate a step-up verification token",
        description = "Issues a short-lived one-time verification token for supervisor-grade commands and delivers it through the configured secure channel."
    )
    public StepUpTokenResponse generateToken(Authentication authentication, HttpServletRequest request) {
        return stepUpAuthenticationService.generateToken(
            authenticatedOperatorService.resolveOperator(authentication),
            request
        );
    }

    @PostMapping("/token/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Resend a step-up verification token",
        description = "Revokes any still-open one-time token for the current operator and issues a fresh delivery."
    )
    public StepUpTokenResponse resendToken(Authentication authentication, HttpServletRequest request) {
        return stepUpAuthenticationService.resendToken(
            authenticatedOperatorService.resolveOperator(authentication),
            request
        );
    }

    @GetMapping("/verify")
    @Operation(
        summary = "Verify a step-up token",
        description = "Consumes a one-time verification token and marks the current operator session as step-up verified for sensitive commands."
    )
    public StepUpVerificationResponse verifyToken(
        @RequestParam String token,
        Authentication authentication,
        HttpServletRequest request
    ) {
        return stepUpAuthenticationService.verifyToken(
            authenticatedOperatorService.resolveOperator(authentication),
            token,
            request
        );
    }

    @PostMapping("/revoke")
    @Operation(
        summary = "Revoke outstanding step-up tokens",
        description = "Revokes still-open one-time verification tokens for the current operator and clears any local verified session."
    )
    public StepUpRevokeResponse revoke(Authentication authentication, HttpServletRequest request) {
        return stepUpAuthenticationService.revoke(
            authenticatedOperatorService.resolveOperator(authentication),
            request
        );
    }

    @GetMapping("/deliveries")
    @Operation(
        summary = "List step-up delivery audit records",
        description = "Returns recent step-up delivery attempts for the current operator. Platform administrators can filter by operator."
    )
    public List<StepUpDeliveryResponse> listDeliveries(
        Authentication authentication,
        @RequestParam(required = false) String operator,
        @RequestParam(required = false) StepUpDeliveryStatus status,
        @RequestParam(required = false) Integer limit
    ) {
        return stepUpAuthenticationService.listDeliveries(
            authenticatedOperatorService.resolveOperator(authentication),
            stepUpAuthenticationService.hasPlatformAdminAuthority(authentication),
            operator,
            status,
            limit
        );
    }
}
