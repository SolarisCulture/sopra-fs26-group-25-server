package ch.uzh.ifi.hase.soprafs26.websocket.handler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.service.ChatService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;

@ExtendWith(MockitoExtension.class)
class GameWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private TurnService turnService;

    @InjectMocks
    private GameWebSocketHandler gameWebSocketHandler;

    @Mock
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes Mocks
        gameWebSocketHandler = new GameWebSocketHandler(messagingTemplate, turnService, chatService);
    }

    // ==================== handleClue tests ====================

    @Test
    public void handleClue_callsSubmitClue() {
        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("animal");
        clueDTO.setCount(3);

        gameWebSocketHandler.handleClue("ABC123", clueDTO);

        verify(turnService).submitClue("ABC123", clueDTO);
    }

    // ==================== handleGuess tests ====================

    @Test
    public void handleGuess_callsSubmitGuess() {
        GuessDTO guessDTO = new GuessDTO("APPLE");

        gameWebSocketHandler.handleGuess("ABC123", guessDTO);

        verify(turnService).submitGuess("ABC123", guessDTO);
    }

    // ==================== handleEndTurn tests ====================

    @Test
    public void handleEndTurn_callsEndTurn() {
        gameWebSocketHandler.handleEndTurn("ABC123");

        verify(turnService).endTurn("ABC123");
    }

    // ==================== broadcast tests ====================

    @Test
    public void broadcastGameState_sendsToBothTopics() {
        GameBoardDTO spymasterView = new GameBoardDTO();
        GameBoardDTO spyView = new GameBoardDTO();

        gameWebSocketHandler.broadcastGameState("ABC123", EventType.CLUE_GIVEN, spymasterView, spyView);

        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/spymaster"), any(GameEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/spy"), any(GameEvent.class));
    }

    @Test
    public void broadcastGameStarted_usesBroadcastGameState() {
        GameBoardDTO spymasterView = new GameBoardDTO();
        GameBoardDTO spyView = new GameBoardDTO();

        gameWebSocketHandler.broadcastGameStarted("ABC123", spymasterView, spyView);

        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/spymaster"), any(GameEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/spy"), any(GameEvent.class));
    }

    @Test
    void broadcastGameRestarting_shouldSendToCorrectTopics() {
        // Given
        String lobbyCode = "ABC123";
        GameBoardDTO spymasterBoard = new GameBoardDTO();
        GameBoardDTO operativeBoard = new GameBoardDTO();

        // When
        gameWebSocketHandler.broadcastGameRestarting(lobbyCode, spymasterBoard, operativeBoard);

        // Then
        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + lobbyCode + "/spymaster"), eventCaptor.capture());

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + lobbyCode + "/spy"), eventCaptor.capture());

        assertEquals(2, eventCaptor.getAllValues().size());

        GameEvent spymasterEvent = eventCaptor.getAllValues().get(0);
        GameEvent spyEvent = eventCaptor.getAllValues().get(1);

        assertEquals(lobbyCode, spymasterEvent.getLobbyCode());
        assertEquals(lobbyCode, spyEvent.getLobbyCode());
    }

    @Test
    void broadcastReturningToLobby_shouldSendToCorrectTopic() {
        // Given
        String lobbyCode = "ABC123";

        // When
        gameWebSocketHandler.broadcastReturningToLobby(lobbyCode);

        // Then
        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + lobbyCode + "/spymaster"),
                eventCaptor.capture()
        );
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + lobbyCode + "/spy"),
                eventCaptor.capture()
        );

        GameEvent event = eventCaptor.getValue();

        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals("ReturningToLobby", event.getType());
    }

    @Test
    void broadcastTimerUpdate_shouldSendToCorrectTopic() {
        // Given
        String lobbyCode = "ABC123";
        Long timer = 10L;

        // When
        gameWebSocketHandler.broadcastTimer(lobbyCode, timer);

        // Then
        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + lobbyCode + "/spymaster"),eventCaptor.capture());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/game/" + lobbyCode + "/spy"),eventCaptor.capture());

        GameEvent event = eventCaptor.getValue();

        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals("TIMER_UPDATE", event.getType());
        assertEquals(10L, event.getTimer());
    }

    // ==================== chat tests ====================
    @Test
    void handleChat_callsSaveChatMessageAndBroadcasts() {
        // Given
        String lobbyCode = "ABC123";
        ChatMessageDTO incomingMsg = new ChatMessageDTO();
        incomingMsg.setChannel("TEAM");
        incomingMsg.setTeam(TeamColor.RED);
        incomingMsg.setContent("Hello team");
        incomingMsg.setSenderName("Player");

        ChatMessageDTO savedMsg = new ChatMessageDTO();
        savedMsg.setChannel("TEAM");
        savedMsg.setTeam(TeamColor.RED);
        savedMsg.setContent("Hello team");
        savedMsg.setSenderName("Player");
        savedMsg.setTimestamp(java.time.LocalDateTime.now());

        when(chatService.saveChatMessage(eq(lobbyCode), any(ChatMessageDTO.class)))
            .thenReturn(savedMsg);

        // When
        gameWebSocketHandler.handleChat(lobbyCode, incomingMsg);

        // Then
        verify(chatService).saveChatMessage(eq(lobbyCode), any(ChatMessageDTO.class));
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(GameEvent.class));
    }

    @Test
    void requestChatHistory_callsGetHistoryAndSendsToUser() {
        // Given
        String lobbyCode = "ABC123";
        String teamStr = "RED";
        boolean isSpymaster = false;
        Map<String, Object> payload = new HashMap<>();
        payload.put("team", teamStr);
        payload.put("isSpymaster", isSpymaster);

        SimpMessageHeaderAccessor headerAccessor = mock(SimpMessageHeaderAccessor.class);
        when(headerAccessor.getSessionId()).thenReturn("session123");

        List<ChatMessageDTO> history = List.of(new ChatMessageDTO(), new ChatMessageDTO());
        when(chatService.getHistory(eq(lobbyCode), eq(TeamColor.RED), eq(false)))
            .thenReturn(history);

        // When
        gameWebSocketHandler.requestChatHistory(lobbyCode, payload, headerAccessor);

        // Then
        verify(chatService).getHistory(lobbyCode, TeamColor.RED, false);
        ArgumentCaptor<ChatMessageDTO> captor = ArgumentCaptor.forClass(ChatMessageDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq("session123"),
            eq("/queue/chat-history"),
            captor.capture()
        );
        ChatMessageDTO response = captor.getValue();
        assertEquals("CHAT_HISTORY", response.getType());
        assertSame(history, response.getHistory());
    }

    @Test
    void broadcastChatMessage_sendsGameEventToBothTopics() {
        // given
        String lobbyCode = "ABC123";
        ChatMessageDTO chatMsg = new ChatMessageDTO();
        chatMsg.setChannel("TEAM");
        chatMsg.setTeam(TeamColor.RED);
        chatMsg.setSenderName("Alice");
        chatMsg.setContent("Hello");
        chatMsg.setTimestamp(java.time.LocalDateTime.now());

        // when
        gameWebSocketHandler.broadcastChatMessage(lobbyCode, chatMsg);

        // then
        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + lobbyCode + "/spymaster"), captor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + lobbyCode + "/spy"), captor.capture());

        List<GameEvent> events = captor.getAllValues();
        assertEquals(2, events.size());
        GameEvent event = events.get(0); 

        assertEquals("CHAT_MESSAGE", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        Map<String, Object> data = event.getData();
        assertNotNull(data);
        assertEquals("TEAM", data.get("channel"));
        assertEquals("RED", data.get("team"));
        assertEquals("Alice", data.get("senderName"));
        assertEquals("Hello", data.get("content"));
        // Second event should be identical
        GameEvent second = events.get(1);
        assertEquals(event.getType(), second.getType());
        assertEquals(event.getLobbyCode(), second.getLobbyCode());
        assertEquals(event.getData(), second.getData());
    }
}