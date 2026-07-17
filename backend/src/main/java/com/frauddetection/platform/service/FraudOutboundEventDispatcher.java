package com.frauddetection.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.platform.config.FraudOutboxProperties;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(value = "fraud.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class FraudOutboundEventDispatcher {

    private final FraudOutboundEventRepository fraudOutboundEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FraudOutboxProperties fraudOutboxProperties;
    private final ObjectMapper objectMapper;

    public FraudOutboundEventDispatcher(
        FraudOutboundEventRepository fraudOutboundEventRepository,
        KafkaTemplate<String, Object> kafkaTemplate,
        FraudOutboxProperties fraudOutboxProperties,
        ObjectMapper objectMapper
    ) {
        this.fraudOutboundEventRepository = fraudOutboundEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.fraudOutboxProperties = fraudOutboxProperties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${fraud.outbox.dispatch-delay-ms:15000}")
    public void dispatchScheduledEvents() {
        dispatchReadyEvents();
    }

    @Transactional
    public int dispatchReadyEvents() {
        Instant now = Instant.now();
        List<FraudOutboundEventEntity> dueEvents =
            fraudOutboundEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                List.of(FraudOutboundEventStatus.PENDING),
                now,
                PageRequest.of(0, fraudOutboxProperties.batchSize())
            );

        dueEvents.forEach(event -> dispatchSingleEvent(event, now));
        return dueEvents.size();
    }

    private void dispatchSingleEvent(FraudOutboundEventEntity event, Instant now) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            kafkaTemplate.send(event.getTopicName(), event.getMessageKey(), payload).get(5, TimeUnit.SECONDS);
            event.markDelivered(now);
        } catch (Exception exception) {
            if (event.getAttemptCount() + 1 >= fraudOutboxProperties.maxAttempts()) {
                event.markFailed(now, exception.getMessage());
                return;
            }
            event.markRetryScheduled(now, now.plusMillis(fraudOutboxProperties.retryDelayMs()), exception.getMessage());
        }
    }
}
