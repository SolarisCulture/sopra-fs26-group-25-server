package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

	// Lobby Creation
	@Test
    public void createLobby_shouldGenerateUniqueCode() {
        // Given
		String hostUsername = "testHost";
        when(lobbyRepository.existsByLobbyCode(anyString())).thenReturn(false);
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        Lobby lobby = lobbyService.createLobby(hostUsername);

        // Then
        assertNotNull(lobby);
        assertNotNull(lobby.getLobbyCode());
        assertEquals(6, lobby.getLobbyCode().length());
        verify(lobbyRepository, times(1)).save(any(Lobby.class));
    }

	@Test
    public void createLobby_shouldRetryOnDuplicateCode() {
        // Given
		String hostUsername = "testHost";
        when(lobbyRepository.existsByLobbyCode(anyString()))
            .thenReturn(true)  // First attempt fails
            .thenReturn(false); // Second attempt succeeds
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        Lobby lobby = lobbyService.createLobby(hostUsername);

        // Then
        assertNotNull(lobby);
        assertNotNull(lobby.getLobbyCode());
        verify(lobbyRepository, times(2)).existsByLobbyCode(anyString());
        verify(lobbyRepository, times(1)).save(any(Lobby.class));
    }

	@Test
    public void createLobby_shouldAddHostPlayer() {
        // Given
		String hostUsername = "a";
        when(lobbyRepository.existsByLobbyCode(anyString())).thenReturn(false);
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby saved = invocation.getArgument(0);
            saved.setId(1L);

			if (saved.getPlayerList() != null && !saved.getPlayerList().isEmpty()) {
				Player host = saved.getPlayerList().get(0);
				host.setId(1L);  // Simulate ID
				saved.setHostId(host.getId());
			}
            return saved;
        });

        // When
        Lobby lobby = lobbyService.createLobby(hostUsername);

        // Then
        assertNotNull(lobby.getPlayerList());
        assertEquals(1, lobby.getPlayerList().size());
		assertEquals("a", lobby.getPlayerList().get(0).getUsername());
        assertTrue(lobby.getPlayerList().get(0).isHost());
        assertNotNull(lobby.getHostId());
		assertEquals(lobby.getPlayerList().get(0).getId(), lobby.getHostId());
    }

	@Test
    public void createLobby_shouldBroadcastEvent() {
        // Given
		String hostUsername = "testHost";
        when(lobbyRepository.existsByLobbyCode(anyString())).thenReturn(false);
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        Lobby lobby = lobbyService.createLobby(hostUsername);

        // Then
        verify(lobbyWebSocketHandler, times(1))
            .broadcastLobbyCreated(lobby.getLobbyCode(), lobby);
    }

	@Test
	public void createLobby_withHostUsername_createsLobbyWithHost() {
		// Given
		String hostUsername = "a";
		when(lobbyRepository.existsByLobbyCode(anyString())).thenReturn(false);
		when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
			Lobby saved = invocation.getArgument(0);
			saved.setId(1L);
			// Set ID on host player
			if (saved.getPlayerList() != null && !saved.getPlayerList().isEmpty()) {
				Player host = saved.getPlayerList().get(0);
				host.setId(1L);
				saved.setHostId(host.getId());
			}
			return saved;
    	});

		// When
		Lobby lobby = lobbyService.createLobby(hostUsername);

		// Then
		assertNotNull(lobby);
		assertNotNull(lobby.getLobbyCode());
		assertEquals(6, lobby.getLobbyCode().length());
		
		assertNotNull(lobby.getPlayerList());
		assertEquals(1, lobby.getPlayerList().size());
		
		Player host = lobby.getPlayerList().get(0);
		assertEquals(hostUsername, host.getUsername());
		assertTrue(host.isHost());
		assertEquals(host.getId(), lobby.getHostId());
		
		verify(lobbyWebSocketHandler, times(1))
			.broadcastLobbyCreated(lobby.getLobbyCode(), lobby);
	}

	@Test
	public void createLobby_withNullHostUsername_throwsBadRequest() {
		// When/Then
		assertThrows(ResponseStatusException.class, () -> {
			lobbyService.createLobby(null);
		});
	}

	@Test
	public void createLobby_withEmptyHostUsername_throwsBadRequest() {
		// When/Then
		assertThrows(ResponseStatusException.class, () -> {
			lobbyService.createLobby("");
		});
	}

	// Get Lobby
	@Test
    public void getLobbyByCode_validCode_returnsLobby() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));

        // When
        Lobby result = lobbyService.getLobbyByCode("ABC123");

        // Then
        assertNotNull(result);
        assertEquals("ABC123", result.getLobbyCode());
        assertEquals(1L, result.getId());
    }

	@Test
    public void getLobbyByCode_invalidCode_throwsNotFound() {
        // Given
        when(lobbyRepository.findByLobbyCode("INVALID")).thenReturn(Optional.empty());

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            lobbyService.getLobbyByCode("INVALID");
        });
        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    public void getLobbyByCode_nullCode_throwsException() {
        // When/Then
        assertThrows(Exception.class, () -> {
            lobbyService.getLobbyByCode(null);
        });
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
	public void assignRole_validInputWithNoSpymasterAssignedYet_changesRole() {
		testLobby.addPlayer(player1);
		testLobby.addPlayer(player2);

		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));
		Mockito.when(dtoMapper.convertEntityToPlayerDTO(Mockito.any())).thenReturn(new PlayerDTO());

		lobbyService.assignRole(testLobby.getLobbyCode(), player2.getId(), Role.SPY);
		lobbyService.assignRole(testLobby.getLobbyCode(), player1.getId(), Role.SPYMASTER);

		assertEquals(Role.SPYMASTER, player1.getRole());
	};

	@Test
	public void assignRole_invalidInput_returnsNotFound() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> lobbyService.assignRole("333", player2.getId(), Role.SPY));
	};

	@Test
	public void assignRole_invalidInput_returnsNotAuthorized() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

		assertThrows(ResponseStatusException.class, () -> lobbyService.assignRole(testLobby.getLobbyCode(), player1.getId(), Role.SPYMASTER));
	};

	// canStartGame
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
