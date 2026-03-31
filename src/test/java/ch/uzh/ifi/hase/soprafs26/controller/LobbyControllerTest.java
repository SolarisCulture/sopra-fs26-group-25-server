package ch.uzh.ifi.hase.soprafs26.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {
    @Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LobbyService lobbyService;
	
	// Lobby Creation/Retrieval
	@Test
    public void createLobby_withHostUsername_returnsCreatedAndLobbyData() throws Exception {
        // Given
        LobbyDTO request = new LobbyDTO();
        request.setHostUsername("a");
        
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);
        
        // Create host player
        Player host = new Player("a");
        host.setId(1L);
        host.setHost(true);
        lobby.addPlayer(host);
        
        given(lobbyService.createLobby("a")).willReturn(lobby);

        given(lobbyService.createLobby("a")).willReturn(lobby);

        // When/Then
        mockMvc.perform(post("/api/lobbies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lobbyCode").value("ABC123"))
                .andExpect(jsonPath("$.hostId").value(1))
                .andExpect(jsonPath("$.players[0].username").value("a"))
                .andExpect(jsonPath("$.players[0].isHost").value(true));
    }

    @Test
    public void createLobby_missingHostUsername_returnsBadRequest() throws Exception {
        // Given: empty request
        LobbyDTO request = new LobbyDTO();

        // When/Then
        mockMvc.perform(post("/api/lobbies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createLobby_emptyHostUsername_returnsBadRequest() throws Exception {
        // Given
        LobbyDTO request = new LobbyDTO();
        request.setHostUsername("");

        // When/Then
        mockMvc.perform(post("/api/lobbies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

	@Test
    public void getLobby_validCode_returnsOkAndLobbyData() throws Exception {
        // Given
        Lobby lobby = new Lobby();
        lobby.setId(1L);
        lobby.setLobbyCode("ABC123");
        lobby.setHostId(1L);

        given(lobbyService.getLobbyByCode("ABC123")).willReturn(lobby);

        // When/Then
        mockMvc.perform(get("/api/lobbies/ABC123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lobbyCode").value("ABC123"))
                .andExpect(jsonPath("$.hostId").value(1));
    }

	@Test
    public void getLobby_invalidCode_returnsNotFound() throws Exception {
        // Given
        given(lobbyService.getLobbyByCode("INVALID"))
            .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby doesn't exist!"));

        // When/Then
        mockMvc.perform(get("/api/lobbies/INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

	// assignPlayerToTeam
	@Test
	public void assignPlayerToTeam_validInput_returnsOk() throws Exception {
		PlayerDTO player = new PlayerDTO();
        player.setTeam(TeamColor.BLUE);

        PlayerDTO teamDTO = new PlayerDTO();
        teamDTO.setTeam(TeamColor.BLUE);

		willDoNothing().given(lobbyService).assignTeam(Mockito.any(), Mockito.any(), Mockito.any());

		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamDTO));

		mockMvc.perform(putRequest).andExpect(status().isOk());
	}

	@Test
	public void assignPlayerToTeam_invalidInput_returnsBadRequest() throws Exception {
		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(null));

		mockMvc.perform(putRequest).andExpect(status().isBadRequest());
	}

	// assignPlayerToRole
    @Test
	public void assignPlayerToRole_validInput_returnsOk() throws Exception {
		PlayerDTO player = new PlayerDTO();
        player.setRole(Role.SPY);

        PlayerDTO roleDTO = new PlayerDTO();
        roleDTO.setRole(Role.SPY);

		willDoNothing().given(lobbyService).assignRole(Mockito.any(), Mockito.any(), Mockito.any());

		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleDTO));

		mockMvc.perform(putRequest).andExpect(status().isOk());
	}

	@Test
	public void assignPlayerToRole_invalidInput_returnsBadRequest() throws Exception {
		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(null));

		mockMvc.perform(putRequest).andExpect(status().isBadRequest());
	}


    /**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}
