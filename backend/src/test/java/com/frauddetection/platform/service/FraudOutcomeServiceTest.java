package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.frauddetection.platform.dto.FraudQualityMetricsResponse;
import com.frauddetection.platform.entity.FraudOutcomeEntity;
import com.frauddetection.platform.model.FraudOutcomeLabel;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudOutcomeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudOutcomeServiceTest {

    @Mock
    private FraudOutcomeRepository fraudOutcomeRepository;

    @Mock
    private FraudAssessmentRecordRepository fraudAssessmentRecordRepository;

    @Test
    void calculatesDecisionQualityFromConclusiveGroundTruth() {
        Instant now = Instant.parse("2026-07-18T12:00:00Z");
        List<FraudOutcomeRepository.QualityAggregateView> aggregates = List.of(
            aggregate(RiskDecision.DECLINE, FraudOutcomeLabel.CONFIRMED_FRAUD, 1, "1000", "250"),
            aggregate(RiskDecision.CHALLENGE, FraudOutcomeLabel.GENUINE, 1, "0", "0")
        );
        when(fraudOutcomeRepository.findQualityAggregates()).thenReturn(aggregates);

        FraudOutcomeService service = new FraudOutcomeService(
            fraudOutcomeRepository,
            fraudAssessmentRecordRepository,
            Clock.fixed(now, ZoneOffset.UTC)
        );

        FraudQualityMetricsResponse metrics = service.qualityMetrics();

        assertThat(metrics.truePositives()).isEqualTo(1);
        assertThat(metrics.falsePositives()).isEqualTo(1);
        assertThat(metrics.precision()).isEqualByComparingTo("0.5000");
        assertThat(metrics.recall()).isEqualByComparingTo("1.0000");
        assertThat(metrics.netLoss()).isEqualByComparingTo("750");
    }

    private FraudOutcomeRepository.QualityAggregateView aggregate(
        RiskDecision decision,
        FraudOutcomeLabel label,
        long total,
        String loss,
        String recovered
    ) {
        FraudOutcomeRepository.QualityAggregateView aggregate = mock(FraudOutcomeRepository.QualityAggregateView.class);
        when(aggregate.getDecision()).thenReturn(decision);
        when(aggregate.getOutcomeLabel()).thenReturn(label);
        when(aggregate.getTotal()).thenReturn(total);
        when(aggregate.getActualLoss()).thenReturn(new BigDecimal(loss));
        when(aggregate.getRecoveredAmount()).thenReturn(new BigDecimal(recovered));
        return aggregate;
    }
}
