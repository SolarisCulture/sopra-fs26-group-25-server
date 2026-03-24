package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobbies")
public class Lobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrementing based on SQL
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String lobbyCode; 

    @Column(nullable = false)
    private Long hostId;

    /* 
    @OneToMany => One lobby can have many players
    cascade => saving/deleting lobby also saves/delets players
    orphanRemoval => removes players from DB when removed from list
    */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "lobby_id") // Column link between players table and lobby
    private List<Player> playerList = new ArrayList<>();

    @Embedded
    private LobbySettings settings;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LobbyStatus lobbyStatus;


    public Lobby() { // Constructor
        this.createdAt = LocalDateTime.now();
        this.lobbyStatus = LobbyStatus.WAITING;
        this.settings = new LobbySettings();
    }

    // Getters, Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode;}

    public Long getHostId() { return hostId;}
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public List<Player> getPlayerList() { return playerList; }
    public void setPlayerList(List<Player> playerList) { this.playerList = playerList; }

    public LobbySettings getSettings() { return settings; }
    public void setSettings(LobbySettings settings) { this.settings = settings; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // Maybe use @PrePersist for this

    public LobbyStatus getLobbyStatus() { return lobbyStatus; }
    public void setLobbyStatus(LobbyStatus lobbyStatus) { this.lobbyStatus = lobbyStatus; }

    // Helper methods
    public void addPlayer(Player player){
        this.playerList.add(player);
    } 

    public void removePlayer(Player player){
        this.playerList.remove(player);
    }

    public void removePlayerById(Long playerId){
        this.playerList.removeIf(player -> player.getId().equals(playerId));
    }

    public Player getPlayerById(Long playerId){
        return PlayerList.stream()
            .filter(player -> player.getId().equals(playerId))
            .findFirst() // Wrapper to create <Optional> object
            .orElse(null); // HAS to unwrap <Optional> to work
    }

    public boolean isUsernameTaken(String username){
        return playerList.stream().anyMatch(player -> player.getUsername().equalsIgnorecase(username));
    }
    
}
