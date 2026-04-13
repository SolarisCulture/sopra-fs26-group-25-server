package ch.uzh.ifi.hase.soprafs26.websocket.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.event.LobbyEvent;
import ch.uzh.ifi.hase.soprafs26.websocket.listener.LobbyWebSocketListener;

class LobbyWebSocketHandlerTest {
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private LobbyService lobbyService;

    @InjectMocks
    private LobbyWebSocketHandler lobbyWebSocketHandler;

    @InjectMocks
    private LobbyWebSocketListener lobbyWebSocketListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initializes Mocks
    }

    // Lobby Created

    @Test
    void broadcastLobbyCreated_shouldSendToCorrectTopic() {
        // Given
        String lobbyCode = "ABC123";
        Object lobbyData = new Object(); // Replace with LobbyDTO when implemented

        // When
        lobbyWebSocketHandler.broadcastLobbyCreated(lobbyCode, lobbyData);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend( // Verify mock was called
            eq("/topic/lobbies/"), // correct destination
            eventCaptor.capture() // save passed argument for assertions
        );
        
        LobbyEvent event = eventCaptor.getValue(); // Get saved argument to inspect every field
        assertEquals("LOBBY_CREATED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(lobbyData, event.getData());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void broadcastLobbyCreated_eventShouldHaveCorrectType() {
        // Given
        String lobbyCode = "ABC123";
        Object lobbyData = new Object(); 

        // When
        lobbyWebSocketHandler.broadcastLobbyCreated(lobbyCode, lobbyData);
        
        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/lobbies/"), 
            eventCaptor.capture());

        LobbyEvent event = eventCaptor.getValue();
        assertEquals("LOBBY_CREATED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertNotNull(event.getTimestamp());
    }

    // Player joined

    @Test
    void broadcastPlayerJoined_shouldSendToCorrectTopic() {
        // Given
        String lobbyCode = "ABC123";
        Object playerData = new Player("a");

        // When
        lobbyWebSocketHandler.broadcastPlayerJoined(lobbyCode, playerData);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/lobbies/" + lobbyCode),
            eventCaptor.capture()
        );
        
        LobbyEvent event = eventCaptor.getValue();
        assertEquals("PLAYER_JOINED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(playerData, event.getData());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void broadcastPlayerJoined_eventShouldHaveCorrectTypeAndData() {
        // Given
        String lobbyCode = "ABC123";
        Player player = new Player("a");
        player.setId(1L);

        // When
        lobbyWebSocketHandler.broadcastPlayerJoined(lobbyCode, player);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/lobbies/" + lobbyCode), eventCaptor.capture());

        LobbyEvent event = eventCaptor.getValue();
        assertEquals("PLAYER_JOINED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(player, event.getData());
        assertNotNull(event.getTimestamp());
    }

    // Player left

    @Test
    void broadcastPlayerLeft_shouldSendToCorrectTopic() {
        String lobbyCode = "ABC123";
        Object playerData = new Player("a");

        lobbyWebSocketHandler.broadcastPlayerLeft(lobbyCode, playerData);

        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/lobbies/" + lobbyCode),
            eventCaptor.capture()
        );
        
        LobbyEvent event = eventCaptor.getValue();
        assertEquals("PLAYER_LEFT", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(playerData, event.getData());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void broadcastPlayerLeft_eventShouldHaveCorrectType() {
        // Given
        String lobbyCode = "ABC123";
        Player player = new Player("a");

        // When
        lobbyWebSocketHandler.broadcastPlayerLeft(lobbyCode, player);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/lobbies/" + lobbyCode), eventCaptor.capture());
        
        LobbyEvent event = eventCaptor.getValue();
        assertEquals("PLAYER_LEFT", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(player, event.getData());
    }

    // Host changed

    @Test
    void broadcastHostChanged_shouldSendToCorrectTopic() {
        String lobbyCode = "ABC123";
        Object newHostData = new Player("b");

        lobbyWebSocketHandler.broadcastHostChanged(lobbyCode, newHostData);

        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/lobbies/" + lobbyCode),
            eventCaptor.capture()
        );
        
        LobbyEvent event = eventCaptor.getValue();
        assertEquals("HOST_CHANGED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(newHostData, event.getData());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void broadcastHostChanged_eventShouldHaveCorrectType() {
        // Given
        String lobbyCode = "ABC123";
        Player newHost = new Player("bob");
        newHost.setId(2L);
        newHost.setHost(true);

        // When
        lobbyWebSocketHandler.broadcastHostChanged(lobbyCode, newHost);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/lobbies/" + lobbyCode), eventCaptor.capture());
        
        LobbyEvent event = eventCaptor.getValue();
        assertEquals("HOST_CHANGED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(newHost, event.getData());
    }

    // Topic specificity

    @Test
    void broadcastPlayerJoined_onlyGoesToSpecificLobbyTopic() {
        // Given
        String lobbyCode = "ABC123";
        Player player = new Player("alice");

        // When
        lobbyWebSocketHandler.broadcastPlayerJoined(lobbyCode, player);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/lobbies/" + lobbyCode),
            any(LobbyEvent.class)
        );
        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/lobbies/"),
            any(LobbyEvent.class)
        );
    }

    @Test
    void broadcastLobbyCreated_onlyGoesToGlobalLobbyTopic() {
        // Given
        String lobbyCode = "ABC123";
        Object lobbyData = new Object();

        // When
        lobbyWebSocketHandler.broadcastLobbyCreated(lobbyCode, lobbyData);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
            eq("/topic/lobbies/"),
            any(LobbyEvent.class)
        );
        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/lobbies/" + lobbyCode),
            any(LobbyEvent.class)
        );
    }

    // Data Persistence

    @Test
    void broadcastEvent_shouldContainTimestamp() {
        // Given
        String lobbyCode = "ABC123";
        Player player = new Player("alice");

        // When
        lobbyWebSocketHandler.broadcastPlayerJoined(lobbyCode, player);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate).convertAndSend(anyString(), eventCaptor.capture());
        
        LobbyEvent event = eventCaptor.getValue();
        assertNotNull(event.getTimestamp());
    }

    @Test
    void broadcastMultipleEvents_shouldSendAll() {
        // Given
        String lobbyCode = "ABC123";
        Player player = new Player("alice");
        Player newHost = new Player("bob");

        // When
        lobbyWebSocketHandler.broadcastPlayerJoined(lobbyCode, player);
        lobbyWebSocketHandler.broadcastPlayerLeft(lobbyCode, player);
        lobbyWebSocketHandler.broadcastHostChanged(lobbyCode, newHost);

        // Then
        verify(messagingTemplate, times(3)).convertAndSend(anyString(), any(LobbyEvent.class));
    }

    // Team Update
    @Test
    void broadcastTeamUpdated_shouldSendToCorrectTopic() {
        // Given
        String lobbyCode = "ABC123";
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(1L);
        playerDTO.setUsername("player1");
        playerDTO.setTeam(TeamColor.RED);

        // When
        lobbyWebSocketHandler.broadcastTeamUpdated(lobbyCode, playerDTO);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate, times(1))
            .convertAndSend(eq("/topic/lobbies/" + lobbyCode), eventCaptor.capture());

        LobbyEvent event = eventCaptor.getValue();
        assertEquals("TEAM_UPDATED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(playerDTO, event.getData());
        assertNotNull(event.getTimestamp());
    }

    // Role Update
    @Test
    void broadcastRoleUpdated_shouldSendToCorrectTopic() {
        // Given
        String lobbyCode = "ABC123";
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(1L);
        playerDTO.setUsername("player1");
        playerDTO.setRole(Role.SPYMASTER);

        // When
        lobbyWebSocketHandler.broadcastRoleUpdated(lobbyCode, playerDTO);

        // Then
        ArgumentCaptor<LobbyEvent> eventCaptor = ArgumentCaptor.forClass(LobbyEvent.class);
        verify(messagingTemplate, times(1))
            .convertAndSend(eq("/topic/lobbies/" + lobbyCode), eventCaptor.capture());

        LobbyEvent event = eventCaptor.getValue();
        assertEquals("ROLE_UPDATED", event.getType());
        assertEquals(lobbyCode, event.getLobbyCode());
        assertEquals(playerDTO, event.getData());
        assertNotNull(event.getTimestamp());
    }

    // Connection Loss --> Player leaves
    @Test
    void handleWebSocketDisconnectListener_shouldCallLeaveLobby() {
        // Given
        String lobbyCode = "ABC123";
        Long playerId = 1L;

        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("lobbyCode", lobbyCode);
        sessionAttributes.put("playerId", playerId);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionAttributes(sessionAttributes);

        // Simulates the message sent when dc happens
        Message<byte[]> message = MessageBuilder.createMessage(
            new byte[0],
            accessor.getMessageHeaders()
        );

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "sessionId", CloseStatus.NORMAL);

        // When
        lobbyWebSocketListener.handleWebSocketDisconnectListener(event);

        // Then
        verify(lobbyService, times(1))
            .leaveLobby(lobbyCode, playerId);
    }

    // No playerId + lobbyCode in message --> nothing happens
    @Test
    void handleWebSocketDisconnectListener_noSessionAttributes_shouldNotCallLeaveLobby() {
        // Given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);

        Message<byte[]> message = MessageBuilder.createMessage(
            new byte[0],
            accessor.getMessageHeaders()
        );

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "sessionId", CloseStatus.NORMAL);

        // When
        lobbyWebSocketListener.handleWebSocketDisconnectListener(event);

        // Then
        verify(lobbyService, never())
            .leaveLobby(anyString(), anyLong());
    }
}

