package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudOutcomeRequest;
import com.frauddetection.platform.dto.FraudOutcomeResponse;
import com.frauddetection.platform.dto.FraudQualityMetricsResponse;
import com.frauddetection.platform.entity.FraudOutcomeEntity;
import com.frauddetection.platform.exception.FraudAssessmentNotFoundException;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudOutcomeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudOutcomeService {

    private final FraudOutcomeRepository fraudOutcomeRepository;
    private final FraudAssessmentRecordRepository fraudAssessmentRecordRepository;
    private final CurrentTenantService currentTenantService;
    private final Clock clock;

    @Autowired
    public FraudOutcomeService(
        FraudOutcomeRepository fraudOutcomeRepository,
        FraudAssessmentRecordRepository fraudAssessmentRecordRepository,
        CurrentTenantService currentTenantService,
        Clock clock
    ) {
        this.fraudOutcomeRepository = fraudOutcomeRepository;
        this.fraudAssessmentRecordRepository = fraudAssessmentRecordRepository;
        this.currentTenantService = currentTenantService;
        this.clock = clock;
    }

    @Transactional
    public FraudOutcomeResponse record(UUID assessmentId, FraudOutcomeRequest request, String operator) {
        UUID organizationId = currentTenantService.organizationId();
        if (!fraudAssessmentRecordRepository.existsByOrganizationIdAndId(organizationId, assessmentId)) {
            throw new FraudAssessmentNotFoundException(assessmentId);
        }
        Instant now = clock.instant();
        FraudOutcomeEntity entity = fraudOutcomeRepository.findByOrganizationIdAndAssessmentId(organizationId, assessmentId)
            .map(existing -> {
                existing.update(
                    request.outcomeLabel(), request.source(), request.actualLoss(), request.recoveredAmount(),
                    request.notes(), operator, request.occurredAt(), now
                );
                return existing;
            })
            .orElseGet(() -> new FraudOutcomeEntity(
                UUID.randomUUID(), organizationId, assessmentId, request.outcomeLabel(), request.source(), request.actualLoss(),
                request.recoveredAmount(), request.notes(), operator, request.occurredAt(), now, now
            ));
        return toResponse(fraudOutcomeRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public FraudOutcomeResponse findByAssessmentId(UUID assessmentId) {
        return fraudOutcomeRepository.findByOrganizationIdAndAssessmentId(currentTenantService.organizationId(), assessmentId)
            .map(this::toResponse)
            .orElseThrow(() -> new FraudAssessmentNotFoundException(assessmentId));
    }

    @Transactional(readOnly = true)
    public FraudQualityMetricsResponse qualityMetrics() {
        long tp = 0;
        long fp = 0;
        long fn = 0;
        long tn = 0;
        BigDecimal actualLoss = BigDecimal.ZERO;
        BigDecimal recovered = BigDecimal.ZERO;
        long totalLabelled = 0;
        UUID organizationId = currentTenantService.organizationId();
        List<FraudOutcomeRepository.QualityAggregateView> aggregates = fraudOutcomeRepository.findQualityAggregates(organizationId);
        for (FraudOutcomeRepository.QualityAggregateView aggregate : aggregates) {
            totalLabelled += aggregate.getTotal();
            actualLoss = actualLoss.add(aggregate.getActualLoss());
            recovered = recovered.add(aggregate.getRecoveredAmount());
            if (!aggregate.getOutcomeLabel().isConclusive()) {
                continue;
            }
            boolean flagged = aggregate.getDecision() != RiskDecision.ALLOW;
            boolean fraud = aggregate.getOutcomeLabel().isFraud();
            long count = aggregate.getTotal();
            if (flagged && fraud) tp += count;
            if (flagged && !fraud) fp += count;
            if (!flagged && fraud) fn += count;
            if (!flagged && !fraud) tn += count;
        }
        long conclusive = tp + fp + fn + tn;
        return new FraudQualityMetricsResponse(
            totalLabelled, conclusive, tp, fp, fn, tn,
            ratio(tp, tp + fp), ratio(tp, tp + fn), ratio(fp, fp + tn),
            actualLoss, recovered, actualLoss.subtract(recovered)
        );
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private FraudOutcomeResponse toResponse(FraudOutcomeEntity entity) {
        return new FraudOutcomeResponse(
            entity.getId(), entity.getAssessmentId(), entity.getOutcomeLabel(), entity.getSource(),
            entity.getActualLoss(), entity.getRecoveredAmount(), entity.getNotes(), entity.getLabelledBy(),
            entity.getOccurredAt(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
