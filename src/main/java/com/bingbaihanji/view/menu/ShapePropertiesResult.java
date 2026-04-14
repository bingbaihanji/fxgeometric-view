package com.bingbaihanji.view.menu;

import javafx.scene.paint.Color;

/**
 * 属性修改结果
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class ShapePropertiesResult {
    private final Color color;
    private final double radius;
    private final String centerName; // 圆心名称(仅圆形使用)

    // 普通图形构造函数(仅颜色)
    public ShapePropertiesResult(Color color) {
        this(color, 0, null);
    }

    // 圆形构造函数(颜色和半径)
    public ShapePropertiesResult(Color color, double radius) {
        this(color, radius, null);
    }

    // 圆形完整构造函数(颜色、半径和圆心名称)
    public ShapePropertiesResult(Color color, double radius, String centerName) {
        this.color = color;
        this.radius = radius;
        this.centerName = centerName;
    }

    public Color getColor() {
        return color;
    }

    public double getRadius() {
        return radius;
    }

    public String getCenterName() {
        return centerName;
    }
}
