package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FraudAssessmentRecordRepository extends JpaRepository<FraudAssessmentRecordEntity, UUID> {

    Optional<FraudAssessmentRecordEntity> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    boolean existsByOrganizationIdAndId(UUID organizationId, UUID id);

    long countByOrganizationId(UUID organizationId);

    @Query("select count(distinct record.customerId) from FraudAssessmentRecordEntity record where record.organizationId = :organizationId")
    long countDistinctCustomerIds(UUID organizationId);

    @Query("select coalesce(avg(record.riskScore), 0) from FraudAssessmentRecordEntity record where record.organizationId = :organizationId")
    double averageRiskScore(UUID organizationId);

    @Query("""
        select record.decision as decision, count(record) as total
        from FraudAssessmentRecordEntity record
        where record.organizationId = :organizationId
        group by record.decision
        """)
    List<DecisionCountView> countGroupedByDecision(UUID organizationId);

    @Query("""
        select record.velocitySource as velocitySource, count(record) as total
        from FraudAssessmentRecordEntity record
        where record.organizationId = :organizationId
        group by record.velocitySource
        """)
    List<VelocitySourceCountView> countGroupedByVelocitySource(UUID organizationId);

    interface DecisionCountView {
        RiskDecision getDecision();

        long getTotal();
    }

    interface VelocitySourceCountView {
        VelocitySource getVelocitySource();

        long getTotal();
    }
}
