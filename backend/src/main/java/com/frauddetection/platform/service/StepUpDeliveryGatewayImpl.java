package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudStepUpProperties;
import com.frauddetection.platform.exception.StepUpDeliveryFailedException;
import com.frauddetection.platform.model.StepUpDeliveryChannel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class StepUpDeliveryGatewayImpl implements StepUpDeliveryGateway {

    private final FraudStepUpProperties fraudStepUpProperties;
    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    public StepUpDeliveryGatewayImpl(
        FraudStepUpProperties fraudStepUpProperties,
        ObjectProvider<JavaMailSender> javaMailSenderProvider
    ) {
        this.fraudStepUpProperties = fraudStepUpProperties;
        this.javaMailSenderProvider = javaMailSenderProvider;
    }

    @Override
    public void deliver(StepUpDeliveryRequest request) {
        if (request.deliveryChannel() == StepUpDeliveryChannel.DEVELOPMENT_LINK) {
            return;
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw new StepUpDeliveryFailedException(
                "Email delivery is enabled for step-up verification, but no mail sender is configured."
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fraudStepUpProperties.fromAddress());
        message.setTo(request.destination());
        message.setSubject("%s Step-up verification required".formatted(fraudStepUpProperties.subjectPrefix()));
        message.setText("""
            Hello %s,

            A protected fraud-platform action requires one-time verification.

            Verification link:
            %s

            This link expires at %s.

            If you did not request this action, contact platform administration immediately.
            """.formatted(
            request.operator().getDisplayName(),
            request.verificationUrl(),
            request.expiresAt()
        ));

        javaMailSender.send(message);
    }
}
