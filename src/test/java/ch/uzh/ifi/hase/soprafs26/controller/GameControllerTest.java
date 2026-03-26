package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    public void startGame_validLobby_returns201() throws Exception {
        // Arrange
        Game game = new Game();
        given(gameService.startGame("ABC123")).willReturn(game);

        // Act & Assert
        mockMvc.perform(post("/api/games/ABC123/start"))
                .andExpect(status().isCreated());

        verify(gameService).startGame("ABC123");
    }

    @Test
    public void getBoard_asSpy_returnsHiddenCardTypes() throws Exception {
        // Arrange
        CardDTO card1 = new CardDTO();
        card1.setWord("APPLE");
        card1.setCardType(null);  // hidden
        card1.setRevealed(false);

        CardDTO card2 = new CardDTO();
        card2.setWord("GHOST");
        card2.setCardType(null);  // hidden
        card2.setRevealed(false);

        GameBoardDTO boardDTO = new GameBoardDTO();
        boardDTO.setId(1L);
        boardDTO.setStatus(GameStatus.ACTIVE);
        boardDTO.setCurrentTurn(TeamColor.RED);
        boardDTO.setRedScore(0);
        boardDTO.setBlueScore(0);
        boardDTO.setCards(List.of(card1, card2));
        boardDTO.setKeyCard(null);  // no key card for spy

        given(gameService.getBoard("ABC123", Role.SPY)).willReturn(boardDTO);

        // Act & Assert
        mockMvc.perform(get("/api/games/ABC123/board")
                        .param("role", "SPYMASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(1)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.currentTurn", is("RED")))
                .andExpect(jsonPath("$.cards", hasSize(2)))
                .andExpect(jsonPath("$.cards[0].word", is("APPLE")))
                .andExpect(jsonPath("$.cards[0].cardType").doesNotExist())
                .andExpect(jsonPath("$.cards[1].word", is("GHOST")))
                .andExpect(jsonPath("$.cards[1].cardType").doesNotExist())
                .andExpect(jsonPath("$.keyCard").doesNotExist());
    }

    @Test
    public void getBoard_asSpymaster_returnsFullBoard() throws Exception {
        // Arrange
        CardDTO card = new CardDTO();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(false);

        GameBoardDTO boardDTO = new GameBoardDTO();
        boardDTO.setId(1L);
        boardDTO.setStatus(GameStatus.ACTIVE);
        boardDTO.setCurrentTurn(TeamColor.RED);
        boardDTO.setCards(List.of(card));
        boardDTO.setKeyCard(List.of(CardType.AGENTRED));

        given(gameService.getBoard("ABC123", Role.SPYMASTER)).willReturn(boardDTO);

        // Act & Assert
        mockMvc.perform(get("/api/games/ABC123/board")
                        .param("role", "SPYMASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].cardType", is("AGENTRED")))
                .andExpect(jsonPath("$.keyCard[0]", is("AGENTRED")));
    }

    @Test
    public void getBoard_missingRoleParam_returns400() throws Exception {
        // Act & Assert — no role parameter
        mockMvc.perform(get("/api/games/ABC123/board"))
                .andExpect(status().isBadRequest());
    }
}