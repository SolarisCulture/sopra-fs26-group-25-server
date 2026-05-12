package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;

public class ChatService {
    // Key = gameId, Valuie = List of all messages ordered by time
    private final Map<Long, List<ChatMessageDTO>> gameChats = new ConcurrentHashMap<>();

    public void saveMessage(Long gameId, ChatMessageDTO message) {
        gameChats.computeIfAbsent(gameId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    public List<ChatMessageDTO> getMessagesForPlayer(Long gameId, TeamColor playerTeam, boolean isSpymaster) {
        List<ChatMessageDTO> all = gameChats.getOrDefault(gameId, Collections.emptyList());
        List<ChatMessageDTO> filtered = new ArrayList<>();
        
        for(ChatMessageDTO msg : all) {
            if("TEAM".equals(msg.getChannel())) {
                if(msg.getTeam() == playerTeam) {
                    filtered.add(msg);
                }
            } else if("SPYMASTER".equals(msg.getChannel())) {
                if(isSpymaster) {
                    filtered.add(msg);
                }
            }
        }
        return filtered;
    }
}
