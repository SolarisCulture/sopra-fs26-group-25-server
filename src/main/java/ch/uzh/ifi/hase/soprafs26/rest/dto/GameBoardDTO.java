package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;

import java.util.List;

public class GameBoardDTO {

    private Long id;
    private GameStatus status;
    private TeamColor currentTurn;
    private int redScore;
    private int blueScore;
    private List<CardDTO> cards;
    private List<CardType> keyCard;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public TeamColor getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(TeamColor currentTurn) { this.currentTurn = currentTurn; }

    public int getRedScore() { return redScore; }
    public void setRedScore(int redScore) { this.redScore = redScore; }

    public int getBlueScore() { return blueScore; }
    public void setBlueScore(int blueScore) { this.blueScore = blueScore; }

    public List<CardDTO> getCards() { return cards; }
    public void setCards(List<CardDTO> cards) { this.cards = cards; }

    public List<CardType> getKeyCard() { return keyCard; }
    public void setKeyCard(List<CardType> keyCard) { this.keyCard = keyCard; }
}
