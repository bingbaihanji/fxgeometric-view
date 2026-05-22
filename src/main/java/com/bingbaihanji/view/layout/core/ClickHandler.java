package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.input.MouseEvent;

import java.util.List;

/**
 * 点击处理器
 * <p>
 * 鼠标左键点击: 吸附计算 → 命中测试 → 分派 onClick 或打印坐标
 */
public class ClickHandler {

    private final GridChartView view;
    private final WorldTransform transform;
    private final SnapCalculator snapCalculator;

    public ClickHandler(GridChartView view, WorldTransform transform, SnapCalculator snapCalculator) {
        this.view = view;
        this.transform = transform;
        this.snapCalculator = snapCalculator;
    }

    public void install() {
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
}
