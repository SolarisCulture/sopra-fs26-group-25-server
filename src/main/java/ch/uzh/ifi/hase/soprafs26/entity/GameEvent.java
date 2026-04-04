package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.GameEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class GameEvent {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private GameEventType type;

    private LocalDateTime timeStamp;

    @ManyToOne
    private Player player;

    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GameEventType getType() { return type; }
    public void setType(GameEventType type) { this.type = type; }

    public LocalDateTime getTimeStamp() { return timeStamp; }
    public void setTimeStamp(LocalDateTime timeStamp) { this.timeStamp = timeStamp; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}