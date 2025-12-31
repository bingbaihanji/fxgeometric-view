package com.bingbaihanji.constant;

import com.bingbaihanji.util.I18nUtil;

/**
 * 单位标签类型枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义坐标轴单位的显示类型
 */
public enum UnitLabelType {
    /**
     * 无单位
     */
    NONE,

    /**
     * 数值单位
     */
    NUMERIC,

    /**
     * π单位（圆周率）
     */
    PI;

    /**
     * 获取国际化显示名称
     */
    public String getDisplayName() {
        String key = "unitLabelType." + name().substring(0, 1).toLowerCase() +
                     name().substring(1).toLowerCase();
        return I18nUtil.getString(key);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
