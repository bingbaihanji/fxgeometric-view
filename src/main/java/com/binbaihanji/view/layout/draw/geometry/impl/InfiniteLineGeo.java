package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 无限直线几何图形
 * <p>
 * 通过两个点定义一条无限直线，直线会延伸至屏幕边界
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class InfiniteLineGeo implements WorldObject {

    private double point1X;
    private double point1Y;
    private double point2X;
    private double point2Y;

    private boolean hover = false;

    public InfiniteLineGeo(double point1X, double point1Y, double point2X, double point2Y) {
        this.point1X = point1X;
        this.point1Y = point1Y;
        this.point2X = point2X;
        this.point2Y = point2Y;
    }

    public double getPoint1X() {
        return point1X;
    }

    public double getPoint1Y() {
        return point1Y;
    }

    public double getPoint2X() {
        return point2X;
    }

    public double getPoint2Y() {
        return point2Y;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        // 转换两个点到屏幕坐标
        double sx1 = transform.worldToScreenX(point1X);
        double sy1 = transform.worldToScreenY(point1Y);
        double sx2 = transform.worldToScreenX(point2X);
        double sy2 = transform.worldToScreenY(point2Y);

        // 计算直线的斜率和截距
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;

        // 计算直线与屏幕边界的交点
        double[] endpoints = calculateLineScreenIntersection(sx1, sy1, sx2, sy2, w, h);

        gc.setStroke(hover ? Color.ORANGE : Color.DODGERBLUE);
        gc.setLineWidth(hover ? 3 : 2);
        gc.strokeLine(endpoints[0], endpoints[1], endpoints[2], endpoints[3]);

        // 绘制两个定义点
        gc.setFill(hover ? Color.ORANGE : Color.RED);
        double pointRadius = hover ? 5 : 4;
        gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
        gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
    }

    /**
     * 计算直线与屏幕边界的交点，使直线延伸至屏幕边界
     */
    private double[] calculateLineScreenIntersection(double sx1, double sy1, double sx2, double sy2, double w, double h) {
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;

        // 如果两点重合，返回原点
        if (Math.abs(dx) < 1e-10 && Math.abs(dy) < 1e-10) {
            return new double[]{sx1, sy1, sx1, sy1};
        }

        // 扩展倍数（足够覆盖整个屏幕）
        double scale = Math.max(w, h) * 2;

        // 计算扩展后的端点
        double p1x, p1y, p2x, p2y;

        if (Math.abs(dx) < 1e-10) {
            // 垂直线
            p1x = sx1;
            p1y = -scale;
            p2x = sx1;
            p2y = scale;
        } else if (Math.abs(dy) < 1e-10) {
            // 水平线
            p1x = -scale;
            p1y = sy1;
            p2x = scale;
            p2y = sy1;
        } else {
            // 一般情况：使用参数化方程
            double t = scale / Math.hypot(dx, dy);
            p1x = sx1 - t * dx;
            p1y = sy1 - t * dy;
            p2x = sx1 + t * dx;
            p2y = sy1 + t * dy;
        }

        return new double[]{p1x, p1y, p2x, p2y};
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        // 计算点到直线的距离
        double dx = point2X - point1X;
        double dy = point2Y - point1Y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            // 如果是点（两点重合）
            return Math.hypot(x - point1X, y - point1Y) <= tolerance;
        }

        // 计算点到直线的距离（使用行列式公式）
        double distance = Math.abs(dy * x - dx * y + point2X * point1Y - point2Y * point1X) / length;

        return distance <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
        // 直线本身暂时不响应点击
    }

    @Override
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 直线的两个定义点可拖动
        return List.of(
                new DraggablePoint(point1X, point1Y, (newX, newY) -> {
                    point1X = newX;
                    point1Y = newY;
                }),
                new DraggablePoint(point2X, point2Y, (newX, newY) -> {
                    point2X = newX;
                    point2Y = newY;
                })
        );
    }
}
