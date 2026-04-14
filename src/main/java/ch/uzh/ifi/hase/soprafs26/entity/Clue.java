package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

@Entity
public class Clue extends GameEvent {
    private String word;
    private int count;

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

}
