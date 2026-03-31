package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class TransferHostRequest {
    private Long currentHostId;
    private Long newHostId;
    
    public Long getCurrentHostId() { return currentHostId; }
    public void setCurrentHostId(Long currentHostId) { this.currentHostId = currentHostId; }
    
    public Long getNewHostId() { return newHostId; }
    public void setNewHostId(Long newHostId) { this.newHostId = newHostId; }
}