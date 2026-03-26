package ch.uzh.ifi.hase.soprafs26.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RoleUpdateDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TeamUpdateDTO;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {
    @Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LobbyService lobbyService;

	@Test
	public void assignPlayerToTeam_validInput_returnsPlayerDTO() throws Exception {
		PlayerDTO player = new PlayerDTO();
        player.setTeamColor(TeamColor.BLUE);

        TeamUpdateDTO teamDTO = new TeamUpdateDTO();
        teamDTO.setTeamColor(TeamColor.BLUE);

		given(lobbyService.assignTeam(Mockito.any(),Mockito.any(),Mockito.any())).willReturn(player);

		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamDTO));

		mockMvc.perform(putRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$.teamColor", is(player.getTeamColor().name())));
	}

	@Test
	public void assignPlayerToTeam_invalidInput_returnsBadRequest() throws Exception {
		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(null));

		mockMvc.perform(putRequest).andExpect(status().isBadRequest());
	}

    @Test
	public void assignPlayerToRole_validInput_returnsPlayerDTO() throws Exception {
		PlayerDTO player = new PlayerDTO();
        player.setRole(Role.SPY);

        RoleUpdateDTO roleDTO = new RoleUpdateDTO();
        roleDTO.setRole(Role.SPY);

		given(lobbyService.assignRole(Mockito.any(),Mockito.any(),Mockito.any())).willReturn(player);

		MockHttpServletRequestBuilder putRequest = put("/api/lobbies/1/player/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(roleDTO));

		mockMvc.perform(putRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$.role", is(player.getRole().name())));
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
