package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {
    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private GameWebSocketHandler webSocketHandler;

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
    void processChatMessage_savesTeamMessage() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        ChatMessageDTO msg = new ChatMessageDTO();
        msg.setChannel("TEAM");
        msg.setTeam(TeamColor.RED);
        msg.setSenderName("Player1");
        msg.setContent("Hello red team");

        chatService.saveChatMessage(LOBBY_CODE, msg);

        // Verify saved
        List<ChatMessageDTO> history = chatService.getHistory(LOBBY_CODE, TeamColor.RED, false);
        assertEquals(1, history.size());
        assertEquals("Hello red team", history.get(0).getContent());
    }

    @Test
    void getHistory_teamMessages_onlySameTeam() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        // Save two team messages: one for RED, one for BLUE
        ChatMessageDTO redMsg = new ChatMessageDTO();
        redMsg.setChannel("TEAM");
        redMsg.setTeam(TeamColor.RED);
        redMsg.setContent("Red secret");
        chatService.saveChatMessage(LOBBY_CODE, redMsg);

        ChatMessageDTO blueMsg = new ChatMessageDTO();
        blueMsg.setChannel("TEAM");
        blueMsg.setTeam(TeamColor.BLUE);
        blueMsg.setContent("Blue secret");
        chatService.saveChatMessage(LOBBY_CODE, blueMsg);

        // RED player sees only RED message
        List<ChatMessageDTO> redHistory = chatService.getHistory(LOBBY_CODE, TeamColor.RED, false);
        assertEquals(1, redHistory.size());
        assertEquals("Red secret", redHistory.get(0).getContent());

        // BLUE player sees only BLUE message
        List<ChatMessageDTO> blueHistory = chatService.getHistory(LOBBY_CODE, TeamColor.BLUE, false);
        assertEquals(1, blueHistory.size());
        assertEquals("Blue secret", blueHistory.get(0).getContent());
    }

    @Test
    void getHistory_spymasterChannel_onlySpymastersSee() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        ChatMessageDTO spymasterMsg = new ChatMessageDTO();
        spymasterMsg.setChannel("SPYMASTER");
        spymasterMsg.setContent("Spymaster only");
        chatService.saveChatMessage(LOBBY_CODE, spymasterMsg);

        // Spymaster (isSpymaster=true) sees the message
        List<ChatMessageDTO> spymasterHistory = chatService.getHistory(LOBBY_CODE, TeamColor.RED, true);
        assertEquals(1, spymasterHistory.size());

        // Regular spy (isSpymaster=false) sees nothing
        List<ChatMessageDTO> spyHistory = chatService.getHistory(LOBBY_CODE, TeamColor.RED, false);
        assertTrue(spyHistory.isEmpty());
    }

    @Test
    void processChatMessage_lobbyNotFound_throwsException() {
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.empty());

        ChatMessageDTO msg = new ChatMessageDTO();
        assertThrows(IllegalArgumentException.class, () -> chatService.saveChatMessage(LOBBY_CODE, msg));
    }

    @Test
    void processChatMessage_gameNotStarted_throwsException() {
        lobby.setGame(null);
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(Optional.of(lobby));

        ChatMessageDTO msg = new ChatMessageDTO();
        assertThrows(IllegalStateException.class, () -> chatService.saveChatMessage(LOBBY_CODE, msg));
    }
}
