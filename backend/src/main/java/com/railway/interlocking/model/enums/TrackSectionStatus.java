package com.railway.interlocking.model.enums;

/**
 * 轨道区段状态枚举
 * Track Section Status Enumeration
 */
public enum TrackSectionStatus {
    /**
     * 空闲 - 轨道区段无车占用
     */
    IDLE("空闲", "IDLE"),

    /**
     * 占用 - 轨道区段有车占用
     */
    OCCUPIED("占用", "OCCUPIED"),

    /**
     * 锁闭 - 轨道区段被进路锁闭
     */
    LOCKED("锁闭", "LOCKED"),

    /**
     * 故障 - 轨道区段设备故障
     */
    FAULT("故障", "FAULT");

    private final String description;
    private final String code;

    TrackSectionStatus(String description, String code) {
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
