package com.bingbaihanji.view.layout.draw.coordinate.grid;

import javafx.geometry.Point2D;

/**
 * 网格元素 —— 描述一条待绘制的网格线、圆或点
 * <p>
 * 使用 sealed interface 保证类型安全，Visitor 模式遍历。
 * 数据对象不包含绘制逻辑，由 GridElementPainter 统一渲染。
 *
 * @author bingbaihanji
 */
public sealed interface GridElement permits GridElement.GridLineSegment, GridElement.GridCircle, GridElement.GridDot {

    /**
     * 主网格线段（直线段）
     *
     * @param start     线段起点（屏幕坐标）
     * @param end       线段终点（屏幕坐标）
     * @param isSubGrid 是否为次网格
     */
    record GridLineSegment(Point2D start, Point2D end, boolean isSubGrid) implements GridElement {
    }

    /**
     * 网格圆弧（极坐标同心圆或圆弧段，屏幕坐标）
     *
     * @param center     圆心（屏幕坐标）
     * @param radiusX    X方向半径（像素）
     * @param radiusY    Y方向半径（像素）
     * @param startAngle 起始角度（度）
     * @param length     弧长（度，360 = 完整圆）
     * @param isSubGrid  是否为次网格
     */
    record GridCircle(Point2D center, double radiusX, double radiusY,
                      double startAngle, double length, boolean isSubGrid) implements GridElement {
    }

    /**
     * 点状网格节点
     *
     * @param position 节点位置（屏幕坐标）
     */
    record GridDot(Point2D position) implements GridElement {
    }
}
