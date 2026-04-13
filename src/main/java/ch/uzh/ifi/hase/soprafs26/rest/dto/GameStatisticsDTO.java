package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;

public class GameStatisticsDTO {
    private int redScore;
    private int blueScore;
    private int roundsPlayed;
    private int totalTime;
    private TeamColor winningTeam;

    public int getRedScore() { return redScore; }
    public void setRedScore(int redScore) { this.redScore = redScore; }

    public int getBlueScore() { return blueScore; }
    public void setBlueScore(int blueScore) { this.blueScore = blueScore; }

    public int getRoundsPlayed() { return roundsPlayed; }
    public void setRoundsPlayed(int roundsPlayed) { this.roundsPlayed = roundsPlayed; }

    public int getTotalTime() { return totalTime; }
    public void setTotalTime(int totalTime) { this.totalTime = totalTime; }

    public TeamColor getWinningTeam() { return winningTeam; }
    public void setWinningTeam(TeamColor winningTeam) { this.winningTeam = winningTeam; }
}
