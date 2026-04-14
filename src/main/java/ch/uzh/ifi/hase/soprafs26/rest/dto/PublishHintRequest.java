package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class PublishHintRequest {
    private Long spymasterId;
    private String hint;
    private int count;

    public Long getSpymasterId() { return spymasterId; }
    public void setSpymasterId(Long spymasterId) { this.spymasterId = spymasterId; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
