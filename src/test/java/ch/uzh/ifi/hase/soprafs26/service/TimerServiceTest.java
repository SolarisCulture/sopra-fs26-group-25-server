package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbySettings;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

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

    @Test
    public void checkTimers_timeExpired_endsTurn() {
        Turn turn = new Turn();
        turn.setStartTime(LocalDateTime.now().minusSeconds(200));
        turn.setPhase(TurnPhase.SPYMASTER_TURN);

        LobbySettings settings = new LobbySettings();
        settings.setSpymasterTimeLimit(120);

        Lobby lobby = new Lobby();
        lobby.setLobbyCode("ABC123");
        lobby.setSettings(settings);

        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(turn);
        game.setLobby(lobby);

        when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(List.of(game));

        timerService.checkTimers();

        verify(gameWebSocketHandler).broadcastTimer("ABC123", 0L);
        verify(turnService).endTurn("ABC123");
    }

    @Test
    public void checkTimers_timeNotExpired_doesNothing() {
        Turn turn = new Turn();
        turn.setStartTime(LocalDateTime.now().minusSeconds(30));
        turn.setPhase(TurnPhase.SPY_TURN);

        LobbySettings settings = new LobbySettings();
        settings.setSpyTimeLimit(120);

        Lobby lobby = new Lobby();
        lobby.setLobbyCode("ABC123");
        lobby.setSettings(settings);

        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(turn);
        game.setLobby(lobby);

        when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(List.of(game));

        timerService.checkTimers();

        verify(gameWebSocketHandler).broadcastTimer(eq("ABC123"), longThat(timer -> timer > 0 && timer <= 90));
        verify(turnService, never()).endTurn(any());
    }

    @Test
    public void checkTimers_noActiveGames_doesNothing() {
        when(gameRepository.findByStatus(GameStatus.ACTIVE)).thenReturn(List.of());

        timerService.checkTimers();

        verify(turnService, never()).endTurn(any());
    }

}
