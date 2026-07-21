package com.frauddetection.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudOutboundEventService {

    private final FraudOutboundEventRepository fraudOutboundEventRepository;
    private final ObjectMapper objectMapper;
    private final CurrentTenantService currentTenantService;

    @Autowired
    public FraudOutboundEventService(
        FraudOutboundEventRepository fraudOutboundEventRepository,
        ObjectMapper objectMapper,
        CurrentTenantService currentTenantService
    ) {
        this.fraudOutboundEventRepository = fraudOutboundEventRepository;
        this.objectMapper = objectMapper;
        this.currentTenantService = currentTenantService;
    }

    @Transactional
    public void enqueue(String eventType, String topicName, String messageKey, Object payload, Instant createdAt) {
        fraudOutboundEventRepository.save(new FraudOutboundEventEntity(
            UUID.randomUUID(),
            currentTenantService.organizationId(),
            eventType,
            topicName,
            messageKey,
            serialize(payload),
            FraudOutboundEventStatus.PENDING,
            0,
            createdAt,
            null,
            null,
            null,
            null,
            null,
            null,
            createdAt,
            createdAt
        ));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbound fraud event payload", exception);
        }
    }
}
