package com.bingbaihanji.util.constraint;

import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PathGeo;
import javafx.geometry.Point2D;

/**
 * 参数化手绘路径约束
 * <p>
 * 使用连续参数表示点在手绘路径上的位置：
 * - 参数范围：[0, n)，其中n是路径的线段数量
 * - 整数部分表示线段的索引
 * - 小数部分表示在该线段上的位置（0到1）
 * <p>
 * 例如，对于一个由3条线段组成的路径：
 * - 0.0: 第0个顶点（起点）
 * - 0.5: 第0条线段的中点
 * - 1.0: 第1个顶点
 * - 1.5: 第1条线段的中点
 * - 2.0: 第2个顶点
 * - 2.5: 第2条线段的中点
 * <p>
 * 当路径的端点移动时，约束点根据参数重新计算位置，
 * 保持在路径上的相对位置不变。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class PathConstraint implements PointConstraint {

    private final PathGeo path;
    private double parameter; // 参数，范围[0, edgeCount)

    public PathConstraint(PathGeo path, double parameter) {
        this.path = path;
        this.parameter = normalizeParameter(parameter);
    }

    public PathConstraint(PathGeo path) {
        this(path, 0.0); // 默认起点
    }

    /**
     * 将参数规范化到[0, edgeCount)范围
     */
    private double normalizeParameter(double param) {
        int edgeCount = path.getEdges().size();
        if (edgeCount == 0) return 0.0;

        // 处理负数和超出范围的参数
        while (param < 0) param += edgeCount;
        while (param >= edgeCount) param -= edgeCount;

        return param;
    }

    @Override
    public Point2D getPointFromParameter() {
        var edges = path.getEdges();
        if (edges.isEmpty()) {
            return new Point2D(0, 0);
        }

        // 获取线段索引和线段上的位置
        int edgeIndex = (int) Math.floor(parameter);
        double t = parameter - edgeIndex;

        // 确保索引在有效范围内
        edgeIndex = Math.min(edgeIndex, edges.size() - 1);

        var edge = edges.get(edgeIndex);

        // 线性插值计算位置
        double x = edge.getStartX() + t * (edge.getEndX() - edge.getStartX());
        double y = edge.getStartY() + t * (edge.getEndY() - edge.getStartY());

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        var edges = path.getEdges();
        if (edges.isEmpty()) {
            return 0.0;
        }

        double minDistance = Double.MAX_VALUE;
        double bestParameter = 0.0;

        // 遍历每条边，找到最近的边和位置
        for (int i = 0; i < edges.size(); i++) {
            var edge = edges.get(i);
            double x1 = edge.getStartX();
            double y1 = edge.getStartY();
            double x2 = edge.getEndX();
            double y2 = edge.getEndY();

            // 计算投影参数
            double dx = x2 - x1;
            double dy = y2 - y1;
            double lengthSquared = dx * dx + dy * dy;

            double t;
            if (lengthSquared < 1e-10) {
                // 边退化为点
                t = 0.0;
            } else {
                t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
                t = MathCalculationUtils.max(0, MathCalculationUtils.min(1, t)); // 限制在边上
            }

            // 计算投影点
            double projX = x1 + t * dx;
            double projY = y1 + t * dy;

            // 计算距离
            double distance = MathCalculationUtils.hypot(x - projX, y - projY);

            // 更新最近的边
            if (distance < minDistance) {
                minDistance = distance;
                bestParameter = i + t;
            }
        }

        return normalizeParameter(bestParameter);
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = normalizeParameter(parameter);
    }

    @Override
    public WorldObject getConstrainedShape() {
        return path;
    }

    @Override
    public String getConstraintType() {
        return "PathConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        var edges = path.getEdges();
        if (edges.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double minDistance = Double.MAX_VALUE;

        // 计算点到路径各边的最短距离
        for (var edge : edges) {
            double x1 = edge.getStartX();
            double y1 = edge.getStartY();
            double x2 = edge.getEndX();
            double y2 = edge.getEndY();

            // 计算点到线段的距离
            double dx = x2 - x1;
            double dy = y2 - y1;
            double lengthSquared = dx * dx + dy * dy;

            double distance;
            if (lengthSquared < 1e-10) {
                // 线段退化为点
                distance = MathCalculationUtils.hypot(x - x1, y - y1);
            } else {
                double t = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
                t = MathCalculationUtils.max(0, MathCalculationUtils.min(1, t));

                double projX = x1 + t * dx;
                double projY = y1 + t * dy;

                distance = MathCalculationUtils.hypot(x - projX, y - projY);
            }

            minDistance = MathCalculationUtils.min(minDistance, distance);
        }

        return minDistance;
    }

    @Override
    public boolean isVertexConstraint() {
        // 手绘路径通常不支持顶点约束
        return false;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
        // 手绘路径不支持顶点约束，不需要实现
    }
}
