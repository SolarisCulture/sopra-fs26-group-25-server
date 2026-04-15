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
    public static GameEvent gameStarted(String lobbyCode, GameBoardDTO board){
        return new GameEvent("GAME_STARTED", lobbyCode, board);
    }

    public static GameEvent gameRestarting(String lobbyCode, GameBoardDTO board) {
        return new GameEvent("GAME_RESTARTING", lobbyCode, board);
    }

    public static GameEvent returningToLobby(String lobbyCode) {
        return new GameEvent("RETURNING_TO_LOBBY", lobbyCode);
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