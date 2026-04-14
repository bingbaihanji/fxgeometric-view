package com.bingbaihanji.util.constraint;

import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.InfiniteLineGeo;
import javafx.geometry.Point2D;

/**
 * 参数化无限直线约束
 * <p>
 * 使用参数t表示点在直线上的位置,t没有范围限制。
 * - t=0: 第一个定义点
 * - t=1: 第二个定义点
 * - t可以是负数或大于1的值(直线是无限的)
 * <p>
 * 当直线的定义点移动时,约束点根据参数t重新计算位置。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class InfiniteLineConstraint implements PointConstraint {

    private final InfiniteLineGeo line;
    private double parameter; // 参数t, 无范围限制
    private boolean isVertexConstraint = false; // 是否是顶点约束

    public InfiniteLineConstraint(InfiniteLineGeo line, double parameter) {
        this.line = line;
        this.parameter = parameter;
    }

    public InfiniteLineConstraint(InfiniteLineGeo line) {
        this(line, 0.5); // 默认两个定义点之间
    }

    @Override
    public Point2D getPointFromParameter() {
        double x1 = line.getPoint1X();
        double y1 = line.getPoint1Y();
        double x2 = line.getPoint2X();
        double y2 = line.getPoint2Y();

        // 根据参数t计算位置: P = P1 + t * (P2 - P1)
        // 注意：t可以小于0或大于1(直线是无限的)
        double x = x1 + parameter * (x2 - x1);
        double y = y1 + parameter * (y2 - y1);

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 如果是顶点约束,参数不变化
        if (isVertexConstraint) {
            return parameter;
        }

        double x1 = line.getPoint1X();
        double y1 = line.getPoint1Y();
        double x2 = line.getPoint2X();
        double y2 = line.getPoint2Y();

        // 计算直线的方向向量
        double dx = x2 - x1;
        double dy = y2 - y1;

        // 直线长度的平方
        double lengthSquared = dx * dx + dy * dy;

        // 如果两点重合(退化为点)
        if (lengthSquared < 1e-10) {
            return 0.0;
        }

        // 计算投影参数 t(不限制范围)
        return ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = parameter; // 无范围限制
    }

    @Override
    public WorldObject getConstrainedShape() {
        return line;
    }

    @Override
    public String getConstraintType() {
        return "InfiniteLineConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double x1 = line.getPoint1X();
        double y1 = line.getPoint1Y();
        double x2 = line.getPoint2X();
        double y2 = line.getPoint2Y();

        // 计算点到直线的距离(使用点到直线距离公式)
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = MathCalculationUtils.hypot(dx, dy);

        if (length < 1e-10) {
            // 直线退化为点
            return MathCalculationUtils.hypot(x - x1, y - y1);
        }

        // 使用点到直线距离公式: |ax + by + c| / sqrt(a^2 + b^2)
        // 直线方程: (y2-y1)x - (x2-x1)y + (x2-x1)y1 - (y2-y1)x1 = 0

        return MathCalculationUtils.abs(dy * x - dx * y + dx * y1 - dy * x1) / length;
    }

    @Override
    public boolean isVertexConstraint() {
        return isVertexConstraint;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
        // 检查点是否是直线的定义点
        com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point1Ref = line.getPoint1Ref();
        com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point2Ref = line.getPoint2Ref();

        final double EPSILON = 1e-6;

        if (point1Ref != null && point1Ref == point) {
            // 点就是第一个定义点引用
            this.isVertexConstraint = true;
            this.parameter = 0.0;
        } else if (point2Ref != null && point2Ref == point) {
            // 点就是第二个定义点引用
            this.isVertexConstraint = true;
            this.parameter = 1.0;
        } else {
            // 检查坐标是否非常接近定义点
            double distToPoint1 = MathCalculationUtils.hypot(
                    point.getX() - line.getPoint1X(),
                    point.getY() - line.getPoint1Y()
            );
            double distToPoint2 = MathCalculationUtils.hypot(
                    point.getX() - line.getPoint2X(),
                    point.getY() - line.getPoint2Y()
            );

            if (distToPoint1 < EPSILON) {
                // 点在第一个定义点位置
                this.isVertexConstraint = true;
                this.parameter = 0.0;
            } else if (distToPoint2 < EPSILON) {
                // 点在第二个定义点位置
                this.isVertexConstraint = true;
                this.parameter = 1.0;
            } else {
                this.isVertexConstraint = false;
            }
        }
    }
}
