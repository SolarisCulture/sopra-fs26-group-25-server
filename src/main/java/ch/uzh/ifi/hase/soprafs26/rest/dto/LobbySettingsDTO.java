package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.Difficulty;
import ch.uzh.ifi.hase.soprafs26.constant.Topic;

public class LobbySettingsDTO {
    private Difficulty difficulty;
    private int timeLimit;
    private int rounds;
    private List<Topic> topics;
    private Integer spymasterTimeLimit;
    private Integer spyTimeLimit;

    // Getters, Setters
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = rounds; }

    public List<Topic> getTopics() { return topics; }
    public void setTopics(List<Topic> topics) { this.topics = topics; }

    public Integer getSpymasterTimeLimit() { return spymasterTimeLimit; }
    public void setSpymasterTimeLimit(Integer spymasterTimeLimit) { this.spymasterTimeLimit = spymasterTimeLimit; }

    public Integer getSpyTimeLimit() { return spyTimeLimit; }
    public void setSpyTimeLimit(Integer spyTimeLimit) { this.spyTimeLimit = spyTimeLimit; }
}
