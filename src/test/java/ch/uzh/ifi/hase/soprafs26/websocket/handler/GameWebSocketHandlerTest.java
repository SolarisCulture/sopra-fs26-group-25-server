package ch.uzh.ifi.hase.soprafs26.websocket.handler;
import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.service.TurnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes Mocks
        gameWebSocketHandler = new GameWebSocketHandler(messagingTemplate, turnService);
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


}