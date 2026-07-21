package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.PaymentStateTransitionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStateTransitionRepository extends JpaRepository<PaymentStateTransitionEntity, UUID> {

    List<PaymentStateTransitionEntity> findAllByOrganizationIdAndPaymentIdOrderByCreatedAtAsc(UUID organizationId, String paymentId);
}
