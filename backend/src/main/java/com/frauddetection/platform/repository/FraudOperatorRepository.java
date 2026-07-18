package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudOperatorEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudOperatorRepository extends JpaRepository<FraudOperatorEntity, UUID> {

    @EntityGraph(attributePaths = {"roles", "organization"})
    Optional<FraudOperatorEntity> findByUsernameIgnoreCase(String username);
}
