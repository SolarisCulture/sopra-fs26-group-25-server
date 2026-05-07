package ch.uzh.ifi.hase.soprafs26.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.Difficulty;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.constant.Topic;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbySettings;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;

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

    @Test
    public void lobbyCode_unique_constraint_throwsException() {
        Lobby lobby1 = new Lobby();
        lobby1.setLobbyCode("ABC123");
        lobby1.setHostId(1L);
        entityManager.persistAndFlush(lobby1);

        Lobby lobby2 = new Lobby();
        lobby2.setLobbyCode("ABC123"); // duplicate
        lobby2.setHostId(2L);

        assertThrows(org.hibernate.exception.ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(lobby2);
        });
    }

    @Test
    public void deleteLobby_cascadesOrphanRemovalToGame() {
        // 1. Create and persist the Turn first -> hibernate expects Turn to be saved BEFORE Game references it
        Turn turn = new Turn();
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        turn.setStartTime(LocalDateTime.now());
        entityManager.persist(turn);
        
        // 2. Create Game and link to Turn
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(turn);
        turn.setGame(game);
        
        // 3. Create Lobby and link to Game
        Lobby lobby = new Lobby();
        lobby.setLobbyCode("DEL123");
        lobby.setHostId(1L);
        lobby.setCreatedAt(LocalDateTime.now());
        lobby.setLobbyStatus(LobbyStatus.WAITING);
        lobby.setGame(game);
        game.setLobby(lobby);
        
        // 4. Persist Lobby -> cascades to Game (TransientPropertyValueException if Turn not persisted before)
        entityManager.persist(lobby);
        entityManager.flush();
        
        Long lobbyId = lobby.getId();
        Long gameId = game.getId();
        Long turnId = turn.getId();
        
        // 5. Verify everything exists
        assertNotNull(entityManager.find(Lobby.class, lobbyId));
        assertNotNull(entityManager.find(Game.class, gameId));
        assertNotNull(entityManager.find(Turn.class, turnId));
        
        // 6. Remove Turn first
        game.setCurrentTurn(null); // Remove reference to Turn first
        entityManager.merge(game);
        entityManager.flush();
        
        // 7. Delete Turn (now orphaned)
        entityManager.remove(turn);
        
        // 8. Delete Lobby (cascades to Game)
        entityManager.remove(lobby);
        entityManager.flush();
        
        // 9. Verify all are gone
        assertNull(entityManager.find(Lobby.class, lobbyId));
        assertNull(entityManager.find(Game.class, gameId));
        assertNull(entityManager.find(Turn.class, turnId));
    }

    @Test
    public void lobbySettings_embedded_persistsAndLoads() {
        Lobby lobby = new Lobby();
        lobby.setLobbyCode("SET123");
        lobby.setHostId(1L);

        LobbySettings settings = lobby.getSettings();
        settings.setDifficulty(Difficulty.HARD);
        settings.setSpymasterTimeLimit(45);
        settings.setSpyTimeLimit(30);
        settings.setRounds(3);
        settings.setTopics(List.of(Topic.ANIMALS, Topic.SPORTS));

        entityManager.persistAndFlush(lobby);
        Long id = lobby.getId();

        entityManager.clear();

        Lobby loaded = entityManager.find(Lobby.class, id);
        assertEquals(Difficulty.HARD, loaded.getSettings().getDifficulty());
        assertEquals(45, loaded.getSettings().getSpymasterTimeLimit());
        assertEquals(30, loaded.getSettings().getSpyTimeLimit());
        assertEquals(3, loaded.getSettings().getRounds());
        assertEquals(List.of(Topic.ANIMALS, Topic.SPORTS), loaded.getSettings().getTopics());
    }
}

