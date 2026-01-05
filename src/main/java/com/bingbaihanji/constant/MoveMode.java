package com.bingbaihanji.constant;

/**
 * 移动模式枚举
 * <p>
 * 定义各种拖动和变换操作的模式,参考 GeoGebra 的 MoveMode 设计
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public enum MoveMode {
    /**
     * 无移动操作
     */
    MOVE_NONE,

    /**
     * 移动单个点(控制点或独立点)
     */
    MOVE_POINT,

    /**
     * 移动多个对象(整体平移)
     */
    MOVE_MULTIPLE_OBJECTS,

    /**
     * 调整 BoundingBox 大小(8个句柄)
     */
    RESIZE_BOUNDING_BOX,

    /**
     * 移动视图(拖动画布)
     */
    MOVE_VIEW,

    /**
     * 移动 X 轴
     */
    MOVE_X_AXIS,

    /**
     * 移动 Y 轴
     */
    MOVE_Y_AXIS,

    /**
     * 旋转对象(围绕中心点)
     */
    ROTATE_OBJECTS
}
