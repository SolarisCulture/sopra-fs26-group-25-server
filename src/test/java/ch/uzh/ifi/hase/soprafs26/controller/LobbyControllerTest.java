package ch.uzh.ifi.hase.soprafs26.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willDoNothing;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TransferHostRequest;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {
    @Autowired
	private MockMvc mockMvc;

	@Autowired
    private ObjectMapper objectMapper;

	@MockitoBean
	private LobbyService lobbyService;

	// Host Transfer
	@Test
    public void transferHost_validRequest_returnsOk() throws Exception {
        TransferHostRequest request = new TransferHostRequest();
        request.setCurrentHostId(1L);
        request.setNewHostId(2L);

        willDoNothing().given(lobbyService).transferHost(anyString(), anyLong(), anyLong());

        mockMvc.perform(put("/api/lobbies/ABC123/host/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

	@Test
    public void transferHost_missingCurrentHostId_returnsBadRequest() throws Exception {
        TransferHostRequest request = new TransferHostRequest();
        request.setNewHostId(2L);

        mockMvc.perform(put("/api/lobbies/ABC123/host/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

	// Leave Lobby
	@Test
    public void leaveLobby_validRequest_returnsNoContent() throws Exception {
        willDoNothing().given(lobbyService).leaveLobby(anyString(), anyLong());

        mockMvc.perform(delete("/api/lobbies/ABC123/players/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
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

	// joinLobby
	@Test
	public void joinLobby_validInput_returnsOk() throws Exception {

		Mockito.when(lobbyService.joinLobby(Mockito.anyString(), Mockito.anyString())).thenReturn(1L);

		MockHttpServletRequestBuilder postRequest = post("/api/lobbies/123/join")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString("Aldin"));

		mockMvc.perform(postRequest).andExpect(status().isOk());
	}

	@Test
	public void joinLobby_invalidInput_returnsBadRequest() throws Exception {

		MockHttpServletRequestBuilder postRequest = post("/api/lobbies/123/join")
				.contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(postRequest).andExpect(status().isBadRequest());
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
