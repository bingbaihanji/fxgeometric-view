package com.bingbaihanji.model;

/**
 * 圆形图形
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public class CircleShape extends GeometricShape {

    /**
     * 半径（世界坐标单位）
     */
    private double radius;

    public CircleShape(double centerX, double centerY, double radius) {
        super(centerX, centerY);
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public void scale(double factor) {
        this.radius *= factor;
    }

    @Override
    public void rotate(double angle) {
        // 圆不需要旋转
    }
}
