package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color; // 添加导入

public class CircleGeo implements WorldObject {

    private final double cx;
    private final double cy;
    private final double r;

    private boolean hover = false;


    public CircleGeo(double cx, double cy, double r) {
        this.cx = cx;
        this.cy = cy;
        this.r = r;
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
}