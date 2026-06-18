package com.railway.interlocking.model.enums;

/**
 * 进路类型枚举
 * Route Type Enumeration
 */
public enum RouteType {
    /**
     * 接车进路
     */
    RECEPTION("接车进路", "RECEPTION"),

    /**
     * 发车进路
     */
    DEPARTURE("发车进路", "DEPARTURE"),

    /**
     * 通过进路
     */
    THROUGH("通过进路", "THROUGH"),

    /**
     * 调车进路
     */
    SHUNTING("调车进路", "SHUNTING"),

    /**
     * 引导进路
     */
    GUIDANCE("引导进路", "GUIDANCE"),

    /**
     * 迂回进路
     */
    DETOUR("迂回进路", "DETOUR");

    private final String description;
    private final String code;

    RouteType(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}
