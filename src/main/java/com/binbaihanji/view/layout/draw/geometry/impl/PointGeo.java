package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.util.PointNameManager;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class PointGeo implements WorldObject {

    private double x;
    private double y;

    private boolean hover = false;
    private Color color = Color.RED; // 默认颜色为红色
    private String name; // 点的名称

    public PointGeo(double x, double y) {
        this.x = x;
        this.y = y;
        // 自动分配名称
        this.name = PointNameManager.getInstance().assignName(x, y);
    }

    public PointGeo(double x, double y, boolean autoName) {
        this.x = x;
        this.y = y;
        // 根据参数决定是否自动命名
        if (autoName) {
            this.name = PointNameManager.getInstance().assignName(x, y);
        }
    }

    // Getter methods
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // 获取点的颜色
    public Color getColor() {
        return color;
    }

    // 设置点的颜色
    public void setColor(Color color) {
        this.color = color;
    }

    // 获取点的名称
    public String getName() {
        return name;
    }

    // 设置点的名称
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform t, double w, double h) {

        double sx = t.worldToScreenX(x);
        double sy = t.worldToScreenY(y);

        gc.setFill(hover ? Color.ORANGE : color);

        double r = hover ? 6 : 4;
        gc.fillOval(sx - r, sy - r, r * 2, r * 2);

        // 绘制点的名称
        if (name != null && !name.isEmpty()) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(12));
            gc.setTextAlign(TextAlignment.LEFT);
            // 在点的右上方显示名称
            gc.fillText(name, sx + 8, sy - 8);
        }
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