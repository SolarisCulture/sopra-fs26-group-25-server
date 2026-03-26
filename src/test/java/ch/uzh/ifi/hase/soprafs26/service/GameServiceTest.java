package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameServiceTest {
    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private WordService wordService;

    @Mock
    private GameWebSocketHandler gameWebSocketHandler;

    @InjectMocks
    private GameService gameService;

    private Lobby testLobby;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testLobby = new Lobby();
        testLobby.setId(1L);
        testLobby.setLobbyCode("ABC123");
        testLobby.setHostId(1L);
    }

    @Test
    public void startGame_validLobby_createsGameWith25Cards() {
        // Arrange
        when(lobbyRepository.findByLobbyCode("ABC123"))
                .thenReturn(Optional.of(testLobby));
        when(wordService.getWordsForGame())
                .thenCallRealMethod();  // use real word list
        when(lobbyRepository.save(any(Lobby.class)))
                .thenReturn(testLobby);

        // Act
        Game game = gameService.startGame("ABC123");

        // Assert
        assertNotNull(game);
        assertNotNull(game.getBoard());
        assertEquals(25, game.getBoard().getCards().size());
        assertEquals(GameStatus.ACTIVE, game.getStatus());
        assertEquals(TeamColor.RED, game.getCurrentTurn());

        // Verify board has correct card type distribution
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

        // Verify all cards start unrevealed
        assertTrue(game.getBoard().getCards().stream()
                .noneMatch(WordCard::isRevealed));

        // Verify save was called
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    public void startGame_lobbyNotFound_throwsException() {
        // Arrange
        when(lobbyRepository.findByLobbyCode("INVALID"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            gameService.startGame("INVALID");
        });
    }

    @Test
    public void startGame_gameAlreadyActive_throwsConflict() {
        // Arrange
        Game existingGame = new Game();
        existingGame.setStatus(GameStatus.ACTIVE);
        testLobby.setGame(existingGame);

        when(lobbyRepository.findByLobbyCode("ABC123"))
                .thenReturn(Optional.of(testLobby));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> {
                    gameService.startGame("ABC123");
                });

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    public void getBoard_asSpy_hidesCardTypes() {
        // Arrange — create a game with known cards
        Game game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(TeamColor.RED);

        Board board = new Board();
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(false);
        board.setCards(java.util.List.of(card));
        game.setBoard(board);
        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode("ABC123"))
                .thenReturn(Optional.of(testLobby));

        // Act
        GameBoardDTO dto = gameService.getBoard("ABC123", Role.SPYMASTER);

        // Assert — spy should NOT see card type
        assertNull(dto.getCards().get(0).getCardType());
        assertNull(dto.getKeyCard());
    }

    @Test
    public void getBoard_asSpymaster_showsCardTypesAndKeyCard() {
        // Arrange
        Game game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(TeamColor.RED);

        Board board = new Board();
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(false);
        board.setCards(java.util.List.of(card));
        game.setBoard(board);
        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode("ABC123"))
                .thenReturn(Optional.of(testLobby));

        // Act
        GameBoardDTO dto = gameService.getBoard("ABC123", Role.SPYMASTER);

        // Assert — spymaster sees everything
        assertEquals(CardType.AGENTRED, dto.getCards().get(0).getCardType());
        assertNotNull(dto.getKeyCard());
        assertEquals(CardType.AGENTRED, dto.getKeyCard().get(0));
    }

    @Test
    public void getBoard_revealedCard_visibleToAllRoles() {
        // Arrange
        Game game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(TeamColor.RED);

        Board board = new Board();
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(true);  // this card has been guessed
        board.setCards(java.util.List.of(card));
        game.setBoard(board);
        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode("ABC123"))
                .thenReturn(Optional.of(testLobby));

        // Act — even a spy can see revealed cards
        GameBoardDTO dto = gameService.getBoard("ABC123", Role.SPYMASTER);

        // Assert
        assertEquals(CardType.AGENTRED, dto.getCards().get(0).getCardType());
    }
}