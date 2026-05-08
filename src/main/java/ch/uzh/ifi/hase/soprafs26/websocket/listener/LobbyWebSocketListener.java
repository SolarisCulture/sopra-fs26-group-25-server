package ch.uzh.ifi.hase.soprafs26.websocket.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import ch.uzh.ifi.hase.soprafs26.service.LobbyPresenceService;

@Component
public class LobbyWebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketListener.class);

    private final LobbyPresenceService lobbyPresenceService;

    public LobbyWebSocketListener(LobbyPresenceService lobbyPresenceService) {
        this.lobbyPresenceService = lobbyPresenceService;
    }

    // This will be called whenever a Websocket session closes
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {

        // Stores WebSocket session data
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getSessionId() != null) {
            log.info("WebSocket session {} disconnected", accessor.getSessionId());
            lobbyPresenceService.handleDisconnect(accessor.getSessionId());
        }
    }
}
