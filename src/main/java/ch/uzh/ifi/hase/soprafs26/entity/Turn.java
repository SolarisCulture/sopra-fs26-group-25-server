package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "turn")
public class Turn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Used when the database automatically generates the primary key using auto-increment for each new record
    private Long id;

    @ManyToOne
    private Game game;

    @Enumerated(EnumType.STRING)
    private TeamColor currentTeamColor;

    @Enumerated(EnumType.STRING)
    private TurnPhase phase;
    private int guessesRemaining;
    private LocalDateTime startTime;

    @OneToOne
    private Clue clue;

    @OneToMany
    private List<Guess> guesses;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public TeamColor getCurrentTeamColor() { return currentTeamColor; }
    public void setCurrentTeamColor(TeamColor currentTeamColor) { this.currentTeamColor = currentTeamColor; }

    public TurnPhase getPhase() { return phase; }
    public void setPhase(TurnPhase phase) { this.phase = phase; }

    public int getGuessesRemaining() { return guessesRemaining; }
    public void setGuessesRemaining(int guessesRemaining) { this.guessesRemaining = guessesRemaining; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public Clue getClue() { return clue; }
    public void setClue(Clue clue) { this.clue = clue; }

    public List<Guess> getGuesses() { return guesses; }
    public void setGuesses(List<Guess> guesses) { this.guesses = guesses; }
}