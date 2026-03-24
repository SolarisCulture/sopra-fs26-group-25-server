package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Embeddable // This can be reused for 'Game Settings' later on => Lobby entity won't get cluttered if we decide to add new settings
public class LobbySettings {

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty = Difficulty.MEDIUM;

    private Integer timeLimit = 60; // seconds where 0 = unlimited

    private Integer rounds = 0; // 0 = unlimited

    @Enumerated(EnumType.STRING)
    private List<Topic> topics = new ArrayList<>();

    private String customWordList; // probably as JSON 


    public LobbySettings(){ // Constructor
        this.difficulty = Difficulty.MEDIUM;
        this.timeLimit = 60;
        this.rounds = 0;
        this.topics = new ArrayList<>();
    }

    // Getters, Setters
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }

    public Integer getRounds() { return rounds; }
    public void setRound(Integer rounds) { this.rounds = rounds; }

    public List<Topic> getTopics() { return topics; }
    public void setTopics(List<Topic> topics) { this.topics = topics; }

    public String getCustomWordList() { return customWordList; }
    public void setCustomWordList(String customWordList) { this.customWordList = customWordList; }
}