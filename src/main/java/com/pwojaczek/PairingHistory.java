package com.pwojaczek;

import java.util.List;

public class PairingHistory {

    private int pairingNumber;
    private List<Match> matches;

    public PairingHistory(int pairingNumber, List<Match> matches) {
        this.pairingNumber = pairingNumber;
        this.matches = matches;
    }

    public int getPairingNumber() {
        return pairingNumber;
    }

    public void setPairingNumber(int pairingNumber) {
        this.pairingNumber = pairingNumber;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }
}
