package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * 图形右键菜单构建器
 *
 * @author bingbaihanji
 * @date 2026-05-23
 */
public final class ShapeMenuBuilder {

    private ShapeMenuBuilder() {
    }

    public static ContextMenu createShapeMenu(
            WorldObject shape, GridChartView canvas, DrawingController controller) {
        ContextMenu menu = new ContextMenu();

        MenuItem propertiesItem = new MenuItem(I18nUtil.getString("geo.menu.properties"));
        propertiesItem.setOnAction(e -> ContextMenuDialogs.showPropertiesDialog(shape, canvas));

        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> ContextMenuDialogs.deleteObject(shape, canvas, controller));

        menu.getItems().addAll(propertiesItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }
}