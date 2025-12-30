package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.Optional;

/**
 * 几何图形右键菜单管理器
 * <p>
 * 提供点、图形和画布的右键菜单功能
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class GeometryContextMenu {

    /**
     * 为点创建右键菜单
     */
    public static ContextMenu createPointMenu(
            PointGeo point,
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        // 修改名称
        MenuItem renameItem = new MenuItem(I18nUtil.getString("geo.menu.rename"));
        renameItem.setOnAction(e -> showRenameDialog(point, canvas));

        // 修改颜色
        MenuItem colorItem = new MenuItem(I18nUtil.getString("geo.menu.changeColor"));
        colorItem.setOnAction(e -> showColorPickerDialog(point, canvas));

        // 移除约束（仅约束点显示）
        if (point.isConstrained()) {
            MenuItem removeConstraintItem = new MenuItem(I18nUtil.getString("geo.menu.removeConstraint"));
            removeConstraintItem.setOnAction(e -> {
                point.setConstraint(null);
                canvas.redraw();
            });
            menu.getItems().add(removeConstraintItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        // 删除
        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> deleteObject(point, canvas, controller));

        menu.getItems().addAll(renameItem, colorItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    /**
     * 为几何图形创建右键菜单
     */
    public static ContextMenu createShapeMenu(
            WorldObject shape,
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        // 删除
        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> deleteObject(shape, canvas, controller));

        menu.getItems().add(deleteItem);
        return menu;
    }

    /**
     * 为画布创建右键菜单
     */
    public static ContextMenu createCanvasMenu(
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        MenuItem undoItem = new MenuItem(I18nUtil.getString("geo.menu.undo"));
        undoItem.setDisable(!controller.canUndo());
        undoItem.setOnAction(e -> controller.undo());

        MenuItem redoItem = new MenuItem(I18nUtil.getString("geo.menu.redo"));
        redoItem.setDisable(!controller.canRedo());
        redoItem.setOnAction(e -> controller.redo());

        MenuItem clearItem = new MenuItem(I18nUtil.getString("geo.menu.clear"));
        clearItem.setOnAction(e -> controller.clearAll());

        // 背景颜色子菜单
        Menu bgColorMenu = new Menu(I18nUtil.getString("geo.menu.backgroundColor"));

        // 预设颜色选项
        MenuItem whiteItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.white"));
        whiteItem.setOnAction(e -> canvas.setBackgroundColor(Color.WHITE));

        MenuItem grayItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.gray"));
        grayItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(240, 240, 240)));

        MenuItem beigeItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.beige"));
        beigeItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(245, 245, 220)));

        MenuItem blueItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.blue"));
        blueItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(230, 240, 255)));

        MenuItem customItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.custom"));
        customItem.setOnAction(e -> showBackgroundColorPicker(canvas));

        bgColorMenu.getItems().addAll(whiteItem, grayItem, beigeItem, blueItem,
                new SeparatorMenuItem(), customItem);

        // 在新窗口打开
        MenuItem detachItem = new MenuItem(I18nUtil.getString("geo.menu.detachWindow"));
        detachItem.setOnAction(e -> {
            DetachedCanvasWindow detachedWindow = new DetachedCanvasWindow(canvas);
            detachedWindow.show();
        });

        menu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(),
                clearItem, new SeparatorMenuItem(), bgColorMenu, new SeparatorMenuItem(), detachItem);
        return menu;
    }

    /**
     * 显示重命名对话框
     */
    private static void showRenameDialog(PointGeo point, GridChartView canvas) {
        TextInputDialog dialog = new TextInputDialog(point.getName());
        dialog.setTitle(I18nUtil.getString("geo.dialog.rename.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.rename.header"));
        dialog.setContentText(I18nUtil.getString("geo.dialog.rename.content"));

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                point.setName(name.trim());
                canvas.redraw();
            }
        });
    }

    /**
     * 显示颜色选择对话框
     */
    private static void showColorPickerDialog(PointGeo point, GridChartView canvas) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.color.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.color.header"));

        ColorPicker picker = new ColorPicker(point.getColor());
        picker.setPrefWidth(200);

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return picker.getValue();
            }
            return null;
        });

        Optional<Color> result = dialog.showAndWait();
        result.ifPresent(color -> {
            point.setColor(color);
            canvas.redraw();
        });
    }

    /**
     * 显示背景颜色选择对话框
     */
    private static void showBackgroundColorPicker(GridChartView canvas) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.bgColor.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.bgColor.header"));

        ColorPicker picker = new ColorPicker(canvas.getBackgroundColor());
        picker.setPrefWidth(200);

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return picker.getValue();
            }
            return null;
        });

        Optional<Color> result = dialog.showAndWait();
        result.ifPresent(canvas::setBackgroundColor);
    }

    /**
     * 删除对象
     */
    private static void deleteObject(WorldObject obj, GridChartView canvas, DrawingController controller) {
        // 查找所有约束到此图形的点，自动移除约束
        for (WorldObject o : canvas.getObjects()) {
            if (o instanceof PointGeo point && point.isConstrained()) {
                if (point.getConstraint().getConstrainedShape() == obj) {
                    point.setConstraint(null);  // 移除约束
                }
            }
        }

        // 删除对象
        canvas.removeObject(obj);
        canvas.redraw();
    }
}
