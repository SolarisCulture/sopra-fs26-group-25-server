package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class GameHistory {
    @Id @GeneratedValue private Long id;
    @ManyToOne private Game game;
    private TeamColor team;
    private String hint;
    private int hintCount;
    private LocalDateTime timestamp;

    public GameHistory() {}
    public GameHistory(Game game, TeamColor team, String hint, int hintCount) {
        this.game = game;
        this.team = team;
        this.hint = hint;
        this.hintCount = hintCount;
        this.timestamp = LocalDateTime.now();
    }

    // Getters, Setters
    public long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public TeamColor getTeam() { return team; }
    public void setTeam(TeamColor team) { this.team = team; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public int getHintCount() { return hintCount; }
    public void setHintCount(int hintCount) { this.hintCount = hintCount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
