package com.railway.interlocking.config;

import com.railway.interlocking.websocket.InterlockingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * WebSocket Configuration
 * 配置WebSocket端点和处理器
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${websocket.endpoint:/ws/interlocking}")
    private String websocketEndpoint;

    private final InterlockingWebSocketHandler interlockingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interlockingWebSocketHandler, websocketEndpoint)
                .setAllowedOrigins("*");

        registry.addHandler(interlockingWebSocketHandler, "/sockjs" + websocketEndpoint)
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
