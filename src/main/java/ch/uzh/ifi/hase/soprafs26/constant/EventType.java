package ch.uzh.ifi.hase.soprafs26.constant;

public enum EventType {
    GAME_STARTED("GameStarted"),
    CLUE_GIVEN("Clue"),
    CARD_REVEALED("Guess"),
    TURN_CHANGED("TurnChanged"),
    BOARD_REGENERATED("BoardRegenerated"),
    GAME_OVER("GameOver");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
