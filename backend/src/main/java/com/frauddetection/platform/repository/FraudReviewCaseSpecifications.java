package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.service.FraudCaseFilterCriteria;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class FraudReviewCaseSpecifications {

    private FraudReviewCaseSpecifications() {
    }

    public static Specification<FraudReviewCaseEntity> forCriteria(
        UUID organizationId,
        FraudCaseFilterCriteria criteria,
        Instant breachThreshold
    ) {
        return Specification.<FraudReviewCaseEntity>unrestricted()
            .and(hasOrganizationId(organizationId))
            .and(hasStatus(criteria.status()))
            .and(hasAssignee(criteria.assignee()))
            .and(hasMinimumRiskScore(criteria.minRiskScore()))
            .and(hasMaximumRiskScore(criteria.maxRiskScore()))
            .and(isBreached(criteria.breachedOnly(), breachThreshold))
            .and(isUnassigned(criteria.unassignedOnly()))
            .and(hasPaymentId(criteria.paymentId()))
            .and(hasCustomerId(criteria.customerId()))
            .and(createdOnOrAfter(criteria.createdFrom()))
            .and(createdOnOrBefore(criteria.createdTo()));
    }

    private static Specification<FraudReviewCaseEntity> hasOrganizationId(UUID organizationId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("organizationId"), organizationId);
    }

    private static Specification<FraudReviewCaseEntity> hasStatus(ReviewCaseStatus status) {
        return (root, query, criteriaBuilder) -> status == null
            ? null
            : criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<FraudReviewCaseEntity> hasAssignee(String assignee) {
        return (root, query, criteriaBuilder) -> isBlank(assignee)
            ? null
            : criteriaBuilder.equal(criteriaBuilder.lower(root.get("currentAssignee")), assignee.trim().toLowerCase());
    }

    private static Specification<FraudReviewCaseEntity> hasMinimumRiskScore(Integer minRiskScore) {
        return (root, query, criteriaBuilder) -> minRiskScore == null
            ? null
            : criteriaBuilder.greaterThanOrEqualTo(root.get("riskScore"), minRiskScore);
    }

    private static Specification<FraudReviewCaseEntity> hasMaximumRiskScore(Integer maxRiskScore) {
        return (root, query, criteriaBuilder) -> maxRiskScore == null
            ? null
            : criteriaBuilder.lessThanOrEqualTo(root.get("riskScore"), maxRiskScore);
    }

    private static Specification<FraudReviewCaseEntity> isBreached(Boolean breachedOnly, Instant breachThreshold) {
        return (root, query, criteriaBuilder) -> !Boolean.TRUE.equals(breachedOnly)
            ? null
            : criteriaBuilder.and(
                root.get("status").in(ReviewCaseStatus.OPEN, ReviewCaseStatus.ESCALATED),
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), breachThreshold)
            );
    }

    private static Specification<FraudReviewCaseEntity> isUnassigned(Boolean unassignedOnly) {
        return (root, query, criteriaBuilder) -> !Boolean.TRUE.equals(unassignedOnly)
            ? null
            : criteriaBuilder.or(
                criteriaBuilder.isNull(root.get("currentAssignee")),
                criteriaBuilder.equal(criteriaBuilder.trim(root.get("currentAssignee")), "")
            );
    }

    private static Specification<FraudReviewCaseEntity> hasPaymentId(String paymentId) {
        return containsIgnoreCase("paymentId", paymentId);
    }

    private static Specification<FraudReviewCaseEntity> hasCustomerId(String customerId) {
        return containsIgnoreCase("customerId", customerId);
    }

    private static Specification<FraudReviewCaseEntity> createdOnOrAfter(Instant createdFrom) {
        return (root, query, criteriaBuilder) -> createdFrom == null
            ? null
            : criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private static Specification<FraudReviewCaseEntity> createdOnOrBefore(Instant createdTo) {
        return (root, query, criteriaBuilder) -> createdTo == null
            ? null
            : criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private static Specification<FraudReviewCaseEntity> containsIgnoreCase(String fieldName, String value) {
        return (root, query, criteriaBuilder) -> isBlank(value)
            ? null
            : criteriaBuilder.like(
                criteriaBuilder.lower(root.get(fieldName)),
                "%" + value.trim().toLowerCase() + "%"
            );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
