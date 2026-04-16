package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs26.websocket.event.LobbyEvent;


// Handles all Websocket communication for lobby events
@Controller
public class LobbyWebSocketHandler {
    
    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketHandler.class); // For debugging and monitoring => should print to server console

    private final SimpMessagingTemplate messagingTemplate; // The tool that sends messages over WebSocket

    // Constructor injection because SonarQube complained about @Autowired => easier to mock and immutability possible with "final"
    public LobbyWebSocketHandler(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastLobbyCreated(String lobbyCode, Object lobbyData){
        LobbyEvent event = LobbyEvent.lobbyCreated(lobbyCode, lobbyData);
        log.info("Broadcasting LOBBY_CREATED for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/lobbies/", event);  // Send to clients subscribed to "/topic/lobbies/"
    }

    public void broadcastPlayerJoined(String lobbyCode, Object playerData){
        LobbyEvent event = LobbyEvent.playerJoined(lobbyCode, playerData);
        log.info("Broadcasting PLAYER_JOINED for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/lobbies/" + lobbyCode, event); // ONLY send to clients subscribed to this specific lobby
    }

    public void broadcastPlayerLeft(String lobbyCode, Object playerData){
        LobbyEvent event = LobbyEvent.playerLeft(lobbyCode, playerData);
        log.info("Broadcasting PLAYER_LEFT for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/lobbies/" + lobbyCode, event);
    }

    public void broadcastHostChanged(String lobbyCode, Object newHostData){
        LobbyEvent event = LobbyEvent.hostChanged(lobbyCode, newHostData);
        log.info("Broadcasting HOST_CHANGED for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/lobbies/" + lobbyCode, event);
    }

    public void broadcastTeamUpdated(String lobbyCode, Object playerData) {
        LobbyEvent event = LobbyEvent.teamUpdated(lobbyCode, playerData);
        log.info("Broadcasting TEAM_UPDATED for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/lobbies/" + lobbyCode, event);
    }

    public void broadcastRoleUpdated(String lobbyCode, Object playerData) {
        LobbyEvent event = LobbyEvent.roleUpdated(lobbyCode, playerData);
        log.info("Broadcasting ROLE_UPDATED for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/lobbies/" + lobbyCode, event);
    }

    @MessageMapping("/lobby/{lobbyCode}/subscribe") // Receive client here if asked to subscribe => run this code
    @SendTo("/topic/lobbies/{lobbyCode}") // Client now subscribed to this lobby
    public LobbyEvent subscribeToLobby(@DestinationVariable String lobbyCode, @Payload Map<String, Object> payload, StompHeaderAccessor accessor){
        log.info("Client subscribed to lobby: {}", lobbyCode);

        // TODO CREATE SPECIFIC PAYLOAD DTO FOR SUBSCRIPTIONS
        Long playerId = null;
        Object dataObj = payload.get("data");
        if(dataObj instanceof Map){
            Map<String, Object> data = (Map<String, Object>) dataObj;
            Object idObj = data.get("id");
            if(idObj instanceof Number) {
                playerId = ((Number) idObj).longValue();
            }
        }

        if(playerId != null){
            accessor.getSessionAttributes().put("playerId", playerId);
            log.info("Stored playerId {} for session {}", playerId, accessor.getSessionId());
        } else {
            log.warn("no playerId in subscription payload");
        }
        return new LobbyEvent("SUBSCRIBE", lobbyCode, null);
    }
}
