package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.util.PointNameManager;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

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
public class PolygonGeo implements WorldObject {

    /**
     * 多边形顶点列表（世界坐标）
     */
    private final List<Point> vertices;

    private boolean hover = false;
    private final List<String> vertexNames; // 顶点名称列表

    /**
     * 构造函数
     *
     * @param vertices 顶点坐标数组 [x1, y1, x2, y2, ...]
     */
    public PolygonGeo(double... vertices) {
        if (vertices.length < 6) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }
        if (vertices.length % 2 != 0) {
            throw new IllegalArgumentException("顶点坐标数组长度必须是偶数");
        }

        this.vertices = new ArrayList<>();
        this.vertexNames = new ArrayList<>();
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
        if (points.size() < 3) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }

        this.vertices = new ArrayList<>();
        this.vertexNames = new ArrayList<>();
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

        // 绘制多边形
        gc.setStroke(hover ? Color.ORANGE : Color.DODGERBLUE);
        gc.setLineWidth(hover ? 3 : 2);
        gc.strokePolygon(xPoints, yPoints, vertices.size());

        // 绘制顶点
        gc.setFill(hover ? Color.ORANGE : Color.RED);
        double pointRadius = 3;
        for (int i = 0; i < vertices.size(); i++) {
            gc.fillOval(xPoints[i] - pointRadius, yPoints[i] - pointRadius,
                    pointRadius * 2, pointRadius * 2);
        }

        // 绘制顶点名称
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));
        gc.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < vertices.size(); i++) {
            String name = vertexNames.get(i);
            if (name != null && !name.isEmpty()) {
                gc.fillText(name, xPoints[i] + 8, yPoints[i] - 8);
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
    public void setHover(boolean hover) {
        this.hover = hover;
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
        List<DraggablePoint> points = new java.util.ArrayList<>();
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
