package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class LineGeo implements WorldObject {

    private double startX;
    private double startY;
    private double endX;
    private double endY;

    private boolean hover = false;
    private String startPointName; // 起点名称
    private String endPointName;   // 终点名称

    public LineGeo(double startX, double startY, double endX, double endY) {
        this(startX, startY, endX, endY, true);
    }

    public LineGeo(double startX, double startY, double endX, double endY, boolean autoName) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        // 根据参数决定是否为起点和终点分配名称
        if (autoName) {
            PointNameManager manager = PointNameManager.getInstance();
            this.startPointName = manager.assignName(startX, startY);
            this.endPointName = manager.assignName(endX, endY);
        }
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

        // 绘制端点名称
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

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // 旋转起点
        double dx1 = startX - centerX;
        double dy1 = startY - centerY;
        startX = centerX + dx1 * cos - dy1 * sin;
        startY = centerY + dx1 * sin + dy1 * cos;

        // 旋转终点
        double dx2 = endX - centerX;
        double dy2 = endY - centerY;
        endX = centerX + dx2 * cos - dy2 * sin;
        endY = centerY + dx2 * sin + dy2 * cos;
    }
}