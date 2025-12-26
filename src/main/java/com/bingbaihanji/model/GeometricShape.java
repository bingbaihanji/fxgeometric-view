package com.bingbaihanji.model;

import javafx.scene.paint.Color;

/**
 * 几何图形基础模型
 * <p>
 * 所有几何图形的抽象基类，定义通用属性和行为
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public abstract class GeometricShape {

    /**
     * 中心点世界坐标X
     */
    protected double centerX;

    /**
     * 中心点世界坐标Y
     */
    protected double centerY;

    /**
     * 图形颜色
     */
    protected Color color;

    /**
     * 填充颜色（如果不填充则为null）
     */
    protected Color fillColor;

    /**
     * 线条宽度
     */
    protected double strokeWidth;

    /**
     * 是否被选中
     */
    protected boolean selected;

    public GeometricShape(double centerX, double centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.color = Color.rgb(77, 144, 254);
        this.fillColor = null;
        this.strokeWidth = 2.0;
        this.selected = false;
    }

    // Getters and Setters

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * 平移图形
     *
     * @param dx X方向偏移量（世界坐标）
     * @param dy Y方向偏移量（世界坐标）
     */
    public void translate(double dx, double dy) {
        this.centerX += dx;
        this.centerY += dy;
    }

    /**
     * 缩放图形
     *
     * @param factor 缩放因子
     */
    public abstract void scale(double factor);

    /**
     * 旋转图形
     *
     * @param angle 旋转角度（弧度）
     */
    public abstract void rotate(double angle);
}
