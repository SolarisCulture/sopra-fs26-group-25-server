package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.handler.GameWebSocketHandler;

@Service
public class ChatService {
    // Key = gameId, Valuie = List of all messages ordered by time
    private final Map<Long, List<ChatMessageDTO>> gameChats = new ConcurrentHashMap<>();
    private final LobbyRepository lobbyRepository;
    private final GameWebSocketHandler webSocketHandler; // For broadcasting

    public ChatService(LobbyRepository lobbyRepository, GameWebSocketHandler webSocketHandler) {
        this.lobbyRepository = lobbyRepository;
        this.webSocketHandler = webSocketHandler;
    }

    public void processChatMessage(String lobbyCode, ChatMessageDTO chatMsg) {
        // Resolve gameId
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
            .orElseThrow(() -> new IllegalArgumentException("Lobby not found: " + lobbyCode));
        if (lobby.getGame() == null) {
            throw new IllegalStateException("Game not started for lobby: " + lobbyCode);
        }
        Long gameId = lobby.getGame().getId();

        if (chatMsg.getTimestamp() == null) {
            chatMsg.setTimestamp(LocalDateTime.now());
        }
        saveMessage(gameId, chatMsg);

        webSocketHandler.broadcastChatMessage(lobbyCode, chatMsg);
    }

    public List<ChatMessageDTO> getHistory(String lobbyCode, TeamColor playerTeam, boolean isSpymaster) {
        Lobby lobby = lobbyRepository.findByLobbyCode(lobbyCode)
            .orElseThrow(() -> new IllegalArgumentException("Lobby not found: " + lobbyCode));
        Long gameId = lobby.getGame().getId();
        return getMessagesForPlayer(gameId, playerTeam, isSpymaster);
    }

    private void saveMessage(Long gameId, ChatMessageDTO message) {
        gameChats.computeIfAbsent(gameId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    private List<ChatMessageDTO> getMessagesForPlayer(Long gameId, TeamColor playerTeam, boolean isSpymaster) {
        List<ChatMessageDTO> all = gameChats.getOrDefault(gameId, Collections.emptyList());
        List<ChatMessageDTO> filtered = new ArrayList<>();
        for (ChatMessageDTO msg : all) {
            if ("TEAM".equals(msg.getChannel()) && msg.getTeam() == playerTeam) {
                filtered.add(msg);
            } else if ("SPYMASTER".equals(msg.getChannel()) && isSpymaster) {
                filtered.add(msg);
            }
        }
        return filtered;
    }
}
