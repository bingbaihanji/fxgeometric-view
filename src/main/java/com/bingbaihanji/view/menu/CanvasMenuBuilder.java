package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.GridMode;
import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.impl.GridPainter;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

/**
 * 画布右键菜单构建器
 *
 * @author bingbaihanji
 * @date 2026-05-23
 */
public final class CanvasMenuBuilder {

    private CanvasMenuBuilder() {
    }

    public static ContextMenu createCanvasMenu(GridChartView canvas, DrawingController controller) {
        return createCanvasMenu(canvas, controller, null);
    }

    public static ContextMenu createCanvasMenu(
            GridChartView canvas, DrawingController controller, DetachedCanvasWindow parentWindow) {
        ContextMenu menu = new ContextMenu();

        MenuItem undoItem = new MenuItem(I18nUtil.getString("geo.menu.undo"));
        undoItem.setDisable(!controller.canUndo());
        undoItem.setOnAction(e -> controller.undo());

        MenuItem redoItem = new MenuItem(I18nUtil.getString("geo.menu.redo"));
        redoItem.setDisable(!controller.canRedo());
        redoItem.setOnAction(e -> controller.redo());

        MenuItem clearItem = new MenuItem(I18nUtil.getString("geo.menu.clear"));
        clearItem.setOnAction(e -> controller.clearAll());

        Menu zoomMenu = new Menu(I18nUtil.getString("menu.zoom"));

        MenuItem zoom25 = new MenuItem("25%");
        zoom25.setOnAction(e -> canvas.zoomToPercent(25));

        MenuItem zoom50 = new MenuItem("50%");
        zoom50.setOnAction(e -> canvas.zoomToPercent(50));

        MenuItem zoom100 = new MenuItem("100%");
        zoom100.setOnAction(e -> canvas.zoomToPercent(100));

        MenuItem zoom200 = new MenuItem("200%");
        zoom200.setOnAction(e -> canvas.zoomToPercent(200));

        MenuItem zoom400 = new MenuItem("400%");
        zoom400.setOnAction(e -> canvas.zoomToPercent(400));

        zoomMenu.getItems().addAll(zoom25, zoom50, zoom100, zoom200, zoom400);

        Menu axisRatioMenu = new Menu(I18nUtil.getString("menu.axisRatio"));

        MenuItem ratio11 = new MenuItem("1:1");
        ratio11.setOnAction(e -> canvas.setAxisRatio(1, 1));

        MenuItem ratio12 = new MenuItem("1:2");
        ratio12.setOnAction(e -> canvas.setAxisRatio(1, 2));

        MenuItem ratio21 = new MenuItem("2:1");
        ratio21.setOnAction(e -> canvas.setAxisRatio(2, 1));

        MenuItem ratio14 = new MenuItem("1:4");
        ratio14.setOnAction(e -> canvas.setAxisRatio(1, 4));

        MenuItem ratio41 = new MenuItem("4:1");
        ratio41.setOnAction(e -> canvas.setAxisRatio(4, 1));

        axisRatioMenu.getItems().addAll(ratio11, ratio12, ratio21, ratio14, ratio41);

        MenuItem fitAllItem = new MenuItem(I18nUtil.getString("menu.showAllObjects"));
        fitAllItem.setOnAction(e -> canvas.fitAllObjects());
        fitAllItem.setDisable(canvas.getObjects().isEmpty());

        MenuItem standardViewItem = new MenuItem(I18nUtil.getString("menu.standardView"));
        standardViewItem.setOnAction(e -> canvas.resetToStandardView());

        MenuItem axesPropsItem = new MenuItem(I18nUtil.getString("menu.axesProperties"));
        axesPropsItem.setOnAction(e -> {
            AxesPropertiesDialog dialog = new AxesPropertiesDialog(canvas.getSettings());
            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    canvas.applySettings();
                }
            });
        });

        MenuItem gridPropsItem = new MenuItem(I18nUtil.getString("menu.gridProperties"));
        gridPropsItem.setOnAction(e -> {
            GridPropertiesDialog dialog = new GridPropertiesDialog(canvas.getSettings());
            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    canvas.applySettings();
                }
            });
        });

        Menu bgColorMenu = new Menu(I18nUtil.getString("geo.menu.backgroundColor"));

        MenuItem whiteItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.white"));
        whiteItem.setOnAction(e -> canvas.setBackgroundColor(Color.WHITE));

        MenuItem grayItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.gray"));
        grayItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(240, 240, 240)));

        MenuItem beigeItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.beige"));
        beigeItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(245, 245, 220)));

        MenuItem blueItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.blue"));
        blueItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(230, 240, 255)));

        MenuItem customItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.custom"));
        customItem.setOnAction(e -> ContextMenuDialogs.showBackgroundColorPicker(canvas));

        bgColorMenu.getItems().addAll(whiteItem, grayItem, beigeItem, blueItem,
                new SeparatorMenuItem(), customItem);

        // 网格类型子菜单
        Menu gridTypeMenu = new Menu(I18nUtil.getString("grid.gridType"));

        MenuItem dotTypeItem = new MenuItem(I18nUtil.getString("gridType.dot"));
        dotTypeItem.setOnAction(e -> {
            setGridMode(canvas, GridMode.DOT);
            canvas.redraw();
        });

        MenuItem lineTypeItem = new MenuItem(I18nUtil.getString("gridType.cartesian"));
        lineTypeItem.setOnAction(e -> {
            setGridMode(canvas, GridMode.SUBGRID);
            canvas.redraw();
        });

        gridTypeMenu.getItems().addAll(dotTypeItem, lineTypeItem);

        MenuItem detachItem = new MenuItem(I18nUtil.getString("geo.menu.detachWindow"));
        detachItem.setOnAction(e -> {
            DetachedCanvasWindow detachedWindow = new DetachedCanvasWindow(canvas, parentWindow);
            controller.addChildWindow(detachedWindow);
            detachedWindow.show();
        });

        menu.getItems().addAll(
                undoItem, redoItem,
                new SeparatorMenuItem(),
                clearItem,
                new SeparatorMenuItem(),
                zoomMenu, axisRatioMenu,
                new SeparatorMenuItem(),
                fitAllItem, standardViewItem,
                new SeparatorMenuItem(),
                axesPropsItem, gridPropsItem,
                new SeparatorMenuItem(),
                bgColorMenu,
                new SeparatorMenuItem(),
                gridTypeMenu,
                new SeparatorMenuItem(),
                detachItem
        );
        return menu;
    }

    private static void setGridMode(GridChartView canvas, GridMode mode) {
        canvas.getPainters().forEach(painter -> {
            if (painter instanceof GridPainter gridPainter) {
                gridPainter.setGridMode(mode);
            }
        });
    }
}