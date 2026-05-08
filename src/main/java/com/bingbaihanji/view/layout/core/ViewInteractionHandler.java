package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.List;

/**
 * 视图交互处理器
 * <p>
 * 负责鼠标交互事件：缩放、平移、点击输出、悬停高亮
 */
public class ViewInteractionHandler {

    private final GridChartView view;
    private final WorldTransform transform;
    private final SnapCalculator snapCalculator;
    private final HoverTooltipManager tooltipManager;

    private boolean panning = false;
    private double lastMouseX;
    private double lastMouseY;

    public ViewInteractionHandler(GridChartView view, WorldTransform transform,
                                  SnapCalculator snapCalculator, HoverTooltipManager tooltipManager) {
        this.view = view;
        this.transform = transform;
        this.snapCalculator = snapCalculator;
        this.tooltipManager = tooltipManager;
    }

    public void install() {
        view.setOnScroll(this::handleZoom);
        initMousePan();
        initMouseClickOutput();
        initMouseObjectHover();
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

    private double clamp(double v) {
        return Math.max(5.0E-5, Math.min(5.0E+5, v));
    }

    private void initMousePan() {
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

            view.redraw();
        });

        view.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            panning = false;
        });
    }

    private void initMouseClickOutput() {
        view.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            double worldX = view.screenToWorldX(e.getX());
            double worldY = view.screenToWorldY(e.getY());

            SnapCalculator.SnapResult snapped = snapCalculator.calculate(worldX, worldY, view.getObjects());
            view.setNearbySpecialPoint(snapped.snappedPoint);
            view.setAxisSnapInfo(snapped.axisSnapInfo);
            worldX = snapped.x;
            worldY = snapped.y;

            double tolerance = GeometryConfig.Tolerance.HIT_TEST_PIXELS / transform.getScale();
            List<WorldObject> objects = view.getObjects();
            for (int i = objects.size() - 1; i >= 0; i--) {
                WorldObject obj = objects.get(i);
                if (obj.hitTest(worldX, worldY, tolerance)) {
                    obj.onClick(worldX, worldY);
                    view.redraw();
                    return;
                }
            }

            System.out.printf("point(x = %.2f, y = %.2f)%n", worldX, worldY);
        });
    }

    private void initMouseObjectHover() {
        view.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            double worldX = view.screenToWorldX(e.getX());
            double worldY = view.screenToWorldY(e.getY());

            SnapCalculator.SnapResult snapped = snapCalculator.calculate(worldX, worldY, view.getObjects());
            view.setNearbySpecialPoint(snapped.snappedPoint);
            view.setAxisSnapInfo(snapped.axisSnapInfo);
            worldX = snapped.x;
            worldY = snapped.y;

            double tolerance = GeometryConfig.Tolerance.HIT_TEST_PIXELS / transform.getScale();

            WorldObject newHover = null;
            List<WorldObject> objects = view.getObjects();
            for (int i = objects.size() - 1; i >= 0; i--) {
                WorldObject obj = objects.get(i);
                if (obj.hitTest(worldX, worldY, tolerance)) {
                    newHover = obj;
                    break;
                }
            }

            WorldObject hoverObject = view.getHoverObject();
            if (newHover != hoverObject) {
                if (hoverObject != null) {
                    hoverObject.setHover(false);
                }
                view.setHoverObject(newHover);
                if (newHover != null) {
                    newHover.setHover(true);
                }
                view.redrawObjects();
                view.redrawInteraction();
            } else if (snapped.snappedPoint != null || snapped.axisSnapInfo != null) {
                view.redrawInteraction();
            }
        });

        view.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            WorldObject hoverObject = view.getHoverObject();
            if (hoverObject != null) {
                hoverObject.setHover(false);
                view.setHoverObject(null);
                view.redrawObjects();
                view.redrawInteraction();
            }

            if (view.getNearbySpecialPoint() != null || view.getAxisSnapInfo() != null) {
                view.setNearbySpecialPoint(null);
                view.setAxisSnapInfo(null);
                view.redrawInteraction();
            }
        });
    }
}
