package com.bingbaihanji.constant;

import com.bingbaihanji.util.I18nUtil;

/**
 * 坐标轴箭头类型枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义坐标轴箭头的样式
 */
public enum AxisArrowType {
    /**
     * 无箭头
     */
    NONE,

    /**
     * 单箭头（空心）
     */
    ARROW,

    /**
     * 单箭头（实心）
     */
    ARROW_FILLED,

    /**
     * 双箭头（空心）
     */
    TWO_ARROWS,

    /**
     * 双箭头（实心）
     */
    TWO_ARROWS_FILLED;

    /**
     * 获取国际化显示名称
     */
    public String getDisplayName() {
        // Convert ARROW_FILLED -> arrowFilled, TWO_ARROWS -> twoArrows, TWO_ARROWS_FILLED -> twoArrowsFilled
        String key;
        if (this == ARROW_FILLED) {
            key = "axisArrowType.arrowFilled";
        } else if (this == TWO_ARROWS) {
            key = "axisArrowType.twoArrows";
        } else if (this == TWO_ARROWS_FILLED) {
            key = "axisArrowType.twoArrowsFilled";
        } else {
            key = "axisArrowType." + name().substring(0, 1).toLowerCase() +
                    name().substring(1).toLowerCase();
        }
        return I18nUtil.getString(key);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
