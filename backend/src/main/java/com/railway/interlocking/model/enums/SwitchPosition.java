package com.railway.interlocking.model.enums;

/**
 * 道岔位置枚举
 * Switch Position Enumeration
 */
public enum SwitchPosition {
    /**
     * 定位 - 道岔正常位置
     */
    NORMAL("定位", "NORMAL"),

    /**
     * 反位 - 道岔切换位置
     */
    REVERSE("反位", "REVERSE"),

    /**
     * 四开 - 道岔处于中间位置（危险状态）
     */
    FOUR_WAY("四开", "FOUR_WAY"),

    /**
     * 故障 - 道岔设备故障
     */
    FAULT("故障", "FAULT"),

    /**
     * 转换中 - 道岔正在转换
     */
    MOVING("转换中", "MOVING");

    private final String description;
    private final String code;

    SwitchPosition(String description, String code) {
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
