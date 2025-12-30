package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

public class PointGeo implements WorldObject {

    private double x;
    private double y;

    private boolean hover = false;
    private Color color = StyleManager.GEOMETRY_DEFAULT; // 默认颜色
    private String name; // 点的名称
    private PointConstraint constraint = null; // 点的约束（可选）

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

    // 获取点的约束
    public PointConstraint getConstraint() {
        return constraint;
    }

    // 设置点的约束
    public void setConstraint(PointConstraint constraint) {
        this.constraint = constraint;
    }

    // 判断点是否被约束
    public boolean isConstrained() {
        return constraint != null;
    }

    // 更新点的位置（用于创建约束点后设置投影位置）
    public void updatePosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform t, double w, double h) {

        double sx = t.worldToScreenX(x);
        double sy = t.worldToScreenY(y);

        // 约束点使用深蓝色，普通点使用原颜色
        Color displayColor = hover ? StyleManager.GEOMETRY_HOVER :
                (isConstrained() ? StyleManager.GEOMETRY_CONSTRAINED : color);

        gc.setFill(displayColor);

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
                    if (constraint != null) {
                        // 如果有约束，计算新参数并根据参数更新位置
                        double newParameter = constraint.calculateParameter(newX, newY);
                        constraint.setParameter(newParameter);
                        Point2D newPos = constraint.getPointFromParameter();
                        x = newPos.getX();
                        y = newPos.getY();
                    } else {
                        // 无约束，直接移动
                        x = newX;
                        y = newY;
                    }
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = x - centerX;
        double dy = y - centerY;
        x = centerX + dx * cos - dy * sin;
        y = centerY + dx * sin + dy * cos;
    }
}