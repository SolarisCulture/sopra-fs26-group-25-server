package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Guess extends GameEvent {

    @ManyToOne
    private WordCard wordCard;

    public Guess() {}

    public WordCard getWordCard() { return wordCard; }
    public void setWordCard(WordCard wordCard) { this.wordCard = wordCard; }
}
