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
        // Create Board and 2 WordCards
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

        // Create minimal Turn 
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
    public void findArchivedByLobbyCode_ignoresActiveAndFinished() { 
        // Create an ACTIVE game
        Lobby activeLobby = new Lobby();
        activeLobby.setLobbyCode("ACTIVE");
        activeLobby.setHostId(1L);
        activeLobby.setCreatedAt(LocalDateTime.now());
        activeLobby.setLobbyStatus(LobbyStatus.WAITING);

        Game activeGame = new Game();
        Turn activeTurn = new Turn();
        activeTurn.setGame(activeGame);
        entityManager.persist(activeTurn);
        activeGame.setCurrentTurn(activeTurn);
        activeGame.setLobby(activeLobby);
        activeLobby.setGame(activeGame);

        entityManager.persist(activeLobby);
        entityManager.flush();
        activeGame.setStatus(GameStatus.ACTIVE);
        entityManager.flush();

        // Create a FINISHED game
        Lobby finishedLobby = new Lobby();
        finishedLobby.setLobbyCode("FINISH");
        finishedLobby.setHostId(1L);
        finishedLobby.setCreatedAt(LocalDateTime.now());
        finishedLobby.setLobbyStatus(LobbyStatus.FINISHED);

        Game finishedGame = new Game();
        Turn finishedTurn = new Turn();
        finishedTurn.setGame(finishedGame);
        entityManager.persist(finishedTurn);
        finishedGame.setCurrentTurn(finishedTurn);
        finishedGame.setLobby(finishedLobby);
        finishedLobby.setGame(finishedGame);

        entityManager.persist(finishedLobby);
        entityManager.flush();
        finishedGame.setStatus(GameStatus.FINISHED);
        entityManager.flush();

        // Create an ARCHIVED game
        Lobby archivedLobby = new Lobby();
        archivedLobby.setLobbyCode("ARCHIV");
        archivedLobby.setHostId(1L);
        archivedLobby.setCreatedAt(LocalDateTime.now());
        archivedLobby.setLobbyStatus(LobbyStatus.IN_PROGRESS);

        Game archivedGame = new Game();
        Turn archivedTurn = new Turn();
        archivedTurn.setGame(archivedGame);
        entityManager.persist(archivedTurn);
        archivedGame.setCurrentTurn(archivedTurn);
        archivedGame.setLobby(archivedLobby);
        archivedLobby.setGame(archivedGame);

        entityManager.persist(archivedLobby);
        entityManager.flush();
        archivedGame.setStatus(GameStatus.ARCHIVED);
        entityManager.flush();

        List<Game> foundGames = gameRepository.findArchivedByLobbyCode("ARCHIV");

        // Assert only the archived game is returned
        assertNotNull(foundGames);
        assertEquals(1, foundGames.size());
        assertEquals(GameStatus.ARCHIVED, foundGames.get(0).getStatus());
        assertEquals("ARCHIV", foundGames.get(0).getLobby().getLobbyCode());
    }

    @Test
    public void game_prePersist_setsDefaults() { 
        // Create and persist a Board
        Board board = new Board();
        entityManager.persistAndFlush(board);

        // Create and persist a Turn
        Turn turn = new Turn();
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        turn.setStartTime(LocalDateTime.now());
        entityManager.persistAndFlush(turn);

        Game game = new Game();
        game.setBoard(board);
        game.setCurrentTurn(turn);

        entityManager.persistAndFlush(game);

        assertNotNull(game.getId());
        assertEquals(GameStatus.ACTIVE, game.getStatus());
        assertEquals(0, game.getRedScore());
        assertEquals(0, game.getBlueScore());
        assertEquals(1, game.getCurrentRound());
    }
}
