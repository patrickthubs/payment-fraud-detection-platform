package com.frauddetection.platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.ott.InvalidOneTimeTokenException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationFailure(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("Validation failed.");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Validation failed");
        return problemDetail;
    }

    @ExceptionHandler(InvalidFraudCaseFilterException.class)
    ProblemDetail handleInvalidFraudCaseFilter(InvalidFraudCaseFilterException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid fraud case filter");
        return problemDetail;
    }

    @ExceptionHandler(FraudCaseNotFoundException.class)
    ProblemDetail handleCaseNotFound(FraudCaseNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Fraud case not found");
        return problemDetail;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Payment not found");
        return problemDetail;
    }

    @ExceptionHandler(FraudReplayBatchNotFoundException.class)
    ProblemDetail handleReplayBatchNotFound(FraudReplayBatchNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Fraud replay batch not found");
        return problemDetail;
    }

    @ExceptionHandler(FraudOutboundEventNotFoundException.class)
    ProblemDetail handleOutboundEventNotFound(FraudOutboundEventNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Fraud outbound event not found");
        return problemDetail;
    }

    @ExceptionHandler(FraudScoringProfileNotFoundException.class)
    ProblemDetail handleScoringProfileNotFound(FraudScoringProfileNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Fraud scoring profile not found");
        return problemDetail;
    }

    @ExceptionHandler(FraudCaseActionNotAllowedException.class)
    ProblemDetail handleActionNotAllowed(FraudCaseActionNotAllowedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Fraud case action not allowed");
        return problemDetail;
    }

    @ExceptionHandler(FraudOutboundEventActionNotAllowedException.class)
    ProblemDetail handleOutboundEventActionNotAllowed(FraudOutboundEventActionNotAllowedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Fraud outbound event action not allowed");
        return problemDetail;
    }

    @ExceptionHandler(FraudScoringProfileConflictException.class)
    ProblemDetail handleScoringProfileConflict(FraudScoringProfileConflictException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Fraud scoring profile conflict");
        return problemDetail;
    }

    @ExceptionHandler(PaymentChallengeActionNotAllowedException.class)
    ProblemDetail handlePaymentChallengeActionNotAllowed(PaymentChallengeActionNotAllowedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Payment challenge action not allowed");
        return problemDetail;
    }

    @ExceptionHandler({StepUpAuthenticationFailedException.class, InvalidOneTimeTokenException.class})
    ProblemDetail handleStepUpAuthenticationFailure(RuntimeException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            exception.getMessage() == null
                ? "The one-time verification token is invalid, expired, or belongs to a different operator."
                : exception.getMessage()
        );
        problemDetail.setTitle("Step-up authentication failed");
        return problemDetail;
    }

    @ExceptionHandler(StepUpDeliveryFailedException.class)
    ProblemDetail handleStepUpDeliveryFailure(StepUpDeliveryFailedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getMessage()
        );
        problemDetail.setTitle("Step-up delivery failed");
        return problemDetail;
    }

    @ExceptionHandler(StepUpRateLimitedException.class)
    ProblemDetail handleStepUpRateLimit(StepUpRateLimitedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            exception.getMessage()
        );
        problemDetail.setTitle("Step-up temporarily locked");
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Your role does not permit this operation."
        );
        problemDetail.setTitle("Access denied");
        return problemDetail;
    }
}
