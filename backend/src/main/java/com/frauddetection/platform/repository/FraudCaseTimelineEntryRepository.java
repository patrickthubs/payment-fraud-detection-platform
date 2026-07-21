package com.frauddetection.platform.repository;

import com.frauddetection.platform.entity.FraudCaseTimelineEntryEntity;
import com.frauddetection.platform.model.FraudCaseActionType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FraudCaseTimelineEntryRepository extends JpaRepository<FraudCaseTimelineEntryEntity, UUID> {

    List<FraudCaseTimelineEntryEntity> findAllByOrganizationIdAndCaseIdOrderByCreatedAtAsc(UUID organizationId, UUID caseId);

    @Query("""
        select entry.actor as actor, entry.actionType as actionType, count(entry) as total
        from FraudCaseTimelineEntryEntity entry
        where entry.organizationId = :organizationId
        group by entry.actor, entry.actionType
        """)
    List<ActorActionCountView> countGroupedByActorAndActionType(UUID organizationId);

    interface ActorActionCountView {
        String getActor();

        FraudCaseActionType getActionType();

        long getTotal();
    }
}
