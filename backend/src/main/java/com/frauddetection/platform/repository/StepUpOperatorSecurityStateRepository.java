package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.StepUpOperatorSecurityStateEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepUpOperatorSecurityStateRepository extends JpaRepository<StepUpOperatorSecurityStateEntity, UUID> {

    Optional<StepUpOperatorSecurityStateEntity> findByOperatorUsernameIgnoreCase(String operatorUsername);
}
