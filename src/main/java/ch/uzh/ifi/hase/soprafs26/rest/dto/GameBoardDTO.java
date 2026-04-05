package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;

import java.util.List;

public class GameBoardDTO {

    private Long id;
    private GameStatus status;
    private TeamColor currentTurn;
    private int redScore;
    private int blueScore;
    private List<PlayerDTO> redTeam;
    private List<PlayerDTO> blueTeam;

    private List<CardDTO> cards;
    private List<CardType> keyCard;

    // Turn
    private TurnPhase currentPhase;
    private int guessesRemaining;
    private long remainingTimeSeconds;
    private String clueWord;
    private int clueCount;



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

    public List<PlayerDTO> getRedTeam() {
        return redTeam;
    }
    public void setRedTeam(List<PlayerDTO> redTeam) {
        this.redTeam = redTeam;
    }

    public List<PlayerDTO> getBlueTeam() {
        return blueTeam;
    }
    public void setBlueTeam(List<PlayerDTO> blueTeam) {
        this.blueTeam = blueTeam;
    }

    public List<CardDTO> getCards() { return cards; }
    public void setCards(List<CardDTO> cards) { this.cards = cards; }

    public List<CardType> getKeyCard() { return keyCard; }
    public void setKeyCard(List<CardType> keyCard) { this.keyCard = keyCard; }

    public TurnPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(TurnPhase currentPhase) { this.currentPhase = currentPhase; }

    public int getGuessesRemaining() { return guessesRemaining; }
    public void setGuessesRemaining(int guessesRemaining) { this.guessesRemaining = guessesRemaining; }

    public long getRemainingTimeSeconds() { return remainingTimeSeconds; }
    public void setRemainingTimeSeconds(long remainingTimeSeconds) { this.remainingTimeSeconds = remainingTimeSeconds; }

    public String getClueWord() { return clueWord; }
    public void setClueWord(String clueWord) { this.clueWord = clueWord; }

    public int getClueCount() { return clueCount; }
    public void setClueCount(int clueCount) {  this.clueCount = clueCount; }
}
