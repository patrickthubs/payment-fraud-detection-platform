package com.frauddetection.platform.model;

public enum FraudOutcomeLabel {
    CONFIRMED_FRAUD(true, true),
    ACCOUNT_TAKEOVER(true, true),
    CHARGEBACK(true, true),
    GENUINE(false, true),
    CUSTOMER_AUTHORIZED(false, true),
    INCONCLUSIVE(false, false);

    private final boolean fraud;
    private final boolean conclusive;

    FraudOutcomeLabel(boolean fraud, boolean conclusive) {
        this.fraud = fraud;
        this.conclusive = conclusive;
    }

    public boolean isFraud() {
        return fraud;
    }

    public boolean isConclusive() {
        return conclusive;
    }
}
