package com.bingbaihanji.view.layout.core;


/**
 * 世界坐标变换类
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 用于格点坐标视图的坐标转换，支持独立的XY轴比例
 */
public final class WorldTransform {

    // X轴缩放比例（世界单位 → 像素）
    private double scaleX = 50;

    // Y轴缩放比例（世界单位 → 像素）
    private double scaleY = 50;

    // 世界原点在屏幕中的像素位置
    private double offsetX;
    private double offsetY;

    public double worldToScreenX(double x) {
        return offsetX + x * scaleX;
    }

    public double worldToScreenY(double y) {
        return offsetY - y * scaleY;
    }

    public double screenToWorldX(double x) {
        return (x - offsetX) / scaleX;
    }

    public double screenToWorldY(double y) {
        return (offsetY - y) / scaleY;
    }

    /**
     * 获取缩放比例（平均值，用于向后兼容）
     * @return XY轴比例的平均值
     */
    public double getScale() {
        return (scaleX + scaleY) / 2.0;
    }

    /**
     * 设置统一缩放比例（XY轴使用相同比例）
     * @param scale 缩放比例
     */
    public void setScale(double scale) {
        this.scaleX = scale;
        this.scaleY = scale;
    }

    /**
     * 获取X轴缩放比例
     * @return X轴缩放比例
     */
    public double getScaleX() {
        return scaleX;
    }

    /**
     * 设置X轴缩放比例
     * @param scaleX X轴缩放比例
     */
    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    /**
     * 获取Y轴缩放比例
     * @return Y轴缩放比例
     */
    public double getScaleY() {
        return scaleY;
    }

    /**
     * 设置Y轴缩放比例
     * @param scaleY Y轴缩放比例
     */
    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    /**
     * 设置轴比例（保持平均缩放不变）
     * @param xRatio X轴比例系数
     * @param yRatio Y轴比例系数
     */
    public void setAxisRatio(double xRatio, double yRatio) {
        double avgScale = getScale();
        double totalRatio = xRatio + yRatio;
        this.scaleX = avgScale * 2.0 * (xRatio / totalRatio);
        this.scaleY = avgScale * 2.0 * (yRatio / totalRatio);
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
     * 保证屏幕中心对应某个世界点（使用平均缩放）
     */
    public void centerWorldAt(
            double worldX,
            double worldY,
            double viewWidth,
            double viewHeight
    ) {
        double avgScale = getScale();
        offsetX = viewWidth / 2 - worldX * scaleX;
        offsetY = viewHeight / 2 + worldY * scaleY;
    }

    /**
     * 使用独立的XY比例保证屏幕中心对应某个世界点
     */
    public void centerWorldAtWithScales(
            double worldX,
            double worldY,
            double viewWidth,
            double viewHeight
    ) {
        offsetX = viewWidth / 2 - worldX * scaleX;
        offsetY = viewHeight / 2 + worldY * scaleY;
    }
}
