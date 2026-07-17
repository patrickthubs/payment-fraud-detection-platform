package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudReplayBatchItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudReplayBatchItemRepository extends JpaRepository<FraudReplayBatchItemEntity, UUID> {

    List<FraudReplayBatchItemEntity> findAllByBatchIdOrderByScenarioIndexAsc(UUID batchId);
}
