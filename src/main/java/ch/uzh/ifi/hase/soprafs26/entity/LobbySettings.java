package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.Difficulty;
import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable // This can be reused for 'Game Settings' later on => Lobby entity won't get cluttered if we decide to add new settings
public class LobbySettings {

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty = Difficulty.MEDIUM;

    private Integer timeLimit = 0; // seconds where 0 = unlimited
    private Integer spymasterTimeLimit;
    private Integer spyTimeLimit;

    private Integer rounds = 0; // 0 = unlimited

    @Enumerated(EnumType.STRING)
    private List<Topic> topics = new ArrayList<>();

    private String customWordList; // probably as JSON 


    public LobbySettings(){ // Constructor with defaults
        this.difficulty = Difficulty.MEDIUM;
        this.timeLimit = 0;
        this.rounds = 0;
        this.topics = new ArrayList<>();
        this.spymasterTimeLimit = 0;
        this.spyTimeLimit = 0;
    }

    // Getters, Setters
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }

    public Integer getRounds() { return rounds; }
    public void setRounds(Integer rounds) { this.rounds = rounds; }

    public List<Topic> getTopics() { return topics; }
    public void setTopics(List<Topic> topics) { this.topics = topics; }

    public String getCustomWordList() { return customWordList; }
    public void setCustomWordList(String customWordList) { this.customWordList = customWordList; }

    public Integer getSpymasterTimeLimit() { return spymasterTimeLimit; }
    public void setSpymasterTimeLimit(Integer spymasterTimeLimit) { this.spymasterTimeLimit = spymasterTimeLimit; }

    public Integer getSpyTimeLimit() { return spyTimeLimit; }
    public void setSpyTimeLimit(Integer spyTimeLimit) { this.spyTimeLimit = spyTimeLimit; }
}