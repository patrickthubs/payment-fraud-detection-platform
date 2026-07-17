package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.StepUpTokenDeliveryEntity;
import com.frauddetection.platform.model.StepUpDeliveryStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepUpTokenDeliveryRepository extends JpaRepository<StepUpTokenDeliveryEntity, UUID> {

    Optional<StepUpTokenDeliveryEntity> findByTokenHash(String tokenHash);

    List<StepUpTokenDeliveryEntity> findByOperatorUsernameIgnoreCaseAndStatusIn(
        String operatorUsername,
        Collection<StepUpDeliveryStatus> statuses
    );

    Optional<StepUpTokenDeliveryEntity> findTopByOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(String operatorUsername);

    Page<StepUpTokenDeliveryEntity> findByOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(
        String operatorUsername,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOperatorUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(
        String operatorUsername,
        StepUpDeliveryStatus status,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<StepUpTokenDeliveryEntity> findByStatusOrderByCreatedAtDesc(
        StepUpDeliveryStatus status,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOperatorUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
        String operatorUsername,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOperatorUsernameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
        String operatorUsername,
        StepUpDeliveryStatus status,
        Pageable pageable
    );
}
