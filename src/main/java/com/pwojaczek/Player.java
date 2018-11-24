package com.pwojaczek;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Player {

    private String name;
    private boolean freePoint;
    private float score;
    private List<Match> matchHistory;

    public Player(String name) {
        this.name = name;
        this.freePoint = false;
        this.score = 0;
        this.matchHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean receivedFreePoint() {
        return freePoint;
    }

    public void setFreePoint(boolean freePoint) {
        this.freePoint = freePoint;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public List<Match> getMatchHistory() {
        return matchHistory;
    }

    public void setMatchHistory(List<Match> matchHistory) {
        this.matchHistory = matchHistory;
    }

    public void draw() {
        score += 0.5f;
    }

    public void win() {
        score += 1f;
    }

    public boolean playedWith(Player player) {
        return player.matchHistory.stream().anyMatch(match -> match.getPlayer2().equals(this) || match.getPlayer1().equals(this));
    }

    public void addMatch(Match match) {
        this.matchHistory.add(match);
    }

    public List<Player> getPlayersPlayed() {
        return this.matchHistory.stream().map(match -> match.getPlayer1() == this ? match.getPlayer2() : match.getPlayer1()).collect(Collectors.toList());
    }
}
