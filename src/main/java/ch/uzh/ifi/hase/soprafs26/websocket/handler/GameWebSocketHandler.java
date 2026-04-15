package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;



@Controller
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class); // For debugging

    private final SimpMessagingTemplate messagingTemplate; // The tool that sends messages over WebSocket

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastGameStarted(String lobbyCode, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        log.info("Broadcasting GAME_STARTED for lobby: {}", lobbyCode);

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spymaster",
                new GameEvent("GAME_STARTED", lobbyCode, spymasterBoard)
        );

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spy",
                new GameEvent("GAME_STARTED", lobbyCode, operativeBoard)
        );
    }

    public void broadcastGameRestarting(String lobbyCode, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        log.info("Broadcasting GAME_RESTARTING for lobby: {}", lobbyCode);

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spymaster",
                GameEvent.gameRestarting(lobbyCode, spymasterBoard)
    public void broadcastGameState(String lobbyCode, EventType eventTypeE, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        log.info("Broadcasting CLUE_GIVEN for lobby: {}", lobbyCode);
        String eventType = eventTypeE.toString();

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spymaster",
                new GameEvent(eventType, lobbyCode, spymasterBoard)
        );

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spy",
                GameEvent.gameRestarting(lobbyCode, operativeBoard)
        );
    }

    public void broadcastReturningToLobby(String lobbyCode) {
        log.info("Broadcasting RETURNING_TO_LOBBY for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode, GameEvent.returningToLobby(lobbyCode));
    }

    // Do we need to add a subscribe to /spy , /spymaster here or how are they subscribed?
}
