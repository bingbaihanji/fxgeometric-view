package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.LineGeo;
import javafx.geometry.Point2D;

/**
 * 参数化线段约束
 * <p>
 * 使用参数t (0到1) 表示点在线段上的位置：
 * - t=0: 起点
 * - t=0.5: 中点
 * - t=1: 终点
 * <p>
 * 当线段的端点移动时，约束点根据参数t重新计算位置，
 * 保持在线段上的相对位置不变。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class LineConstraint implements PointConstraint {

    private final LineGeo line;
    private double parameter; // 参数t, 范围[0, 1]

    public LineConstraint(LineGeo line, double parameter) {
        this.line = line;
        this.parameter = Math.max(0, Math.min(1, parameter)); // 限制在[0,1]
    }

    public LineConstraint(LineGeo line) {
        this(line, 0.5); // 默认中点
    }

    @Override
    public Point2D getPointFromParameter() {
        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        // 根据参数t计算位置: P = P1 + t * (P2 - P1)
        double x = x1 + parameter * (x2 - x1);
        double y = y1 + parameter * (y2 - y1);

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        // 计算线段的方向向量
        double dx = x2 - x1;
        double dy = y2 - y1;

        // 线段长度的平方
        double lengthSquared = dx * dx + dy * dy;

        // 如果线段退化为点
        if (lengthSquared < 1e-10) {
            return 0.0;
        }

        // 计算投影参数 t
        double t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;

        // 限制在线段范围内 [0, 1]
        return Math.max(0, Math.min(1, t));
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = Math.max(0, Math.min(1, parameter));
    }

    @Override
    public WorldObject getConstrainedShape() {
        return line;
    }

    @Override
    public String getConstraintType() {
        return "LineConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        // 计算点到线段的最短距离
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared < 1e-10) {
            // 线段退化为点
            return Math.hypot(x - x1, y - y1);
        }

        // 计算投影参数
        double t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t)); // 限制在线段范围内

        // 计算投影点
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        // 返回距离
        return Math.hypot(x - projX, y - projY);
    }
}
