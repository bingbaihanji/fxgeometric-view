package com.bingbaihanji.model;

/**
 * 点图形
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public class PointShape extends GeometricShape {

    /**
     * 点的显示半径（屏幕像素）
     */
    private double radius = 4.0;

    public PointShape(double x, double y) {
        super(x, y);
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public void scale(double factor) {
        // 点不支持缩放
    }

    @Override
    public void rotate(double angle) {
        // 点不支持旋转
    }
}
