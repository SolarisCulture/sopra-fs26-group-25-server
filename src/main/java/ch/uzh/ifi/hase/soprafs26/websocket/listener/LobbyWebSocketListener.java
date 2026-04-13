package ch.uzh.ifi.hase.soprafs26.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import ch.uzh.ifi.hase.soprafs26.service.LobbyService;

@Component
public class LobbyWebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketListener.class);

    private final LobbyService lobbyService;

    public LobbyWebSocketListener(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    // This will be called whenever a Websocket session closes
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {

        // Stores WebSocket session data
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // Gets the lobby code (If lobbycode exists --> get it, else --> null)
        String lobbyCode = accessor.getSessionAttributes() != null
                ? (String) accessor.getSessionAttributes().get("lobbyCode")
                : null;

        // Same for playerId
        Long playerId = accessor.getSessionAttributes() != null
                ? (Long) accessor.getSessionAttributes().get("playerId")
                : null;

        if (lobbyCode != null && playerId != null) {
            log.info("Player {} disconnected from lobby {}", playerId, lobbyCode);
            lobbyService.leaveLobby(lobbyCode, playerId);
        }
    }
}