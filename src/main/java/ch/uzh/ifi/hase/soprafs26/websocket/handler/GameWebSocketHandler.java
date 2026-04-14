package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
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

    public void broadcastClueGiven(String lobbyCode, String hint, int count, TeamColor team, Long spymasterId) {
        log.info("Broadcasting CLUE_GIVEN for lobby: {}", lobbyCode);
        GameEvent event = GameEvent.clueGiven(lobbyCode, hint, count, team, spymasterId);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", event);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", event);
    }
}
