package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class CircleGeo implements WorldObject {

    private final double r;
    private double cx;
    private double cy;
    private boolean hover = false;


    public CircleGeo(double cx, double cy, double r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
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

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double w,
                      double h) {

        double sx = transform.worldToScreenX(cx);
        double sy = transform.worldToScreenY(cy);
        double sr = r * transform.getScale();

        gc.setStroke(hover ? Color.ORANGE
                : Color.DODGERBLUE);
        gc.setLineWidth(2);

        gc.strokeOval(
                sx - sr,
                sy - sr,
                sr * 2,
                sr * 2
        );

        // 根据项目规范要求，绘制圆形时显示圆心点
        // 绘制圆心点以便提供明确的几何定位反馈
        gc.setFill(hover ? Color.ORANGE : Color.RED);
        double pointRadius = hover ? 4 : 3;
        gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);

        // TODO: 优化建议 - 当前圆心点的颜色和大小是固定的，可以考虑与预览阶段保持一致
        // 预览阶段的圆心点使用浅灰色，而这里是红色，可能会让用户感到困惑
        // 建议在预览阶段和最终绘制阶段使用一致的视觉表现
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