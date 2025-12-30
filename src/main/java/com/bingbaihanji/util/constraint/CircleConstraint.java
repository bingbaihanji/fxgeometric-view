package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import javafx.geometry.Point2D;

/**
 * 参数化圆约束
 * <p>
 * 使用角度参数θ (0到2π) 表示点在圆周上的位置。
 * 参数θ=0对应圆的右侧(3点钟方向)，逆时针增加。
 * <p>
 * 当圆心移动时，约束点根据参数θ重新计算位置，
 * 保持在圆周上的相对角度位置不变。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class CircleConstraint implements PointConstraint {

    private final CircleGeo circle;
    private double parameter; // 参数θ, 角度(弧度), 范围[0, 2π]

    public CircleConstraint(CircleGeo circle, double parameter) {
        this.circle = circle;
        this.parameter = normalizeAngle(parameter);
    }

    public CircleConstraint(CircleGeo circle) {
        this(circle, 0.0); // 默认右侧
    }

    /**
     * 将角度规范化到[0, 2π]范围
     */
    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    @Override
    public Point2D getPointFromParameter() {
        double cx = circle.getCx();
        double cy = circle.getCy();
        double r = circle.getR();

        // 根据角度参数θ计算位置
        double x = cx + r * Math.cos(parameter);
        double y = cy + r * Math.sin(parameter);

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        double cx = circle.getCx();
        double cy = circle.getCy();

        // 计算从圆心指向点的角度
        double dx = x - cx;
        double dy = y - cy;

        double angle = Math.atan2(dy, dx);
        return normalizeAngle(angle);
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = normalizeAngle(parameter);
    }

    @Override
    public WorldObject getConstrainedShape() {
        return circle;
    }

    @Override
    public String getConstraintType() {
        return "CircleConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double cx = circle.getCx();
        double cy = circle.getCy();
        double r = circle.getR();

        // 计算点到圆心的距离
        double distToCenter = Math.hypot(x - cx, y - cy);

        // 返回点到圆周的距离
        return Math.abs(distToCenter - r);
    }
}
