package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.model.VelocitySource;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisVelocityFeatureService implements VelocityFeatureService {

    private static final Duration FIVE_MINUTE_TTL = Duration.ofMinutes(5);
    private static final Duration ONE_HOUR_TTL = Duration.ofHours(1);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisVelocityFeatureService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public VelocitySnapshot resolveFeatures(PaymentRiskAssessmentRequest request) {
        try {
            String countKey = countKey(request.customerId());
            String spendKey = spendKey(request.customerId());
            String storedCount = stringRedisTemplate.opsForValue().get(countKey);
            String storedSpend = stringRedisTemplate.opsForValue().get(spendKey);

            if (storedCount == null || storedSpend == null) {
                return fallbackSnapshot(request);
            }

            return new VelocitySnapshot(
                Integer.parseInt(storedCount),
                new BigDecimal(storedSpend),
                VelocitySource.REDIS
            );
        } catch (DataAccessException | NumberFormatException exception) {
            return fallbackSnapshot(request);
        }
    }

    @Override
    public void recordAssessment(PaymentRiskAssessmentRequest request, VelocitySnapshot velocitySnapshot) {
        try {
            String countKey = countKey(request.customerId());
            String spendKey = spendKey(request.customerId());

            Long updatedCount = stringRedisTemplate.opsForValue().increment(countKey);
            if (updatedCount != null && updatedCount == 1L) {
                stringRedisTemplate.expire(countKey, FIVE_MINUTE_TTL);
            }

            Double updatedSpend = stringRedisTemplate.opsForValue().increment(spendKey, request.amount().doubleValue());
            if (updatedSpend != null && updatedSpend.doubleValue() == request.amount().doubleValue()) {
                stringRedisTemplate.expire(spendKey, ONE_HOUR_TTL);
            }
        } catch (DataAccessException exception) {
            // The request should still succeed even when Redis is temporarily unavailable.
        }
    }

    private VelocitySnapshot fallbackSnapshot(PaymentRiskAssessmentRequest request) {
        return new VelocitySnapshot(
            request.transactionCountLastFiveMinutes(),
            request.spendLastHour(),
            VelocitySource.REQUEST_FALLBACK
        );
    }

    private String countKey(String customerId) {
        return "velocity:customer:%s:5m:count".formatted(customerId);
    }

    private String spendKey(String customerId) {
        return "velocity:customer:%s:1h:amount".formatted(customerId);
    }
}
