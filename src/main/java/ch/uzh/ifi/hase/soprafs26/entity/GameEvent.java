package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.GameEventType;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    //@Column(nullable = false)
    //@Enumerated(EnumType.STRING)
    //private GameEventType type;

    @Column(nullable = false)
    private LocalDateTime timeStamp;

    private String description;

    public GameEvent() {
        this.timeStamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    //public GameEventType getType() { return type; }
    //public void setType(GameEventType type) { this.type = type; }

    public LocalDateTime getTimeStamp() { return timeStamp; }
    public void setTimeStamp(LocalDateTime timeStamp) { this.timeStamp = timeStamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}