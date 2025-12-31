package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.FillRenderer;
import com.bingbaihanji.util.LabelRenderer;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形几何图形
 * <p>
 * 支持依次选点绘制多边形，当终点与起点重合时完成绘制
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class PolygonGeo extends AbstractWorldObject {

    /**
     * 多边形顶点列表（世界坐标）
     */
    private final List<Point> vertices;
    private final List<String> vertexNames; // 顶点名称列表

    /**
     * 构造函数
     *
     * @param vertices 顶点坐标数组 [x1, y1, x2, y2, ...]
     */
    public PolygonGeo(double... vertices) {
        super(ObjectType.POLYGON);
        if (vertices.length < 6) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }
        if (vertices.length % 2 != 0) {
            throw new IllegalArgumentException("顶点坐标数组长度必须是偶数");
        }

        this.vertices = new ArrayList<>();
        this.vertexNames = new ArrayList<>();
        this.color = StyleManager.GEOMETRY_LINE;
        PointNameManager manager = PointNameManager.getInstance();
        for (int i = 0; i < vertices.length; i += 2) {
            this.vertices.add(new Point(vertices[i], vertices[i + 1]));
            // 为每个顶点分配名称
            this.vertexNames.add(manager.assignName(vertices[i], vertices[i + 1]));
        }
    }

    /**
     * 构造函数（从点列表）
     *
     * @param points 顶点列表
     */
    public PolygonGeo(List<javafx.geometry.Point2D> points) {
        super(ObjectType.POLYGON);
        if (points.size() < 3) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }

        this.vertices = new ArrayList<>();
        this.vertexNames = new ArrayList<>();
        this.color = StyleManager.GEOMETRY_LINE;
        PointNameManager manager = PointNameManager.getInstance();
        for (javafx.geometry.Point2D p : points) {
            this.vertices.add(new Point(p.getX(), p.getY()));
            // 为每个顶点分配名称
            this.vertexNames.add(manager.assignName(p.getX(), p.getY()));
        }
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (vertices.isEmpty()) return;

        // 转换顶点到屏幕坐标
        double[] xPoints = new double[vertices.size()];
        double[] yPoints = new double[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            Point vertex = vertices.get(i);
            xPoints[i] = transform.worldToScreenX(vertex.x);
            yPoints[i] = transform.worldToScreenY(vertex.y);
        }

        // 先绘制填充
        FillRenderer.fillPolygon(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, xPoints, yPoints, vertices.size());

        // 再绘制多边形边框
        // 应用线型
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokePolygon(xPoints, yPoints, vertices.size());

        // 重置线型
        LineStyleUtil.resetLineStyle(gc);

        // 绘制顶点
        gc.setFill(getEffectiveColor());
        double pointRadius = 3;
        for (int i = 0; i < vertices.size(); i++) {
            gc.fillOval(xPoints[i] - pointRadius, yPoints[i] - pointRadius,
                    pointRadius * 2, pointRadius * 2);
        }

        // 使用LabelRenderer绘制顶点名称
        for (int i = 0; i < vertices.size(); i++) {
            String name = vertexNames.get(i);
            if (name != null && !name.isEmpty()) {
                LabelRenderer.renderLabel(gc, name, xPoints[i], yPoints[i]);
            }
        }
    }

    @Override
    public boolean hitTest(double wx, double wy, double tol) {
        // 简单的命中测试：检查是否在多边形边界附近或内部
        for (int i = 0; i < vertices.size(); i++) {
            Point p1 = vertices.get(i);
            Point p2 = vertices.get((i + 1) % vertices.size());

            // 检查点到线段的距离
            double dist = pointToSegmentDistance(wx, wy, p1.x, p1.y, p2.x, p2.y);
            if (dist < tol) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算点到线段的距离
     */
    private double pointToSegmentDistance(double px, double py,
                                          double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return Math.hypot(px - x1, py - y1);
        }

        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));
        double nearestX = x1 + t * dx;
        double nearestY = y1 + t * dy;

        return Math.hypot(px - nearestX, py - nearestY);
    }

    @Override
    public void onClick(double wx, double wy) {
        System.out.println("多边形被点击");
    }

    /**
     * 获取顶点数量
     */
    public int getVertexCount() {
        return vertices.size();
    }

    /**
     * 获取指定索引的顶点
     */
    public javafx.geometry.Point2D getVertex(int index) {
        Point p = vertices.get(index);
        return new javafx.geometry.Point2D(p.x, p.y);
    }

    /**
     * 获取多边形的所有边（作为线段）
     *
     * @return 线段列表
     */
    public List<LineGeo> getEdges() {
        List<LineGeo> edges = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            Point p1 = vertices.get(i);
            Point p2 = vertices.get((i + 1) % vertices.size());
            edges.add(new LineGeo(p1.x, p1.y, p2.x, p2.y, false));  // 不自动命名
        }
        return edges;
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 所有顶点都可拖动
        List<DraggablePoint> points = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            final int index = i;
            Point vertex = vertices.get(index);
            points.add(new DraggablePoint(vertex.x, vertex.y, (newX, newY) -> {
                vertices.get(index).x = newX;
                vertices.get(index).y = newY;
            }));
        }
        return points;
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // 旋转所有顶点
        for (Point vertex : vertices) {
            double dx = vertex.x - centerX;
            double dy = vertex.y - centerY;
            vertex.x = centerX + dx * cos - dy * sin;
            vertex.y = centerY + dx * sin + dy * cos;
        }
    }

    @Override
    public double[] getBoundingBox() {
        if (vertices.isEmpty()) {
            return null;
        }

        // 计算所有顶点的边界框
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Point vertex : vertices) {
            minX = Math.min(minX, vertex.x);
            maxX = Math.max(maxX, vertex.x);
            minY = Math.min(minY, vertex.y);
            maxY = Math.max(maxY, vertex.y);
        }

        return new double[]{minX, maxX, minY, maxY};
    }

    /**
     * 内部点类
     */
    private static class Point {
        double x;
        double y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
