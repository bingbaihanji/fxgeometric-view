package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class CircleGeo implements WorldObject {

    private double r;
    private double cx;
    private double cy;
    private boolean hover = false;
    private Color color = StyleManager.GEOMETRY_LINE;
    private String centerName; // 圆心名称


    public CircleGeo(double cx, double cy, double r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        // 自动为圆心分配名称
        this.centerName = PointNameManager.getInstance().assignName(cx, cy);
    }

    public CircleGeo(double cx, double cy, double r, boolean autoNameCenter) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        // 根据参数决定是否为圆心自动命名
        if (autoNameCenter) {
            this.centerName = PointNameManager.getInstance().assignName(cx, cy);
        }
    }

    // Getter methods for intersection calculations
    public double getCx() {
        return cx;
    }

    public double getCy() {
        return cy;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double w,
                      double h) {

        double sx = transform.worldToScreenX(cx);
        double sy = transform.worldToScreenY(cy);
        double sr = r * transform.getScale();

        gc.setStroke(hover ? StyleManager.GEOMETRY_HOVER : color);
        gc.setLineWidth(2);

        gc.strokeOval(
                sx - sr,
                sy - sr,
                sr * 2,
                sr * 2
        );

        // 根据项目规范要求，绘制圆形时显示圆心点
        // 绘制圆心点以便提供明确的几何定位反馈
        gc.setFill(hover ? StyleManager.GEOMETRY_HOVER : color);
        double pointRadius = hover ? 4 : 3;
        gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);

        // 绘制圆心名称
        if (centerName != null && !centerName.isEmpty()) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(12));
            gc.setTextAlign(TextAlignment.LEFT);
            // 在圆心的右上方显示名称
            gc.fillText(centerName, sx + 8, sy - 8);
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double d = Math.hypot(x - cx, y - cy);
        return Math.abs(d - r) <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
        // 圆本身暂时不响应点击
    }

    @Override
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 圆心可拖动
        return List.of(
                new DraggablePoint(cx, cy, (newX, newY) -> {
                    cx = newX;
                    cy = newY;
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        // 旋转圆心
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = cx - centerX;
        double dy = cy - centerY;
        cx = centerX + dx * cos - dy * sin;
        cy = centerY + dx * sin + dy * cos;
    }
}