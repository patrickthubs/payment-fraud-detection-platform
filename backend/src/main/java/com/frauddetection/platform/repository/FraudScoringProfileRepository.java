package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudScoringProfileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FraudScoringProfileRepository extends JpaRepository<FraudScoringProfileEntity, UUID> {

    Optional<FraudScoringProfileEntity> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<FraudScoringProfileEntity> findAllByOrganizationIdOrderByVersionNumberDesc(UUID organizationId);

    Optional<FraudScoringProfileEntity> findTopByOrganizationIdOrderByVersionNumberDesc(UUID organizationId);

    boolean existsByOrganizationIdAndProfileNameIgnoreCase(UUID organizationId, String profileName);

    @Modifying
    @Query("""
        update FraudScoringProfileEntity profile
        set profile.active = false
        where profile.organizationId = :organizationId
          and profile.active = true
          and profile.id <> :profileId
        """)
    void deactivateOtherProfiles(UUID organizationId, UUID profileId);

}
