package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.FraudReplayBatchCreateRequest;
import com.frauddetection.platform.dto.FraudReplayBatchResponse;
import com.frauddetection.platform.dto.FraudScoringOverrideRequest;
import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.entity.FraudReplayBatchEntity;
import com.frauddetection.platform.entity.FraudReplayBatchItemEntity;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.RiskFactor;
import com.frauddetection.platform.model.RiskFactorCode;
import com.frauddetection.platform.model.VelocitySource;
import com.frauddetection.platform.repository.FraudReplayBatchItemRepository;
import com.frauddetection.platform.repository.FraudReplayBatchRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudReplayBatchServiceTest {

    private FraudReplayBatchRepository fraudReplayBatchRepository;
    private FraudReplayBatchItemRepository fraudReplayBatchItemRepository;
    private FraudSimulationService fraudSimulationService;
    private FraudReplayBatchService fraudReplayBatchService;

    @BeforeEach
    void setUp() {
        fraudReplayBatchRepository = mock(FraudReplayBatchRepository.class);
        fraudReplayBatchItemRepository = mock(FraudReplayBatchItemRepository.class);
        fraudSimulationService = mock(FraudSimulationService.class);
        fraudReplayBatchService = new FraudReplayBatchService(
            fraudReplayBatchRepository,
            fraudReplayBatchItemRepository,
            fraudSimulationService
        );
    }

    @Test
    void createsReplayBatchAndAggregatesResults() {
        PaymentRiskAssessmentRequest firstScenario = scenario("PAY-1", "CUST-1", true);
        PaymentRiskAssessmentRequest secondScenario = scenario("PAY-2", "CUST-2", false);
        FraudScoringProfile profile = new FraudScoringProfile(40, 60, 80);

        when(fraudSimulationService.mergeOverrides(any(FraudScoringOverrideRequest.class))).thenReturn(profile);
        when(fraudReplayBatchRepository.save(any(FraudReplayBatchEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudSimulationService.simulate(firstScenario, profile)).thenReturn(new FraudSimulationResult(
            VelocitySource.REDIS,
            PaymentStatus.HELD,
            true,
            new FraudRiskAssessment(
                72,
                RiskDecision.HOLD,
                "HOLD because 3 risk factors were triggered.",
                List.of(
                    new RiskFactor(RiskFactorCode.HIGH_VELOCITY, 20, "high velocity"),
                    new RiskFactor(RiskFactorCode.NEW_DEVICE, 10, "new device")
                )
            )
        ));
        when(fraudSimulationService.simulate(secondScenario, profile)).thenReturn(new FraudSimulationResult(
            VelocitySource.REQUEST_FALLBACK,
            PaymentStatus.APPROVED,
            false,
            new FraudRiskAssessment(
                15,
                RiskDecision.ALLOW,
                "ALLOW because 1 risk factors were triggered.",
                List.of(new RiskFactor(RiskFactorCode.NEW_DEVICE, 10, "new device"))
            )
        ));
        when(fraudReplayBatchItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FraudReplayBatchResponse response = fraudReplayBatchService.create(
            new FraudReplayBatchCreateRequest(
                "July 17 replay",
                new FraudScoringOverrideRequest(40, 60, 80),
                List.of(firstScenario, secondScenario)
            ),
            "analyst.one"
        );

        assertThat(response.batchName()).isEqualTo("July 17 replay");
        assertThat(response.scenarioCount()).isEqualTo(2);
        assertThat(response.thresholds().challengeThreshold()).isEqualTo(40);
        assertThat(response.reviewCaseWouldBeCreatedCount()).isEqualTo(1);
        assertThat(response.decisions()).extracting("label").containsExactly("ALLOW", "HOLD");
        assertThat(response.projectedPaymentStatuses()).extracting("label").containsExactly("APPROVED", "HELD");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).scenarioIndex()).isEqualTo(0);
        assertThat(response.items().get(1).scenarioIndex()).isEqualTo(1);
    }

    @Test
    void returnsStoredReplayBatchById() {
        UUID batchId = UUID.randomUUID();
        FraudReplayBatchEntity batch = new FraudReplayBatchEntity(
            batchId,
            "Stored replay",
            1,
            45,
            65,
            85,
            "analyst.one",
            Instant.parse("2026-07-17T10:15:30Z")
        );
        FraudReplayBatchItemEntity item = new FraudReplayBatchItemEntity(
            UUID.randomUUID(),
            batchId,
            0,
            "PAY-100",
            "CUST-100",
            88,
            RiskDecision.DECLINE,
            PaymentStatus.DECLINED,
            VelocitySource.REDIS,
            true,
            "DECLINE because 4 risk factors were triggered.",
            "[HIGH_VELOCITY, IMPOSSIBLE_TRAVEL]",
            Instant.parse("2026-07-17T10:15:30Z")
        );

        when(fraudReplayBatchRepository.findById(batchId)).thenReturn(java.util.Optional.of(batch));
        when(fraudReplayBatchItemRepository.findAllByBatchIdOrderByScenarioIndexAsc(batchId)).thenReturn(List.of(item));

        FraudReplayBatchResponse response = fraudReplayBatchService.findById(batchId);

        assertThat(response.batchId()).isEqualTo(batchId);
        assertThat(response.items()).singleElement().satisfies(replayItem -> {
            assertThat(replayItem.triggeredFactorCodes()).containsExactly("HIGH_VELOCITY", "IMPOSSIBLE_TRAVEL");
            assertThat(replayItem.decision()).isEqualTo(RiskDecision.DECLINE);
        });
    }

    private PaymentRiskAssessmentRequest scenario(String paymentId, String customerId, boolean newDevice) {
        return new PaymentRiskAssessmentRequest(
            paymentId,
            customerId,
            BigDecimal.valueOf(5000),
            "ZAR",
            "ELECTRONICS",
            "MOBILE_APP",
            BigDecimal.valueOf(1500),
            3,
            BigDecimal.valueOf(8000),
            4,
            newDevice,
            false,
            false,
            false
        );
    }
}
