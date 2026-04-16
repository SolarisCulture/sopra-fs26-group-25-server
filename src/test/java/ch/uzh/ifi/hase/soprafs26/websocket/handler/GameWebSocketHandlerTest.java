package ch.uzh.ifi.hase.soprafs26.websocket.handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.GameEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private LobbyService lobbyService;

    @InjectMocks
    private GameWebSocketHandler gameWebSocketHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes Mocks
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

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + lobbyCode + "/spymaster"),
                eventCaptor.capture()
        );

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/game/" + lobbyCode + "/spy"),
                eventCaptor.capture()
        );

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
                eq("/topic/game/" + lobbyCode),
                eventCaptor.capture()
        );

        GameEvent event = eventCaptor.getValue();

        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals("RETURNING_TO_LOBBY", event.getType());
    }
}