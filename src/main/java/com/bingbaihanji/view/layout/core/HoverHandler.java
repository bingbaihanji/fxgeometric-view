package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.input.MouseEvent;

import java.util.List;

/**
 * 悬停处理器
 * <p>
 * 鼠标移动: 吸附计算 → 命中测试 → 悬停状态切换 → 重绘
 */
public class HoverHandler {

    private final GridChartView view;
    private final WorldTransform transform;
    private final SnapCalculator snapCalculator;

    public HoverHandler(GridChartView view, WorldTransform transform, SnapCalculator snapCalculator) {
        this.view = view;
        this.transform = transform;
        this.snapCalculator = snapCalculator;
    }

    public void install() {
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
