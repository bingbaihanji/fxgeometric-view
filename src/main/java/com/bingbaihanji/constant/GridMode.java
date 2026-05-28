package com.bingbaihanji.constant;

/**
 * 网格模式
 * <p>
 * 定义网格的显示样式
 *
 * @author bingbaihanji
 * @date 2025-12-20
 */
public enum GridMode {
    /**
     * 点状网格
     */
    DOT,

    /**
     * 线状网格
     */
    LINE,

    /**
     * 带次网格的线状网格(主网格 + 次网格)
     */
    SUBGRID,

    /**
     * 极坐标网格(同心圆 + 放射线)
     */
    POLAR,

    /**
     * 等距网格(三角形格子)
     */
    ISOMETRIC
}
