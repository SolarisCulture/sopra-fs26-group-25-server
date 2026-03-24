package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamColor team = TeamColor.UNASSIGNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.NONE;

    @Column(name = "is_host", nullable = false)
    private boolean isHost = false;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    // Constructors
    public Player() { // Required by JPA
        this.joinedAt = LocalDateTime.now();
        this.team = TeamColor.UNASSIGNED;
        this.role = Role.NONE;
        this.isHost = false;
    }

    public Player(String username){ 
        this();
        this.username = username;
    }

    // Getters, Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public TeamColor getTeam() { return team; }
    public void setTeam(TeamColor team) { this.team = team; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { this.isHost = host; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; } // Needed here since players could possibly "rejoin"


    // Helper methods
    public boolean isSpymaster() { return role == Role.SPYMASTER; }
    public boolean isSpy() { return role == Role.SPY; }
    public boolean isUnassigned() { return team == TeamColor.UNASSIGNED; }

}