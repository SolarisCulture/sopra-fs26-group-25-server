package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Turn;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TurnRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ClueHistoryEntryDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStatisticsDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final WordService wordService;
    private final GameWebSocketHandler gameWebSocketHandler;
    private final TurnRepository turnRepository;


    public GameService(LobbyRepository lobbyRepository, GameRepository gameRepository, WordService wordService, GameWebSocketHandler gameWebSocketHandler, TurnRepository turnRepository) {
        this.lobbyRepository = lobbyRepository;
        this.turnRepository = turnRepository;
        this.gameRepository = gameRepository;
        this.wordService = wordService;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }


    private Game createNewGame(Lobby lobby) {
        // 3. Fetch 25 random words
        // for now short hardcoded list of words
        Difficulty difficulty =  lobby.getSettings().getDifficulty();
        List<String> words = wordService.getWordsForGame(difficulty);

        // 4.Generate card type distribution (the "key card")
        // Pick starting team randomly
        TeamColor startingTeam = Math.random() < 0.5 ? TeamColor.RED : TeamColor.BLUE;

        List<CardType> types = generateCardTypes(startingTeam);

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
        game.setStartingTeam(startingTeam);
        board.setGame(game);
        game.setStatus(GameStatus.ACTIVE);
        game.setMaxRounds(lobby.getSettings().getRounds());
        // Set totals based on who starts
        game.setRedTotal(startingTeam == TeamColor.RED ? 9 : 8);
        game.setBlueTotal(startingTeam == TeamColor.BLUE ? 9 : 8);

        game = gameRepository.save(game);

        // 8. Create first turn
        Turn firstTurn = new Turn();
        firstTurn.setGame(game);
        // First turn goes to starting team
        firstTurn.setCurrentTeamColor(startingTeam);
        firstTurn.setPhase(TurnPhase.SPYMASTER_TURN);
        firstTurn.setGuessesRemaining(0); // or don't set it up in the beginning?
        firstTurn.setStartTime(LocalDateTime.now());
        firstTurn.setGuesses(new ArrayList<>());

        firstTurn = turnRepository.save(firstTurn);

        game.setCurrentTurn(firstTurn);
        game.setTurns(new ArrayList<>(List.of(firstTurn)));

        game = gameRepository.save(game);

        lobby.setGame(game);
        game.setLobby(lobby);

        game = gameRepository.save(game);

        lobbyRepository.save(lobby);

        return game;
    }

    public Game startGame(String lobbyCode) {
        // Moved the game building to seperate function so it can be utilized for restarting the game
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Lobby lobby = lobbyOptional.get();

        if (lobby.getGame() != null) {
            System.out.println("IT GOT INTO IF STATEMENT (CONFLICT) IN STARTGAME!");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game already in progress");
        }

        Game game = createNewGame(lobby);


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

        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active game");
        }

        return buildBoardDTO(game, role);
    }

    public Game regenerateBoard(String lobbyCode, Long playerId ) {
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);
        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found!");
        }
        Lobby lobby = lobbyOptional.get();
        Game game = lobby.getGame();

        if (game == null || game.getStatus() != GameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active game!");
        }

        // Check player is spymaster
        Player player = lobby.getPlayerById(playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }
        if (player.getRole() != Role.SPYMASTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only spymaster can regenerate the board");
        }

        // Only allow regeneration during the first turn before any clue is given
        Turn currentTurn = game.getCurrentTurn();
        if (currentTurn.getClue() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot regenerate after a clue has been given!");
        }

        // New starting team
        TeamColor startingTeam = Math.random() < 0.5 ? TeamColor.RED : TeamColor.BLUE;
        game.setStartingTeam(startingTeam);
        game.setRedTotal(startingTeam == TeamColor.RED ? 9 : 8);
        game.setBlueTotal(startingTeam == TeamColor.BLUE ? 9 : 8);

        // New words and card types
        Difficulty difficulty =  lobby.getSettings().getDifficulty();
        List<String> words = wordService.getWordsForGame(difficulty);
        List<CardType> types = generateCardTypes(startingTeam);

        List<WordCard> cards = game.getBoard().getCards();
        for (int i = 0; i < 25; i++) {
            cards.get(i).setWord(words.get(i));
            cards.get(i).setCardType(types.get(i));
            cards.get(i).setRevealed(false);
        }

        // Reset turn to starting team
        currentTurn.setCurrentTeamColor(startingTeam);

        gameRepository.save(game);

        // Broadcast new board
        GameBoardDTO spymasterView = buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO spyView = buildBoardDTO(game, Role.SPY);
        gameWebSocketHandler.broadcastGameState(lobbyCode, EventType.BOARD_REGENERATED, spymasterView, spyView);

        return game;
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

        Game oldGame = lobby.getGame();

        // Maybe remove later/add check for .PAUSED for restarting game during pause
        if (oldGame == null || oldGame.getStatus() != GameStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not finished yet!");
        }

        oldGame.setStatus(GameStatus.ARCHIVED);
        lobby.setGame(null);
        lobbyRepository.save(lobby);

        Game game = createNewGame(lobby);

        GameBoardDTO spymasterBoard = buildBoardDTO(game, Role.SPYMASTER);
        GameBoardDTO operativeBoard = buildBoardDTO(game, Role.SPY);

        gameWebSocketHandler.broadcastGameRestarting(lobbyCode, spymasterBoard, operativeBoard);

        return game;
    }

    public void backToLobby(String lobbyCode) {
        Optional<Lobby> lobbyOptional = lobbyRepository.findByLobbyCode(lobbyCode);

        if (lobbyOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        Lobby lobby = lobbyOptional.get();

        // Maybe remove later/add check for .PAUSED for restarting game during pause
        if (lobby.getGame().getStatus() != GameStatus.FINISHED) {
            System.out.println("IT GOT INTO THE IF STATEMENT IN BACKTOLOBBY!");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not finished yet!");
        }

        lobby.getGame().setStatus(GameStatus.ARCHIVED);
        lobby.setLobbyStatus(LobbyStatus.WAITING);

        lobby.setGame(null);

        System.out.println("LobbyStatus (after): " + lobby.getLobbyStatus());

        lobbyRepository.save(lobby);

        gameWebSocketHandler.broadcastReturningToLobby(lobbyCode);
    }

    public GameBoardDTO buildBoardDTO(Game game, Role role) {
        // validation
        if(game.getLobby() == null) {
            throw new IllegalStateException("Game not associated with a lobby");
        }

        if(game.getLobby().getPlayerList() == null) {
            throw new IllegalStateException("Lobby has no player list");
        }

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
        boardDTO.setCurrentTurn(game.getCurrentTurn().getCurrentTeamColor());
        boardDTO.setRedScore(game.getRedScore());

        // Add team players
        List<PlayerDTO> redTeam = new ArrayList<>();
        List<PlayerDTO> blueTeam = new ArrayList<>();

        for (Player player : game.getLobby().getPlayerList()) {
            PlayerDTO dto = new PlayerDTO();
            dto.setId(player.getId());
            dto.setUsername(player.getUsername());
            dto.setRole(player.getRole());
            dto.setIsHost(player.isHost());

            if (player.getTeam() == TeamColor.RED) {
                redTeam.add(dto);
            } else {
                blueTeam.add(dto);
            }
        }

        boardDTO.setRedTeam(redTeam);
        boardDTO.setBlueTeam(blueTeam);

        boardDTO.setBlueScore(game.getBlueScore());
        boardDTO.setCards(cardDTOs);

        Turn currentTurn = game.getCurrentTurn();
        if (currentTurn != null) {
            boardDTO.setCurrentPhase(currentTurn.getPhase());
            boardDTO.setGuessesRemaining(currentTurn.getGuessesRemaining());

            // Calculate remaining time
            if (currentTurn.getStartTime() != null) {
                long elapsed = Duration.between(currentTurn.getStartTime(), LocalDateTime.now()).getSeconds();
                long timeLimit = game.getLobby().getSettings().getTimeLimit();
                boardDTO.setRemainingTimeSeconds(Math.max(0, timeLimit - elapsed));
            }
        }

        // 4. Only spymaster gets the key card
        if (role == Role.SPYMASTER) {
            boardDTO.setKeyCard(keyCard);
        }

        List<ClueHistoryEntryDTO> clueHistory = new ArrayList<>();

        if (game.getTurns() != null) {
            List<Turn> sortedTurns = new ArrayList<>(game.getTurns());
            sortedTurns.sort(Comparator.comparing(Turn::getId).reversed());

            for (Turn turn : sortedTurns) {
                if (turn.getClue() != null) {
                    ClueHistoryEntryDTO entry = new ClueHistoryEntryDTO();
                    entry.setWord(turn.getClue().getWord());
                    entry.setCount(turn.getClue().getCount());
                    entry.setTeam(turn.getCurrentTeamColor().name().toLowerCase());
                    clueHistory.add(entry);
                }
}
        }

        boardDTO.setClueHistory(clueHistory);

        return boardDTO;
    }


    private List<CardType> generateCardTypes(TeamColor startingTeam) {
        // Codenames rules: starting team gets 9, other gets 8, 7 neutral, 1 assassin
        List<CardType> types = new ArrayList<>();

        CardType team1 = (startingTeam == TeamColor.RED) ? CardType.AGENTRED : CardType.AGENTBLUE;
        CardType team2 = (startingTeam == TeamColor.RED) ? CardType.AGENTBLUE : CardType.AGENTRED;

        for (int i = 0; i < 9; i++) {
            types.add(team1);
        }
        for (int i = 0; i < 8; i++) {
            types.add(team2);
        }
        for (int i = 0; i < 7; i++) {
            types.add(CardType.CIVILIAN);
        }
        types.add(CardType.ASSASSIN);

        Collections.shuffle(types);
        return types;
    }

}
