package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.constant.TurnPhase;
import ch.uzh.ifi.hase.soprafs26.entity.Clue;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Guess;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TurnRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

@Service
@Transactional
public class TurnService {
    private final LobbyRepository lobbyRepository;
    private final GameService gameService;
    private final GameWebSocketHandler gameWebSocketHandler;
    private final TurnRepository turnRepository;
    private final TimerService timerService;

    public TurnService(LobbyRepository lobbyRepository, GameService gameService, GameWebSocketHandler gameWebSocketHandler, TurnRepository turnRepository, @Lazy TimerService timerService) {
        this.lobbyRepository = lobbyRepository;
        this.turnRepository = turnRepository;
        this.gameService = gameService;
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.timerService = timerService;
    }

    public void submitClue(String lobbyCode, ClueDTO clueDTO) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPYMASTER_TURN);
        Clue clue = getClue(clueDTO);

        turn.setClue(clue);

        boolean unlimited = clueDTO.getCount() == 0;
        turn.setGuessesRemaining(unlimited ? Integer.MAX_VALUE : clue.getCount() + 1);

        turn.setPhase(TurnPhase.SPY_TURN);
        turn.setStartTime(LocalDateTime.now());

        turnRepository.saveAndFlush(turn);

        restartTimerForNextPhase(lobbyCode, getSpyTimeLimit(game));

        //  Broadcast updated game state
        GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);

        spymasterView.setClueWord(clue.getWord());
        spymasterView.setClueCount(clue.getCount());
        spyView.setClueWord(clue.getWord());
        spyView.setClueCount(clue.getCount());

        gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.CLUE_GIVEN, spymasterView, spyView);
    }

    private static @NonNull Clue getClue(ClueDTO clueDTO) {
        Clue clue = new Clue();
        if (clueDTO.getWord() != null  && !clueDTO.getWord().isEmpty()) {
            clue.setWord(clueDTO.getWord());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Clue word is missing");
        }
        if (clueDTO.getCount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Count should be positive or zero!");
        }
        if (clueDTO.getCount() == 0) {
            clue.setCount(25);  // set as amount of words on the board
        }
        else {
            clue.setCount(clueDTO.getCount());
        }

        clue.setDescription("Clue: " + clueDTO.getWord() + " (" + clueDTO.getCount() + ")");
        return clue;
    }

    public void submitGuess(String lobbyCode, GuessDTO guessDTO) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPY_TURN);

        // Find the card
        WordCard wordCard = game.getBoard().findCardByWord(guessDTO.getWord());
        if (wordCard == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "WordCard with word" + guessDTO.getWord() + " not exist!");
        }
        if (wordCard.isRevealed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card already revealed!");
        }

        // Reveal the card
        wordCard.setRevealed(true);

        Guess guess = new Guess();
        guess.setWordCard(wordCard);
        //guess.setType(GameEventType.GUESS);
        //guess.setTimeStamp(LocalDateTime.now());
        guess.setDescription("Guessed: " + wordCard.getWord());
        turn.getGuesses().add(guess);
        turn.setGuessesRemaining(turn.getGuessesRemaining()- 1);

        TeamColor team = turn.getCurrentTeamColor();
        CardType cardType = wordCard.getCardType();

        boolean turnEnded = false;

        if (cardType == CardType.ASSASSIN) {
            // Other team wins
            TeamColor winner = (team == TeamColor.RED) ? TeamColor.BLUE : TeamColor.RED;
            game.setWinningTeam(winner);
            game.setStatus(GameStatus.FINISHED);
            timerService.stopTimer(lobbyCode);
        } else if (cardType == CardType.CIVILIAN) {
            turnEnded = true;
        } else if ((cardType == CardType.AGENTRED && team == TeamColor.RED) ||
                (cardType == CardType.AGENTBLUE && team == TeamColor.BLUE)) {
            // Correct guess
            if (cardType == CardType.AGENTRED) {
                game.setRedScore(game.getRedScore() + 1);
                if (game.getRedTotal() == game.getRedScore()) {
                    game.setWinningTeam(TeamColor.RED);
                    game.setStatus(GameStatus.FINISHED);
                }
            } else {
                game.setBlueScore(game.getBlueScore() + 1);
                if (game.getBlueScore() == game.getBlueTotal()) {
                    game.setWinningTeam(TeamColor.BLUE);
                    game.setStatus(GameStatus.FINISHED);
                }
            }

            if (turn.getGuessesRemaining() == 0) {
                turnEnded = true;
            }

        } else {
            // Wrong team's card — give them the point, end turn
            if (cardType == CardType.AGENTRED) {
                game.setRedScore(game.getRedScore() + 1);
                turnEnded = true;
                if (game.getRedScore() == game.getRedTotal()) {
                    game.setWinningTeam(TeamColor.RED);
                    game.setStatus(GameStatus.FINISHED);
                }
            } else {
                game.setBlueScore(game.getBlueScore() + 1);
                turnEnded = true;
                if (game.getBlueScore() == game.getBlueTotal()) {
                    game.setWinningTeam(TeamColor.BLUE);
                    game.setStatus(GameStatus.FINISHED);
                }
            }
        }

        if (game.getStatus() == GameStatus.FINISHED) {
            timerService.stopTimer(lobbyCode);
            gameService.calculateGameStatistics(lobbyCode);
            GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
            GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
            gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.GAME_OVER, spymasterView, spyView);
            return;
        }

        if (turnEnded) {
            GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
            GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
            gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.CARD_REVEALED, spymasterView, spyView);
            endTurn(lobbyCode);
            return;
        }

        /*if (turnEnded && game.getStatus() != GameStatus.FINISHED) {
            GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
            GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
            gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.CARD_REVEALED, spymasterView, spyView);
            endTurn(lobbyCode);
            return;
        }*/

        turnRepository.saveAndFlush(turn);

        //  Broadcast updated game state
        GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
        //EventType eventType = game.getStatus() == GameStatus.FINISHED ? EventType.GAME_OVER : EventType.CARD_REVEALED;
        gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.CARD_REVEALED, spymasterView, spyView);
    }

    public void submitReportedGuess(String lobbyCode, GuessDTO guessDTO) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPY_TURN);

        if (turn.getClue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No reported clue to penalize");
        }

        WordCard wordCard = game.getBoard().findCardByWord(guessDTO.getWord());
        if (wordCard == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "WordCard with word" + guessDTO.getWord() + " not exist!");
        }
        if (wordCard.isRevealed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card already revealed!");
        }

        TeamColor reportedTeam = turn.getCurrentTeamColor();
        TeamColor penaltyTeam = getOtherTeam(reportedTeam);
        CardType expectedPenaltyCard = getAgentCardType(penaltyTeam);

        if (wordCard.getCardType() != expectedPenaltyCard) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Penalty reveal must be one of the opposing spymaster's own cards");
        }

        wordCard.setRevealed(true);
        scoreTeamCard(game, penaltyTeam);

        Guess guess = new Guess();
        guess.setWordCard(wordCard);
        guess.setDescription("Penalty revealed: " + wordCard.getWord());
        turn.getGuesses().add(guess);

        if (hasTeamWon(game, penaltyTeam)) {
            game.setWinningTeam(penaltyTeam);
            game.setStatus(GameStatus.FINISHED);
            timerService.stopTimer(lobbyCode);
            gameService.calculateGameStatistics(lobbyCode);
            GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
            GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
            gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.GAME_OVER, spymasterView, spyView);
            return;
        }

        turn.setClue(null);
        turn.setGuessesRemaining(0);
        turn.setClueUnderReview(false);
        turn.setInvalidCluePenaltyPending(false);
        turn.setPhase(TurnPhase.SPYMASTER_TURN);
        turn.setCurrentTeamColor(penaltyTeam);
        turn.setStartTime(LocalDateTime.now());

        turnRepository.saveAndFlush(turn);

        restartTimerForNextPhase(lobbyCode, getSpymasterTimeLimit(game));

        GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
        gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.CARD_REVEALED, spymasterView, spyView);
    }

    public void markClueRuledInvalid(String lobbyCode) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPY_TURN);

        if (turn.getClue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No clue to rule invalid");
        }

        turn.setClueUnderReview(false);
        turn.setInvalidCluePenaltyPending(true);
        turnRepository.saveAndFlush(turn);
        timerService.stopTimer(lobbyCode);
    }

    public void reportClue(String lobbyCode) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPY_TURN);

        if (turn.getClue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No clue to report");
        }

        turn.setClueUnderReview(true);
        turn.setInvalidCluePenaltyPending(false);
        turnRepository.saveAndFlush(turn);
    }

    public void approveReportedClue(String lobbyCode) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPY_TURN);

        if (turn.getClue() == null || !turn.isClueUnderReview()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No reported clue to approve");
        }

        turn.setClueUnderReview(false);
        turn.setInvalidCluePenaltyPending(false);
        turnRepository.saveAndFlush(turn);
    }

    public void endTurn(String lobbyCode) {
        endTurn(lobbyCode, false);
    }

    public void endTurn(String lobbyCode, boolean voluntary) {

        Game game = getActiveGame(lobbyCode);
        Turn currentTurn = game.getCurrentTurn();

        if (voluntary && currentTurn.getPhase() != TurnPhase.SPY_TURN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only end turn during guessing phase");
        }

        // Figure out the other team
        TeamColor nextTeam = getOtherTeam(currentTurn.getCurrentTeamColor());

        // Create a new Turn for that team
        Turn nextTurn = new Turn();
        nextTurn.setGame(game);
        nextTurn.setCurrentTeamColor(nextTeam);
        nextTurn.setPhase(TurnPhase.SPYMASTER_TURN);
        nextTurn.setGuesses(new ArrayList<>());
        nextTurn.setGuessesRemaining(0);
        nextTurn.setStartTime(LocalDateTime.now());

        // Set it as the current turn on the game
        turnRepository.saveAndFlush(nextTurn);
        game.getTurns().add(nextTurn);
        game.setCurrentTurn(nextTurn);

        restartTimerForNextPhase(lobbyCode, getSpymasterTimeLimit(game));

        // Save and broadcast
        GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
        gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.TURN_CHANGED, spymasterView, spyView);

    }

    private Lobby findLobbyByCode(String lobbyCode) {
        // 1. Find the lobby
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        return lobbyOptional.get();
    }

    private Game getActiveGame(String lobbyCode) {
        Lobby lobby = findLobbyByCode(lobbyCode);
        if (lobby.getGame() == null || lobby.getGame().getStatus() != GameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is not active!");
        }
        return lobby.getGame();
    }

    private Turn getCurrentTurn(Game game, TurnPhase expectedPhase) {
        Turn turn = game.getCurrentTurn();
        //System.out.println("GUESS DEBUG - Turn ID: " + turn.getId() + " Phase: " + turn.getPhase());
        if (turn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active turn!");
        }
        if (turn.getPhase() != expectedPhase) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected phase " + expectedPhase + " but current phase is " + turn.getPhase());
        }
        return turn;
    }

    private TeamColor getOtherTeam(TeamColor team) {
        return team == TeamColor.RED ? TeamColor.BLUE : TeamColor.RED;
    }

    private Integer getSpyTimeLimit(Game game) {
        if (game.getLobby() == null || game.getLobby().getSettings() == null) {
            return null;
        }
        return game.getLobby().getSettings().getSpyTimeLimit();
    }

    private Integer getSpymasterTimeLimit(Game game) {
        if (game.getLobby() == null || game.getLobby().getSettings() == null) {
            return null;
        }
        return game.getLobby().getSettings().getSpymasterTimeLimit();
    }

    private void restartTimerForNextPhase(String lobbyCode, Integer timeLimit) {
        timerService.stopTimer(lobbyCode);
        if (timeLimit != null && timeLimit > 0) {
            timerService.startTimer(lobbyCode, timeLimit);
        }
    }

    private CardType getAgentCardType(TeamColor team) {
        return team == TeamColor.RED ? CardType.AGENTRED : CardType.AGENTBLUE;
    }

    private void scoreTeamCard(Game game, TeamColor team) {
        if (team == TeamColor.RED) {
            game.setRedScore(game.getRedScore() + 1);
        } else {
            game.setBlueScore(game.getBlueScore() + 1);
        }
    }

    private boolean hasTeamWon(Game game, TeamColor team) {
        if (team == TeamColor.RED) {
            return game.getRedScore() == game.getRedTotal();
        }
        return game.getBlueScore() == game.getBlueTotal();
    }

}
