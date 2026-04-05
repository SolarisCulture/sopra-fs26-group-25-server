package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStatisticsDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final WordService wordService;
    private final GameWebSocketHandler gameWebSocketHandler;


    public GameService(LobbyRepository lobbyRepository, GameRepository gameRepository, WordService wordService, GameWebSocketHandler gameWebSocketHandler) {
        this.lobbyRepository = lobbyRepository;
        this.gameRepository = gameRepository;
        this.wordService = wordService;
        this.gameWebSocketHandler = gameWebSocketHandler;
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not finished!");
        }

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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not finished!");
        }

        lobby.getGame().setStatus(GameStatus.ARCHIVED);
        lobby.setLobbyStatus(LobbyStatus.WAITING);

        lobbyRepository.save(lobby);
        // Coming with next task: websocket things
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
