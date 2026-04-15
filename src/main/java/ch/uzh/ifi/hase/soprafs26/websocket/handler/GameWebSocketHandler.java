package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs26.websocket.event.LobbyEvent;


@Controller
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class); // For debugging

    private final SimpMessagingTemplate messagingTemplate; // The tool that sends messages over WebSocket

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastGameStarted(String lobbyCode, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        broadcastGameState(lobbyCode, EventType.GAME_STARTED, spymasterBoard, operativeBoard);
    }

    public void broadcastGameState(String lobbyCode, EventType eventTypeE, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        log.info("Broadcasting {} for lobby: {}", eventTypeE, lobbyCode);

        String eventType = eventTypeE.toString();

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spymaster",
                new GameEvent(eventType, lobbyCode, spymasterBoard)
        );

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spy",
                new GameEvent(eventType, lobbyCode, operativeBoard)
        );
    }

}
