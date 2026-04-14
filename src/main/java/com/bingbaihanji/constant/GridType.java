package com.bingbaihanji.constant;

import com.bingbaihanji.util.I18nUtil;

/**
 * 网格类型枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义各种网格类型, 扩展原有的GridMode功能
 */
public enum GridType {
    /**
     * 笛卡尔网格(线状)
     */
    CARTESIAN,

    /**
     * 点状网格
     */
    DOT,

    /**
     * 极坐标网格(同心圆 + 放射线)
     */
    POLAR,

    /**
     * 等距网格(三角形格子)
     */
    ISOMETRIC,

    /**
     * 带次网格的笛卡尔网格(主网格 + 次网格)
     */
    CARTESIAN_WITH_SUBGRID;

    /**
     * 从旧的GridMode转换为GridType
     *
     * @param gridMode 旧的网格模式
     * @return 对应的GridType
     */
    public static GridType fromGridMode(GridMode gridMode) {
        switch (gridMode) {
            case LINE:
                return CARTESIAN;
            case DOT:
                return DOT;
            default:
                return CARTESIAN;
        }
    }

    /**
     * 获取国际化显示名称
     */
    public String getDisplayName() {
        String key = "gridType." + this.name().substring(0, 1).toLowerCase() +
                this.name().substring(1).toLowerCase().replace("_", "");
        // Convert CARTESIAN_WITH_SUBGRID -> cartesianWithSubgrid
        if (this == CARTESIAN_WITH_SUBGRID) {
            key = "gridType.cartesianWithSubgrid";
        } else {
            key = "gridType." + name().substring(0, 1).toLowerCase() +
                    name().substring(1).toLowerCase();
        }
        return I18nUtil.getString(key);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
