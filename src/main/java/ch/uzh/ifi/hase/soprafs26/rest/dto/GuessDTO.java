package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GuessDTO {
    private String word;

    public GuessDTO() {}
    public GuessDTO(String word) { this.word = word; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
}
