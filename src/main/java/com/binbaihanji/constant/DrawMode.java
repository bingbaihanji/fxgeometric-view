package com.binbaihanji.constant;

/**
 * 绘制模式枚举
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public enum DrawMode {
    /**
     * 无绘制模式
     */
    NONE,

    /**
     * 点
     */
    POINT,

    /**
     * 线段
     */
    LINE,

    /**
     * 无限直线
     */
    INFINITE_LINE,

    /**
     * 圆
     */
    CIRCLE,

    /**
     * 多边形（依次选点绘制，终点与起点重合时完成）
     */
    POLYGON,

    /**
     * 手绘线
     */
    FREEHAND
}