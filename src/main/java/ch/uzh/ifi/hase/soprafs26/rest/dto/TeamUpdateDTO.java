package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;

public class TeamUpdateDTO {
    private TeamColor teamColor;

    public void setTeamColor(TeamColor teamColor){
        this.teamColor = teamColor;
    }

    public TeamColor getTeamColor(){
        return this.teamColor;
    }
}
