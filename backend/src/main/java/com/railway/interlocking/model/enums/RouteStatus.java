package com.railway.interlocking.model.enums;

/**
 * 进路状态枚举
 * Route Status Enumeration
 */
public enum RouteStatus {
    /**
     * 未建立 - 进路未办理
     */
    NOT_ESTABLISHED("未建立", "NOT_ESTABLISHED"),

    /**
     * 锁闭中 - 进路正在锁闭
     */
    LOCKING("锁闭中", "LOCKING"),

    /**
     * 已锁闭 - 进路已锁闭
     */
    LOCKED("已锁闭", "LOCKED"),

    /**
     * 开放 - 进路信号已开放
     */
    CLEARED("开放", "CLEARED"),

    /**
     * 占用中 - 列车占用进路
     */
    OCCUPIED("占用中", "OCCUPIED"),

    /**
     * 解锁中 - 进路正在解锁
     */
    UNLOCKING("解锁中", "UNLOCKING"),

    /**
     * 取消中 - 进路正在取消
     */
    CANCELLING("取消中", "CANCELLING"),

    /**
     * 故障 - 进路设备故障
     */
    FAULT("故障", "FAULT");

    private final String description;
    private final String code;

    RouteStatus(String description, String code) {
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
