package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FraudScoringProfileTest {

    @Test
    void rejectsHoldThresholdThatIsNotAboveChallengeThreshold() {
        assertThatThrownBy(() -> new FraudScoringProfile(45, 45, 85))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("holdThreshold");
    }

    @Test
    void rejectsDeclineThresholdAboveHundred() {
        assertThatThrownBy(() -> new FraudScoringProfile(45, 65, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("declineThreshold");
    }
}
