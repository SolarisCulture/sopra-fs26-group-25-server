package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.GameEventType;
import jakarta.persistence.*;

@Entity
@Table(name = "clue")
public class Clue extends GameEvent {

    @Column(nullable = false)
    private String word;

    @Column(name = "clue_count", nullable = false)
    private Integer count;

    public Clue() {}


    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public int getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

}
