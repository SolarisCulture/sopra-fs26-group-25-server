package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.CardType;
import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.WordCard;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
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
    private final WordService wordService;
    private final GameWebSocketHandler gameWebSocketHandler;


    public GameService(LobbyRepository lobbyRepository, WordService wordService, GameWebSocketHandler gameWebSocketHandler) {
        this.lobbyRepository = lobbyRepository;
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

        // 2. Fetch 25 random words
        // for now short hardcoded list of words
        List<String> words = wordService.getWordsForGame();

        // 3.Generate card type distribution (the "key card")
        List<CardType> types = generateCardTypes();

        // 4. Create word cards
        List<WordCard> cards = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            WordCard card = new WordCard();
            card.setWord(words.get(i));
            card.setCardType(types.get(i));
            card.setRevealed(false);
            cards.add(card);
        }

        // 5. Create board
        Board board = new Board();
        board.setCards(cards);

        // 6. Create game
        Game game = new Game();
        game.setBoard(board);
        game.setCurrentTurn(TeamColor.RED);
        game.setMaxRounds(lobby.getSettings().getRounds());

        lobby.setGame(game);
        lobbyRepository.save(lobby);

        // 7. Build both views and pass them to the handler
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
