package ch.uzh.ifi.hase.soprafs26.websocket.handler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PauseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SubscribeDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyPresenceService;
import ch.uzh.ifi.hase.soprafs26.service.TimerService;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
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

    @Mock
    private LobbyPresenceService lobbyPresenceService;

    @Mock
    private TimerService timerService;

    @InjectMocks
    private GameWebSocketHandler gameWebSocketHandler;

    @Mock
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes Mocks
        gameWebSocketHandler = new GameWebSocketHandler(messagingTemplate, turnService, chatService, lobbyPresenceService, timerService);
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

    @Test
    public void handleReportedGuess_callsSubmitReportedGuess() {
        GuessDTO guessDTO = new GuessDTO("APPLE");

        gameWebSocketHandler.handleReportedGuess("ABC123", guessDTO);

        verify(turnService).submitReportedGuess("ABC123", guessDTO);
    }

    @Test
    public void handleClueReport_marksClueReported() {
        gameWebSocketHandler.handleClueReport("ABC123");

        verify(turnService).reportClue("ABC123");
    }

    @Test
    public void handleClueApproved_marksReportedClueApproved() {
        gameWebSocketHandler.handleClueApproved("ABC123");

        verify(turnService).approveReportedClue("ABC123");
    }

    // ==================== handleEndTurn tests ====================

    @Test
    public void handleEndTurn_callsEndTurn() {
        gameWebSocketHandler.handleEndTurn("ABC123");

        verify(turnService).endTurn("ABC123");
    }

    @Test
    public void handlePause_callsPauseTimer() {
        PauseDTO pauseDTO = new PauseDTO();
        pauseDTO.setPaused(true);

        gameWebSocketHandler.handlePause("ABC123", pauseDTO);

        verify(timerService).pauseTimer("ABC123", true);
    }

    @Test
    public void handleGameSubscribe_registersGamePresence() {
        SubscribeDTO payload = new SubscribeDTO();
        SubscribeDTO.Data data = new SubscribeDTO.Data();
        data.setId(1L);
        payload.setData(data);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("sessionId");

        gameWebSocketHandler.handleGameSubscribe("ABC123", payload, accessor);

        assertEquals("ABC123", accessor.getSessionAttributes().get("lobbyCode"));
        assertEquals(1L, accessor.getSessionAttributes().get("playerId"));
        verify(lobbyPresenceService).registerGameConnection("sessionId", "ABC123", 1L);
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

}
