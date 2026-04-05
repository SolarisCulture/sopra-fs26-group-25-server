package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.TurnRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private final TurnRepository turnRepository;


    public GameService(LobbyRepository lobbyRepository, GameRepository gameRepository, WordService wordService, GameWebSocketHandler gameWebSocketHandler, TurnRepository turnRepository) {
        this.lobbyRepository = lobbyRepository;
        this.turnRepository = turnRepository;
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
        game.setMaxRounds(lobby.getSettings().getRounds());

        // 8. Create first turn
        Turn firstTurn = new Turn();
        firstTurn.setGame(game);
        firstTurn.setCurrentTeamColor(TeamColor.RED);
        firstTurn.setPhase(TurnPhase.SPYMASTER_TURN);
        firstTurn.setGuessesRemaining(0); // or don't set it up in the beginning?
        firstTurn.setStartTime(LocalDateTime.now());
        firstTurn.setGuesses(new ArrayList<>());

        turnRepository.save(firstTurn);

        game.setCurrentTurn(firstTurn);
        game.setTurns(new ArrayList<>(List.of(firstTurn)));

        gameRepository.save(game);

        lobby.setGame(game);
        game.setLobby(lobby);

        lobbyRepository.save(lobby);

        // 9. Build both views and pass them to the handler
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


    public GameBoardDTO buildBoardDTO(Game game, Role role) {
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
