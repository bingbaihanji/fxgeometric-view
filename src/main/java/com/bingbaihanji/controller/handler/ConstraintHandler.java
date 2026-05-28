package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.IDrawingContext;
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
        return createConstraint(shape, null);
    }

    /**
     * 创建约束对象,并自动检测点是否为顶点
     *
     * @param shape 约束的几何图形
     * @param point 要约束的点对象(用于检测是否为顶点)
     * @return 约束对象
     * @throws IllegalArgumentException 如果图形类型不支持
     */
    public PointConstraint createConstraint(WorldObject shape, PointGeo point) {
        PointConstraint constraint;

        if (shape instanceof LineGeo line) {
            constraint = new LineConstraint(line);
        } else if (shape instanceof InfiniteLineGeo infiniteLine) {
            constraint = new InfiniteLineConstraint(infiniteLine);
        } else if (shape instanceof CircleGeo circle) {
            constraint = new CircleConstraint(circle);
        } else if (shape instanceof EllipseGeo ellipse) {
            constraint = new EllipseConstraint(ellipse);
        } else if (shape instanceof PolygonGeo polygon) {
            constraint = new PolygonConstraint(polygon);
        } else if (shape instanceof PathGeo path) {
            constraint = new PathConstraint(path);
        } else if (shape instanceof LinearFunctionGeo linearFunction) {
            constraint = new LinearFunctionConstraint(linearFunction);
        } else if (shape instanceof QuadraticFunctionGeo quadraticFunction) {
            constraint = new QuadraticFunctionConstraint(quadraticFunction);
        } else if (shape instanceof ReciprocalFunctionGeo reciprocalFunction) {
            constraint = new ReciprocalFunctionConstraint(reciprocalFunction);
        } else if (shape instanceof TrigonometricFunctionGeo trigFunction) {
            constraint = new TrigonometricFunctionConstraint(trigFunction);
        } else if (shape instanceof EllipseFunctionGeo ellipseFunction) {
            constraint = new EllipseFunctionConstraint(ellipseFunction);
        } else if (shape instanceof HyperbolaFunctionGeo hyperbolaFunction) {
            constraint = new HyperbolaFunctionConstraint(hyperbolaFunction);
        } else if (shape instanceof ParabolaConicFunctionGeo parabolaFunction) {
            constraint = new ParabolaConicFunctionConstraint(parabolaFunction);
        } else if (shape instanceof ExponentialFunctionGeo exponentialFunction) {
            constraint = new ExponentialFunctionConstraint(exponentialFunction);
        } else if (shape instanceof LogarithmicFunctionGeo logarithmicFunction) {
            constraint = new LogarithmicFunctionConstraint(logarithmicFunction);
        } else {
            throw new IllegalArgumentException("不支持的图形类型: " + shape.getClass().getName());
        }

        // 如果提供了点对象,检测是否为顶点并设置顶点约束
        if (point != null) {
            constraint.setAsVertexConstraintIfApplicable(point);
        }

        return constraint;
    }

    /**
     * 更新所有约束点的位置
     * <p>
     * 当图形的控制点被拖动后,约束点需要根据参数重新计算位置
     *
     * @param context 绘制上下文
     */
    public void updateAllConstrainedPoints(IDrawingContext context) {
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
