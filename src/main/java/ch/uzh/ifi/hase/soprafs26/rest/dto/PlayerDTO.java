package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.Role;
import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;

public class PlayerDTO {
	private String username;
    private Long playerId;
    private TeamColor teamColor;
    private Role role;
    private boolean isHost;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

    public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

    public TeamColor getTeamColor() {
        return this.teamColor;
    }

    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean getIsHost() {
        return this.isHost;
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }
}
