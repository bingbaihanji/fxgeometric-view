package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.constraint.*;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

/**
 * 约束管理处理器
 * <p>
 * 负责管理点的约束关系
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class ConstraintHandler {

    /**
     * 创建约束对象
     *
     * @param shape 约束的几何图形
     * @return 约束对象
     * @throws IllegalArgumentException 如果图形类型不支持
     */
    public PointConstraint createConstraint(WorldObject shape) {
        if (shape instanceof LineGeo line) {
            return new LineConstraint(line);
        } else if (shape instanceof InfiniteLineGeo infiniteLine) {
            return new InfiniteLineConstraint(infiniteLine);
        } else if (shape instanceof CircleGeo circle) {
            return new CircleConstraint(circle);
        } else if (shape instanceof PolygonGeo polygon) {
            return new PolygonConstraint(polygon);
        } else if (shape instanceof PathGeo path) {
            return new PathConstraint(path);
        }
        throw new IllegalArgumentException("不支持的图形类型: " + shape.getClass().getName());
    }

    /**
     * 更新所有约束点的位置
     * <p>
     * 当图形的控制点被拖动后，约束点需要根据参数重新计算位置
     *
     * @param context 绘制上下文
     */
    public void updateAllConstrainedPoints(DrawingContext context) {
        for (WorldObject obj : context.getObjects()) {
            if (obj instanceof PointGeo point) {
                if (point.isConstrained()) {
                    // 根据约束参数重新计算位置
                    Point2D newPos = point.getConstraint().getPointFromParameter();
                    point.updatePosition(newPos.getX(), newPos.getY());
                }
            }
        }
    }
}
