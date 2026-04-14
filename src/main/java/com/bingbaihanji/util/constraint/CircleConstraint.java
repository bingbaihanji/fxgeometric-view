package com.bingbaihanji.util.constraint;

import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import javafx.geometry.Point2D;

/**
 * 参数化圆约束
 * <p>
 * 使用角度参数θ (0到2π) 表示点在圆周上的位置。
 * 参数θ=0对应圆的右侧(3点钟方向),逆时针增加。
 * <p>
 * 当圆心移动时,约束点根据参数θ重新计算位置,
 * 保持在圆周上的相对角度位置不变。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class CircleConstraint implements PointConstraint {

    private final CircleGeo circle;
    private double parameter; // 参数θ, 角度(弧度), 范围[0, 2π]
    private boolean isVertexConstraint = false; // 是否是顶点约束(圆心)
    private boolean isCenterConstraint = false; // 是否是圆心约束

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
        return MathCalculationUtils.normalizeAngle(angle);
    }

    @Override
    public Point2D getPointFromParameter() {
        double cx = circle.getCx();
        double cy = circle.getCy();

        // 如果是圆心约束,直接返回圆心位置
        if (isCenterConstraint) {
            return new Point2D(cx, cy);
        }

        // 否则在圆周上
        double r = circle.getR();

        // 根据角度参数θ计算位置
        double x = cx + r * MathCalculationUtils.cos(parameter);
        double y = cy + r * MathCalculationUtils.sin(parameter);

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 如果是圆心约束,参数不变化
        if (isCenterConstraint) {
            return parameter;
        }

        double cx = circle.getCx();
        double cy = circle.getCy();

        // 计算从圆心指向点的角度
        double dx = x - cx;
        double dy = y - cy;

        double angle = MathCalculationUtils.atan2(dy, dx);
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
        double distToCenter = MathCalculationUtils.hypot(x - cx, y - cy);

        // 返回点到圆周的距离
        return MathCalculationUtils.abs(distToCenter - r);
    }

    @Override
    public boolean isVertexConstraint() {
        return isVertexConstraint;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
        // 检查点是否是圆心
        com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo centerRef = circle.getCenterPointRef();

        final double EPSILON = 1e-6;

        if (centerRef != null && centerRef == point) {
            // 点就是圆心引用
            this.isVertexConstraint = true;
            this.isCenterConstraint = true;
            this.parameter = 0.0; // 圆心约束时参数无意义
        } else {
            // 检查坐标是否非常接近圆心
            double distToCenter = MathCalculationUtils.hypot(
                    point.getX() - circle.getCx(),
                    point.getY() - circle.getCy()
            );

            if (distToCenter < EPSILON) {
                // 点在圆心位置
                this.isVertexConstraint = true;
                this.isCenterConstraint = true;
                this.parameter = 0.0;
            } else {
                this.isVertexConstraint = false;
                this.isCenterConstraint = false;
            }
        }
    }
}
