package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudOutcomeEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import com.frauddetection.platform.model.FraudOutcomeLabel;
import com.frauddetection.platform.model.RiskDecision;

public interface FraudOutcomeRepository extends JpaRepository<FraudOutcomeEntity, UUID> {
    Optional<FraudOutcomeEntity> findByAssessmentId(UUID assessmentId);

    @Query("""
        select assessment.decision as decision,
               outcome.outcomeLabel as outcomeLabel,
               count(outcome) as total,
               coalesce(sum(outcome.actualLoss), 0) as actualLoss,
               coalesce(sum(outcome.recoveredAmount), 0) as recoveredAmount
        from FraudOutcomeEntity outcome, FraudAssessmentRecordEntity assessment
        where assessment.id = outcome.assessmentId
        group by assessment.decision, outcome.outcomeLabel
        """)
    List<QualityAggregateView> findQualityAggregates();

    interface QualityAggregateView {
        RiskDecision getDecision();
        FraudOutcomeLabel getOutcomeLabel();
        long getTotal();
        BigDecimal getActualLoss();
        BigDecimal getRecoveredAmount();
    }
}
