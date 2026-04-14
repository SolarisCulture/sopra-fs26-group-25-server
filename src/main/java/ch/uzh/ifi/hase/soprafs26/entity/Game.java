package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;


@Entity
@Table(name = "game")
public class Game {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Used when the database automatically generates the primary key using auto-increment for each new record
	private Long id;

    @OneToOne(mappedBy = "game")
	private Lobby lobby;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "board_id")
    private Board board;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamColor currentTurn;

    private int currentRound;
    private int maxRounds;
    private int roundsPlayed;
    private int totalTime;

    private int redScore;
    private int blueScore;
    private int redTotal;
    private int blueTotal;

    private TeamColor winningTeam;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String currentHint;
    private Integer currentHintCount;
    private int remainingGuesses;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameHistory> gameHistories = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = GameStatus.ACTIVE;
        this.currentRound = 1;
        this.redScore = 0;
        this.blueScore = 0;
        this.redTotal = 9;
        this.blueTotal = 8;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lobby getLobby() { return lobby; }
    public void setLobby(Lobby lobby) { this.lobby = lobby; }

    public Board getBoard() { return board; }
    public void setBoard(Board board) { this.board = board; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public TeamColor getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(TeamColor currentTurn) { this.currentTurn = currentTurn; }

    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }

    public int getRoundsPlayed() { return roundsPlayed; }
    public void setRoundsPlayed(int roundsPlayed) { this.roundsPlayed = roundsPlayed; }

    public int getTotalTime() { return totalTime; }
    public void setTotalTime(int totalTime) { this.totalTime = totalTime; }

    public int getRedScore() { return redScore; }
    public void setRedScore(int redScore) { this.redScore = redScore; }

    public int getBlueScore() { return blueScore; }
    public void setBlueScore(int blueScore) { this.blueScore = blueScore; }

    public int getRedTotal() { return redTotal; }
    public void setRedTotal(int redTotal) { this.redTotal = redTotal; }

    public int getBlueTotal() { return blueTotal; }
    public void setBlueTotal(int blueTotal) { this.blueTotal = blueTotal; }

    public TeamColor getWinningTeam() { return winningTeam; }
    public void setWinningTeam(TeamColor winningTeam) { this.winningTeam = winningTeam; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getCurrentHint() { return currentHint; }
    public void setCurrentHint(String currentHint) { this.currentHint = currentHint; }

    public Integer getCurrentHintCount() { return currentHintCount; }
    public void setCurrentHintCount(Integer currentHintCount) { this.currentHintCount = currentHintCount; }

    public int getRemainingGuesses() { return remainingGuesses; }
    public void setRemainingGuesses(int remainingGuesses) { this.remainingGuesses = remainingGuesses; }

    public List<GameHistory> getGameHistories() { return gameHistories; }
    public void setGameHistories(List<GameHistory> gameHistories) { this.gameHistories = gameHistories; }
}
