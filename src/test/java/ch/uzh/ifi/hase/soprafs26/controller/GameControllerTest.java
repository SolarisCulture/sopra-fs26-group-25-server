package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameStatisticsDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
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
        // Set up
        Game game = new Game();
        given(gameService.startGame("ABC123")).willReturn(game);

        // simulate an HTTP request
        mockMvc.perform(post("/api/games/ABC123/start"))
                .andExpect(status().isCreated());

        // make sure the service was actually called
        verify(gameService).startGame("ABC123");
    }

    @Test
    public void getBoard_asSpy_returnsHiddenCardTypes() throws Exception {
        // Set up
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

        // Tell the mock to return boardDTO when called correctly
        given(gameService.getBoard("ABC123", Role.SPY)).willReturn(boardDTO);

        // Simulate the HTTP request
        mockMvc.perform(get("/api/games/ABC123/board")
                        .param("role", "SPY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
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
        // Set up
        CardDTO card = new CardDTO();
        card.setWord("APPLE");
        card.setCardType(CardType.AGENTRED);
        card.setRevealed(false);

        GameBoardDTO boardDTO = new GameBoardDTO();
        boardDTO.setId(1L);
        boardDTO.setStatus(GameStatus.ACTIVE);
        boardDTO.setCurrentTurn(TeamColor.RED);
        boardDTO.setCards(List.of(card));
        boardDTO.setKeyCard(List.of(CardType.AGENTRED)); // key card for spymaster

        // Tell the mock to return boardDTO when called correctly
        given(gameService.getBoard("ABC123", Role.SPYMASTER)).willReturn(boardDTO);

        // Simulate the HTTP request
        mockMvc.perform(get("/api/games/ABC123/board")
                        .param("role", "SPYMASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].cardType", is("AGENTRED")))
                .andExpect(jsonPath("$.keyCard[0]", is("AGENTRED")));
    }

    @Test
    public void getBoard_missingRoleParam_returns400() throws Exception {
        // Simulate the HTTP request without role parameter
        mockMvc.perform(get("/api/games/ABC123/board"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getGameStatistics_validLobby_returns200AndDTO() throws Exception {
        GameStatisticsDTO dto = new GameStatisticsDTO();
        dto.setBlueScore(5);
        dto.setRedScore(9);

        given(gameService.getGameStatistics("ABC123")).willReturn(dto);

        mockMvc.perform(get("/api/games/ABC123/statistics")).andExpect(status().isOk());

        verify(gameService).getGameStatistics("ABC123");
    }

    @Test
    public void restartGame_validLobby_returns200() throws Exception {
        Game game = new Game();
        given(gameService.restartGame("ABC123")).willReturn(game);

        mockMvc.perform(post("/api/games/ABC123/restart")).andExpect(status().isOk());

        verify(gameService).restartGame("ABC123");
    }

    @Test
    public void backToLobby_validLobby_returns200() throws Exception {
        willDoNothing().given(gameService).backToLobby("ABC123");

        mockMvc.perform(post("/api/games/ABC123/backToLobby")).andExpect(status().isOk());

        verify(gameService).backToLobby("ABC123");
    }
    
}