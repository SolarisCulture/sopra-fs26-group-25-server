package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;

public class CardDTO {
    private String word;
    private CardType cardType;
    private boolean revealed;

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = cardType; }

    public boolean isRevealed() { return revealed; }
    public void setRevealed(boolean revealed) { this.revealed = revealed; }
}
