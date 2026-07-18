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

    Optional<FraudAssessmentRecordEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("select count(distinct record.customerId) from FraudAssessmentRecordEntity record")
    long countDistinctCustomerIds();

    @Query("select coalesce(avg(record.riskScore), 0) from FraudAssessmentRecordEntity record")
    double averageRiskScore();

    @Query("select record.decision as decision, count(record) as total from FraudAssessmentRecordEntity record group by record.decision")
    List<DecisionCountView> countGroupedByDecision();

    @Query("select record.velocitySource as velocitySource, count(record) as total from FraudAssessmentRecordEntity record group by record.velocitySource")
    List<VelocitySourceCountView> countGroupedByVelocitySource();

    interface DecisionCountView {
        RiskDecision getDecision();

        long getTotal();
    }

    interface VelocitySourceCountView {
        VelocitySource getVelocitySource();

        long getTotal();
    }
}
