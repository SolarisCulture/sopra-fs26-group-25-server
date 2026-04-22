package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class ClueHistoryEntryDTO {
    private String word;
    private int count;
    private String team;

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }
}
