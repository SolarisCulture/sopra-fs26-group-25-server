package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class UpdateLobbySettingsRequestDTO {
    private Integer spymasterTimeLimit;
    private Integer spyTimeLimit;
    private Integer rounds;

    // Getters, Setters
    public Integer getSpymasterTimeLimit() { return spymasterTimeLimit; }
    public void setSpymasterTimeLimit(Integer spymasterTimeLimit) { this.spymasterTimeLimit = spymasterTimeLimit; }

    public Integer getSpyTimeLimit() { return spyTimeLimit; }
    public void setSpyTimeLimit(Integer spyTimeLimit) { this.spyTimeLimit = spyTimeLimit; }

    public Integer getRounds() { return rounds; }
    public void setRounds(Integer rounds) { this.rounds = rounds; }
}
