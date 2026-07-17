package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudScoringProperties;
import com.frauddetection.platform.dto.FraudScoringProfileActivationRequest;
import com.frauddetection.platform.dto.FraudScoringProfileCreateRequest;
import com.frauddetection.platform.dto.FraudScoringProfileResponse;
import com.frauddetection.platform.dto.FraudScoringThresholdsResponse;
import com.frauddetection.platform.entity.FraudScoringProfileEntity;
import com.frauddetection.platform.exception.FraudScoringProfileConflictException;
import com.frauddetection.platform.exception.FraudScoringProfileNotFoundException;
import com.frauddetection.platform.repository.FraudScoringProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudScoringProfileService {

    private final FraudScoringProfileRepository fraudScoringProfileRepository;
    private final FraudScoringProperties fraudScoringProperties;
    private final Clock clock;

    public FraudScoringProfileService(
        FraudScoringProfileRepository fraudScoringProfileRepository,
        FraudScoringProperties fraudScoringProperties,
        Clock clock
    ) {
        this.fraudScoringProfileRepository = fraudScoringProfileRepository;
        this.fraudScoringProperties = fraudScoringProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FraudScoringProfile activeProfile() {
        return fraudScoringProfileRepository.findByActiveTrue()
            .map(this::toProfile)
            .orElseGet(() -> FraudScoringProfile.from(fraudScoringProperties));
    }

    @Transactional(readOnly = true)
    public FraudScoringProfile findProfile(UUID profileId) {
        return toProfile(findEntity(profileId));
    }

    @Transactional(readOnly = true)
    public FraudScoringProfileResponse readActiveProfile() {
        return fraudScoringProfileRepository.findByActiveTrue()
            .map(this::toResponse)
            .orElseGet(this::systemDefaultResponse);
    }

    @Transactional(readOnly = true)
    public List<FraudScoringProfileResponse> listProfiles() {
        List<FraudScoringProfileResponse> profiles = fraudScoringProfileRepository.findAllByOrderByVersionNumberDesc()
            .stream()
            .map(this::toResponse)
            .toList();
        return profiles.isEmpty() ? List.of(systemDefaultResponse()) : profiles;
    }

    @Transactional
    public FraudScoringProfileResponse createProfile(FraudScoringProfileCreateRequest request, String operator) {
        if (fraudScoringProfileRepository.existsByProfileNameIgnoreCase(request.profileName())) {
            throw new FraudScoringProfileConflictException(
                "A fraud scoring profile named '%s' already exists.".formatted(request.profileName())
            );
        }

        FraudScoringProfile thresholds = new FraudScoringProfile(
            request.challengeThreshold(),
            request.holdThreshold(),
            request.declineThreshold()
        );
        Instant createdAt = clock.instant();
        int nextVersion = fraudScoringProfileRepository.findTopByOrderByVersionNumberDesc()
            .map(profile -> profile.getVersionNumber() + 1)
            .orElse(1);

        FraudScoringProfileEntity entity = new FraudScoringProfileEntity(
            UUID.randomUUID(),
            nextVersion,
            request.profileName(),
            thresholds.challengeThreshold(),
            thresholds.holdThreshold(),
            thresholds.declineThreshold(),
            request.changeSummary(),
            operator,
            null,
            false,
            createdAt,
            createdAt,
            null
        );

        fraudScoringProfileRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public FraudScoringProfileResponse activateProfile(UUID profileId, FraudScoringProfileActivationRequest request, String operator) {
        FraudScoringProfileEntity entity = findEntity(profileId);
        if (entity.isActive()) {
            return toResponse(entity);
        }

        Instant activatedAt = clock.instant();
        fraudScoringProfileRepository.findByActiveTrue()
            .filter(activeProfile -> !activeProfile.getId().equals(profileId))
            .ifPresent(activeProfile -> activeProfile.deactivate(activatedAt));
        fraudScoringProfileRepository.deactivateOtherProfiles(profileId);
        entity.activate(activatedAt, operator);
        return toResponse(entity);
    }

    private FraudScoringProfileEntity findEntity(UUID profileId) {
        return fraudScoringProfileRepository.findById(profileId)
            .orElseThrow(() -> new FraudScoringProfileNotFoundException(profileId));
    }

    private FraudScoringProfile toProfile(FraudScoringProfileEntity entity) {
        return new FraudScoringProfile(
            entity.getChallengeThreshold(),
            entity.getHoldThreshold(),
            entity.getDeclineThreshold()
        );
    }

    private FraudScoringProfileResponse toResponse(FraudScoringProfileEntity entity) {
        return new FraudScoringProfileResponse(
            entity.getId(),
            entity.getVersionNumber(),
            entity.getProfileName(),
            entity.isActive(),
            false,
            new FraudScoringThresholdsResponse(
                entity.getChallengeThreshold(),
                entity.getHoldThreshold(),
                entity.getDeclineThreshold()
            ),
            entity.getChangeSummary(),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getActivatedBy(),
            entity.getActivatedAt()
        );
    }

    private FraudScoringProfileResponse systemDefaultResponse() {
        FraudScoringProfile profile = FraudScoringProfile.from(fraudScoringProperties);
        return new FraudScoringProfileResponse(
            null,
            0,
            "System Default",
            true,
            true,
            new FraudScoringThresholdsResponse(
                profile.challengeThreshold(),
                profile.holdThreshold(),
                profile.declineThreshold()
            ),
            "Active configuration sourced from application properties.",
            "system",
            null,
            "system",
            null
        );
    }
}
