package com.bingbaihanji.util.constraint;

import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PolygonGeo;
import javafx.geometry.Point2D;

/**
 * 参数化多边形约束
 * <p>
 * 使用连续参数表示点在多边形周长上的位置：
 * - 参数范围：[0, n)，其中n是顶点数量
 * - 整数部分表示边的索引
 * - 小数部分表示在该边上的位置（0到1）
 * <p>
 * 例如，对于三角形（3个顶点）：
 * - 0.0: 第0个顶点
 * - 0.5: 第0条边的中点
 * - 1.0: 第1个顶点
 * - 1.5: 第1条边的中点
 * - 2.0: 第2个顶点
 * - 2.5: 第2条边的中点
 * <p>
 * 当多边形的顶点移动时，约束点根据参数重新计算位置，
 * 保持在多边形边界上的相对位置不变。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class PolygonConstraint implements PointConstraint {

    private final PolygonGeo polygon;
    private double parameter; // 参数,范围[0, vertexCount)
    private boolean isVertexConstraint = false; // 是否是顶点约束

    public PolygonConstraint(PolygonGeo polygon, double parameter) {
        this.polygon = polygon;
        this.parameter = normalizeParameter(parameter);
    }

    public PolygonConstraint(PolygonGeo polygon) {
        this(polygon, 0.0); // 默认第一个顶点
    }

    /**
     * 将参数规范化到[0, vertexCount)范围
     */
    private double normalizeParameter(double param) {
        int vertexCount = polygon.getVertexCount();
        if (vertexCount == 0) return 0.0;

        // 处理负数和超出范围的参数
        while (param < 0) param += vertexCount;
        while (param >= vertexCount) param -= vertexCount;

        return param;
    }

    @Override
    public Point2D getPointFromParameter() {
        int vertexCount = polygon.getVertexCount();
        if (vertexCount == 0) {
            return new Point2D(0, 0);
        }

        // 获取边索引和边上的位置
        int edgeIndex = (int) Math.floor(parameter);
        double t = parameter - edgeIndex;

        // 确保索引在有效范围内
        edgeIndex = edgeIndex % vertexCount;

        // 获取边的两个端点
        Point2D p1 = polygon.getVertex(edgeIndex);
        Point2D p2 = polygon.getVertex((edgeIndex + 1) % vertexCount);

        // 线性插值计算位置
        double x = p1.getX() + t * (p2.getX() - p1.getX());
        double y = p1.getY() + t * (p2.getY() - p1.getY());

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 如果是顶点约束,参数不变化
        if (isVertexConstraint) {
            return parameter;
        }

        int vertexCount = polygon.getVertexCount();
        if (vertexCount == 0) {
            return 0.0;
        }

        double minDistance = Double.MAX_VALUE;
        double bestParameter = 0.0;

        // 遍历每条边,找到最近的边和位置
        for (int i = 0; i < vertexCount; i++) {
            Point2D p1 = polygon.getVertex(i);
            Point2D p2 = polygon.getVertex((i + 1) % vertexCount);

            double x1 = p1.getX();
            double y1 = p1.getY();
            double x2 = p2.getX();
            double y2 = p2.getY();

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
        return polygon;
    }

    @Override
    public String getConstraintType() {
        return "PolygonConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        int vertexCount = polygon.getVertexCount();
        if (vertexCount == 0) {
            return Double.MAX_VALUE;
        }

        double minDistance = Double.MAX_VALUE;

        // 计算点到多边形各边的最短距离
        for (int i = 0; i < vertexCount; i++) {
            Point2D p1 = polygon.getVertex(i);
            Point2D p2 = polygon.getVertex((i + 1) % vertexCount);

            double x1 = p1.getX();
            double y1 = p1.getY();
            double x2 = p2.getX();
            double y2 = p2.getY();

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
        return isVertexConstraint;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
        // 检查点是否是多边形的顶点
        java.util.List<com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo> vertexPoints = polygon.getVertexPoints();

        for (int i = 0; i < vertexPoints.size(); i++) {
            if (vertexPoints.get(i) == point) {
                // 点就是第i个顶点
                this.isVertexConstraint = true;
                this.parameter = (double) i;
                return;
            }
        }

        // 检查坐标是否非常接近某个顶点
        final double EPSILON = 1e-6;
        for (int i = 0; i < polygon.getVertexCount(); i++) {
            Point2D vertex = polygon.getVertex(i);
            double dist = MathCalculationUtils.hypot(
                    point.getX() - vertex.getX(),
                    point.getY() - vertex.getY()
            );
            if (dist < EPSILON) {
                // 点在顶点位置
                this.isVertexConstraint = true;
                this.parameter = (double) i;
                return;
            }
        }

        this.isVertexConstraint = false;
    }
}
