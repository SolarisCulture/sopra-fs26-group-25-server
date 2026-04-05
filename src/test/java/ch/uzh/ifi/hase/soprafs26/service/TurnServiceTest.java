package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TurnRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @Test
    public void submitClue_validClue_success() {
        // Setup
        when(lobbyRepository.findByLobbyCode("ABC123")).thenReturn(Optional.of(testLobby));
        when(turnRepository.save(any(Turn.class))).thenReturn(testTurn);
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
        verify(turnRepository).save(testTurn);
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
}