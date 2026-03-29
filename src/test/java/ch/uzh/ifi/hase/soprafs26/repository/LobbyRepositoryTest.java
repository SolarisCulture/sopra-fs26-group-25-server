package ch.uzh.ifi.hase.soprafs26.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;

@DataJpaTest
public class LobbyRepositoryTest {
    @Autowired
	private TestEntityManager entityManager;

	@Autowired
	private LobbyRepository lobbyRepository;

    private Lobby testLobby;
    private Player player;

    @Test
	public void findPlayersInLobby_validInput_returnsPlayerList() {
		testLobby = new Lobby();
        testLobby.setLobbyCode("123456");
        testLobby.setHostId(1L);
        testLobby.setCreatedAt(LocalDateTime.now());
        testLobby.setLobbyStatus(LobbyStatus.WAITING);

		player = new Player();
        player.setUsername("A");
        player.setHost(true);
        player.setRole(Role.SPY);
        player.setTeam(TeamColor.BLUE);
        player.setJoinedAt(LocalDateTime.now());

        List<Player> playerList = new ArrayList<>(List.of(player));
        testLobby.setPlayerList(playerList);

		entityManager.persist(testLobby);
        entityManager.persist(player);
		entityManager.flush();

		List<Player> foundPlayerList = lobbyRepository.findPlayersInLobby(testLobby.getLobbyCode());

        assertNotNull(foundPlayerList);
		assertEquals(1, foundPlayerList.size());
        assertEquals("A", foundPlayerList.get(0).getUsername());
        assertEquals(TeamColor.BLUE, foundPlayerList.get(0).getTeam());
        assertEquals(Role.SPY, foundPlayerList.get(0).getRole());
	}

    @Test
    public void existsByLobbyCode_validInput_returnsTrue() {
		testLobby = new Lobby();
        testLobby.setLobbyCode("123456");
        testLobby.setHostId(1L);
        testLobby.setCreatedAt(LocalDateTime.now());
        testLobby.setLobbyStatus(LobbyStatus.WAITING);

        entityManager.persist(testLobby);
        entityManager.flush();

        assertTrue(lobbyRepository.existsByLobbyCode(testLobby.getLobbyCode()));
    }
}

