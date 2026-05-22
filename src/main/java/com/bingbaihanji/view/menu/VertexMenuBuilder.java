package com.bingbaihanji.view.menu;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.List;

/**
 * 顶点右键菜单构建器
 *
 * @author bingbaihanji
 * @date 2026-05-23
 */
public final class VertexMenuBuilder {

    private VertexMenuBuilder() {
    }

    public static ContextMenu createVertexMenu(
            WorldObject.DraggablePoint vertex,
            WorldObject parentShape,
            GridChartView canvas,
            DrawingController controller) {

        ContextMenu menu = new ContextMenu();

        double vx = vertex.getX();
        double vy = vertex.getY();

        double scale = canvas.getTransform().getScale();
        double threshold = GeometryConfig.Tolerance.OBJECT_HIT_TEST_PIXELS / scale;
        PointGeo existingPoint = PointReuseManager.findExistingPoint(vx, vy, canvas.getObjects(), threshold);

        boolean hasOverlappingPoints = false;
        if (existingPoint == null) {
            PointGeo tempPoint = new PointGeo(vx, vy, false);
            List<PointGeo> overlapping = PointReuseManager.findOverlappingPoints(
                    tempPoint, canvas.getObjects(), GeometryConfig.Tolerance.POINT_REUSE_THRESHOLD_PIXELS / scale);

            int circleCount = 0;
            for (WorldObject obj : canvas.getObjects()) {
                if (obj instanceof CircleGeo circle) {
                    double dist = Math.hypot(circle.getCx() - vx, circle.getCy() - vy);
                    if (dist < GeometryConfig.Tolerance.POINT_REUSE_THRESHOLD_PIXELS / scale) {
                        circleCount++;
                    }
                }
            }
            hasOverlappingPoints = !overlapping.isEmpty() || circleCount > 1;
        }

        if (existingPoint != null) {
            MenuItem convertInfo = new MenuItem("✓ 已有独立点");
            convertInfo.setDisable(true);
            menu.getItems().add(convertInfo);
            menu.getItems().add(new SeparatorMenuItem());

            PointMenuBuilder.addReuseMenuItems(menu, existingPoint, canvas, controller);

            menu.getItems().add(new SeparatorMenuItem());
            PointMenuBuilder.addConstraintMenuItems(menu, existingPoint, canvas, controller);
        } else if (hasOverlappingPoints) {
            MenuItem convertItem = new MenuItem(I18nUtil.getString("geo.menu.createPointHere"));
            convertItem.setOnAction(e -> {
                PointGeo newPoint = new PointGeo(vx, vy);
                controller.getContext().addObject(newPoint);
                canvas.redraw();
            });
            menu.getItems().add(convertItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem positionItem = new MenuItem(I18nUtil.getString("geo.menu.position"));
        positionItem.setOnAction(e -> ContextMenuDialogs.showVertexPositionDialog(vertex, parentShape, canvas, controller));
        menu.getItems().add(positionItem);

        return menu;
    }
}