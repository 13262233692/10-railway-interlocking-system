package com.railway.interlocking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket消息DTO
 * WebSocket Message DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {

    /**
     * 消息类型：STATUS_UPDATE, ROUTE_UPDATE, SECTION_UPDATE, SWITCH_UPDATE, SIGNAL_UPDATE, ALERT
     */
    private String type;

    /**
     * 消息子类型
     */
    private String subType;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息数据
     */
    private T data;

    /**
     * 消息级别：INFO, WARNING, ERROR, SUCCESS
     */
    private String level;

    /**
     * 发送时间
     */
    private LocalDateTime timestamp;

    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 车站ID
     */
    private String stationId;

    /**
     * 创建状态更新消息
     */
    public static <T> WebSocketMessage<T> statusUpdate(T data) {
        return WebSocketMessage.<T>builder()
                .type("STATUS_UPDATE")
                .title("状态更新")
                .content("联锁系统状态已更新")
                .data(data)
                .level("INFO")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建进路更新消息
     */
    public static <T> WebSocketMessage<T> routeUpdate(String title, String content, T data, String level) {
        return WebSocketMessage.<T>builder()
                .type("ROUTE_UPDATE")
                .title(title)
                .content(content)
                .data(data)
                .level(level)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建告警消息
     */
    public static <T> WebSocketMessage<T> alert(String title, String content, T data, String level) {
        return WebSocketMessage.<T>builder()
                .type("ALERT")
                .title(title)
                .content(content)
                .data(data)
                .level(level)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
