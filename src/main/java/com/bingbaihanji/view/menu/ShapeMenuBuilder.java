package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.i18n.I18nUtil;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.geometry.WorldObject;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
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

        // 层级调整子菜单
        Menu layerMenu = new Menu(I18nUtil.getString("geo.menu.layer"));

        MenuItem toFrontItem = new MenuItem(I18nUtil.getString("geo.menu.layer.toFront"));
        toFrontItem.setOnAction(e -> controller.bringToFront(shape));

        MenuItem forwardItem = new MenuItem(I18nUtil.getString("geo.menu.layer.forward"));
        forwardItem.setOnAction(e -> controller.bringForward(shape));

        MenuItem backwardItem = new MenuItem(I18nUtil.getString("geo.menu.layer.backward"));
        backwardItem.setOnAction(e -> controller.sendBackward(shape));

        MenuItem toBackItem = new MenuItem(I18nUtil.getString("geo.menu.layer.toBack"));
        toBackItem.setOnAction(e -> controller.sendToBack(shape));

        layerMenu.getItems().addAll(toFrontItem, forwardItem, backwardItem, toBackItem);

        menu.getItems().addAll(propertiesItem, layerMenu, new SeparatorMenuItem(), deleteItem);
        return menu;
    }
}