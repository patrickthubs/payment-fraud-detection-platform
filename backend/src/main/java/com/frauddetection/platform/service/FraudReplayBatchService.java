package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudMetricCountResponse;
import com.frauddetection.platform.dto.FraudReplayBatchCreateRequest;
import com.frauddetection.platform.dto.FraudReplayBatchItemResponse;
import com.frauddetection.platform.dto.FraudReplayBatchResponse;
import com.frauddetection.platform.dto.FraudScoringThresholdsResponse;
import com.frauddetection.platform.entity.FraudReplayBatchEntity;
import com.frauddetection.platform.entity.FraudReplayBatchItemEntity;
import com.frauddetection.platform.exception.FraudReplayBatchNotFoundException;
import com.frauddetection.platform.repository.FraudReplayBatchItemRepository;
import com.frauddetection.platform.repository.FraudReplayBatchRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudReplayBatchService {

    private final FraudReplayBatchRepository fraudReplayBatchRepository;
    private final FraudReplayBatchItemRepository fraudReplayBatchItemRepository;
    private final FraudSimulationService fraudSimulationService;

    public FraudReplayBatchService(
        FraudReplayBatchRepository fraudReplayBatchRepository,
        FraudReplayBatchItemRepository fraudReplayBatchItemRepository,
        FraudSimulationService fraudSimulationService
    ) {
        this.fraudReplayBatchRepository = fraudReplayBatchRepository;
        this.fraudReplayBatchItemRepository = fraudReplayBatchItemRepository;
        this.fraudSimulationService = fraudSimulationService;
    }

    @Transactional
    public FraudReplayBatchResponse create(FraudReplayBatchCreateRequest request, String operator) {
        FraudScoringProfile profile = request.overrides() == null
            ? fraudSimulationService.activeProfile()
            : fraudSimulationService.mergeOverrides(request.overrides());
        Instant createdAt = Instant.now();

        FraudReplayBatchEntity batch = fraudReplayBatchRepository.save(new FraudReplayBatchEntity(
            UUID.randomUUID(),
            request.batchName().trim(),
            request.scenarios().size(),
            profile.challengeThreshold(),
            profile.holdThreshold(),
            profile.declineThreshold(),
            operator,
            createdAt
        ));

        List<FraudReplayBatchItemEntity> items = IntStream.range(0, request.scenarios().size())
            .mapToObj(index -> toItemEntity(batch.getId(), index, request, profile, createdAt))
            .toList();

        List<FraudReplayBatchItemEntity> savedItems = fraudReplayBatchItemRepository.saveAll(items);
        return toResponse(batch, savedItems);
    }

    @Transactional(readOnly = true)
    public List<FraudReplayBatchResponse> findAll() {
        return fraudReplayBatchRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(batch -> toResponse(batch, fraudReplayBatchItemRepository.findAllByBatchIdOrderByScenarioIndexAsc(batch.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public FraudReplayBatchResponse findById(UUID batchId) {
        FraudReplayBatchEntity batch = fraudReplayBatchRepository.findById(batchId)
            .orElseThrow(() -> new FraudReplayBatchNotFoundException(batchId));
        return toResponse(batch, fraudReplayBatchItemRepository.findAllByBatchIdOrderByScenarioIndexAsc(batchId));
    }

    private FraudReplayBatchItemEntity toItemEntity(
        UUID batchId,
        int index,
        FraudReplayBatchCreateRequest request,
        FraudScoringProfile profile,
        Instant createdAt
    ) {
        var scenario = request.scenarios().get(index);
        FraudSimulationResult result = fraudSimulationService.simulate(scenario, profile);
        FraudRiskAssessment assessment = result.assessment();

        return new FraudReplayBatchItemEntity(
            UUID.randomUUID(),
            batchId,
            index,
            scenario.paymentId(),
            scenario.customerId(),
            assessment.riskScore(),
            assessment.decision(),
            result.projectedPaymentStatus(),
            result.velocitySource(),
            result.reviewCaseWouldBeCreated(),
            assessment.summary(),
            assessment.triggeredFactors().stream().map(factor -> factor.code().name()).toList().toString(),
            createdAt
        );
    }

    private FraudReplayBatchResponse toResponse(
        FraudReplayBatchEntity batch,
        List<FraudReplayBatchItemEntity> items
    ) {
        List<FraudReplayBatchItemResponse> itemResponses = items.stream()
            .map(this::toItemResponse)
            .toList();

        return new FraudReplayBatchResponse(
            batch.getId(),
            batch.getBatchName(),
            batch.getScenarioCount(),
            new FraudScoringThresholdsResponse(
                batch.getChallengeThreshold(),
                batch.getHoldThreshold(),
                batch.getDeclineThreshold()
            ),
            batch.getCreatedBy(),
            batch.getCreatedAt(),
            groupCounts(items, item -> item.getDecision().name()),
            groupCounts(items, item -> item.getProjectedPaymentStatus().name()),
            items.stream().filter(FraudReplayBatchItemEntity::isReviewCaseWouldBeCreated).count(),
            itemResponses
        );
    }

    private FraudReplayBatchItemResponse toItemResponse(FraudReplayBatchItemEntity entity) {
        return new FraudReplayBatchItemResponse(
            entity.getId(),
            entity.getScenarioIndex(),
            entity.getPaymentId(),
            entity.getCustomerId(),
            entity.getRiskScore(),
            entity.getDecision(),
            entity.getProjectedPaymentStatus(),
            entity.getVelocitySource(),
            entity.isReviewCaseWouldBeCreated(),
            entity.getSummary(),
            parseTriggeredFactorCodes(entity.getTriggeredFactorCodes())
        );
    }

    private List<FraudMetricCountResponse> groupCounts(
        List<FraudReplayBatchItemEntity> items,
        Function<FraudReplayBatchItemEntity, String> classifier
    ) {
        Map<String, Long> counts = items.stream()
            .collect(Collectors.groupingBy(classifier, Collectors.counting()));
        return counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
            .map(entry -> new FraudMetricCountResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<String> parseTriggeredFactorCodes(String rawTriggeredFactorCodes) {
        String cleaned = rawTriggeredFactorCodes.substring(1, rawTriggeredFactorCodes.length() - 1).trim();
        if (cleaned.isEmpty()) {
            return List.of();
        }
        return List.of(cleaned.split(", ")).stream()
            .map(String::trim)
            .toList();
    }
}
