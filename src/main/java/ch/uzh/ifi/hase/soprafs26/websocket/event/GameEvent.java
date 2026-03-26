package ch.uzh.ifi.hase.soprafs26.websocket.event;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameBoardDTO;

public class GameEvent {

    private String type;
    private String lobbyCode;
    private GameBoardDTO board;

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

    // Factory methods for convenience
    public static LobbyEvent gameStarted(String lobbyCode, GameBoardDTO board){
        return new LobbyEvent("GAME_STARTED", lobbyCode, board);
    }
}