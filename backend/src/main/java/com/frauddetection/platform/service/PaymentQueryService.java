package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentStatusResponse;
import com.frauddetection.platform.dto.PaymentTransitionResponse;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.entity.PaymentStateTransitionEntity;
import com.frauddetection.platform.exception.PaymentNotFoundException;
import com.frauddetection.platform.repository.PaymentRecordRepository;
import com.frauddetection.platform.repository.PaymentStateTransitionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentQueryService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentStateTransitionRepository paymentStateTransitionRepository;

    public PaymentQueryService(
        PaymentRecordRepository paymentRecordRepository,
        PaymentStateTransitionRepository paymentStateTransitionRepository
    ) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentStateTransitionRepository = paymentStateTransitionRepository;
    }

    public List<PaymentStatusResponse> findAll() {
        return paymentRecordRepository.findAll().stream()
            .map(entity -> toResponse(entity, List.of()))
            .toList();
    }

    public PaymentStatusResponse findByPaymentId(String paymentId) {
        PaymentRecordEntity entity = paymentRecordRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        List<PaymentTransitionResponse> transitions = paymentStateTransitionRepository
            .findAllByPaymentIdOrderByCreatedAtAsc(paymentId)
            .stream()
            .map(this::toTransitionResponse)
            .toList();

        return toResponse(entity, transitions);
    }

    private PaymentStatusResponse toResponse(
        PaymentRecordEntity entity,
        List<PaymentTransitionResponse> transitions
    ) {
        return new PaymentStatusResponse(
            entity.getId(),
            entity.getPaymentId(),
            entity.getCustomerId(),
            entity.getAmount(),
            entity.getCurrency(),
            entity.getPaymentChannel(),
            entity.getMerchantCategory(),
            entity.getLatestAssessmentId(),
            entity.getLatestRiskScore(),
            entity.getLatestDecision(),
            entity.getPaymentStatus(),
            entity.getChallengeOutcome(),
            entity.getChallengedAt(),
            entity.getChallengeCompletedAt(),
            entity.getChallengeCompletedBy(),
            entity.getChallengeOutcomeNote(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            transitions
        );
    }

    private PaymentTransitionResponse toTransitionResponse(PaymentStateTransitionEntity entity) {
        return new PaymentTransitionResponse(
            entity.getId(),
            entity.getFromStatus(),
            entity.getToStatus(),
            entity.getReason(),
            entity.getAssessmentId(),
            entity.getCreatedAt()
        );
    }
}
