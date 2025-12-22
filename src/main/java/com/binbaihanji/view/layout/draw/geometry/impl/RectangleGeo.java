package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RectangleGeo implements WorldObject {

    private final double x;
    private final double y;
    private final double width;
    private final double height;

    private boolean hover = false;

    public RectangleGeo(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        double sx = transform.worldToScreenX(x);
        double sy = transform.worldToScreenY(y);
        double sw = width * transform.getScale();
        double sh = height * transform.getScale();

        // 矩形需要根据坐标系方向调整绘制方式
        // 在标准数学坐标系中，Y轴向上为正，但在屏幕坐标系中，Y轴向下为正
        // 因此我们需要调整绘制方式
        double screenY = sy - sh; // 调整Y坐标
        
        gc.setStroke(hover ? Color.ORANGE : Color.DODGERBLUE);
        gc.setLineWidth(hover ? 3 : 2);
        gc.strokeRect(sx, screenY, sw, sh);
    }

    @Override
    public boolean hitTest(double wx, double wy, double tolerance) {
        // 检查点是否在矩形内（考虑容差）
        return wx >= x - tolerance && wx <= x + width + tolerance && 
               wy >= y - tolerance && wy <= y + height + tolerance;
    }

    @Override
    public void onClick(double wx, double wy) {
        // 矩形本身暂时不响应点击
    }

    @Override
    public void setHover(boolean hover) {
        this.hover = hover;
    }
}