package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.scene.control.ContextMenu;

/**
 * 几何图形右键菜单管理器 — 薄外观
 * <p>
 * 所有菜单构建逻辑已委托到对应的 Builder 类：
 * {@link PointMenuBuilder}、{@link VertexMenuBuilder}、
 * {@link ShapeMenuBuilder}、{@link CanvasMenuBuilder}
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class GeometryContextMenu {

    private GeometryContextMenu() {
    }

    public static ContextMenu createPointMenu(
            PointGeo point, GridChartView canvas, DrawingController controller) {
        return PointMenuBuilder.createPointMenu(point, canvas, controller);
    }

    public static ContextMenu createVertexMenu(
            WorldObject.DraggablePoint vertex, WorldObject parentShape,
            GridChartView canvas, DrawingController controller) {
        return VertexMenuBuilder.createVertexMenu(vertex, parentShape, canvas, controller);
    }

    public static ContextMenu createShapeMenu(
            WorldObject shape, GridChartView canvas, DrawingController controller) {
        return ShapeMenuBuilder.createShapeMenu(shape, canvas, controller);
    }

    public static ContextMenu createCanvasMenu(
            GridChartView canvas, DrawingController controller) {
        return CanvasMenuBuilder.createCanvasMenu(canvas, controller);
    }

    public static ContextMenu createCanvasMenu(
            GridChartView canvas, DrawingController controller, DetachedCanvasWindow parentWindow) {
        return CanvasMenuBuilder.createCanvasMenu(canvas, controller, parentWindow);
    }
}