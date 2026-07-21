package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudReplayBatchEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudReplayBatchRepository extends JpaRepository<FraudReplayBatchEntity, UUID> {

    List<FraudReplayBatchEntity> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    java.util.Optional<FraudReplayBatchEntity> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
