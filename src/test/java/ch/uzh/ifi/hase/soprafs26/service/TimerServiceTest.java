package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbySettings;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimerServiceTest {
    @Mock
    private GameRepository gameRepository;
    @Mock
    private TurnService turnService;
    @Mock
    private GameWebSocketHandler gameWebSocketHandler;

    @InjectMocks
    private TimerService timerService;

    @BeforeEach
    void setup() { // Clear timers before each test
        timerService.stopTimer("ABC123");
    }

    @Test
    public void checkTimers_timeExpired_endsTurn() { 
        // Manually start timer (without DB)
        timerService.startTimer("ABC123", 1); // 1 second
        timerService.startTimer("ABC123", 1);

        try {
            Thread.sleep(1500); // Let timer expire milliseconds
        } catch ( InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        timerService.checkTimers();

        verify(turnService, atLeastOnce()).endTurn("ABC123");
    }

    @Test
    public void checkTimers_timeNotExpired_doesNothing() { 
        timerService.startTimer("ABC123", 120); // 2 minutes

        timerService.checkTimers();

        // Verify turn wasn't ended
        verify(turnService, never()).endTurn("ABC123");
    }

    @Test
    public void checkTimers_noActiveGames_doesNothing() { // TODO
        // No timers started => activeTimers is empty
        timerService.checkTimers();

        // Verify nothing happened
        verify(turnService, never()).endTurn(any());
        verify(gameWebSocketHandler, never()).broadcastTimer(any(), anyLong());
    }

    @Test
    public void pauseTimer_pausesActiveTimerAndGame() throws InterruptedException {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        when(gameRepository.findByLobbyLobbyCode("ABC123")).thenReturn(Optional.of(game));
        timerService.startTimer("ABC123", 1);

        timerService.pauseTimer("ABC123", true);
        Thread.sleep(1200);
        timerService.checkTimers();

        assertEquals(GameStatus.PAUSE, game.getStatus());
        verify(gameRepository).save(game);
        verify(gameWebSocketHandler).broadcastGamePaused("ABC123");
        verify(turnService, never()).endTurn("ABC123");
    }

    @Test
    public void pauseTimer_resumeUnfreezesRemainingTime() throws InterruptedException {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        when(gameRepository.findByLobbyLobbyCode("ABC123")).thenReturn(Optional.of(game));
        timerService.startTimer("ABC123", 3);

        timerService.pauseTimer("ABC123", true);
        long remainingAfterPause = timerService.getRemainingSeconds("ABC123");
        Thread.sleep(1200);
        assertEquals(remainingAfterPause, timerService.getRemainingSeconds("ABC123"));

        timerService.pauseTimer("ABC123", false);

        assertEquals(GameStatus.ACTIVE, game.getStatus());
        verify(gameRepository, times(2)).save(game);
        verify(gameWebSocketHandler).broadcastGameResumed("ABC123");
    }

}
