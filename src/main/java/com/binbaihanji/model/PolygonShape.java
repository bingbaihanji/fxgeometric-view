package com.binbaihanji.model;

/**
 * 多边形图形
 * <p>
 * 支持任意正多边形（三角形、四边形、五边形等）
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public class PolygonShape extends GeometricShape {

    /**
     * 边数
     */
    private int sides;

    /**
     * 外接圆半径（世界坐标单位）
     */
    private double radius;

    /**
     * 旋转角度（弧度）
     */
    private double rotation;

    public PolygonShape(double centerX, double centerY, int sides, double radius) {
        super(centerX, centerY);
        this.sides = sides;
        this.radius = radius;
        this.rotation = 0;
    }

    public int getSides() {
        return sides;
    }

    public void setSides(int sides) {
        this.sides = sides;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    @Override
    public void scale(double factor) {
        this.radius *= factor;
    }

    @Override
    public void rotate(double angle) {
        this.rotation += angle;
    }

    /**
     * 获取多边形顶点坐标（世界坐标）
     *
     * @return 顶点坐标数组 [x1, y1, x2, y2, ...]
     */
    public double[] getVertices() {
        double[] vertices = new double[sides * 2];

        for (int i = 0; i < sides; i++) {
            double angle = rotation + 2 * Math.PI * i / sides - Math.PI / 2;
            vertices[i * 2] = centerX + radius * Math.cos(angle);
            vertices[i * 2 + 1] = centerY + radius * Math.sin(angle);
        }

        return vertices;
    }
}
