package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudStepUpProperties;
import com.frauddetection.platform.dto.StepUpDeliveryResponse;
import com.frauddetection.platform.dto.StepUpRevokeResponse;
import com.frauddetection.platform.dto.StepUpTokenResponse;
import com.frauddetection.platform.dto.StepUpVerificationResponse;
import com.frauddetection.platform.entity.FraudOperatorEntity;
import com.frauddetection.platform.entity.StepUpOperatorSecurityStateEntity;
import com.frauddetection.platform.entity.StepUpTokenDeliveryEntity;
import com.frauddetection.platform.exception.StepUpAuthenticationFailedException;
import com.frauddetection.platform.exception.StepUpDeliveryFailedException;
import com.frauddetection.platform.exception.StepUpRateLimitedException;
import com.frauddetection.platform.model.StepUpDeliveryChannel;
import com.frauddetection.platform.model.StepUpDeliveryStatus;
import com.frauddetection.platform.repository.FraudOperatorRepository;
import com.frauddetection.platform.repository.StepUpOperatorSecurityStateRepository;
import com.frauddetection.platform.repository.StepUpTokenDeliveryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.InvalidOneTimeTokenException;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StepUpAuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StepUpAuthenticationService.class);

    static final String STEP_UP_OPERATOR_SESSION_KEY = "fraud.step-up.operator";
    static final String STEP_UP_VERIFIED_AT_SESSION_KEY = "fraud.step-up.verified-at";

    private static final String TOKEN_EXPIRY_REASON = "The one-time token expired before it was used.";
    private static final String TOKEN_REVOKED_REASON = "The one-time token was revoked before it was used.";
    private static final String TOKEN_SUPERSEDED_REASON = "The one-time token was superseded by a newer request.";

    private final OneTimeTokenService oneTimeTokenService;
    private final FraudStepUpProperties fraudStepUpProperties;
    private final Clock clock;
    private final RequestMatcher protectedRouteMatcher;
    private final FraudOperatorRepository fraudOperatorRepository;
    private final StepUpOperatorSecurityStateRepository stepUpOperatorSecurityStateRepository;
    private final StepUpTokenDeliveryRepository stepUpTokenDeliveryRepository;
    private final StepUpDeliveryGateway stepUpDeliveryGateway;
    private final JdbcOperations jdbcOperations;
    private final CurrentTenantService currentTenantService;

    public StepUpAuthenticationService(
        OneTimeTokenService oneTimeTokenService,
        FraudStepUpProperties fraudStepUpProperties,
        Clock clock,
        FraudOperatorRepository fraudOperatorRepository,
        StepUpOperatorSecurityStateRepository stepUpOperatorSecurityStateRepository,
        StepUpTokenDeliveryRepository stepUpTokenDeliveryRepository,
        StepUpDeliveryGateway stepUpDeliveryGateway,
        JdbcOperations jdbcOperations,
        CurrentTenantService currentTenantService
    ) {
        this.oneTimeTokenService = oneTimeTokenService;
        this.fraudStepUpProperties = fraudStepUpProperties;
        this.clock = clock;
        this.fraudOperatorRepository = fraudOperatorRepository;
        this.stepUpOperatorSecurityStateRepository = stepUpOperatorSecurityStateRepository;
        this.stepUpTokenDeliveryRepository = stepUpTokenDeliveryRepository;
        this.stepUpDeliveryGateway = stepUpDeliveryGateway;
        this.jdbcOperations = jdbcOperations;
        this.currentTenantService = currentTenantService;
        this.protectedRouteMatcher = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/fraud-cases/export"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-cases/{caseId}/release"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-cases/{caseId}/confirm-decline"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-cases/{caseId}/resolve"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-assessments/scoring-profiles/{profileId}/activate"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-operations/outbound-events/{eventId}/retry"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-operations/outbound-events/retry-failed"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/v1/fraud-operations/outbound-events/dispatch-now")
        );
    }

    public StepUpTokenResponse generateToken(String operator, HttpServletRequest request) {
        return issueToken(operator, request, true);
    }

    public StepUpTokenResponse resendToken(String operator, HttpServletRequest request) {
        return issueToken(operator, request, true);
    }

    public StepUpVerificationResponse verifyToken(String operator, String token, HttpServletRequest request) {
        Instant now = Instant.now(clock);
        FraudOperatorEntity operatorEntity = resolveOperator(operator);
        UUID organizationId = operatorEntity.getOrganization().getId();
        StepUpOperatorSecurityStateEntity securityState = resolveSecurityState(operatorEntity, now);
        assertNotLocked(securityState, now);
        String tokenHash = hashToken(token);
        StepUpTokenDeliveryEntity delivery = stepUpTokenDeliveryRepository.findByOrganizationIdAndTokenHash(organizationId, tokenHash)
            .orElseThrow(() -> recordVerificationFailure(
                securityState,
                now,
                "The one-time verification token is invalid or no longer available."
            ));

        expireDeliveryIfNeeded(delivery, now);
        if (!delivery.getOperatorUsername().equalsIgnoreCase(operator)) {
            throw recordVerificationFailure(
                securityState,
                now,
                "The one-time token does not belong to the authenticated operator."
            );
        }
        if (delivery.getStatus() == StepUpDeliveryStatus.REVOKED) {
            throw recordVerificationFailure(securityState, now, TOKEN_REVOKED_REASON);
        }
        if (delivery.getStatus() == StepUpDeliveryStatus.CONSUMED) {
            throw recordVerificationFailure(
                securityState,
                now,
                "The one-time verification token has already been used."
            );
        }
        if (delivery.getStatus() == StepUpDeliveryStatus.EXPIRED) {
            throw recordVerificationFailure(securityState, now, TOKEN_EXPIRY_REASON);
        }
        if (delivery.getStatus() == StepUpDeliveryStatus.FAILED) {
            throw recordVerificationFailure(
                securityState,
                now,
                "The one-time verification token could not be delivered successfully."
            );
        }

        try {
            OneTimeToken consumedToken = oneTimeTokenService.consume(new OneTimeTokenAuthenticationToken(token));
            if (!consumedToken.getUsername().equalsIgnoreCase(operator)) {
                throw recordVerificationFailure(
                    securityState,
                    now,
                    "The one-time token does not belong to the authenticated operator."
                );
            }
        }
        catch (InvalidOneTimeTokenException exception) {
            throw recordVerificationFailure(
                securityState,
                now,
                "The one-time verification token is invalid or no longer available."
            );
        }

        delivery.markConsumed(now);
        stepUpTokenDeliveryRepository.save(delivery);
        securityState.clearVerificationFailures(now);
        stepUpOperatorSecurityStateRepository.save(securityState);

        Instant validUntil = now.plus(fraudStepUpProperties.sessionValidity());
        HttpSession session = request.getSession(true);
        session.setAttribute(STEP_UP_OPERATOR_SESSION_KEY, normalize(operator));
        session.setAttribute(STEP_UP_VERIFIED_AT_SESSION_KEY, now);
        return new StepUpVerificationResponse(operator, now, validUntil);
    }

    public StepUpRevokeResponse revoke(String operator, HttpServletRequest request) {
        Instant now = Instant.now(clock);
        FraudOperatorEntity operatorEntity = resolveOperator(operator);
        UUID organizationId = operatorEntity.getOrganization().getId();
        expireOutstandingDeliveries(organizationId, operator, now);
        List<StepUpTokenDeliveryEntity> activeDeliveries = stepUpTokenDeliveryRepository
            .findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusIn(
                organizationId,
                operator,
                List.of(StepUpDeliveryStatus.PENDING, StepUpDeliveryStatus.SENT)
            );

        activeDeliveries.forEach(delivery -> delivery.markRevoked(now, TOKEN_REVOKED_REASON));
        if (!activeDeliveries.isEmpty()) {
            stepUpTokenDeliveryRepository.saveAll(activeDeliveries);
        }
        jdbcOperations.update("delete from one_time_tokens where lower(username) = lower(?)", operator);

        HttpSession session = request.getSession(false);
        if (session != null) {
            clearStepUp(session);
        }
        return new StepUpRevokeResponse(activeDeliveries.size(), now);
    }

    public List<StepUpDeliveryResponse> listDeliveries(
        String requestingOperator,
        boolean platformAdmin,
        String operatorFilter,
        StepUpDeliveryStatus status,
        Integer limit
    ) {
        int resolvedLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        Instant now = Instant.now(clock);
        UUID organizationId = currentTenantService.organizationId();
        expireOutstandingDeliveries(organizationId, now);

        if (!platformAdmin) {
            return map(status == null
                ? stepUpTokenDeliveryRepository
                    .findByOrganizationIdAndOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(
                        organizationId,
                        requestingOperator,
                        PageRequest.of(0, resolvedLimit)
                    )
                    .getContent()
                : stepUpTokenDeliveryRepository
                    .findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        organizationId,
                        requestingOperator,
                        status,
                        PageRequest.of(0, resolvedLimit)
                    )
                    .getContent());
        }

        if (operatorFilter != null && !operatorFilter.isBlank()) {
            return map(status == null
                ? stepUpTokenDeliveryRepository
                    .findByOrganizationIdAndOperatorUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
                        organizationId,
                        operatorFilter,
                        PageRequest.of(0, resolvedLimit)
                    )
                    .getContent()
                : stepUpTokenDeliveryRepository
                    .findByOrganizationIdAndOperatorUsernameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        organizationId,
                        operatorFilter,
                        status,
                        PageRequest.of(0, resolvedLimit)
                    )
                    .getContent());
        }

        return map(status == null
            ? stepUpTokenDeliveryRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, PageRequest.of(0, resolvedLimit)).getContent()
            : stepUpTokenDeliveryRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                organizationId,
                status,
                PageRequest.of(0, resolvedLimit)
            ).getContent());
    }

    public boolean hasValidStepUp(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object sessionOperator = session.getAttribute(STEP_UP_OPERATOR_SESSION_KEY);
        Object verifiedAtValue = session.getAttribute(STEP_UP_VERIFIED_AT_SESSION_KEY);
        if (!(sessionOperator instanceof String operator) || !(verifiedAtValue instanceof Instant verifiedAt)) {
            clearStepUp(session);
            return false;
        }

        if (!operator.equals(normalize(authentication.getName()))) {
            clearStepUp(session);
            return false;
        }

        if (verifiedAt.plus(fraudStepUpProperties.sessionValidity()).isBefore(Instant.now(clock))) {
            clearStepUp(session);
            return false;
        }

        return true;
    }

    public boolean isProtectedRoute(HttpServletRequest request) {
        return protectedRouteMatcher.matches(request);
    }

    public boolean hasSupervisorAuthority(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority ->
                "ROLE_FRAUD_SUPERVISOR".equals(authority) || "ROLE_PLATFORM_ADMIN".equals(authority)
            );
    }

    public boolean hasPlatformAdminAuthority(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_PLATFORM_ADMIN"::equals);
    }

    private StepUpTokenResponse issueToken(String operator, HttpServletRequest request, boolean revokeExisting) {
        Instant now = Instant.now(clock);
        FraudOperatorEntity operatorEntity = fraudOperatorRepository.findByUsernameIgnoreCase(operator)
            .orElseThrow(() -> new StepUpDeliveryFailedException(
                "The authenticated operator could not be resolved for step-up delivery."
            ));
        UUID organizationId = operatorEntity.getOrganization().getId();
        StepUpOperatorSecurityStateEntity securityState = resolveSecurityState(operatorEntity, now);
        assertNotLocked(securityState, now);
        securityState.registerIssueAttempt(now, fraudStepUpProperties.issueWindow());
        if (securityState.getIssueAttemptCount() > fraudStepUpProperties.maxIssueAttempts()) {
            securityState.lockUntil(
                now,
                fraudStepUpProperties.lockoutDuration(),
                "Too many step-up token requests were submitted in a short period."
            );
            stepUpOperatorSecurityStateRepository.save(securityState);
            LOGGER.warn(
                "Step-up token issuance locked for operator {} until {} after {} attempts in the current window.",
                operatorEntity.getUsername(),
                securityState.getLockedUntil(),
                securityState.getIssueAttemptCount()
            );
            throw new StepUpRateLimitedException(
                "Too many step-up token requests were submitted. Retry after %s."
                    .formatted(securityState.getLockedUntil())
            );
        }
        stepUpOperatorSecurityStateRepository.save(securityState);
        expireOutstandingDeliveries(organizationId, operator, now);
        int resendSequence = stepUpTokenDeliveryRepository
            .findTopByOrganizationIdAndOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(organizationId, operator)
            .map(previous -> previous.getResendSequence() + 1)
            .orElse(0);

        if (revokeExisting) {
            revokeOutstandingTokens(organizationId, operator, now, TOKEN_SUPERSEDED_REASON);
        }

        OneTimeToken token = oneTimeTokenService.generate(
            new GenerateOneTimeTokenRequest(operator, fraudStepUpProperties.tokenValidity())
        );
        String verificationUrl = buildVerificationUrl(request, token);
        String tokenHash = hashToken(token.getTokenValue());
        StepUpDeliveryChannel deliveryChannel = resolveDeliveryChannel(operatorEntity);
        StepUpTokenDeliveryEntity delivery = stepUpTokenDeliveryRepository.save(new StepUpTokenDeliveryEntity(
            UUID.randomUUID(),
            organizationId,
            operatorEntity.getId(),
            operatorEntity.getUsername(),
            deliveryChannel,
            resolveDeliveryDestination(operatorEntity, deliveryChannel),
            maskDestination(resolveDeliveryDestination(operatorEntity, deliveryChannel), deliveryChannel),
            StepUpDeliveryStatus.PENDING,
            tokenHash,
            resendSequence,
            1,
            null,
            now,
            token.getExpiresAt(),
            now,
            null,
            null,
            null
        ));

        try {
            stepUpDeliveryGateway.deliver(new StepUpDeliveryRequest(
                delivery.getId(),
                operatorEntity,
                deliveryChannel,
                delivery.getDeliveryDestination(),
                delivery.getDestinationMasked(),
                verificationUrl,
                token.getExpiresAt()
            ));
            delivery.markSent(now);
            stepUpTokenDeliveryRepository.save(delivery);
        }
        catch (RuntimeException exception) {
            delivery.markFailed(exception.getMessage(), now);
            stepUpTokenDeliveryRepository.save(delivery);
            jdbcOperations.update("delete from one_time_tokens where token_value = ?", token.getTokenValue());
            throw exception instanceof StepUpDeliveryFailedException
                ? exception
                : new StepUpDeliveryFailedException("Step-up verification could not be delivered.");
        }

        return new StepUpTokenResponse(
            delivery.getId(),
            token.getExpiresAt(),
            deliveryChannel,
            delivery.getDestinationMasked(),
            fraudStepUpProperties.exposeVerificationUrl() ? verificationUrl : null
        );
    }

    private void revokeOutstandingTokens(UUID organizationId, String operator, Instant revokedAt, String reason) {
        List<StepUpTokenDeliveryEntity> openDeliveries = stepUpTokenDeliveryRepository
            .findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusIn(
                organizationId,
                operator,
                List.of(StepUpDeliveryStatus.PENDING, StepUpDeliveryStatus.SENT)
            );
        openDeliveries.forEach(delivery -> delivery.markRevoked(revokedAt, reason));
        if (!openDeliveries.isEmpty()) {
            stepUpTokenDeliveryRepository.saveAll(openDeliveries);
            jdbcOperations.update("delete from one_time_tokens where lower(username) = lower(?)", operator);
        }
    }

    private void expireOutstandingDeliveries(UUID organizationId, String operator, Instant now) {
        List<StepUpTokenDeliveryEntity> openDeliveries = stepUpTokenDeliveryRepository
            .findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusIn(
                organizationId,
                operator,
                List.of(StepUpDeliveryStatus.PENDING, StepUpDeliveryStatus.SENT)
            );
        boolean changed = false;
        for (StepUpTokenDeliveryEntity delivery : openDeliveries) {
            if (delivery.getExpiresAt().isBefore(now)) {
                delivery.markExpired(TOKEN_EXPIRY_REASON);
                changed = true;
            }
        }
        if (changed) {
            stepUpTokenDeliveryRepository.saveAll(openDeliveries);
        }
    }

    private void expireOutstandingDeliveries(UUID organizationId, Instant now) {
        List<StepUpTokenDeliveryEntity> openDeliveries = stepUpTokenDeliveryRepository.findByOrganizationIdAndStatusIn(
            organizationId,
            List.of(StepUpDeliveryStatus.PENDING, StepUpDeliveryStatus.SENT)
        );
        boolean changed = false;
        for (StepUpTokenDeliveryEntity delivery : openDeliveries) {
            if (delivery.getExpiresAt().isBefore(now)) {
                delivery.markExpired(TOKEN_EXPIRY_REASON);
                changed = true;
            }
        }
        if (changed) {
            stepUpTokenDeliveryRepository.saveAll(openDeliveries);
        }
    }

    private void expireDeliveryIfNeeded(StepUpTokenDeliveryEntity delivery, Instant now) {
        if (delivery.isOpen() && delivery.getExpiresAt().isBefore(now)) {
            delivery.markExpired(TOKEN_EXPIRY_REASON);
            stepUpTokenDeliveryRepository.save(delivery);
        }
    }

    private List<StepUpDeliveryResponse> map(List<StepUpTokenDeliveryEntity> deliveries) {
        return deliveries.stream()
            .map(delivery -> new StepUpDeliveryResponse(
                delivery.getId(),
                delivery.getOperatorUsername(),
                delivery.getDeliveryChannel(),
                delivery.getDestinationMasked(),
                delivery.getStatus(),
                delivery.getResendSequence(),
                delivery.getAttemptCount(),
                delivery.getFailureReason(),
                delivery.getCreatedAt(),
                delivery.getExpiresAt(),
                delivery.getDeliveredAt(),
                delivery.getConsumedAt(),
                delivery.getRevokedAt()
            ))
            .toList();
    }

    private void clearStepUp(HttpSession session) {
        session.removeAttribute(STEP_UP_OPERATOR_SESSION_KEY);
        session.removeAttribute(STEP_UP_VERIFIED_AT_SESSION_KEY);
    }

    private StepUpDeliveryChannel resolveDeliveryChannel(FraudOperatorEntity operatorEntity) {
        return fraudStepUpProperties.deliveryChannel() == StepUpDeliveryChannel.DEVELOPMENT_LINK
            ? StepUpDeliveryChannel.DEVELOPMENT_LINK
            : operatorEntity.getStepUpDeliveryChannel();
    }

    private String resolveDeliveryDestination(FraudOperatorEntity operatorEntity, StepUpDeliveryChannel deliveryChannel) {
        if (deliveryChannel == StepUpDeliveryChannel.EMAIL) {
            if (operatorEntity.getEmail() == null || operatorEntity.getEmail().isBlank()) {
                throw new StepUpDeliveryFailedException(
                    "The operator does not have a step-up email destination configured."
                );
            }
            return operatorEntity.getEmail();
        }
        return "development-link";
    }

    private String maskDestination(String destination, StepUpDeliveryChannel deliveryChannel) {
        if (deliveryChannel == StepUpDeliveryChannel.DEVELOPMENT_LINK) {
            return "development-link";
        }
        int atIndex = destination.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return destination.charAt(0) + "***" + destination.substring(atIndex - 1);
    }

    private String buildVerificationUrl(HttpServletRequest request, OneTimeToken oneTimeToken) {
        String token = URLEncoder.encode(oneTimeToken.getTokenValue(), StandardCharsets.UTF_8);
        return "%s://%s:%d%s/api/v1/security/step-up/verify?token=%s".formatted(
            request.getScheme(),
            request.getServerName(),
            request.getServerPort(),
            request.getContextPath(),
            token
        );
    }

    private String normalize(String operator) {
        return operator.toUpperCase(Locale.ROOT);
    }

    private StepUpOperatorSecurityStateEntity resolveSecurityState(FraudOperatorEntity operatorEntity, Instant now) {
        UUID organizationId = operatorEntity.getOrganization().getId();
        return stepUpOperatorSecurityStateRepository
            .findByOrganizationIdAndOperatorUsernameIgnoreCase(organizationId, operatorEntity.getUsername())
            .orElseGet(() -> new StepUpOperatorSecurityStateEntity(
                operatorEntity.getId(),
                organizationId,
                operatorEntity.getUsername(),
                null,
                0,
                null,
                0,
                null,
                null,
                now,
                now
            ));
    }

    private FraudOperatorEntity resolveOperator(String operator) {
        return fraudOperatorRepository.findByUsernameIgnoreCase(operator)
            .orElseThrow(() -> new StepUpDeliveryFailedException(
                "The authenticated operator could not be resolved for step-up delivery."
            ));
    }

    private void assertNotLocked(StepUpOperatorSecurityStateEntity securityState, Instant now) {
        if (securityState.isLockedAt(now)) {
            throw new StepUpRateLimitedException(
                "Step-up verification is temporarily locked due to repeated failures. Retry after %s."
                    .formatted(securityState.getLockedUntil())
            );
        }
    }

    private RuntimeException recordVerificationFailure(
        StepUpOperatorSecurityStateEntity securityState,
        Instant now,
        String message
    ) {
        securityState.registerVerificationFailure(now, fraudStepUpProperties.verifyWindow(), message);
        if (securityState.getVerifyFailureCount() >= fraudStepUpProperties.maxVerifyFailures()) {
            securityState.lockUntil(now, fraudStepUpProperties.lockoutDuration(), message);
            stepUpOperatorSecurityStateRepository.save(securityState);
            LOGGER.warn(
                "Step-up verification locked for operator {} until {} after {} failures in the current window.",
                securityState.getOperatorUsername(),
                securityState.getLockedUntil(),
                securityState.getVerifyFailureCount()
            );
            return new StepUpRateLimitedException(
                "Too many failed verification attempts were recorded. Retry after %s."
                    .formatted(securityState.getLockedUntil())
            );
        }
        stepUpOperatorSecurityStateRepository.save(securityState);
        return new StepUpAuthenticationFailedException(message);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for step-up token hashing.", exception);
        }
    }
}
