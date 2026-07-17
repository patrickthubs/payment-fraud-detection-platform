package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.entity.FraudCaseTimelineEntryEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.exception.InvalidFraudCaseFilterException;
import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.FraudCaseActionType;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudCaseTimelineEntryRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class FraudCaseQueryServiceTest {

    private FraudReviewCaseRepository fraudReviewCaseRepository;
    private FraudCaseTimelineEntryRepository fraudCaseTimelineEntryRepository;
    private FraudCaseQueryService fraudCaseQueryService;

    @BeforeEach
    void setUp() {
        fraudReviewCaseRepository = mock(FraudReviewCaseRepository.class);
        fraudCaseTimelineEntryRepository = mock(FraudCaseTimelineEntryRepository.class);
        fraudCaseQueryService = new FraudCaseQueryService(
            fraudReviewCaseRepository,
            fraudCaseTimelineEntryRepository,
            Clock.fixed(Instant.parse("2026-07-17T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void returnsFilteredCasesSortedByNewestFirst() {
        UUID caseId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        when(fraudReviewCaseRepository.findAll(typedSpecification(), eq(Sort.by(Sort.Direction.DESC, "createdAt"))))
            .thenReturn(List.of(buildCase(caseId, "PAY-FILTER-1001", "analyst.one", ReviewCaseStatus.OPEN, "2026-07-17T08:00:00Z")));

        List<com.frauddetection.platform.dto.FraudCaseResponse> responses = fraudCaseQueryService.findAll(
            new FraudCaseFilterCriteria(
                ReviewCaseStatus.OPEN,
                "analyst.one",
                60,
                95,
                false,
                false,
                "PAY-FILTER",
                "CUST-FILTER",
                Instant.parse("2026-07-17T00:00:00Z"),
                Instant.parse("2026-07-17T09:00:00Z")
            )
        );

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.caseId()).isEqualTo(caseId);
            assertThat(response.paymentId()).isEqualTo("PAY-FILTER-1001");
            assertThat(response.currentAssignee()).isEqualTo("analyst.one");
            assertThat(response.timelineEntries()).isEmpty();
        });
    }

    @Test
    void exportsSupervisorCsvFromPersistedCases() {
        when(fraudReviewCaseRepository.findAll(typedSpecification(), eq(Sort.by(Sort.Direction.DESC, "createdAt"))))
            .thenReturn(List.of(
                buildCase(
                    UUID.fromString("10000000-0000-0000-0000-000000000001"),
                    "PAY-EXPORT-1001",
                    "senior.analyst",
                    ReviewCaseStatus.ESCALATED,
                    "2026-07-16T08:30:00Z"
                )
            ));

        String csv = fraudCaseQueryService.export(new FraudCaseFilterCriteria(
            ReviewCaseStatus.ESCALATED,
            null,
            null,
            null,
            true,
            false,
            "PAY-EXPORT",
            null,
            null,
            null
        ));

        assertThat(csv).contains("case_id,assessment_id,payment_id,customer_id,status,decision,risk_score,current_assignee,created_at,updated_at,breached_sla,resolution_outcome,resolution_summary,summary");
        assertThat(csv).contains("\"PAY-EXPORT-1001\"");
        assertThat(csv).contains("\"true\"");
        assertThat(csv).contains("\"Needs callback verification\"");
    }

    @Test
    void returnsTimelineForCaseDetail() {
        UUID caseId = UUID.fromString("10000000-0000-0000-0000-000000000009");
        when(fraudReviewCaseRepository.findById(caseId))
            .thenReturn(java.util.Optional.of(buildCase(caseId, "PAY-DETAIL-1001", "analyst.one", ReviewCaseStatus.OPEN, "2026-07-17T07:00:00Z")));
        when(fraudCaseTimelineEntryRepository.findAllByCaseIdOrderByCreatedAtAsc(caseId))
            .thenReturn(List.of(new FraudCaseTimelineEntryEntity(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                caseId,
                FraudCaseActionType.NOTE_ADDED,
                "analyst.one",
                "Called customer for verification.",
                Instant.parse("2026-07-17T07:30:00Z")
            )));

        var response = fraudCaseQueryService.findById(caseId);

        assertThat(response.timelineEntries()).singleElement().satisfies(entry -> {
            assertThat(entry.actionType()).isEqualTo(FraudCaseActionType.NOTE_ADDED);
            assertThat(entry.detail()).isEqualTo("Called customer for verification.");
        });
    }

    @Test
    void rejectsInvalidRiskRange() {
        assertThatThrownBy(() -> fraudCaseQueryService.findAll(new FraudCaseFilterCriteria(
            null,
            null,
            95,
            60,
            null,
            null,
            null,
            null,
            null,
            null
        )))
            .isInstanceOf(InvalidFraudCaseFilterException.class)
            .hasMessage("Minimum risk score cannot be greater than maximum risk score.");
    }

    private FraudReviewCaseEntity buildCase(
        UUID caseId,
        String paymentId,
        String assignee,
        ReviewCaseStatus status,
        String createdAt
    ) {
        Instant createdTimestamp = Instant.parse(createdAt);
        return new FraudReviewCaseEntity(
            caseId,
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            paymentId,
            "CUST-FILTER-1001",
            82,
            RiskDecision.HOLD,
            status,
            "Needs callback verification",
            assignee,
            status == ReviewCaseStatus.RESOLVED ? "Resolved" : null,
            status == ReviewCaseStatus.RESOLVED ? CaseResolutionOutcome.RELEASE_PAYMENT : null,
            createdTimestamp,
            createdTimestamp.plusSeconds(600)
        );
    }

    @SuppressWarnings("unchecked")
    private Specification<FraudReviewCaseEntity> typedSpecification() {
        return any(Specification.class);
    }
}
