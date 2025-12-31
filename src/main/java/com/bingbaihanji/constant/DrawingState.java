package com.bingbaihanji.constant;

/**
 * 绘制状态枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public enum DrawingState {
    /**
     * 空闲状态
     */
    IDLE,

    /**
     * 已点击第一个点，等待第二次点击
     */
    FIRST_CLICK,

    /**
     * 多边形绘制中（依次选择顶点）
     */
    POLYGON_DRAWING,

    /**
     * 旋转模式：选择要旋转的图形
     */
    ROTATE_SELECT_SHAPE,

    /**
     * 旋转模式：选择旋转中心点
     */
    ROTATE_SELECT_CENTER
}
