package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.config.FraudScoringProperties;
import com.frauddetection.platform.dto.FraudScoringProfileActivationRequest;
import com.frauddetection.platform.dto.FraudScoringProfileCreateRequest;
import com.frauddetection.platform.dto.FraudScoringProfileResponse;
import com.frauddetection.platform.entity.FraudScoringProfileEntity;
import com.frauddetection.platform.exception.FraudScoringProfileConflictException;
import com.frauddetection.platform.repository.FraudScoringProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudScoringProfileServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    private FraudScoringProfileRepository fraudScoringProfileRepository;
    private CurrentTenantService currentTenantService;
    private FraudScoringProfileService fraudScoringProfileService;

    @BeforeEach
    void setUp() {
        fraudScoringProfileRepository = mock(FraudScoringProfileRepository.class);
        currentTenantService = mock(CurrentTenantService.class);
        when(currentTenantService.organizationId()).thenReturn(ORGANIZATION_ID);
        fraudScoringProfileService = new FraudScoringProfileService(
            fraudScoringProfileRepository,
            new FraudScoringProperties(45, 65, 85),
            currentTenantService,
            Clock.fixed(Instant.parse("2026-07-17T18:00:00Z"), ZoneOffset.UTC),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @Test
    void returnsSystemDefaultWhenNoStoredProfileExists() {
        when(fraudScoringProfileRepository.findByOrganizationIdAndActiveTrue(ORGANIZATION_ID)).thenReturn(Optional.empty());
        when(fraudScoringProfileRepository.findAllByOrganizationIdOrderByVersionNumberDesc(ORGANIZATION_ID)).thenReturn(List.of());

        FraudScoringProfileResponse response = fraudScoringProfileService.readActiveProfile();

        assertThat(response.systemDefault()).isTrue();
        assertThat(response.profileName()).isEqualTo("System Default");
        assertThat(response.thresholds().challengeThreshold()).isEqualTo(45);
        assertThat(fraudScoringProfileService.listProfiles()).singleElement()
            .extracting(FraudScoringProfileResponse::systemDefault)
            .isEqualTo(true);
    }

    @Test
    void createsDraftProfileWithNextVersionNumber() {
        when(fraudScoringProfileRepository.existsByOrganizationIdAndProfileNameIgnoreCase(ORGANIZATION_ID, "July tuning profile")).thenReturn(false);
        when(fraudScoringProfileRepository.findTopByOrganizationIdOrderByVersionNumberDesc(ORGANIZATION_ID))
            .thenReturn(Optional.of(profile(UUID.randomUUID(), 2, false)));
        when(fraudScoringProfileRepository.save(any(FraudScoringProfileEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        FraudScoringProfileResponse response = fraudScoringProfileService.createProfile(
            new FraudScoringProfileCreateRequest(
                "July tuning profile",
                40,
                60,
                82,
                "Reduce first-line challenge pressure for known-good traffic."
            ),
            "analyst.one"
        );

        assertThat(response.versionNumber()).isEqualTo(3);
        assertThat(response.active()).isFalse();
        assertThat(response.thresholds().holdThreshold()).isEqualTo(60);
    }

    @Test
    void rejectsDuplicateProfileName() {
        when(fraudScoringProfileRepository.existsByOrganizationIdAndProfileNameIgnoreCase(ORGANIZATION_ID, "July tuning profile")).thenReturn(true);

        assertThatThrownBy(() -> fraudScoringProfileService.createProfile(
            new FraudScoringProfileCreateRequest(
                "July tuning profile",
                40,
                60,
                82,
                "Reduce first-line challenge pressure for known-good traffic."
            ),
            "analyst.one"
        )).isInstanceOf(FraudScoringProfileConflictException.class);
    }

    @Test
    void activatesStoredProfileAndDeactivatesPreviousOne() {
        UUID activeId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        FraudScoringProfileEntity currentActive = profile(activeId, 2, true);
        FraudScoringProfileEntity candidate = profile(candidateId, 3, false);

        when(fraudScoringProfileRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(fraudScoringProfileRepository.findByOrganizationIdAndActiveTrue(ORGANIZATION_ID)).thenReturn(Optional.of(currentActive));

        FraudScoringProfileResponse response = fraudScoringProfileService.activateProfile(
            candidateId,
            new FraudScoringProfileActivationRequest(),
            "senior.analyst"
        );

        assertThat(response.active()).isTrue();
        assertThat(response.activatedBy()).isEqualTo("senior.analyst");
        assertThat(currentActive.isActive()).isFalse();
        verify(fraudScoringProfileRepository).deactivateOtherProfiles(ORGANIZATION_ID, candidateId);
    }

    @Test
    void doesNotDeactivateAnythingWhenProfileAlreadyActive() {
        UUID candidateId = UUID.randomUUID();
        FraudScoringProfileEntity candidate = profile(candidateId, 3, true);

        when(fraudScoringProfileRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        FraudScoringProfileResponse response = fraudScoringProfileService.activateProfile(
            candidateId,
            new FraudScoringProfileActivationRequest(),
            "senior.analyst"
        );

        assertThat(response.active()).isTrue();
        verify(fraudScoringProfileRepository, never()).deactivateOtherProfiles(ORGANIZATION_ID, candidateId);
    }

    private FraudScoringProfileEntity profile(UUID id, int versionNumber, boolean active) {
        Instant timestamp = Instant.parse("2026-07-17T12:00:00Z");
        return new FraudScoringProfileEntity(
            id,
            ORGANIZATION_ID,
            versionNumber,
            "Profile %d".formatted(versionNumber),
            45,
            65,
            85,
            "Baseline thresholds.",
            "analyst.one",
            active ? "senior.analyst" : null,
            active,
            timestamp,
            timestamp,
            active ? timestamp : null
        );
    }
}
