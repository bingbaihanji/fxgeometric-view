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
    FREEHAND,

    /**
     * 中点（点击线段绘制中点）
     */
    MIDPOINT,

    /**
     * 垂线（选择线段/直线 -> 选择一点 -> 过此点绘制垂线）
     */
    PERPENDICULAR,

    /**
     * 垂直平分线（选择线段/直线 -> 选择一点 -> 过此点绘制垂直平分线）
     */
    PERPENDICULAR_BISECTOR,

    /**
     * 平行线（选择线段/直线 -> 选择一点 -> 过此点绘制平行线）
     */
    PARALLEL,

    /**
     * 切线（选择圆上一点 -> 绘制过此点的切线）
     */
    TANGENT
}