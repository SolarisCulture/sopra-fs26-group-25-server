package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @MessageMapping("/lobby/{lobbyCode}/subscribe") // Receive client here if asked to subscribe => run this code
    @SendTo("/topic/lobbies/{lobbyCode}") // Client now subscribed to this lobby
    public LobbyEvent subscribeToLobby(@DestinationVariable String lobbyCode, LobbyEvent event){
        log.info("Client subscribed to lobby: {}", lobbyCode);
        return event;
    }
}
