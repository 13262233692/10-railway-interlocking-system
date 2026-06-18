package com.railway.interlocking.model.enums;

/**
 * 信号机显示状态枚举
 * Signal Aspect Enumeration
 */
public enum SignalAspect {
    /**
     * 红灯 - 禁止越过
     */
    RED("红灯", "H", false),

    /**
     * 黄灯 - 注意运行，前方至少一个闭塞分区空闲
     */
    YELLOW("黄灯", "HU", true),

    /**
     * 绿灯 - 允许通过，前方至少两个闭塞分区空闲
     */
    GREEN("绿灯", "L", true),

    /**
     * 双黄灯 - 引导信号，允许以不超过20km/h速度进入
     */
    DOUBLE_YELLOW("双黄灯", "UUS", true),

    /**
     * 红黄闪 - 引导信号（特殊情况）
     */
    RED_YELLOW_FLASH("红黄闪", "HB", true),

    /**
     * 灭灯 - 信号机故障或关闭
     */
    OFF("灭灯", "OFF", false);

    private final String description;
    private final String code;
    private final boolean allowPass;

    SignalAspect(String description, String code, boolean allowPass) {
        this.description = description;
        this.code = code;
        this.allowPass = allowPass;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public boolean isAllowPass() {
        return allowPass;
    }
}
