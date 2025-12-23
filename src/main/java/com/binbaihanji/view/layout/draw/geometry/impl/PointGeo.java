package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class PointGeo implements WorldObject {

    private double x;
    private double y;

    private boolean hover = false;
    private Color color = Color.RED; // 默认颜色为红色

    public PointGeo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Getter methods
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // 设置点的颜色
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform t, double w, double h) {

        double sx = t.worldToScreenX(x);
        double sy = t.worldToScreenY(y);

        gc.setFill(hover ? Color.ORANGE : color);

        double r = hover ? 6 : 4;
        gc.fillOval(sx - r, sy - r, r * 2, r * 2);
    }

    @Override
    public boolean hitTest(double wx, double wy, double tol) {
        return Math.hypot(wx - x, wy - y) < tol;
    }

    @Override
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    @Override
    public void onClick(double wx, double wy) {
        System.out.println("点被点击：" + x + ", " + y);
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 点本身可拖动
        return List.of(
                new DraggablePoint(x, y, (newX, newY) -> {
                    x = newX;
                    y = newY;
                })
        );
    }
}