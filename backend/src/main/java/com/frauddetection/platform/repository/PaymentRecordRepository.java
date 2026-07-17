package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, UUID> {

    Optional<PaymentRecordEntity> findByPaymentId(String paymentId);

    @Query("select payment.paymentStatus as paymentStatus, count(payment) as total from PaymentRecordEntity payment group by payment.paymentStatus")
    List<PaymentStatusCountView> countGroupedByPaymentStatus();

    long countByLatestDecision(RiskDecision latestDecision);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByChallengeOutcome(ChallengeOutcome challengeOutcome);

    @Query("""
        select payment.challengeOutcome as challengeOutcome, count(payment) as total
        from PaymentRecordEntity payment
        where payment.challengeOutcome is not null
        group by payment.challengeOutcome
        """)
    List<ChallengeOutcomeCountView> countGroupedByChallengeOutcome();

    @Query("""
        select coalesce(avg(cast(payment.latestRiskScore as double)), 0)
        from PaymentRecordEntity payment
        where payment.challengeOutcome = :challengeOutcome
        """)
    double averageRiskScoreByChallengeOutcome(ChallengeOutcome challengeOutcome);

    interface PaymentStatusCountView {
        PaymentStatus getPaymentStatus();

        long getTotal();
    }

    interface ChallengeOutcomeCountView {
        ChallengeOutcome getChallengeOutcome();

        long getTotal();
    }
}
