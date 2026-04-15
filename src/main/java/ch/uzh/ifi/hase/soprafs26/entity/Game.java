package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;


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
    private TeamColor startingTeam;

    @OneToMany(mappedBy = "game")
    private List<Turn> turns;

    // to know which turn is currently active
    @OneToOne
    @JoinColumn(name = "current_turn_id")
    private Turn currentTurn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    //@Enumerated(EnumType.STRING)
    //@Column(nullable = false)
    //private TeamColor team;

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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = GameStatus.ACTIVE;
        this.currentRound = 1;
        this.redScore = 0;
        this.blueScore = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lobby getLobby() { return lobby; }
    public void setLobby(Lobby lobby) { this.lobby = lobby; }

    public Board getBoard() { return board; }
    public void setBoard(Board board) { this.board = board; }

    public TeamColor getStartingTeam() { return startingTeam; }
    public void setStartingTeam(TeamColor startingTeam) { this.startingTeam = startingTeam; }

    public List<Turn> getTurns() { return turns; }
    public void setTurns(List<Turn> turns) { this.turns = turns; }

    public Turn getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(Turn currentTurn) { this.currentTurn = currentTurn; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    //public TeamColor getTeam() { return team; }
    //public void setTeam(TeamColor team) { this.team = team; }

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
}
