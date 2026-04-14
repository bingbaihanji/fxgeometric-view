package com.bingbaihanji.util.visitor;

import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * 边吸附访问者
 * <p>
 * 使用访问者模式消除EdgeSnapManager中的instanceof判断
 * 计算鼠标位置到各种几何图形边缘的最近点
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class EdgeSnapVisitor implements GeometryVisitor<EdgeSnapVisitor.SnapResult> {

    private final double mouseX;
    private final double mouseY;
    private final double threshold;

    public EdgeSnapVisitor(double mouseX, double mouseY, double threshold) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.threshold = threshold;
    }

    @Override
    public SnapResult visitPoint(PointGeo point) {
        // 点没有边,不参与边吸附
        return null;
    }

    @Override
    public SnapResult visitLine(LineGeo line) {
        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        Point2D nearestPoint = projectPointOntoSegment(mouseX, mouseY, x1, y1, x2, y2);
        double distance = Math.hypot(nearestPoint.getX() - mouseX, nearestPoint.getY() - mouseY);

        if (distance < threshold) {
            return new SnapResult(nearestPoint.getX(), nearestPoint.getY(), distance, "LINE");
        }
        return null;
    }

    @Override
    public SnapResult visitInfiniteLine(InfiniteLineGeo line) {
        double x1 = line.getPoint1X();
        double y1 = line.getPoint1Y();
        double x2 = line.getPoint2X();
        double y2 = line.getPoint2Y();

        Point2D nearestPoint = projectPointOntoLine(mouseX, mouseY, x1, y1, x2, y2);
        double distance = Math.hypot(nearestPoint.getX() - mouseX, nearestPoint.getY() - mouseY);

        if (distance < threshold) {
            return new SnapResult(nearestPoint.getX(), nearestPoint.getY(), distance, "LINE");
        }
        return null;
    }

    @Override
    public SnapResult visitCircle(CircleGeo circle) {
        double cx = circle.getCx();
        double cy = circle.getCy();
        double radius = circle.getR();

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distToCenter = Math.hypot(dx, dy);
        double distToEdge = Math.abs(distToCenter - radius);

        if (distToEdge < threshold && distToCenter > 0) {
            double factor = radius / distToCenter;
            double snapX = cx + dx * factor;
            double snapY = cy + dy * factor;
            return new SnapResult(snapX, snapY, distToEdge, "CIRCLE");
        }
        return null;
    }

    @Override
    public SnapResult visitPolygon(PolygonGeo polygon) {
        List<LineGeo> edges = polygon.getEdges();
        SnapResult bestSnap = null;

        for (LineGeo edge : edges) {
            SnapResult snap = visitLine(edge);
            if (snap != null && (bestSnap == null || snap.distance < bestSnap.distance)) {
                bestSnap = new SnapResult(snap.x, snap.y, snap.distance, "POLYGON_EDGE");
            }
        }

        return bestSnap;
    }

    @Override
    public SnapResult visitRegularPolygon(RegularPolygonGeo regularPolygon) {
        List<Point2D> vertices = regularPolygon.getVertices();
        SnapResult bestSnap = null;

        // 遍历正多边形的所有边
        for (int i = 0; i < vertices.size(); i++) {
            Point2D p1 = vertices.get(i);
            Point2D p2 = vertices.get((i + 1) % vertices.size());

            Point2D projected = projectPointOntoSegment(
                    mouseX, mouseY,
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY()
            );
            double distance = Math.hypot(projected.getX() - mouseX, projected.getY() - mouseY);

            if (distance < threshold && (bestSnap == null || distance < bestSnap.distance)) {
                bestSnap = new SnapResult(projected.getX(), projected.getY(), distance, "REGULAR_POLYGON_EDGE");
            }
        }

        return bestSnap;
    }

    @Override
    public SnapResult visitPath(PathGeo path) {
        List<LineGeo> edges = path.getEdges();
        SnapResult bestSnap = null;

        for (LineGeo edge : edges) {
            SnapResult snap = visitLine(edge);
            if (snap != null && (bestSnap == null || snap.distance < bestSnap.distance)) {
                bestSnap = new SnapResult(snap.x, snap.y, snap.distance, "PATH_EDGE");
            }
        }

        return bestSnap;
    }

    @Override
    public SnapResult visitFunction(FunctionGeo function) {
        List<Point2D> points = function.getSampledPoints();
        if (points == null || points.size() < 2) {
            return null;
        }

        Point2D nearestPoint = null;
        double minDistance = threshold;

        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i + 1);

            if (!isValidPoint(p1) || !isValidPoint(p2)) {
                continue;
            }

            Point2D projected = projectPointOntoSegment(mouseX, mouseY, p1.getX(), p1.getY(), p2.getX(), p2.getY());
            double distance = Math.hypot(projected.getX() - mouseX, projected.getY() - mouseY);

            if (distance < minDistance) {
                minDistance = distance;
                nearestPoint = projected;
            }
        }

        if (nearestPoint != null) {
            return new SnapResult(nearestPoint.getX(), nearestPoint.getY(), minDistance, "FUNCTION");
        }
        return null;
    }

    // 辅助方法

    /**
     * 将点投影到线段上
     */
    private Point2D projectPointOntoSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return new Point2D(x1, y1);
        }

        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));
        return new Point2D(x1 + t * dx, y1 + t * dy);
    }

    /**
     * 将点投影到无限直线上
     */
    private Point2D projectPointOntoLine(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return new Point2D(x1, y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
        return new Point2D(x1 + t * dx, y1 + t * dy);
    }

    /**
     * 检查点是否有效
     */
    private boolean isValidPoint(Point2D p) {
        return p != null && Double.isFinite(p.getX()) && Double.isFinite(p.getY());
    }

    /**
     * 吸附结果
     */
    public static class SnapResult {
        public final double x;
        public final double y;
        public final double distance;
        public final String edgeType;

        public SnapResult(double x, double y, double distance, String edgeType) {
            this.x = x;
            this.y = y;
            this.distance = distance;
            this.edgeType = edgeType;
        }
    }
}
