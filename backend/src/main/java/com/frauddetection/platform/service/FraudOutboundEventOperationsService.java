package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudOutboundDispatchResponse;
import com.frauddetection.platform.dto.FraudOutboundEventResponse;
import com.frauddetection.platform.dto.FraudOutboundIncidentNoteRequest;
import com.frauddetection.platform.dto.FraudOutboundRetryBatchResponse;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.exception.FraudOutboundEventActionNotAllowedException;
import com.frauddetection.platform.exception.FraudOutboundEventNotFoundException;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudOutboundEventOperationsService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 100;

    private final FraudOutboundEventRepository fraudOutboundEventRepository;
    private final ObjectProvider<FraudOutboundEventDispatcher> fraudOutboundEventDispatcherProvider;

    public FraudOutboundEventOperationsService(
        FraudOutboundEventRepository fraudOutboundEventRepository,
        ObjectProvider<FraudOutboundEventDispatcher> fraudOutboundEventDispatcherProvider
    ) {
        this.fraudOutboundEventRepository = fraudOutboundEventRepository;
        this.fraudOutboundEventDispatcherProvider = fraudOutboundEventDispatcherProvider;
    }

    @Transactional(readOnly = true)
    public List<FraudOutboundEventResponse> findEvents(
        FraudOutboundEventStatus status,
        String topicName,
        String eventType,
        String messageKey,
        Boolean onlyWithNotes,
        Integer limit
    ) {
        PageRequest pageRequest = PageRequest.of(0, normalizeLimit(limit));
        Specification<FraudOutboundEventEntity> specification =
            (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status)
            );
        }

        if (hasText(topicName)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("topicName"), topicName)
            );
        }

        if (hasText(eventType)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("eventType"), eventType)
            );
        }

        if (hasText(messageKey)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("messageKey")),
                    "%" + messageKey.toLowerCase() + "%"
                )
            );
        }

        if (Boolean.TRUE.equals(onlyWithNotes)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.isNotNull(root.get("operatorNote"))
            );
        }

        List<FraudOutboundEventEntity> entities = fraudOutboundEventRepository.findAll(specification, pageRequest).getContent();
        return entities.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FraudOutboundEventResponse retryEvent(UUID eventId) {
        FraudOutboundEventEntity entity = fraudOutboundEventRepository.findById(eventId)
            .orElseThrow(() -> new FraudOutboundEventNotFoundException(eventId));

        if (entity.getStatus() == FraudOutboundEventStatus.DELIVERED) {
            throw new FraudOutboundEventActionNotAllowedException(
                "Delivered outbound events cannot be retried."
            );
        }

        entity.retryNow(Instant.now());
        return toResponse(entity);
    }

    @Transactional
    public FraudOutboundEventResponse addIncidentNote(UUID eventId, String operator, FraudOutboundIncidentNoteRequest request) {
        FraudOutboundEventEntity entity = fraudOutboundEventRepository.findById(eventId)
            .orElseThrow(() -> new FraudOutboundEventNotFoundException(eventId));
        entity.updateOperatorNote(request.note(), operator, Instant.now());
        return toResponse(entity);
    }

    @Transactional
    public FraudOutboundRetryBatchResponse retryFailedEvents(Integer limit) {
        List<FraudOutboundEventEntity> failedEvents = fraudOutboundEventRepository.findByStatusOrderByCreatedAtDesc(
            FraudOutboundEventStatus.FAILED,
            PageRequest.of(0, normalizeLimit(limit))
        );

        Instant requestedAt = Instant.now();
        failedEvents.forEach(event -> event.retryNow(requestedAt));
        return new FraudOutboundRetryBatchResponse(normalizeLimit(limit), failedEvents.size());
    }

    @Transactional(readOnly = true)
    public String exportFailedEvents(Integer limit) {
        List<FraudOutboundEventEntity> failedEvents = fraudOutboundEventRepository.findByStatusOrderByCreatedAtDesc(
            FraudOutboundEventStatus.FAILED,
            PageRequest.of(0, normalizeLimit(limit))
        );

        StringBuilder csv = new StringBuilder(
            "eventId,eventType,topicName,messageKey,status,attemptCount,nextAttemptAt,lastAttemptedAt,publishedAt,lastError,operatorNote,notedBy,notedAt,createdAt,updatedAt\n"
        );

        failedEvents.forEach(event -> csv.append(String.join(",",
            csvField(event.getId().toString()),
            csvField(event.getEventType()),
            csvField(event.getTopicName()),
            csvField(event.getMessageKey()),
            csvField(event.getStatus().name()),
            csvField(Integer.toString(event.getAttemptCount())),
            csvField(stringValue(event.getNextAttemptAt())),
            csvField(stringValue(event.getLastAttemptedAt())),
            csvField(stringValue(event.getPublishedAt())),
            csvField(event.getLastError()),
            csvField(event.getOperatorNote()),
            csvField(event.getNotedBy()),
            csvField(stringValue(event.getNotedAt())),
            csvField(stringValue(event.getCreatedAt())),
            csvField(stringValue(event.getUpdatedAt()))
        )).append('\n'));

        return csv.toString();
    }

    public FraudOutboundDispatchResponse dispatchNow() {
        FraudOutboundEventDispatcher dispatcher = fraudOutboundEventDispatcherProvider.getIfAvailable();
        int processedCount = dispatcher == null ? 0 : dispatcher.dispatchReadyEvents();
        return new FraudOutboundDispatchResponse(processedCount);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private String csvField(String value) {
        String safeValue = value == null ? "" : value;
        return "\"%s\"".formatted(safeValue.replace("\"", "\"\""));
    }

    private FraudOutboundEventResponse toResponse(FraudOutboundEventEntity entity) {
        return new FraudOutboundEventResponse(
            entity.getId(),
            entity.getEventType(),
            entity.getTopicName(),
            entity.getMessageKey(),
            entity.getStatus(),
            entity.getAttemptCount(),
            entity.getNextAttemptAt(),
            entity.getLastAttemptedAt(),
            entity.getPublishedAt(),
            entity.getLastError(),
            entity.getOperatorNote(),
            entity.getNotedBy(),
            entity.getNotedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
