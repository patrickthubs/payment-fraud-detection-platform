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

    Optional<StepUpTokenDeliveryEntity> findByOrganizationIdAndTokenHash(UUID organizationId, String tokenHash);

    List<StepUpTokenDeliveryEntity> findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusIn(
        UUID organizationId,
        String operatorUsername,
        Collection<StepUpDeliveryStatus> statuses
    );

    List<StepUpTokenDeliveryEntity> findByOrganizationIdAndStatusIn(
        UUID organizationId,
        Collection<StepUpDeliveryStatus> statuses
    );

    Optional<StepUpTokenDeliveryEntity> findTopByOrganizationIdAndOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(
        UUID organizationId,
        String operatorUsername
    );

    Page<StepUpTokenDeliveryEntity> findByOrganizationIdAndOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(
        UUID organizationId,
        String operatorUsername,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusOrderByCreatedAtDesc(
        UUID organizationId,
        String operatorUsername,
        StepUpDeliveryStatus status,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<StepUpTokenDeliveryEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
        UUID organizationId,
        StepUpDeliveryStatus status,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOrganizationIdAndOperatorUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
        UUID organizationId,
        String operatorUsername,
        Pageable pageable
    );

    Page<StepUpTokenDeliveryEntity> findByOrganizationIdAndOperatorUsernameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
        UUID organizationId,
        String operatorUsername,
        StepUpDeliveryStatus status,
        Pageable pageable
    );
}
