package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TurnRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

@ExtendWith(MockitoExtension.class)
public class TurnServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;
    @Mock
    private GameService gameService;
    @Mock
    private GameWebSocketHandler gameWebSocketHandler;
    @Mock
    private TurnRepository turnRepository;

    @InjectMocks
    private TurnService turnService;

    private Lobby testLobby;
    private Game testGame;
    private Turn testTurn;

    @BeforeEach
    public void setup() {
        testTurn = new Turn();
        testTurn.setPhase(TurnPhase.SPYMASTER_TURN);

        testGame = new Game();
        testGame.setStatus(GameStatus.ACTIVE);
        testGame.setCurrentTurn(testTurn);

        testLobby = new Lobby();
        testLobby.setLobbyCode("ABC123");
        testLobby.setGame(testGame);
    }

    // CLUE
    @Test
    public void submitClue_validClue_success() {
        // Setup
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));
        //when(turnRepository.save(any(Turn.class))).thenReturn(testTurn);
        when(gameService.buildBoardDTO(any(Game.class), eq(Role.SPYMASTER))).thenReturn(new GameBoardDTO());
        when(gameService.buildBoardDTO(any(Game.class), eq(Role.SPY))).thenReturn(new GameBoardDTO());

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("animal");
        clueDTO.setCount(3);

        // Act
        turnService.submitClue("ABC123", clueDTO);

        // Assert
        assertEquals(TurnPhase.SPY_TURN, testTurn.getPhase());
        assertEquals(4, testTurn.getGuessesRemaining()); // count + 1
        assertNotNull(testTurn.getClue());
        assertEquals("animal", testTurn.getClue().getWord());
        assertEquals(3, testTurn.getClue().getCount());
        assertNotNull(testTurn.getStartTime());

        // Verify saves and broadcasts happened
        verify(turnRepository).saveAndFlush(testTurn);
        verify(gameWebSocketHandler).broadcastGameState(
                eq("ABC123"), any(), any(GameBoardDTO.class), any(GameBoardDTO.class));
    }

    @Test
    public void submitClue_emptyWord_throwsBadRequest() {
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("");
        clueDTO.setCount(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> turnService.submitClue("ABC123", clueDTO));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(turnRepository, never()).save(any());
    }

    @Test
    public void submitClue_nullWord_throwsBadRequest() {
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord(null);
        clueDTO.setCount(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> turnService.submitClue("ABC123", clueDTO));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(turnRepository, never()).save(any());
    }

    @Test
    public void submitClue_negativeCount_throwsBadRequest() {
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("animal");
        clueDTO.setCount(-1);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> turnService.submitClue("ABC123", clueDTO));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(turnRepository, never()).save(any());
    }

    @Test
    public void submitClue_gameNotActive_throwsBadRequest() {
        testGame.setStatus(GameStatus.FINISHED);
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("animal");
        clueDTO.setCount(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> turnService.submitClue("ABC123", clueDTO));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(turnRepository, never()).save(any());
    }

    @Test
    public void submitClue_wrongPhase_throwsBadRequest() {
        testTurn.setPhase(TurnPhase.SPY_TURN);
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("animal");
        clueDTO.setCount(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> turnService.submitClue("ABC123", clueDTO));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(turnRepository, never()).save(any());
    }

    @Test
    public void submitClue_lobbyNotFound_throwsNotFound() {
        when(lobbyRepository.findByLobbyCode("INVALID")).thenReturn(Optional.empty());

        ClueDTO clueDTO = new ClueDTO();
        clueDTO.setWord("animal");
        clueDTO.setCount(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> turnService.submitClue("INVALID", clueDTO));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(turnRepository, never()).save(any());
    }

    // GUESS
    private WordCard setupGuessTest(CardType cardType, String word) {
        testTurn.setPhase(TurnPhase.SPY_TURN);
        testTurn.setCurrentTeamColor(TeamColor.RED);
        testTurn.setGuessesRemaining(3);
        testTurn.setGuesses(new ArrayList<>());

        WordCard testCard = new WordCard();
        testCard.setWord(word);
        testCard.setCardType(cardType);
        testCard.setRevealed(false);

        Board testBoard = new Board();
        testBoard.setCards(new ArrayList<>(List.of(testCard)));
        testGame.setBoard(testBoard);
        testGame.setRedScore(0);
        testGame.setBlueScore(0);
        testGame.setRedTotal(9);
        testGame.setBlueTotal(8);
        testGame.setTurns(new ArrayList<>(List.of(testTurn)));
        testGame.setLobby(testLobby);
        testLobby.setPlayerList(new ArrayList<>());

        LobbySettings settings = new LobbySettings();
        settings.setTimeLimit(120);
        testLobby.setSettings(settings);

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));
        // lenient() tells Mockito: "don't complain if this mock isn't used." (happen in tests with exceptions)
        lenient().when(turnRepository.save(any(Turn.class))).thenReturn(testTurn);
        lenient().when(gameService.buildBoardDTO(any(Game.class), eq(Role.SPYMASTER))).thenReturn(new GameBoardDTO());
        lenient().when(gameService.buildBoardDTO(any(Game.class), eq(Role.SPY))).thenReturn(new GameBoardDTO());

        return testCard;
    }

    @Test
    public void submitGuess_correctGuess_scoresPoint() {
        WordCard card = setupGuessTest(CardType.AGENTRED, "APPLE");

        turnService.submitGuess("ABC123", new GuessDTO("APPLE"));

        assertTrue(card.isRevealed());
        assertEquals(1, testGame.getRedScore());
        assertEquals(2, testTurn.getGuessesRemaining());
        assertEquals(GameStatus.ACTIVE, testGame.getStatus());
    }

    @Test
    public void submitGuess_assassin_gameOver() {
        setupGuessTest(CardType.ASSASSIN, "BOMB");

        turnService.submitGuess("ABC123", new GuessDTO("BOMB"));

        assertEquals(GameStatus.FINISHED, testGame.getStatus());
        assertEquals(TeamColor.BLUE, testGame.getWinningTeam());

        verify(gameWebSocketHandler).broadcastGameState(
                eq("ABC123"), eq(EventType.GAME_OVER), any(GameBoardDTO.class), any(GameBoardDTO.class));
    }

    @Test
    public void submitGuess_civilian_endsTurn() {
        setupGuessTest(CardType.CIVILIAN, "TABLE");

        turnService.submitGuess("ABC123", new GuessDTO("TABLE"));

        // endTurn creates a new turn, so verify it was called
        assertTrue(testGame.getBoard().findCardByWord("TABLE").isRevealed());
        assertEquals(2, testTurn.getGuessesRemaining());
        assertEquals(GameStatus.ACTIVE, testGame.getStatus());
    }

    @Test
    public void submitGuess_wrongTeamCard_scoresForOtherTeam() {
        setupGuessTest(CardType.AGENTBLUE, "OCEAN");

        turnService.submitGuess("ABC123", new GuessDTO("OCEAN"));

        assertEquals(1, testGame.getBlueScore());
    }

    @Test
    public void submitGuess_allCardsFound_teamWins() {
        setupGuessTest(CardType.AGENTRED, "APPLE");
        testGame.setRedScore(8);  // one away from winning

        turnService.submitGuess("ABC123", new GuessDTO("APPLE"));

        assertEquals(GameStatus.FINISHED, testGame.getStatus());
        assertEquals(TeamColor.RED, testGame.getWinningTeam());

        verify(gameService).calculateGameStatistics(testLobby.getLobbyCode());
        verify(gameWebSocketHandler).broadcastGameState(
                eq("ABC123"), eq(EventType.GAME_OVER), any(GameBoardDTO.class), any(GameBoardDTO.class));
    }

    @Test
    public void submitGuess_cardNotFound_throwsBadRequest() {
        setupGuessTest(CardType.AGENTRED, "APPLE");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> turnService.submitGuess("ABC123", new GuessDTO("NOTEXIST")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void submitGuess_correctGuess_noGuessesRemaining_endsTurn() {
        setupGuessTest(CardType.AGENTRED, "APPLE");
        testTurn.setGuessesRemaining(1); // last guess

        turnService.submitGuess("ABC123", new GuessDTO("APPLE"));

        assertEquals(1, testGame.getRedScore());
        assertEquals(0, testTurn.getGuessesRemaining());

        verify(gameWebSocketHandler).broadcastGameState(
                eq("ABC123"), eq(EventType.CARD_REVEALED), any(GameBoardDTO.class), any(GameBoardDTO.class));
        verify(gameWebSocketHandler).broadcastGameState(
                eq("ABC123"), eq(EventType.TURN_CHANGED), any(GameBoardDTO.class), any(GameBoardDTO.class));
    }

    @Test
    public void endTurn_voluntary_duringSpyTurn_success() {
        testTurn.setPhase(TurnPhase.SPY_TURN);
        testTurn.setCurrentTeamColor(TeamColor.RED);
        testGame.setTurns(new ArrayList<>(List.of(testTurn)));

        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));
        when(turnRepository.saveAndFlush(any(Turn.class))).thenAnswer(i -> i.getArgument(0));
        when(gameService.buildBoardDTO(any(Game.class), eq(Role.SPYMASTER))).thenReturn(new GameBoardDTO());
        when(gameService.buildBoardDTO(any(Game.class), eq(Role.SPY))).thenReturn(new GameBoardDTO());

        turnService.endTurn("ABC123", true);

        assertEquals(TeamColor.BLUE, testGame.getCurrentTurn().getCurrentTeamColor());
        assertEquals(TurnPhase.SPYMASTER_TURN, testGame.getCurrentTurn().getPhase());
        verify(gameWebSocketHandler).broadcastGameState(
                eq("ABC123"), eq(EventType.TURN_CHANGED), any(GameBoardDTO.class), any(GameBoardDTO.class));
    }

    @Test
    public void endTurn_voluntary_duringSpymasterTurn_throwsBadRequest() {
        testTurn.setPhase(TurnPhase.SPYMASTER_TURN);
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> turnService.endTurn("ABC123", true));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    public void submitGuess_correctGuess_setsGameEventFields() {
        WordCard card = setupGuessTest(CardType.AGENTRED, "APPLE");

        turnService.submitGuess("ABC123", new GuessDTO("APPLE"));

        Guess guess = testTurn.getGuesses().get(0);
        assertNotNull(guess.getType(), "GameEvent type must be set");
        assertNotNull(guess.getTimeStamp(), "GameEvent timeStamp must be set");
        assertNotNull(guess.getDescription(), "GameEvent description must be set");
        assertEquals(GameEventType.GUESS, guess.getType());
    }
}