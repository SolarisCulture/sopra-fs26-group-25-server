package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TurnRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStatisticsDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {
    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private WordService wordService;

    @Mock
    private GameWebSocketHandler gameWebSocketHandler;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TurnRepository turnRepository;

    @Spy
    @InjectMocks
    private GameService gameService;

    private Lobby testLobby;

    @BeforeEach
    public void setup() {
        testLobby = new Lobby();
        testLobby.setId(1L);
        testLobby.setLobbyCode("ABC123");
        testLobby.setHostId(1L);

    }

    @Test
    public void startGame_validLobby_createsGameWith25Cards() {
        // Tell the mocks what to return
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));
        when(wordService.getWordsForGame(testLobby.getSettings().getDifficulty())).thenCallRealMethod();  // use real word list
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(testLobby);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game g = invocation.getArgument(0);
            g.setId(1L);  // simulate DB auto-generated ID
            return g;
        });
        when(turnRepository.save(any(Turn.class))).thenAnswer(invocation -> {
           Turn t = invocation.getArgument(0);
           t.setId(1L);
           return t;
        });

        // Call the real method we're testing
        Game game = gameService.startGame("ABC123");

        // Check the game was created correctly
        assertNotNull(game);
        assertNotNull(game.getBoard());
        assertEquals(25, game.getBoard().getCards().size());
        assertEquals(GameStatus.ACTIVE, game.getStatus());
        assertEquals(TurnPhase.SPYMASTER_TURN, game.getCurrentTurn().getPhase());

        // Verify board has correct card type distribution
        // `.stream().filter().count()` goes through all 25 cards and counts how many are each type
        long redCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.AGENTRED).count();
        long blueCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.AGENTBLUE).count();
        long civilianCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.CIVILIAN).count();
        long assassinCount = game.getBoard().getCards().stream()
                .filter(c -> c.getCardType() == CardType.ASSASSIN).count();

        assertEquals(9, game.getStartingTeam() == TeamColor.RED ? redCount : blueCount);
        assertEquals(7, civilianCount);
        assertEquals(1, assassinCount);

        // Verify all cards start unrevealed
        assertTrue(game.getBoard().getCards().stream().noneMatch(WordCard::isRevealed));

        // Verify save was called
        verify(lobbyRepository).save(any(Lobby.class));
        verify(gameRepository).save(any(Game.class));
        verify(turnRepository).save(any(Turn.class));
    }

    @Test
    public void startGame_lobbyNotFound_throwsException() {
        // Set up
        when(lobbyRepository.findByLobbyCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> { gameService.startGame("INVALID"); });
    }

    @Test
    public void startGame_gameAlreadyActive_throwsConflict() {
        // Set up
        Game existingGame = new Game();
        existingGame.setStatus(GameStatus.ACTIVE);
        testLobby.setGame(existingGame);

        // Tell the mocks what to return
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> {
                    gameService.startGame("ABC123");
                });

        assertEquals(409, exception.getStatusCode().value()); // HTTP 409 Conflict response status code
    }

    @Test
    public void getBoard_asSpy_hidesCardTypes() {
        // Set up
        Game game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setLobby(testLobby);
        // New
        Turn turn = new Turn();
        turn.setCurrentTeamColor(TeamColor.RED);
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        game.setCurrentTurn(turn);

        Board board = new Board();
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(false);
        board.setCards(java.util.List.of(card));
        game.setBoard(board);
        testLobby.setGame(game);

        // Tell the mocks what to return
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        GameBoardDTO dto = gameService.getBoard("ABC123", Role.SPY);

        // spy should NOT see card type
        assertNull(dto.getCards().get(0).getCardType());
        assertNull(dto.getKeyCard());
    }

    @Test
    public void getBoard_asSpymaster_showsCardTypesAndKeyCard() {
        // Set up
        Game game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setLobby(testLobby);
        // New
        Turn turn = new Turn();
        turn.setCurrentTeamColor(TeamColor.RED);
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        game.setCurrentTurn(turn);

        Board board = new Board();
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(false);
        board.setCards(java.util.List.of(card));
        game.setBoard(board);
        testLobby.setGame(game);

        // Tell the mocks what to return
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        GameBoardDTO dto = gameService.getBoard("ABC123", Role.SPYMASTER);

        // spymaster sees everything
        assertEquals(CardType.AGENTRED, dto.getCards().get(0).getCardType());
        assertNotNull(dto.getKeyCard());
        assertEquals(CardType.AGENTRED, dto.getKeyCard().get(0));
    }

    @Test
    public void getBoard_revealedCard_visibleToAllRoles() {
        // Set up
        Game game = new Game();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setLobby(testLobby);
        // New
        Turn turn = new Turn();
        turn.setCurrentTeamColor(TeamColor.RED);
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        game.setCurrentTurn(turn);

        Board board = new Board();
        WordCard card = new WordCard();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(true);  // this card has been guessed
        board.setCards(java.util.List.of(card));
        game.setBoard(board);
        testLobby.setGame(game);

        // Tell the mocks what to return
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        //  spy and spymaster can see revealed cards
        GameBoardDTO dtoSpymaster = gameService.getBoard("ABC123", Role.SPYMASTER);
        GameBoardDTO dtoSpy = gameService.getBoard("ABC123", Role.SPY);

        assertEquals(CardType.AGENTRED, dtoSpymaster.getCards().get(0).getCardType());
        assertEquals(CardType.AGENTRED, dtoSpy.getCards().get(0).getCardType());
    }

    @Test
    public void calculateGameStatistics_validFinishedGame_setsStatisticsCorrectly() {
        Game game = new Game();
        game.setStatus(GameStatus.FINISHED);
        game.setCurrentRound(5);

        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        gameService.calculateGameStatistics(testLobby.getLobbyCode());

        assertEquals(5, game.getRoundsPlayed());
        assertEquals(0, game.getTotalTime());
    }

    @Test
    public void calculateGameStatistics_gameNotFinished_throwsException() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertThrows(ResponseStatusException.class, () -> {gameService.calculateGameStatistics(testLobby.getLobbyCode());});
    }

    @Test
    public void getGameStatistics_validFinishedGame_returnsDTO() {
        Game game = new Game();
        game.setStatus(GameStatus.FINISHED);
        game.setBlueScore(5);
        game.setRedScore(9);
        game.setRoundsPlayed(7);
        game.setTotalTime(100);
        game.setWinningTeam(TeamColor.RED);

        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        GameStatisticsDTO result = gameService.getGameStatistics(testLobby.getLobbyCode());

        assertEquals(5, result.getBlueScore());
        assertEquals(9, result.getRedScore());
        assertEquals(7, result.getRoundsPlayed());
        assertEquals(100, result.getTotalTime());
        assertEquals(TeamColor.RED, result.getWinningTeam());
    }

    @Test
    public void getGameStatistics_gameNotFinished_throwsException() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertThrows(ResponseStatusException.class, () -> {gameService.getGameStatistics(testLobby.getLobbyCode());});
    }

    @Test
    public void restartGame_validFinishedGame_recreatesGame() {

        Game finishedGame = new Game();
        finishedGame.setStatus(GameStatus.FINISHED);
        testLobby.setGame(finishedGame);

        when(lobbyRepository.findByLobbyCode(any()))
            .thenReturn(Optional.of(testLobby));

        // Mock the words for the game (else IndexOutOfBounds)
        when(wordService.getWordsForGame(testLobby.getSettings().getDifficulty())).thenReturn(IntStream.range(0, 25).mapToObj(i -> "word" + i).toList());

        Game result = gameService.restartGame(testLobby.getLobbyCode());

        assertNotNull(result);
        assertEquals(GameStatus.ACTIVE, result.getStatus());
    }

    @Test
    public void restartGame_gameNotFinished_throwsException() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        testLobby.setGame(game);

        when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertThrows(ResponseStatusException.class, () -> {gameService.restartGame(testLobby.getLobbyCode());});
    }

    @Test
    public void backToLobby_validFinishedGame_updatesLobbyAndGame() {
        Game game = new Game();
        game.setStatus(GameStatus.FINISHED);
        testLobby.setGame(game);

        Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        gameService.backToLobby(testLobby.getLobbyCode());

        assertEquals(GameStatus.ARCHIVED, game.getStatus());
        assertEquals(LobbyStatus.WAITING, testLobby.getLobbyStatus());
        Mockito.verify(lobbyRepository, Mockito.times(1)).save(testLobby);
    }

    @Test
    public void backToLobby_gameNotFinished_throwsException() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        testLobby.setGame(game);

        Mockito.when(lobbyRepository.findByLobbyCode(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertThrows(ResponseStatusException.class, () -> {gameService.backToLobby(testLobby.getLobbyCode());});
    }

    // ==================== regenerateBoard tests ====================

    @Test
    public void regenerateBoard_validSpymaster_success() {
        // Setup
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);

        Turn turn = new Turn();
        turn.setClue(null); // no clue given yet
        turn.setCurrentTeamColor(TeamColor.RED);
        game.setCurrentTurn(turn);

        Board board = new Board();
        List<WordCard> cards = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            WordCard card = new WordCard();
            card.setWord("WORD" + i);
            card.setCardType(CardType.CIVILIAN);
            card.setRevealed(false);
            cards.add(card);
        }
        board.setCards(cards);
        game.setBoard(board);

        Player spymaster = new Player("testPlayer");
        spymaster.setId(1L);
        spymaster.setRole(Role.SPYMASTER);

        testLobby.setGame(game);
        game.setLobby(testLobby);
        testLobby.setPlayerList(new ArrayList<>(List.of(spymaster)));

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        List<String> testWords = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            testWords.add("NEWWORD" + i);
        }
        when(wordService.getWordsForGame(testLobby.getSettings().getDifficulty())).thenReturn(testWords);

        gameService.regenerateBoard("ABC123", 1L);

        // Board still has 25 cards
        assertEquals(25, game.getBoard().getCards().size());
        // All cards unrevealed
        assertTrue(game.getBoard().getCards().stream().noneMatch(WordCard::isRevealed));
        // Starting team is set
        assertNotNull(game.getStartingTeam());
        verify(gameRepository).save(game);
    }

    @Test
    public void regenerateBoard_notSpymaster_throwsForbidden() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        Turn turn = new Turn();
        turn.setClue(null);
        game.setCurrentTurn(turn);

        Player spy = new Player("testPlayer");
        spy.setId(1L);
        spy.setRole(Role.SPY);

        testLobby.setGame(game);
        game.setLobby(testLobby);
        testLobby.setPlayerList(new ArrayList<>(List.of(spy)));

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.regenerateBoard("ABC123", 1L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    public void regenerateBoard_clueAlreadyGiven_throwsBadRequest() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        Turn turn = new Turn();
        Clue clue = new Clue();
        clue.setWord("animal");
        turn.setClue(clue); // clue already given
        game.setCurrentTurn(turn);

        Player spymaster = new Player("testPlayer");
        spymaster.setId(1L);
        spymaster.setRole(Role.SPYMASTER);

        testLobby.setGame(game);
        game.setLobby(testLobby);
        testLobby.setPlayerList(new ArrayList<>(List.of(spymaster)));

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.regenerateBoard("ABC123", 1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void regenerateBoard_gameNotActive_throwsBadRequest() {
        Game game = new Game();
        game.setStatus(GameStatus.FINISHED);

        testLobby.setGame(game);

        Player spymaster = new Player("testPlayer");
        spymaster.setId(1L);
        spymaster.setRole(Role.SPYMASTER);
        testLobby.setPlayerList(new ArrayList<>(List.of(spymaster)));

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.regenerateBoard("ABC123", 1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void regenerateBoard_playerNotFound_throwsNotFound() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);

        testLobby.setGame(game);
        testLobby.setPlayerList(new ArrayList<>());

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.regenerateBoard("ABC123", 999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}