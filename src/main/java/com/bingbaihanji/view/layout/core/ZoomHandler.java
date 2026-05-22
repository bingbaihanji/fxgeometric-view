package com.bingbaihanji.view.layout.core;

import javafx.scene.input.ScrollEvent;

/**
 * 缩放处理器
 * <p>
 * 鼠标滚轮缩放,以鼠标位置为中心点
 */
public class ZoomHandler {

    private final GridChartView view;
    private final WorldTransform transform;
    private final HoverTooltipManager tooltipManager;

    public ZoomHandler(GridChartView view, WorldTransform transform, HoverTooltipManager tooltipManager) {
        this.view = view;
        this.transform = transform;
        this.tooltipManager = tooltipManager;
    }

    private static double clamp(double v) {
        return Math.max(5.0E-5, Math.min(5.0E+5, v));
    }

    public void install() {
        view.setOnScroll(this::handleZoom);
    }

    private void handleZoom(ScrollEvent e) {
        tooltipManager.cancel();

        double newScale = transform.getScale();
        if (e.getDeltaY() > 0) {
            newScale *= 1.1;
        } else {
            newScale *= 0.9;
        }

        newScale = clamp(newScale);

        double mouseX = e.getX();
        double mouseY = e.getY();

        double worldX = transform.screenToWorldX(mouseX);
        double worldY = transform.screenToWorldY(mouseY);

        transform.setScale(newScale);

        double newOffsetX = mouseX - worldX * newScale;
        double newOffsetY = mouseY + worldY * newScale;

        transform.setOffset(newOffsetX, newOffsetY);

        view.redraw();
    }
}
