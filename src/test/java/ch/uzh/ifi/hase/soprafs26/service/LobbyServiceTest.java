package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.LobbyWebSocketHandler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LobbyServiceTest {

	@Mock
	private LobbyRepository lobbyRepository;

	@Mock
	private DTOMapper dtoMapper;

	@Mock
	private LobbyWebSocketHandler lobbyWebSocketHandler;

	@InjectMocks
	private LobbyService lobbyService;

	private Lobby testLobby;
	private Player player1;
	private Player player2;
	private Player player3;
	private Player player4;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given lobby
		testLobby = new Lobby();
		testLobby.setId(1L);
        testLobby.setHostId(1L);
		testLobby.setCreatedAt(LocalDateTime.now());
        testLobby.setLobbyCode("123");

		// given playerList
		player1 = new Player();
		player1.setId(1L);
		player1.setTeam(TeamColor.BLUE);
		player1.setRole(Role.SPY);

		player2 = new Player();
		player2.setId(2L);
		player2.setTeam(TeamColor.BLUE);
		player2.setRole(Role.SPYMASTER);

		player3 = new Player();
		player3.setId(3L);
		player3.setTeam(TeamColor.RED);
		player3.setRole(Role.SPY);

		player4 = new Player();
		player4.setId(4L);
		player4.setTeam(TeamColor.RED);
		player4.setRole(Role.SPYMASTER);

		List<Player> playerList = new ArrayList<>(List.of(player1, player2, player3, player4));
		testLobby.setPlayerList(playerList);
	}

	// assignTeam
    @Test
	public void assignTeam_validInput_changesTeam() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));
		Mockito.when(dtoMapper.convertEntityToPlayerDTO(player1)).thenReturn(new PlayerDTO());

		lobbyService.assignTeam(testLobby.getLobbyCode(), player1.getId(), TeamColor.RED);

		assertEquals(TeamColor.RED, player1.getTeam());
	};

	@Test
	public void assignTeam_invalidInput_returnsNotFound() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> lobbyService.assignTeam("333", player1.getId(), TeamColor.RED));
	};

	// assignRole
	@Test
	public void assignRole_validInput_changesRole() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));
		Mockito.when(dtoMapper.convertEntityToPlayerDTO(player2)).thenReturn(new PlayerDTO());

		lobbyService.assignRole(testLobby.getLobbyCode(), player2.getId(), Role.SPY);

		assertEquals(Role.SPY, player2.getRole());
	};

	@Test
	public void assignRole_invalidInput_returnsNotFound() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> lobbyService.assignRole("333", player2.getId(), Role.SPY));
	};

	// canStartGame
	@Test
	public void assignTeam_invalidInput_returnsNotAuthorized() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

		assertThrows(ResponseStatusException.class, () -> lobbyService.assignRole(testLobby.getLobbyCode(), player1.getId(), Role.SPYMASTER));
	};

    @Test
	public void canStartGame_validInput_returnsTrue() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

		assertTrue(lobbyService.canStartGame(testLobby.getLobbyCode()));
	};

	@Test
	public void canStartGame_validInput_returnsFalse() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

		player1.setTeam(TeamColor.RED);

		assertFalse(lobbyService.canStartGame(testLobby.getLobbyCode()));
	};

	@Test
	public void canStartGame_invalidInput_returnsNotFound() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> lobbyService.canStartGame("333"));
	};
}
