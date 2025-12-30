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

    public ShapePropertiesResult(Color color, double radius) {
        this.color = color;
        this.radius = radius;
    }

    public Color getColor() {
        return color;
    }

    public double getRadius() {
        return radius;
    }
}
