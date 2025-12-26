package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.util.PointNameManager;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * 手绘路径几何图形
 * <p>
 * 保留完整的曲线形状，但只显示起点和终点
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class PathGeo implements WorldObject {

    /**
     * 路径上的所有点（世界坐标）
     */
    private final List<Point> pathPoints;

    private boolean hover = false;
    private String startPointName; // 起点名称
    private String endPointName;   // 终点名称

    /**
     * 构造函数
     *
     * @param points 路径点列表
     */
    public PathGeo(List<Point2D> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("路径至少需要2个点");
        }

        this.pathPoints = new ArrayList<>();
        for (Point2D p : points) {
            this.pathPoints.add(new Point(p.getX(), p.getY()));
        }

        // 为起点和终点分配名称
        PointNameManager manager = PointNameManager.getInstance();
        Point2D startPoint = points.get(0);
        Point2D endPoint = points.get(points.size() - 1);
        this.startPointName = manager.assignName(startPoint.getX(), startPoint.getY());
        this.endPointName = manager.assignName(endPoint.getX(), endPoint.getY());
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (pathPoints.size() < 2) return;

        // 绘制曲线路径
        gc.setStroke(hover ? Color.ORANGE : Color.DODGERBLUE);
        gc.setLineWidth(hover ? 3 : 2);

        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);

            double sx1 = transform.worldToScreenX(p1.x);
            double sy1 = transform.worldToScreenY(p1.y);
            double sx2 = transform.worldToScreenX(p2.x);
            double sy2 = transform.worldToScreenY(p2.y);

            gc.strokeLine(sx1, sy1, sx2, sy2);
        }

        // 只绘制起点和终点
        Point startPoint = pathPoints.get(0);
        Point endPoint = pathPoints.get(pathPoints.size() - 1);

        double sx1 = transform.worldToScreenX(startPoint.x);
        double sy1 = transform.worldToScreenY(startPoint.y);
        double sx2 = transform.worldToScreenX(endPoint.x);
        double sy2 = transform.worldToScreenY(endPoint.y);

        gc.setFill(hover ? Color.ORANGE : Color.RED);
        double pointRadius = hover ? 5 : 4;
        gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
        gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);

        // 绘制起点和终点名称
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));
        gc.setTextAlign(TextAlignment.LEFT);
        if (startPointName != null && !startPointName.isEmpty()) {
            gc.fillText(startPointName, sx1 + 8, sy1 - 8);
        }
        if (endPointName != null && !endPointName.isEmpty()) {
            gc.fillText(endPointName, sx2 + 8, sy2 - 8);
        }
    }

    @Override
    public boolean hitTest(double wx, double wy, double tol) {
        // 检查点到路径的距离
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);

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
        System.out.println("手绘路径被点击");
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 只有起点和终点可拖动
        List<DraggablePoint> points = new ArrayList<>();

        // 起点
        points.add(new DraggablePoint(
                pathPoints.get(0).x,
                pathPoints.get(0).y,
                (newX, newY) -> {
                    pathPoints.get(0).x = newX;
                    pathPoints.get(0).y = newY;
                }
        ));

        // 终点
        int lastIndex = pathPoints.size() - 1;
        points.add(new DraggablePoint(
                pathPoints.get(lastIndex).x,
                pathPoints.get(lastIndex).y,
                (newX, newY) -> {
                    pathPoints.get(lastIndex).x = newX;
                    pathPoints.get(lastIndex).y = newY;
                }
        ));

        return points;
    }

    /**
     * 获取路径的所有边（作为线段，用于交点计算）
     */
    public List<LineGeo> getEdges() {
        List<LineGeo> edges = new ArrayList<>();
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);
            edges.add(new LineGeo(p1.x, p1.y, p2.x, p2.y, false));  // 不自动命名
        }
        return edges;
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // 旋转所有路径点
        for (Point point : pathPoints) {
            double dx = point.x - centerX;
            double dy = point.y - centerY;
            point.x = centerX + dx * cos - dy * sin;
            point.y = centerY + dx * sin + dy * cos;
        }
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
