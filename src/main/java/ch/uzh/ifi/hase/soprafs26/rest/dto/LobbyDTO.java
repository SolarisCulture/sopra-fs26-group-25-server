package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;

public class LobbyDTO {
    private Long id;
    private String lobbyCode;
    private Long hostId;
    private List<PlayerDTO> players;
    private LobbySettingsDTO settings;
    private LocalDateTime createdAt;
    private LobbyStatus lobbyStatus;
    private String hostUsername;

    // Getters, Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id;}

    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public List<PlayerDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerDTO> players) { this.players = players; }

    public LobbySettingsDTO getSettings() { return settings; }
    public void setSettings(LobbySettingsDTO settings) { this.settings = settings; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LobbyStatus getLobbyStatus() { return lobbyStatus; }
    public void setLobbyStatus(LobbyStatus lobbyStatus) { this.lobbyStatus = lobbyStatus; }

    public String getHostUsername() { return hostUsername; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }
}
