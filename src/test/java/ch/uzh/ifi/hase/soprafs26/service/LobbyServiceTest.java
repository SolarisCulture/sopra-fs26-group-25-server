package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
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
		testLobby.setLobbyStatus(LobbyStatus.WAITING);
		testLobby.setCreatedAt(LocalDateTime.now());
        testLobby.setLobbyCode("123");

		// given playerList
		player1 = new Player();
		player1.setId(1L);
		player1.setUsername("A");
		player1.setTeam(TeamColor.BLUE);
		player1.setRole(Role.SPY);

		player2 = new Player();
		player2.setId(2L);
		player2.setUsername("B");
		player2.setTeam(TeamColor.BLUE);
		player2.setRole(Role.SPYMASTER);

		player3 = new Player();
		player3.setId(3L);
		player3.setUsername("C");
		player3.setTeam(TeamColor.RED);
		player3.setRole(Role.SPY);

		player4 = new Player();
		player4.setId(4L);
		player4.setUsername("D");
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
        verify(lobbyRepository, times(2)).save(any(Lobby.class));
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
        verify(lobbyRepository, times(2)).save(any(Lobby.class));
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

	// Host Transfer
	@Test
    public void transferHost_validTransfer_success() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        Player currentHost = new Player("a");
        currentHost.setId(1L);
        currentHost.setHost(true);
        
        Player newHost = new Player("b");
        newHost.setId(2L);
        newHost.setHost(false);
        
        lobby.addPlayer(currentHost);
        lobby.addPlayer(newHost);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(lobby);
        
        // When
        lobbyService.transferHost("ABC123", 1L, 2L);
        
        // Then
        assertFalse(currentHost.isHost());
        assertTrue(newHost.isHost());
        assertEquals(2L, lobby.getHostId());
        verify(lobbyWebSocketHandler).broadcastHostChanged("ABC123", newHost);
    }

	@Test
    public void transferHost_nonHost_throwsForbidden() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        Player nonHost = new Player("b");
        nonHost.setId(2L);
        nonHost.setHost(false);
        lobby.addPlayer(nonHost);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        
        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.transferHost("ABC123", 2L, 3L);
        });
    }

	@Test
    public void transferHost_playerNotFound_throwsNotFound() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        Player currentHost = new Player("a");
        currentHost.setId(1L);
        currentHost.setHost(true);
        lobby.addPlayer(currentHost);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        
        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.transferHost("ABC123", 1L, 99L);
        });
    }

	// Leave Lobby
	@Test
    public void leaveLobby_nonHost_playerRemoved() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        Player host = new Player("a");
        host.setId(1L);
        host.setHost(true);
        
        Player player = new Player("b");
        player.setId(2L);
        player.setHost(false);
        
        lobby.addPlayer(host);
        lobby.addPlayer(player);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(lobby);
        
        // When
        lobbyService.leaveLobby("ABC123", 2L);
        
        // Then
        assertEquals(1, lobby.getPlayerList().size());
        assertTrue(lobby.getPlayerList().get(0).isHost());
        verify(lobbyWebSocketHandler).broadcastPlayerLeft("ABC123", player);
        verify(lobbyWebSocketHandler, never()).broadcastHostChanged(anyString(), any());
    }

	@Test
    public void leaveLobby_host_assignsNewHostRandomly() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        Player host = new Player("a");
        host.setId(1L);
        host.setHost(true);
        
        Player player1 = new Player("b");
        player1.setId(2L);
        player1.setHost(false);
        
        Player player2 = new Player("c");
        player2.setId(3L);
        player2.setHost(false);
        
        lobby.addPlayer(host);
        lobby.addPlayer(player1);
        lobby.addPlayer(player2);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(lobby);
        
        // When
        lobbyService.leaveLobby("ABC123", 1L);
        
        // Then
        assertEquals(2, lobby.getPlayerList().size());
        boolean hasHost = lobby.getPlayerList().stream().anyMatch(Player::isHost);
        assertTrue(hasHost);
        assertNotNull(lobby.getHostId());
        verify(lobbyWebSocketHandler).broadcastPlayerLeft("ABC123", host);
        verify(lobbyWebSocketHandler).broadcastHostChanged(eq("ABC123"), any(Player.class));
    }

	@Test
    public void leaveLobby_lastPlayer_deletesLobby() {
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
        Lobby lobby = new Lobby();
        lobby.setHostId(1L);
        
        Player host = new Player("a");
        host.setId(1L);
        host.setHost(true);
        lobby.addPlayer(host);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        
        // When
        lobbyService.leaveLobby("ABC123", 1L);
        
        // Then
        verify(lobbyRepository).delete(lobby);
        verify(lobbyWebSocketHandler, never()).broadcastHostChanged(anyString(), any());
    }

	@Test
    public void leaveLobby_playerNotFound_throwsNotFound() {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        Player host = new Player("a");
        host.setId(1L);
        host.setHost(true);
        lobby.addPlayer(host);
        
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(lobby));
        
        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.leaveLobby("ABC123", 99L);
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

	// joinLobby
	@Test
	public void joinLobby_validInput_addsPlayer() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        Mockito.when(lobbyRepository.save(Mockito.any(Lobby.class))).thenAnswer(invocation -> {
            Lobby lobbyArg = invocation.getArgument(0);
            lobbyArg.getPlayerList().stream()
                .filter(p -> p.getId() == null)
                .forEach(p -> p.setId(ThreadLocalRandom.current().nextLong()));
            return lobbyArg;
        });

		int sizeBefore = testLobby.getPlayerList().size();
        Long playerId = lobbyService.joinLobby("123", "E");
        
        assertNotNull(playerId);
		assertEquals(sizeBefore + 1, testLobby.getPlayerList().size());
	}

	@Test
	public void joinLobby_lobbyNotFound_throwsNotFound() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,() -> lobbyService.joinLobby("999", "E"));

		assertEquals(404, exception.getStatusCode().value());
	}

	@Test
	public void joinLobby_usernameNotUnique_throwsConflict() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any()))
				.thenReturn(Optional.of(testLobby));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,() -> lobbyService.joinLobby("123", "A"));

		assertEquals(409, exception.getStatusCode().value());
	}

	@Test
	public void joinLobby_gameInProgress_throwsForbidden() {
		testLobby.setLobbyStatus(LobbyStatus.IN_PROGRESS);

		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,() -> lobbyService.joinLobby("123", "E"));

		assertEquals(409, exception.getStatusCode().value());
	}

	// getPlayerList
	@Test
	public void getPlayerList_validInput_returnsList() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

		List<Player> result = lobbyService.getPlayerList("123");

		assertEquals(4, result.size());
		assertEquals(player1, result.get(0));
		assertEquals(player4, result.get(3));
	}

	@Test
	public void getPlayerList_lobbyNotFound_throwsNotFound() {
		Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.empty());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,() -> lobbyService.getPlayerList("999"));

		assertEquals(404, exception.getStatusCode().value());
	}
}
