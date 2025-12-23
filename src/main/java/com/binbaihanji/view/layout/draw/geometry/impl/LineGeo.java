package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class LineGeo implements WorldObject {

    private double startX;
    private double startY;
    private double endX;
    private double endY;

    private boolean hover = false;

    public LineGeo(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }


    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        double sx1 = transform.worldToScreenX(startX);
        double sy1 = transform.worldToScreenY(startY);
        double sx2 = transform.worldToScreenX(endX);
        double sy2 = transform.worldToScreenY(endY);

        gc.setStroke(hover ? Color.ORANGE : Color.DODGERBLUE);
        gc.setLineWidth(hover ? 3 : 2);
        gc.strokeLine(sx1, sy1, sx2, sy2);

        // 绘制端点
        gc.setFill(hover ? Color.ORANGE : Color.RED);
        double pointRadius = hover ? 5 : 4;
        gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
        gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        // 点到线段的距离计算
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            // 如果是点（起点和终点重合）
            return Math.hypot(x - startX, y - startY) <= tolerance;
        }

        // 计算点到直线的距离
        double distance = Math.abs(dy * x - dx * y + endX * startY - endY * startX) / length;

        // 检查点是否在线段的延长线上，但距离在容差范围内
        return distance <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
        // 线段本身暂时不响应点击
    }

    @Override
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 线段的两个端点可拖动
        return List.of(
                new DraggablePoint(startX, startY, (newX, newY) -> {
                    startX = newX;
                    startY = newY;
                }),
                new DraggablePoint(endX, endY, (newX, newY) -> {
                    endX = newX;
                    endY = newY;
                })
        );
    }
}