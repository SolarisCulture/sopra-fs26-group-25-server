package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @InjectMocks
    private ChatService chatService;

    private Lobby lobby;
    private Game game;
    private final String LOBBY_CODE = "ABC123";
    private final Long GAME_ID = 1L;

    @BeforeEach
    void setup() {
        lobby = new Lobby();
        lobby.setLobbyCode(LOBBY_CODE);
        game = new Game();
        game.setId(GAME_ID);
        lobby.setGame(game);
    }

    @Test
    void saveChatMessage_shouldSaveAndReturnMessageWithTimestampAndGlobalChannel() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        ChatMessageDTO input = new ChatMessageDTO();
        input.setSenderName("Alice");
        input.setContent("Hello world");

        ChatMessageDTO saved = chatService.saveChatMessage(LOBBY_CODE, input);

        assertNotNull(saved.getTimestamp());
        assertEquals("GLOBAL", saved.getChannel());
        assertEquals("Alice", saved.getSenderName());
        assertEquals("Hello world", saved.getContent());
    }

    @Test
    void getHistory_shouldReturnAllMessagesForLobby() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        ChatMessageDTO msg1 = new ChatMessageDTO();
        msg1.setSenderName("Alice");
        msg1.setContent("First");
        msg1.setTimestamp(LocalDateTime.now());
        chatService.saveChatMessage(LOBBY_CODE, msg1);

        ChatMessageDTO msg2 = new ChatMessageDTO();
        msg2.setSenderName("Bob");
        msg2.setContent("Second");
        msg2.setTimestamp(LocalDateTime.now());
        chatService.saveChatMessage(LOBBY_CODE, msg2);

        List<ChatMessageDTO> history = chatService.getHistory(LOBBY_CODE);
        assertEquals(2, history.size());
        assertEquals("First", history.get(0).getContent());
        assertEquals("Second", history.get(1).getContent());
    }

    @Test
    void getHistory_shouldReturnEmptyListWhenNoMessages() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        List<ChatMessageDTO> history = chatService.getHistory(LOBBY_CODE);
        assertTrue(history.isEmpty());
    }

    @Test
    void saveChatMessage_lobbyNotFound_throwsException() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.empty());
        ChatMessageDTO msg = new ChatMessageDTO();
        assertThrows(IllegalArgumentException.class, () -> chatService.saveChatMessage(LOBBY_CODE, msg));
    }

    @Test
    void saveChatMessage_gameNotStarted_throwsException() {
        lobby.setGame(null);
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));
        ChatMessageDTO msg = new ChatMessageDTO();
        assertThrows(IllegalStateException.class, () -> chatService.saveChatMessage(LOBBY_CODE, msg));
    }
}