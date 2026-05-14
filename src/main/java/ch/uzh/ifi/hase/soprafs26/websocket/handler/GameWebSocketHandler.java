package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SubscribeDTO;
import ch.uzh.ifi.hase.soprafs26.service.ChatService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyPresenceService;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;
import ch.uzh.ifi.hase.soprafs26.websocket.event.LobbyEvent;


@Controller
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class); // For debugging
    private final SimpMessagingTemplate messagingTemplate; // The tool that sends messages over WebSocket
    private final TurnService turnService;
    private final ChatService chatService;
    private final LobbyPresenceService lobbyPresenceService;

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate, @Lazy TurnService turnService, ChatService chatService, LobbyPresenceService lobbyPresenceService) {
        this.messagingTemplate = messagingTemplate;
        this.turnService = turnService;
        this.chatService = chatService;
        this.lobbyPresenceService = lobbyPresenceService;
    }
    // ==================== Incoming messages ====================

    @MessageMapping("/{lobbyCode}/clue")
    public void handleClue(@DestinationVariable String lobbyCode, ClueDTO clueDTO) {
        log.info("Submit Clue log");
        turnService.submitClue(lobbyCode, clueDTO);
    }

    @MessageMapping("/{lobbyCode}/guess")
    public void handleGuess(@DestinationVariable String lobbyCode, GuessDTO guessDTO) {
        log.info("Submit Clue log");
        turnService.submitGuess(lobbyCode, guessDTO);
    }

    @MessageMapping("/{lobbyCode}/turn-change")
    public void handleEndTurn(@DestinationVariable String lobbyCode) {
        turnService.endTurn(lobbyCode);
    }

    @MessageMapping("/{lobbyCode}/chat")
    public void handleChat(@DestinationVariable String lobbyCode, ChatMessageDTO chatMsg) {
        log.info("Chat message in lobby {}: {}", lobbyCode, chatMsg.getContent());
        // Save and get the message
        ChatMessageDTO savedMsg = chatService.saveChatMessage(lobbyCode, chatMsg);
        broadcastChatMessage(lobbyCode, savedMsg);
    }
      
    @MessageMapping("/{lobbyCode}/game-subscribe")
    public void handleGameSubscribe(@DestinationVariable String lobbyCode, @Payload SubscribeDTO payload, StompHeaderAccessor accessor) {
        Long playerId = null;
        if (payload.getData() != null) {
            playerId = payload.getData().getId();
        }

        if (playerId != null) {
            if (accessor.getSessionAttributes() == null) {
                accessor.setSessionAttributes(new HashMap<>());
            }
            accessor.getSessionAttributes().put("lobbyCode", lobbyCode);
            accessor.getSessionAttributes().put("playerId", playerId);
            lobbyPresenceService.registerGameConnection(accessor.getSessionId(), lobbyCode, playerId);
            log.info("Stored game playerId {} for session {}", playerId, accessor.getSessionId());
        } else {
            log.warn("No playerId in game subscription payload");
        }
    }

    @MessageMapping("/{lobbyCode}/clue-report")
    public void handleClueReport(@DestinationVariable String lobbyCode) {
        log.info("Broadcasting ClueReported for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", new GameEvent("ClueReported", lobbyCode));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", new GameEvent("ClueReported", lobbyCode));
    }

    @MessageMapping("/{lobbyCode}/clue-approved")
    public void handleClueApproved(@DestinationVariable String lobbyCode) {
        log.info("Broadcasting ClueApproved for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", new GameEvent("ClueApproved", lobbyCode));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", new GameEvent("ClueApproved", lobbyCode));
    }

    @MessageMapping("/{lobbyCode}/clue-ruled-invalid")
    public void handleClueRuledInvalid(@DestinationVariable String lobbyCode) {
        log.info("Broadcasting ClueRuledInvalid for lobby: {}", lobbyCode);
        turnService.markClueRuledInvalid(lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", new GameEvent("ClueRuledInvalid", lobbyCode));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", new GameEvent("ClueRuledInvalid", lobbyCode));
    }

    @MessageMapping("/{lobbyCode}/reported-guess")
    public void handleReportedGuess(@DestinationVariable String lobbyCode, GuessDTO guessDTO) {
        turnService.submitReportedGuess(lobbyCode, guessDTO);
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
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", GameEvent.returningToLobby(lobbyCode));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", GameEvent.returningToLobby(lobbyCode));
    }

    public void broadcastReturningToLobbyAfterDisconnect(String lobbyCode) {
        log.info("Broadcasting RETURNING_TO_LOBBY_AFTER_DISCONNECT for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", GameEvent.returningToLobbyAfterDisconnect(lobbyCode));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", GameEvent.returningToLobbyAfterDisconnect(lobbyCode));
    }

    public void broadcastTimer(String lobbyCode, Long timer) {
        log.info("Broadcasting TIMER_UPDATE for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", GameEvent.timerUpdate(lobbyCode, timer));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", GameEvent.timerUpdate(lobbyCode, timer));
    }

    public void broadcastChatMessage(String lobbyCode, ChatMessageDTO chatMsg) {
        log.info("Broadcasting CHAT_MESSAGE for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", chatMsg);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", chatMsg);
    }
      
    public void broadcastPlayersUpdated(String lobbyCode) {
        log.info("Broadcasting PlayersUpdated for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", new GameEvent("PlayersUpdated", lobbyCode));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", new GameEvent("PlayersUpdated", lobbyCode));
    }
}
