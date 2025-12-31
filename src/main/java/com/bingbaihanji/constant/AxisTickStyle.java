package com.bingbaihanji.constant;

import com.bingbaihanji.util.I18nUtil;

/**
 * 坐标轴刻度样式枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义坐标轴刻度的显示样式
 */
public enum AxisTickStyle {
    /**
     * 主刻度 + 次刻度
     */
    MAJOR_MINOR,

    /**
     * 仅主刻度
     */
    MAJOR_ONLY,

    /**
     * 无刻度
     */
    NONE;

    /**
     * 获取国际化显示名称
     */
    public String getDisplayName() {
        // Convert MAJOR_MINOR -> majorMinor, MAJOR_ONLY -> majorOnly
        String key;
        if (this == MAJOR_MINOR) {
            key = "axisTickStyle.majorMinor";
        } else if (this == MAJOR_ONLY) {
            key = "axisTickStyle.majorOnly";
        } else {
            key = "axisTickStyle." + name().substring(0, 1).toLowerCase() +
                    name().substring(1).toLowerCase();
        }
        return I18nUtil.getString(key);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
