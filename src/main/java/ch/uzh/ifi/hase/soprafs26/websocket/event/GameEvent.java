package ch.uzh.ifi.hase.soprafs26.websocket.event;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;

public class GameEvent {

    private String type;
    private String lobbyCode;
    private GameBoardDTO board;
    private String hint;
    private int count;
    private TeamColor team;
    private Long spymasterId;

    public GameEvent(String type, String lobbyCode) {
        this.type = type;
        this.lobbyCode = lobbyCode;
    }

    public GameEvent(String type, String lobbyCode, GameBoardDTO board) {
        this.type = type;
        this.lobbyCode = lobbyCode;
        this.board = board;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }

    public GameBoardDTO getBoard() { return board; }
    public void setBoard(GameBoardDTO board) { this.board = board; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public TeamColor getTeam() { return team; }
    public void setTeam(TeamColor team) { this.team = team; }

    public Long getSpymasterId() { return spymasterId; }
    public void setSpymasterId(Long spymasterId) { this.spymasterId = spymasterId; }

    // Factory methods for convenience
    public static GameEvent gameStarted(String lobbyCode, GameBoardDTO board){
        return new GameEvent("GAME_STARTED", lobbyCode, board);
    }

    public static GameEvent clueGiven(String lobbyCode, GameBoardDTO board){
        return new GameEvent("CLUE_GIVEN", lobbyCode, board);
    }

    public static GameEvent guessGiven(String lobbyCode, GameBoardDTO board){
        return new GameEvent("CARD_REVEALED", lobbyCode, board);
    }

    public static GameEvent turnChanged(String lobbyCode, GameBoardDTO board){
        return new GameEvent("TURN_CHANGED", lobbyCode, board);
    }

}