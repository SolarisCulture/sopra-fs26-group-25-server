package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.GameHistory;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;
import ch.uzh.ifi.hase.soprafs26.repository.GameHistoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStatisticsDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final WordService wordService;
    private final GameWebSocketHandler gameWebSocketHandler;
    private final TurnRepository turnRepository; // TODO: import from a different PR
    private final GameHistoryRepository gameHistoryRepository;


    public GameService(LobbyRepository lobbyRepository, GameRepository gameRepository, WordService wordService, GameWebSocketHandler gameWebSocketHandler, 
                        TurnRepository turnRepository, GameHistoryRepository gameHistoryRepository) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.wordService = wordService;
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.turnRepository = turnRepository;
        this.gameHistoryRepository = gameHistoryRepository;
    }


    public Game startGame(String lobbyCode) {

        // 1. Find the lobby
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Lobby lobby = lobbyOptional.get();

        // 2. Check if game already running
        if (lobby.getGame() != null && lobby.getGame().getStatus() == GameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already in progress");
        }

        // 3. Fetch 25 random words
        // for now short hardcoded list of words
        List<String> words = wordService.getWordsForGame();

        // 4.Generate card type distribution (the "key card")
        List<CardType> types = generateCardTypes();

        // 5. Create word cards
        List<WordCard> cards = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            WordCard card = new WordCard();
            card.setWord(words.get(i));
            card.setCardType(types.get(i));
            card.setRevealed(false);
            cards.add(card);
        }

        // 6. Create board
        Board board = new Board();
        board.setCards(cards);

        // 7. Create game
        Game game = new Game();
        game.setBoard(board);
        board.setGame(game);

        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentTurn(TeamColor.RED);
        game.setMaxRounds(lobby.getSettings().getRounds());
        gameRepository.save(game);

        lobby.setGame(game);
        game.setLobby(lobby);

        lobbyRepository.save(lobby);

        // 8. Build both views and pass them to the handler
        GameBoardDTO spymasterBoard = buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO operativeBoard = buildBoardDTO(game, Role.SPY);
        gameWebSocketHandler.broadcastGameStarted(lobbyCode, spymasterBoard, operativeBoard);

        return game;
    }


    public GameBoardDTO getBoard(String lobbyCode, Role role) {
        // 1. Find the lobby
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Lobby lobby = lobbyOptional.get();
        Game game = lobby.getGame();

        return buildBoardDTO(game, role);
    }

    public void calculateGameStatistics(String lobbyCode) {
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Game game = lobbyOptional.get().getGame();
        if (game == null) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No game found for this lobby");}
        if (game.getStatus() != GameStatus.FINISHED) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is not finished yet!");}

        game.setRoundsPlayed(game.getCurrentRound());
        game.setTotalTime(0);               // Needs timer implementation --> Has to be implemented yet
        if (game.getBlueScore() == 8) {game.setWinningTeam(TeamColor.BLUE);}        // Currently only checks if the max score is reached as a win condition --> Depends on how we implement the losing condition of hitting the black card (set other teams score to max?)
        else {game.setWinningTeam(TeamColor.RED);}
    }

    public GameStatisticsDTO getGameStatistics(String lobbyCode) {
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Game game = lobbyOptional.get().getGame();
        if (game == null) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No game found for this lobby");}
        if (game.getStatus() != GameStatus.FINISHED) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is not finished yet!");}

        GameStatisticsDTO gameStatistics = new GameStatisticsDTO();
        gameStatistics.setBlueScore(game.getBlueScore());
        gameStatistics.setRedScore(game.getRedScore());
        gameStatistics.setRoundsPlayed(game.getRoundsPlayed());
        gameStatistics.setTotalTime(game.getTotalTime());
        gameStatistics.setWinningTeam(game.getWinningTeam());

        return gameStatistics;
    }

    public Game restartGame(String lobbyCode) {
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Lobby lobby = lobbyOptional.get();

        // Maybe remove later/add check for .PAUSED for restarting game during pause
        if (lobby.getGame().getStatus() != GameStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not finished yet!");
        }

        // TODO: Broadcast the game restarted event to all players with GameWebSocketHandler before startGame(lobbyCode)

        return startGame(lobbyCode);
    }

    public void backToLobby(String lobbyCode) {
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Lobby lobby = lobbyOptional.get();

        // Maybe remove later/add check for .PAUSED for restarting game during pause
        if (lobby.getGame().getStatus() != GameStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not finished yet!");
        }

        lobby.getGame().setStatus(GameStatus.ARCHIVED);
        lobby.setLobbyStatus(LobbyStatus.WAITING);

        lobbyRepository.save(lobby);
        // Coming with next task: websocket things
    }

    public void publishHint(String lobbyCode, Long spymasterId, String hint, int count) {
        // Find lobby and game
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
        Game game = lobby.getGame();
        if(game == null || game.getStatus() != GameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active game");
        }

        // Validate spymaster and turn
        Player spymaster = lobby.getPlayerById(spymasterId);
        if (spymaster == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in lobby");
        }
        // TODO: Import Turn entity
        Turn currentTurn = game.getCurrentTurn(); 
        if(currentTurn == null || currentTurn.getPhase() != TurnPhase.SPYMASTER_TURN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not the spymaster's turn");
        }
        if(spymaster.getRole() != Role.SPYMASTER || spymaster.getTeam() != currentTurn.getCurrentTeamColor()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the current team's spymaster can give a hint");
        }

        // Validate hint and count
        if(hint == null || hint.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hint cannot be empty");
        }
        if(count < 1 || count > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Count must be between 1 and 9");
        }

        // Update game state
        game.setCurrentHint(hint);
        game.setCurrentHintCount(count);
        game.setRemainingGuesses(count + 1);

        // Update turn
        currentTurn.setPhase(TurnPhase.SPY_TURN);
        currentTurn.setGuessesRemaining(count + 1);
        currentTurn.setStartTime(LocalDateTime.now());
        turnRepository.save(currentTurn);

        // Store hint in history
        GameHistory history = new GameHistory(game, currentTurn.getCurrentTeamColor(), hint, count);
        gameHistoryRepository.save(history);
        game.getGameHistories().add(history);
        gameRepository.save(game);

        // Broadcast event
        gameWebSocketHandler.broadcastClueGiven(lobbyCode, hint, count, currentTurn.getCurrentTeamColor(), spymasterId);
    }

    private GameBoardDTO buildBoardDTO(Game game, Role role) {
        // 1. Create CardDTO
        List<CardDTO> cardDTOs = new ArrayList<>();
        List<CardType> keyCard = new ArrayList<>();

        for (WordCard card : game.getBoard().getCards()) {
            CardDTO dto = new CardDTO();
            dto.setWord(card.getWord());
            dto.setRevealed(card.isRevealed());

            if (role == Role.SPYMASTER || card.isRevealed()) {
                dto.setCardType(card.getCardType());
            }

            cardDTOs.add(dto);
            keyCard.add(card.getCardType());
        }

        // 3. Build the response
        GameBoardDTO boardDTO = new GameBoardDTO();
        boardDTO.setId(game.getId());
        boardDTO.setStatus(game.getStatus());
        boardDTO.setCurrentTurn(game.getCurrentTurn());
        boardDTO.setRedScore(game.getRedScore());
        boardDTO.setBlueScore(game.getBlueScore());
        boardDTO.setCards(cardDTOs);

        // 4. Only spymaster gets the key card
        if (role == Role.SPYMASTER) {
            boardDTO.setKeyCard(keyCard);
        }

        return boardDTO;
    }


    private List<CardType> generateCardTypes() {
        // Codenames rules: starting team gets 9, other gets 8, 7 neutral, 1 assassin
        List<CardType> types = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            types.add(CardType.AGENTRED);
        }
        for (int i = 0; i < 8; i++) {
            types.add(CardType.AGENTBLUE);
        }
        for (int i = 0; i < 7; i++) {
            types.add(CardType.CIVILIAN);
        }
        types.add(CardType.ASSASSIN);

        Collections.shuffle(types);
        return types;
    }

}
