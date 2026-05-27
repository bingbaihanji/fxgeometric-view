package com.bingbaihanji.view.layout.core;

import javafx.scene.input.MouseEvent;

/**
 * 平移处理器
 * <p>
 * 鼠标中键拖拽平移视图
 */
public class PanHandler {

    private final GridChartView view;
    private final WorldTransform transform;
    private final HoverTooltipManager tooltipManager;

    private boolean panning = false;
    private double lastMouseX;
    private double lastMouseY;

    public PanHandler(GridChartView view, WorldTransform transform, HoverTooltipManager tooltipManager) {
        this.view = view;
        this.transform = transform;
        this.tooltipManager = tooltipManager;
    }

    public void install() {
        view.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            tooltipManager.cancel();
            if (e.isMiddleButtonDown()) {
                panning = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        view.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            tooltipManager.cancel();
            if (!panning) return;

            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            transform.setOffset(
                    transform.getOffsetX() + dx,
                    transform.getOffsetY() + dy
            );

            lastMouseX = e.getX();
            lastMouseY = e.getY();

            view.invalidateBackground();
            view.redraw();
        });

        view.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            panning = false;
        });
    }
}
