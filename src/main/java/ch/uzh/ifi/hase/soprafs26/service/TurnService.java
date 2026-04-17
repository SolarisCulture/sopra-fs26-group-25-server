package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.EventType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class TurnService {
    private final LobbyRepository lobbyRepository;
    private final GameService gameService;
    private final GameWebSocketHandler gameWebSocketHandler;
    private final TurnRepository turnRepository;

    public TurnService(LobbyRepository lobbyRepository, GameService gameService, GameWebSocketHandler gameWebSocketHandler, TurnRepository turnRepository) {
        this.lobbyRepository = lobbyRepository;
        this.turnRepository = turnRepository;
        this.gameService = gameService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    public void submitClue(String lobbyCode, ClueDTO clueDTO) {
        Game game = getActiveGame(lobbyCode);
        Turn turn = getCurrentTurn(game, TurnPhase.SPYMASTER_TURN);

        Clue clue = new Clue();
        if (clueDTO.getWord() != null  && !clueDTO.getWord().isEmpty()) {
            clue.setWord(clueDTO.getWord());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Clue word is missing");
        }
        if (clueDTO.getCount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Count should be positive!");
        } else {
            clue.setCount(clueDTO.getCount());
        }
        turn.setClue(clue);

        turn.setGuessesRemaining(clue.getCount() + 1);
        turn.setPhase(TurnPhase.SPY_TURN);
        turn.setStartTime(LocalDateTime.now());

        turnRepository.save(turn);

        //  Broadcast updated game state
        GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
        gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.CLUE_GIVEN, spymasterView, spyView);
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
            } else {
                game.setBlueScore(game.getBlueScore() + 1);
            }
            turnEnded = true;
        }

        if (turnEnded && game.getStatus() != GameStatus.FINISHED) {
            endTurn(lobbyCode);
            return;
        }

        turnRepository.save(turn);

        //  Broadcast updated game state
        GameBoardDTO spymasterView = gameService.buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = gameService.buildBoardDTO(game, Role.SPY);
        EventType eventType = game.getStatus() == GameStatus.FINISHED ? EventType.GAME_OVER : EventType.CARD_REVEALED;
        gameWebSocketHandler.broadcastGameState(lobbyCode, eventType, spymasterView, spyView);
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
        TeamColor nextTeam = (currentTurn.getCurrentTeamColor() == TeamColor.RED) ? TeamColor.BLUE : TeamColor.RED;

        // Create a new Turn for that team
        Turn nextTurn = new Turn();
        nextTurn.setGame(game);
        nextTurn.setCurrentTeamColor(nextTeam);
        nextTurn.setPhase(TurnPhase.SPYMASTER_TURN);
        nextTurn.setGuesses(new ArrayList<>());
        nextTurn.setGuessesRemaining(0);
        nextTurn.setStartTime(LocalDateTime.now());

        // Set it as the current turn on the game
        turnRepository.save(nextTurn);
        game.getTurns().add(nextTurn);
        game.setCurrentTurn(nextTurn);

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
        if (turn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active turn!");
        }
        if (turn.getPhase() != expectedPhase) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected phase " + expectedPhase + " but current phase is " + turn.getPhase());
        }
        return turn;
    }


}
