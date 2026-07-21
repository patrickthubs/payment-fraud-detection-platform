package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FraudOutboundEventRepository
    extends JpaRepository<FraudOutboundEventEntity, UUID>, JpaSpecificationExecutor<FraudOutboundEventEntity> {

    long countByOrganizationIdAndStatus(UUID organizationId, FraudOutboundEventStatus status);

    List<FraudOutboundEventEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        Collection<FraudOutboundEventStatus> statuses,
        Instant nextAttemptAt,
        Pageable pageable
    );

    List<FraudOutboundEventEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
        UUID organizationId,
        FraudOutboundEventStatus status,
        Pageable pageable
    );

    List<FraudOutboundEventEntity> findByOrganizationIdAndStatusOrderByCreatedAtAsc(UUID organizationId, FraudOutboundEventStatus status);

    List<FraudOutboundEventEntity> findByOrganizationIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(UUID organizationId, Instant createdAt);

    List<FraudOutboundEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<FraudOutboundEventEntity> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}
