package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;

public class ChatMessageDTO {
    private String type;            // "CHAT_MESSAGE" or "CHAT_HISTORY"
    private String channel;         // "TEAM" or "SPYMASTER"
    private TeamColor team;         // Only for TEAM channel
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
    private List<ChatMessageDTO> history;  // used only for CHAT_HISTORY response

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public TeamColor getTeam() { return team; }
    public void setTeam(TeamColor team) { this.team = team; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public List<ChatMessageDTO> getHistory() { return history; }
    public void setHistory(List<ChatMessageDTO> history) { this.history = history; }
}
