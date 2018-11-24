package com.pwojaczek;

public enum OutcomeEnum {
    WIN(" 1.0 - 0.0 "),
    LOSS(" 0.0 - 1.0 "),
    DRAW(" 0.5 - 0.5 "),
    FREE_POINT("");


    private String outcome;

    OutcomeEnum(String outcome) {
        this.outcome = outcome;
    }

    public String getOutcome() {
        return outcome;
    }
}
