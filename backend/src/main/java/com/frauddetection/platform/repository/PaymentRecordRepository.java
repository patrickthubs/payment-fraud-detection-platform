package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, UUID>, JpaSpecificationExecutor<PaymentRecordEntity> {

    Optional<PaymentRecordEntity> findByOrganizationIdAndPaymentId(UUID organizationId, String paymentId);

    long countByOrganizationId(UUID organizationId);

    @Query("""
        select payment.paymentStatus as paymentStatus, count(payment) as total
        from PaymentRecordEntity payment
        where payment.organizationId = :organizationId
        group by payment.paymentStatus
        """)
    List<PaymentStatusCountView> countGroupedByPaymentStatus(UUID organizationId);

    long countByOrganizationIdAndLatestDecision(UUID organizationId, RiskDecision latestDecision);

    long countByOrganizationIdAndPaymentStatus(UUID organizationId, PaymentStatus paymentStatus);

    long countByOrganizationIdAndChallengeOutcome(UUID organizationId, ChallengeOutcome challengeOutcome);

    @Query("""
        select payment.challengeOutcome as challengeOutcome, count(payment) as total
        from PaymentRecordEntity payment
        where payment.organizationId = :organizationId
          and payment.challengeOutcome is not null
        group by payment.challengeOutcome
        """)
    List<ChallengeOutcomeCountView> countGroupedByChallengeOutcome(UUID organizationId);

    @Query("""
        select coalesce(avg(cast(payment.latestRiskScore as double)), 0)
        from PaymentRecordEntity payment
        where payment.organizationId = :organizationId
          and payment.challengeOutcome = :challengeOutcome
        """)
    double averageRiskScoreByChallengeOutcome(UUID organizationId, ChallengeOutcome challengeOutcome);

    interface PaymentStatusCountView {
        PaymentStatus getPaymentStatus();

        long getTotal();
    }

    interface ChallengeOutcomeCountView {
        ChallengeOutcome getChallengeOutcome();

        long getTotal();
    }
}
