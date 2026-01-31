package com.bingbaihanji.constant;

/**
 * 吸附模式枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义各种吸附模式
 */
public enum SnapMode {
    /**
     * 关闭吸附
     */
    OFF("关闭吸附"),

    /**
     * 网格吸附
     */
    GRID("网格吸附"),

    /**
     * 点吸附
     */
    POINT("点吸附"),

    /**
     * 垂直轴吸附
     */
    AXIS_VERTICAL("垂直轴吸附"),

    /**
     * 水平轴吸附
     */
    AXIS_HORIZONTAL("水平轴吸附");

    private final String displayName;

    SnapMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
