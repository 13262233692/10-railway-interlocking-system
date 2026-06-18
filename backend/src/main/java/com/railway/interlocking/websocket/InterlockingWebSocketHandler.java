package com.railway.interlocking.websocket;

import com.railway.interlocking.dto.WebSocketMessage;
import com.railway.interlocking.service.InterlockingService;
import com.railway.interlocking.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;

/**
 * 联锁系统WebSocket处理器
 * Interlocking WebSocket Handler
 * 处理WebSocket连接、消息接收和发送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterlockingWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketService webSocketService;
    private final InterlockingService interlockingService;

    /**
     * 连接建立时触发
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        webSocketService.addSession(session);

        WebSocketMessage<String> welcomeMessage = WebSocketMessage.<String>builder()
                .type("CONNECTED")
                .title("连接成功")
                .content("已连接到铁路联锁系统WebSocket服务")
                .data(session.getId())
                .level("INFO")
                .timestamp(LocalDateTime.now())
                .messageId("WELCOME_" + System.currentTimeMillis())
                .build();

        webSocketService.sendMessageToSession(session.getId(), welcomeMessage);

        WebSocketMessage<Object> statusMessage = WebSocketMessage.statusUpdate(
                interlockingService.getInterlockingStatus());
        webSocketService.sendMessageToSession(session.getId(), statusMessage);

        log.info("WebSocket连接已建立: sessionId={}, remoteAddress={}",
                session.getId(), session.getRemoteAddress());
    }

    /**
     * 收到消息时触发
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到WebSocket消息: sessionId={}, payload={}", session.getId(), payload);

        try {
            if ("PING".equalsIgnoreCase(payload.trim())) {
                WebSocketMessage<String> pongMessage = WebSocketMessage.<String>builder()
                        .type("PONG")
                        .title("心跳响应")
                        .content("PONG")
                        .data("PONG")
                        .level("INFO")
                        .timestamp(LocalDateTime.now())
                        .build();
                webSocketService.sendMessageToSession(session.getId(), pongMessage);
                return;
            }

            if ("GET_STATUS".equalsIgnoreCase(payload.trim())) {
                WebSocketMessage<Object> statusMessage = WebSocketMessage.statusUpdate(
                        interlockingService.getInterlockingStatus());
                webSocketService.sendMessageToSession(session.getId(), statusMessage);
                return;
            }

            WebSocketMessage<String> response = WebSocketMessage.<String>builder()
                    .type("MESSAGE_RECEIVED")
                    .title("消息已接收")
                    .content("服务器已收到消息")
                    .data(payload)
                    .level("INFO")
                    .timestamp(LocalDateTime.now())
                    .build();
            webSocketService.sendMessageToSession(session.getId(), response);

        } catch (Exception e) {
            log.error("处理WebSocket消息异常: sessionId={}", session.getId(), e);
            WebSocketMessage<String> errorMessage = WebSocketMessage.<String>builder()
                    .type("ERROR")
                    .title("消息处理错误")
                    .content("消息处理失败: " + e.getMessage())
                    .data(e.getMessage())
                    .level("ERROR")
                    .timestamp(LocalDateTime.now())
                    .build();
            webSocketService.sendMessageToSession(session.getId(), errorMessage);
        }
    }

    /**
     * 连接关闭时触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        webSocketService.removeSession(session);
        log.info("WebSocket连接已关闭: sessionId={}, status={}, reason={}",
                session.getId(), status.getCode(), status.getReason());
    }

    /**
     * 连接出错时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
        webSocketService.removeSession(session);
    }

    /**
     * 是否支持部分消息
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
