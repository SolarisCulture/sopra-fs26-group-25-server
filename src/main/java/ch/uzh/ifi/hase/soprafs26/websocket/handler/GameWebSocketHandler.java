package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;
import ch.uzh.ifi.hase.soprafs26.websocket.event.LobbyEvent;


@Controller
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class); // For debugging
    private final SimpMessagingTemplate messagingTemplate; // The tool that sends messages over WebSocket
    private final TurnService turnService;

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate, @Lazy TurnService turnService) {
        this.messagingTemplate = messagingTemplate;
        this.turnService = turnService;
    }
    // ==================== Incoming messages ====================

    @MessageMapping("/{lobbyCode}/clue")
    //@SendTo("/topic/game/{lobbyCode}/spymaster")
    public void handleClue(@DestinationVariable String lobbyCode, ClueDTO clueDTO) {
        System.out.println("Controller received clue");
        log.info("Submit Clue log");
        turnService.submitClue(lobbyCode, clueDTO);
    }

    @MessageMapping("/{lobbyCode}/guess")
    //@SendTo("/topic/game/{lobbyCode}/spymaster")
    public void handleGuess(@DestinationVariable String lobbyCode, GuessDTO guessDTO) {
        System.out.println("Controller received guess");
        log.info("Submit Clue log");
        turnService.submitGuess(lobbyCode, guessDTO);
    }

    @MessageMapping("/{lobbyCode}/turn-change")
    //@SendTo("/topic/game/{lobbyCode}/spymaster")
    public void handleEndTurn(@DestinationVariable String lobbyCode) {
        turnService.endTurn(lobbyCode);
    }


    // ==================== Broadcasting ====================
    public void broadcastGameStarted(String lobbyCode, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        broadcastGameState(lobbyCode, EventType.GAME_STARTED, spymasterBoard, operativeBoard);
    }

    public void broadcastGameRestarting(String lobbyCode, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        log.info("Broadcasting GAME_RESTARTING for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spymaster",
                GameEvent.gameRestarting(lobbyCode, spymasterBoard)
        );
        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spy",
                GameEvent.gameRestarting(lobbyCode, operativeBoard)
        );
    }

    public void broadcastGameState(String lobbyCode, EventType eventTypeE, GameBoardDTO spymasterBoard, GameBoardDTO operativeBoard) {
        log.info("Broadcasting {} for lobby: {}", eventTypeE, lobbyCode);

        String eventType = eventTypeE.toString();

        messagingTemplate.convertAndSend(
            "/topic/lobbies/" + lobbyCode,
            LobbyEvent.statusUpdated(lobbyCode, "IN_PROGRESS")
        );

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spymaster",
                new GameEvent(eventType, lobbyCode, spymasterBoard)
        );

        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyCode + "/spy",
                new GameEvent(eventType, lobbyCode, operativeBoard)
        );
    }

    public void broadcastReturningToLobby(String lobbyCode) {
        log.info("Broadcasting RETURNING_TO_LOBBY for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode, GameEvent.returningToLobby(lobbyCode));
    }

}
