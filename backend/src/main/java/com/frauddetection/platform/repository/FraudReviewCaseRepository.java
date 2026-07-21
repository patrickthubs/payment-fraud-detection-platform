package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.ReviewCaseStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FraudReviewCaseRepository extends JpaRepository<FraudReviewCaseEntity, UUID>, JpaSpecificationExecutor<FraudReviewCaseEntity> {

    java.util.Optional<FraudReviewCaseEntity> findByOrganizationIdAndAssessmentId(UUID organizationId, UUID assessmentId);

    java.util.Optional<FraudReviewCaseEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);

    long countByOrganizationId(UUID organizationId);

    long countByOrganizationIdAndStatusIn(UUID organizationId, List<ReviewCaseStatus> statuses);

    @Query("""
        select reviewCase.status as status, count(reviewCase) as total
        from FraudReviewCaseEntity reviewCase
        where reviewCase.organizationId = :organizationId
        group by reviewCase.status
        """)
    List<CaseStatusCountView> countGroupedByStatus(UUID organizationId);

    @Query("""
        select reviewCase.resolutionOutcome as resolutionOutcome, count(reviewCase) as total
        from FraudReviewCaseEntity reviewCase
        where reviewCase.organizationId = :organizationId
          and reviewCase.resolutionOutcome is not null
        group by reviewCase.resolutionOutcome
        """)
    List<ResolutionOutcomeCountView> countGroupedByResolutionOutcome(UUID organizationId);

    interface CaseStatusCountView {
        ReviewCaseStatus getStatus();

        long getTotal();
    }

    interface ResolutionOutcomeCountView {
        CaseResolutionOutcome getResolutionOutcome();

        long getTotal();
    }
}
