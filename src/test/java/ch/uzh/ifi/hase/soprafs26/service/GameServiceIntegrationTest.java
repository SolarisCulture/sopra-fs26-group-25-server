package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GameServiceIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private LobbyRepository lobbyRepository;

    private Lobby testLobby;

    @BeforeEach
    public void setup() {
        testLobby = new Lobby();
        testLobby.setHostId(1L);
        testLobby = lobbyRepository.save(testLobby);
    }

    @Test
    public void startGame_createsFullGameAndPersistsToDatabase() {
        // Start the game
        String lobbyCode = testLobby.getLobbyCode();
        Game game = gameService.startGame(lobbyCode);

        System.out.println("Game object: " + game);
        System.out.println("Game ID: " + game.getId());
        System.out.println("Board ID: " + (game.getBoard() != null ? game.getBoard().getId() : null));

        // Check that game created correctly
        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals(GameStatus.ACTIVE, game.getStatus());
        assertEquals(TeamColor.RED, game.getCurrentTurn());

        // Check that board has 25 cards
        assertNotNull(game.getBoard());
        assertNotNull(game.getBoard().getId());  // board also persisted
        assertEquals(25, game.getBoard().getCards().size());

        // Assert — correct card type distribution
        long redCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.AGENTRED).count();
        long blueCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.AGENTBLUE).count();
        long civilianCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.CIVILIAN).count();
        long assassinCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.ASSASSIN).count();

        assertEquals(9, redCount);
        assertEquals(8, blueCount);
        assertEquals(7, civilianCount);
        assertEquals(1, assassinCount);

        // Chack that all words are unique
        long uniqueWords = game.getBoard().getCards().stream()
                .map(WordCard::getWord)
                .distinct().count();
        assertEquals(25, uniqueWords, "All 25 words should be unique");

        // Chack that game is linked to lobby in database
        Lobby reloaded = lobbyRepository.findByLobbyCode(lobbyCode).orElseThrow();
        assertNotNull(reloaded.getGame());
        assertEquals(game.getId(), reloaded.getGame().getId());

        // Chack that  role-based board filtering works end to end
        GameBoardDTO spymasterView = gameService.getBoard(lobbyCode, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.getBoard(lobbyCode, Role.SPY);

        // Spymaster sees card types and key card
        assertNotNull(spymasterView.getKeyCard());
        assertEquals(25, spymasterView.getKeyCard().size());
        assertNotNull(spymasterView.getCards().get(0).getCardType());

        // Spy sees no card types and no key card
        assertNull(spyView.getKeyCard());
        assertNull(spyView.getCards().get(0).getCardType());
    }

    @Test
    public void startGame_lobbyNotFound_throwsNotFound() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> {
                    gameService.startGame("INVALID");
                });
        // Assertion
        assertEquals(404, exception.getStatusCode().value());
    }
}