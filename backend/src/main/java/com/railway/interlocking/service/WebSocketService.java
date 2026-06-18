package com.railway.interlocking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.interlocking.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket服务
 * WebSocket Service
 * 负责管理WebSocket连接和消息广播
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final ObjectMapper objectMapper;

    /**
     * 已连接的WebSocket会话集合
     */
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    /**
     * 会话ID到会话的映射
     */
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        sessionMap.put(session.getId(), session);
        log.info("WebSocket连接已建立: sessionId={}, 当前连接数={}",
                session.getId(), sessions.size());
    }

    /**
     * 移除会话
     */
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        sessionMap.remove(session.getId());
        log.info("WebSocket连接已关闭: sessionId={}, 当前连接数={}",
                session.getId(), sessions.size());
    }

    /**
     * 向所有连接的客户端广播消息
     */
    public <T> void broadcastMessage(WebSocketMessage<T> message) {
        if (sessions.isEmpty()) {
            log.debug("没有WebSocket连接，跳过消息广播");
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);

            int successCount = 0;
            int failCount = 0;

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        successCount++;
                    } catch (IOException e) {
                        failCount++;
                        log.warn("发送WebSocket消息失败: sessionId={}, error={}",
                                session.getId(), e.getMessage());
                    }
                }
            }

            log.debug("WebSocket消息广播完成: 类型={}, 成功={}, 失败={}",
                    message.getType(), successCount, failCount);

        } catch (Exception e) {
            log.error("序列化WebSocket消息失败", e);
        }
    }

    /**
     * 向指定会话发送消息
     */
    public <T> void sendMessageToSession(String sessionId, WebSocketMessage<T> message) {
        WebSocketSession session = sessionMap.get(sessionId);
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket会话不存在或已关闭: sessionId={}", sessionId);
            return;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("WebSocket消息已发送: sessionId={}, 类型={}", sessionId, message.getType());
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * 检查会话是否存在
     */
    public boolean hasSession(String sessionId) {
        return sessionMap.containsKey(sessionId)
                && sessionMap.get(sessionId).isOpen();
    }

    /**
     * 关闭所有连接
     */
    public void closeAllSessions() {
        for (WebSocketSession session : sessions) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭WebSocket会话失败: sessionId={}", session.getId(), e);
            }
        }
        sessions.clear();
        sessionMap.clear();
        log.info("所有WebSocket连接已关闭");
    }
}
