package com.bingbaihanji.constant;

import com.bingbaihanji.util.I18nUtil;

/**
 * 线型枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义各种线型样式及其对应的虚线模式
 */
public enum LineType {
    /**
     * 实线
     */
    FULL(null),

    /**
     * 短虚线
     */
    DASHED_SHORT(new double[]{5, 5}),

    /**
     * 长虚线
     */
    DASHED_LONG(new double[]{10, 5}),

    /**
     * 点线
     */
    DOTTED(new double[]{2, 3}),

    /**
     * 点划线
     */
    DASHED_DOTTED(new double[]{10, 3, 2, 3});

    private final double[] dashPattern;

    LineType(double[] dashPattern) {
        this.dashPattern = dashPattern;
    }

    /**
     * 获取国际化显示名称
     */
    public String getDisplayName() {
        // Convert DASHED_SHORT -> dashedShort
        String key;
        if (this == DASHED_SHORT) {
            key = "lineType.dashedShort";
        } else if (this == DASHED_LONG) {
            key = "lineType.dashedLong";
        } else if (this == DASHED_DOTTED) {
            key = "lineType.dashedDotted";
        } else {
            key = "lineType." + name().substring(0, 1).toLowerCase() +
                    name().substring(1).toLowerCase();
        }
        return I18nUtil.getString(key);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * 获取虚线模式数组
     *
     * @return 虚线模式数组，实线返回null
     */
    public double[] getDashPattern() {
        return dashPattern;
    }
}
