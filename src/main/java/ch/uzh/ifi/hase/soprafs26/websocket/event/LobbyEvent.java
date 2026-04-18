package ch.uzh.ifi.hase.soprafs26.websocket.event;

import java.time.Instant;

public class LobbyEvent {

    /*
    Possible values: => Allows client to handle different event types differently
    - LOBBY_CREATED -> A new lobby was created
    - PLAYER_JOINED -> A player joined a lobby
    - PLAYER_LEFT -> A player left a lobby
    - HOST_CHANGED -> Host role was transferred to another player
    */
    
    private String type;
    private String lobbyCode;
    private Object data;
    private Instant timestamp;

    public LobbyEvent() {
        this.timestamp = Instant.now();
    }

    public LobbyEvent(String type, String lobbyCode, Object data){
        this.type = type;
        this.lobbyCode = lobbyCode;
        this.data = data;
        this.timestamp = Instant.now();
    }

    // Getters
    public String getType() { return type; }
    public String getLobbyCode() { return lobbyCode; }
    public Object getData() { return data; }
    public Instant getTimestamp() { return timestamp; }
    
    // Setters
    public void setType(String type) { this.type = type; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }
    public void setData(Object data) { this.data = data; } 
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }


    // Factory methods for convenience
    public static LobbyEvent lobbyCreated(String lobbyCode, Object lobbyData){
        return new LobbyEvent("LOBBY_CREATED", lobbyCode, lobbyData);
    }

    public static LobbyEvent playerJoined(String lobbyCode, Object playerData){
        return new LobbyEvent("PLAYER_JOINED", lobbyCode, playerData);
    }

    public static LobbyEvent playerLeft(String lobbyCode, Object playerData){
        return new LobbyEvent("PLAYER_LEFT", lobbyCode, playerData);
    }

    public static LobbyEvent hostChanged(String lobbyCode, Object newHostData){
        return new LobbyEvent("HOST_CHANGED", lobbyCode, newHostData);
    }

    public static LobbyEvent teamUpdated(String lobbyCode, Object playerData) {
        return new LobbyEvent("TEAM_UPDATED", lobbyCode, playerData);
    }

    public static LobbyEvent roleUpdated(String lobbyCode, Object playerData) {
        return new LobbyEvent("ROLE_UPDATED", lobbyCode, playerData);
    }

    public static LobbyEvent statusUpdated(String lobbyCode, Object lobbyData) {
        return new LobbyEvent("STATUS_UPDATED", lobbyCode, lobbyData);
    }
}