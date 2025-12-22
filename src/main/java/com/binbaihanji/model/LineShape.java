package com.binbaihanji.model;

/**
 * 线段图形
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public class LineShape extends GeometricShape {

    /**
     * 起点世界坐标X
     */
    private double startX;

    /**
     * 起点世界坐标Y
     */
    private double startY;

    /**
     * 终点世界坐标X
     */
    private double endX;

    /**
     * 终点世界坐标Y
     */
    private double endY;

    public LineShape(double startX, double startY, double endX, double endY) {
        super((startX + endX) / 2, (startY + endY) / 2);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public double getStartX() {
        return startX;
    }

    public void setStartX(double startX) {
        this.startX = startX;
    }

    public double getStartY() {
        return startY;
    }

    public void setStartY(double startY) {
        this.startY = startY;
    }

    public double getEndX() {
        return endX;
    }

    public void setEndX(double endX) {
        this.endX = endX;
    }

    public double getEndY() {
        return endY;
    }

    public void setEndY(double endY) {
        this.endY = endY;
    }

    @Override
    public void translate(double dx, double dy) {
        super.translate(dx, dy);
        startX += dx;
        startY += dy;
        endX += dx;
        endY += dy;
    }

    @Override
    public void scale(double factor) {
        double dx1 = startX - centerX;
        double dy1 = startY - centerY;
        double dx2 = endX - centerX;
        double dy2 = endY - centerY;

        startX = centerX + dx1 * factor;
        startY = centerY + dy1 * factor;
        endX = centerX + dx2 * factor;
        endY = centerY + dy2 * factor;
    }

    @Override
    public void rotate(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double dx1 = startX - centerX;
        double dy1 = startY - centerY;
        startX = centerX + dx1 * cos - dy1 * sin;
        startY = centerY + dx1 * sin + dy1 * cos;

        double dx2 = endX - centerX;
        double dy2 = endY - centerY;
        endX = centerX + dx2 * cos - dy2 * sin;
        endY = centerY + dx2 * sin + dy2 * cos;
    }
}
