package com.bingbaihanji.controller;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.menu.GeometryContextMenu;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;

import java.util.List;

/**
 * 菜单控制器
 * <p>
 * 负责右键菜单的显示、隐藏和命中检测逻辑
 */
public class MenuController {

    private final DrawingController drawingController;
    private final GridChartView view;
    private final DetachedCanvasWindow parentWindow;
    private ContextMenu currentContextMenu = null;

    public MenuController(DrawingController drawingController, GridChartView view, DetachedCanvasWindow parentWindow) {
        this.drawingController = drawingController;
        this.view = view;
        this.parentWindow = parentWindow;
    }

    public void install() {
        view.setOnContextMenuRequested(this::handleContextMenu);
    }

    private void handleContextMenu(ContextMenuEvent event) {
        if (currentContextMenu != null && currentContextMenu.isShowing()) {
            hideCurrentContextMenu();
            event.consume();
            return;
        }

        double worldX = view.screenToWorldX(event.getX());
        double worldY = view.screenToWorldY(event.getY());
        double scale = view.getTransform().getScale();
        double vertexTolerance = GeometryConfig.Tolerance.VERTEX_HIT_TEST_PIXELS / scale;
        double objectTolerance = GeometryConfig.Tolerance.OBJECT_HIT_TEST_PIXELS / scale;

        List<WorldObject> objects = view.getObjects();

        // 优先级1：检查是否点击了独立的点对象(PointGeo)
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj instanceof PointGeo point) {
                if (point.hitTest(worldX, worldY, vertexTolerance)) {
                    ContextMenu menu = GeometryContextMenu.createPointMenu(point, view, drawingController);
                    showContextMenu(menu, event);
                    return;
                }
            }
        }

        // 优先级2：检查是否点击了图形的顶点(排除PointGeo)
        for (WorldObject obj : objects) {
            if (obj instanceof PointGeo) {
                continue;
            }
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, vertexTolerance)) {
                    ContextMenu menu = GeometryContextMenu.createVertexMenu(point, obj, view, drawingController);
                    showContextMenu(menu, event);
                    return;
                }
            }
        }

        // 优先级3：检查是否点击了其他图形对象
        WorldObject clickedObject = null;
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj instanceof PointGeo) {
                continue;
            }
            if (obj.hitTest(worldX, worldY, objectTolerance)) {
                clickedObject = obj;
                break;
            }
        }

        ContextMenu menu;
        if (clickedObject != null) {
            menu = GeometryContextMenu.createShapeMenu(clickedObject, view, drawingController);
        } else {
            menu = GeometryContextMenu.createCanvasMenu(view, drawingController, parentWindow);
        }

        showContextMenu(menu, event);
    }

    private void showContextMenu(ContextMenu menu, ContextMenuEvent event) {
        currentContextMenu = menu;
        menu.show(view, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    public void hideCurrentContextMenu() {
        if (currentContextMenu != null && currentContextMenu.isShowing()) {
            currentContextMenu.hide();
        }
        currentContextMenu = null;
    }
}
