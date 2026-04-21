package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class ClueDTO {
    private String word;
    private int count = 1;

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
