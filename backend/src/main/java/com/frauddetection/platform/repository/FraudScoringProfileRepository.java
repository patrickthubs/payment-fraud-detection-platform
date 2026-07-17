package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudScoringProfileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FraudScoringProfileRepository extends JpaRepository<FraudScoringProfileEntity, UUID> {

    Optional<FraudScoringProfileEntity> findByActiveTrue();

    List<FraudScoringProfileEntity> findAllByOrderByVersionNumberDesc();

    Optional<FraudScoringProfileEntity> findTopByOrderByVersionNumberDesc();

    boolean existsByProfileNameIgnoreCase(String profileName);

    @Modifying
    @Query("update FraudScoringProfileEntity profile set profile.active = false where profile.active = true and profile.id <> :profileId")
    void deactivateOtherProfiles(UUID profileId);
}
