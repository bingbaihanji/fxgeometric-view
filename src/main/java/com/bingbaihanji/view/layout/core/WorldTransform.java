package com.bingbaihanji.view.layout.core;


// 用于格点坐标视图的坐标转换
public final class WorldTransform {

    // 世界 → 像素
    private double scale = 50;

    // 世界原点在屏幕中的像素位置
    private double offsetX;
    private double offsetY;

    public double worldToScreenX(double x) {
        return offsetX + x * scale;
    }

    public double worldToScreenY(double y) {
        return offsetY - y * scale;
    }

    public double screenToWorldX(double x) {
        return (x - offsetX) / scale;
    }

    public double screenToWorldY(double y) {
        return (offsetY - y) / scale;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setOffset(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    /**
     * 保证屏幕中心对应某个世界点
     */
    public void centerWorldAt(
            double worldX,
            double worldY,
            double viewWidth,
            double viewHeight
    ) {
        offsetX = viewWidth / 2 - worldX * scale;
        offsetY = viewHeight / 2 + worldY * scale;
    }
}
