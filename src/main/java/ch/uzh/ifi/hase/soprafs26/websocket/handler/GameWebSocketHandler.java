package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.service.ChatService;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;
import ch.uzh.ifi.hase.soprafs26.websocket.event.LobbyEvent;


@Controller
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class); // For debugging
    private final SimpMessagingTemplate messagingTemplate; // The tool that sends messages over WebSocket
    private final TurnService turnService;
    private final ChatService chatService;

    public GameWebSocketHandler(SimpMessagingTemplate messagingTemplate, @Lazy TurnService turnService, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.turnService = turnService;
        this.chatService = chatService;
    }
    // ==================== Incoming messages ====================

    @MessageMapping("/{lobbyCode}/clue")
    //@SendTo("/topic/game/{lobbyCode}/spymaster")
    public void handleClue(@DestinationVariable String lobbyCode, ClueDTO clueDTO) {
        log.info("Submit Clue log");
        turnService.submitClue(lobbyCode, clueDTO);
    }

    @MessageMapping("/{lobbyCode}/guess")
    //@SendTo("/topic/game/{lobbyCode}/spymaster")
    public void handleGuess(@DestinationVariable String lobbyCode, GuessDTO guessDTO) {
        log.info("Submit Clue log");
        turnService.submitGuess(lobbyCode, guessDTO);
    }

    @MessageMapping("/{lobbyCode}/turn-change")
    //@SendTo("/topic/game/{lobbyCode}/spymaster")
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

    @MessageMapping("/{lobbyCode}/chat/history") // For requesting chat history due to disconnects
    public void requestChatHistory(@DestinationVariable String lobbyCode,
                                @Payload Map<String, Object> payload,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            String teamStr = (String) payload.get("team");
            boolean isSpymaster = (boolean) payload.get("isSpymaster");
            TeamColor team = TeamColor.valueOf(teamStr.toUpperCase());

            List<ChatMessageDTO> history = chatService.getHistory(lobbyCode, team, isSpymaster);
            ChatMessageDTO response = new ChatMessageDTO();
            response.setType("CHAT_HISTORY");
            response.setHistory(history);

            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/chat-history", response);
        } catch (Exception e) {
            log.error("Failed to get chat history", e);
        }
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

    public void broadcastTimer(String lobbyCode, Long timer) {
        log.info("Broadcasting TIMER_UPDATE for lobby: {}", lobbyCode);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", GameEvent.timerUpdate(lobbyCode, timer));
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", GameEvent.timerUpdate(lobbyCode, timer));
    }

    public void broadcastChatMessage(String lobbyCode, ChatMessageDTO chatMsg) {
        log.info("Broadcasting CHAT_MESSAGE for lobby: {}", lobbyCode);
        GameEvent chatEvent = GameEvent.chatMessage(lobbyCode, chatMsg);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spymaster", chatEvent);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyCode + "/spy", chatEvent);
    }
}
