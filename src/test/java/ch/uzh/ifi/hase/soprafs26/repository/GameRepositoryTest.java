package ch.uzh.ifi.hase.soprafs26.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;

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

    @Test
    public void saveGame_cascadesToBoardAndCards() {
        // Create Board and 2 WordCards (simulate 25 but 2 is enough for cascade check)
        Board board = new Board();
        WordCard card1 = new WordCard();
        card1.setWord("WORD1");
        card1.setCardType(CardType.CIVILIAN);
        WordCard card2 = new WordCard();
        card2.setWord("WORD2");
        card2.setCardType(CardType.AGENTRED);
        board.setCards(List.of(card1, card2));

        // Create Game
        Game game = new Game();
        game.setBoard(board);
        board.setGame(game);
        game.setStatus(GameStatus.ACTIVE);

        // Create minimal Turn (required because game.currentTurn is not nullable)
        Turn turn = new Turn();
        turn.setGame(game);
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        turn.setStartTime(LocalDateTime.now());
        game.setCurrentTurn(turn);
        game.setTurns(List.of(turn));

        entityManager.persistAndFlush(game);
        Long gameId = game.getId();

        entityManager.clear();

        Game loaded = entityManager.find(Game.class, gameId);
        assertNotNull(loaded.getBoard());
        assertEquals(2, loaded.getBoard().getCards().size());
        assertNotNull(loaded.getBoard().getCards().get(0).getId());
        assertNotNull(loaded.getBoard().getCards().get(1).getId());
        assertNotNull(loaded.getCurrentTurn());
    }

    @Test
    public void findArchivedByLobbyCode_ignoresActiveAndFinished() { // TODO: Fails due to SQL DataException
        // Create Lobby
        Lobby lobby = new Lobby();
        lobby.setLobbyCode("ARCHTEST");
        lobby.setHostId(1L);
        entityManager.persistAndFlush(lobby);

        // Helper to create a game with given status
        java.util.function.BiConsumer<Lobby, GameStatus> createGame = (l, status) -> {
            Game g = new Game();
            g.setStatus(status);
            g.setLobby(l);
            Turn t = new Turn();
            t.setGame(g);
            g.setCurrentTurn(t);
            entityManager.persist(g);
        };

        createGame.accept(lobby, GameStatus.ACTIVE);
        createGame.accept(lobby, GameStatus.FINISHED);
        createGame.accept(lobby, GameStatus.ARCHIVED);

        entityManager.flush();

        List<Game> archived = gameRepository.findArchivedByLobbyCode("ARCHTEST");
        assertEquals(1, archived.size());
        assertEquals(GameStatus.ARCHIVED, archived.get(0).getStatus());
    }

    @Test
    public void game_prePersist_setsDefaults() { // TODO: Fails die tp TransientPropertyValueException
        Game game = new Game();
        // No explicit status, createdAt, scores set
        Turn turn = new Turn();
        turn.setGame(game);
        game.setCurrentTurn(turn);
        turn.setPhase(TurnPhase.SPYMASTER_TURN);

        entityManager.persistAndFlush(game);
        Long id = game.getId();

        entityManager.clear();

        Game loaded = entityManager.find(Game.class, id);
        assertNotNull(loaded.getCreatedAt());
        assertEquals(GameStatus.ACTIVE, loaded.getStatus());
        assertEquals(0, loaded.getRedScore());
        assertEquals(0, loaded.getBlueScore());
        assertEquals(1, loaded.getCurrentRound());
    }
}
