package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.Topic;

import java.util.List;

public class UpdateLobbySettingsRequestDTO {
    private Integer spymasterTimeLimit;
    private Integer spyTimeLimit;
    private Integer rounds;
    private List<Topic> topics;
    private String customWordList;

    // Getters, Setters
    public Integer getSpymasterTimeLimit() { return spymasterTimeLimit; }
    public void setSpymasterTimeLimit(Integer spymasterTimeLimit) { this.spymasterTimeLimit = spymasterTimeLimit; }

    public Integer getSpyTimeLimit() { return spyTimeLimit; }
    public void setSpyTimeLimit(Integer spyTimeLimit) { this.spyTimeLimit = spyTimeLimit; }

    public Integer getRounds() { return rounds; }
    public void setRounds(Integer rounds) { this.rounds = rounds; }

    public List<Topic> getTopics() { return topics; }
    public void setTopics(List<Topic> topics) { this.topics = topics; }

    public String getCustomWordList() { return customWordList; }
    public void setCustomWordList(String customWordList) { this.customWordList = customWordList; }
}
