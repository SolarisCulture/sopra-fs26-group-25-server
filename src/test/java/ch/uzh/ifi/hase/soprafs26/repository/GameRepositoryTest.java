package ch.uzh.ifi.hase.soprafs26.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;

@DataJpaTest
public class GameRepositoryTest {
    @Autowired
	private TestEntityManager entityManager;

    @Autowired
	private GameRepository gameRepository;

    private Game game;
    private Lobby testLobby;

    @Test
    public void findArchivedByLobbyCode_validInput_returnsGameList() {
        // given
        testLobby = new Lobby();
        testLobby.setLobbyCode("123456");
        testLobby.setHostId(1L);
        testLobby.setCreatedAt(LocalDateTime.now());
        testLobby.setLobbyStatus(LobbyStatus.WAITING);

        game = new Game();
        Turn turn = new Turn();
        turn.setGame(game);
        entityManager.persist(turn);
        game.setCurrentTurn(turn);

        game.setLobby(testLobby);
        testLobby.setGame(game);

        entityManager.persist(testLobby);
        entityManager.flush();

        // This has to be after the first flush because the status gets reset to active
        game.setStatus(GameStatus.ARCHIVED);
        entityManager.flush();

        // when
        List<Game> foundGames = gameRepository.findArchivedByLobbyCode(testLobby.getLobbyCode());

        // then
        assertNotNull(foundGames);
        assertEquals(1, foundGames.size());
        assertEquals(GameStatus.ARCHIVED, foundGames.get(0).getStatus());
        assertEquals(testLobby.getLobbyCode(), foundGames.get(0).getLobby().getLobbyCode());
    }

    @Test
    public void findArchivedByLobbyCode_noArchivedGame_returnsEmptyList() {
        // given
        testLobby = new Lobby();
        testLobby.setLobbyCode("123456");
        testLobby.setHostId(1L);
        testLobby.setCreatedAt(LocalDateTime.now());
        testLobby.setLobbyStatus(LobbyStatus.WAITING);

        game = new Game();

        game.setLobby(testLobby);
        testLobby.setGame(game);

        Turn turn = new Turn();
        turn.setGame(game);
        entityManager.persist(turn);
        game.setCurrentTurn(turn);


        entityManager.persist(testLobby);
        entityManager.flush();

        game.setStatus(GameStatus.ACTIVE);
        entityManager.flush();

        // when
        List<Game> foundGames = gameRepository.findArchivedByLobbyCode(testLobby.getLobbyCode());

        // then
        assertNotNull(foundGames);
        assertTrue(foundGames.isEmpty());
    }
}
