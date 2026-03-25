package ch.uzh.ifi.hase.soprafs26.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Enables message handling via WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        /* 
        Clients subscribe to these prefixes to receive messages, whereas
        - Topics: One-to-many broadcast
        - Queues: Point-to-point messages (private)
        */
        config.enableSimpleBroker("/topic", "/queue"); 

        // Client messages with this prefix are routed to @MessageMapping methods => Prefix distinguishes client-to-server and server-to-client messages
        config.setApplicationDestinationPrefixes("/app"); 
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws") // Defines URL path for WebSocket connections
                .setAllowedOrigins( // For CORS (Cross-Origin Resource Sharing)
                    "http://localhost:3000",
                    "https://sopra-fs26-group-25-client.vercel.app/"
                )
                .withSockJS(); // Reason for SockJS: fallback support, since not all browsers use WebSocket => Client MUST use SockJS library to connect
    }
    
}
