package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
public class Guess extends GameEvent {
    @ManyToOne
    private WordCard wordCard;

    public WordCard getWordCard() { return wordCard; }
    public void setWordCard(WordCard wordCard) { this.wordCard = wordCard; }
}
