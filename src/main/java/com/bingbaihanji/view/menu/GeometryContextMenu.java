package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.Optional;

/**
 * 几何图形右键菜单管理器
 * <p>
 * 提供点、图形和画布的右键菜单功能
 * 支持窗口层级管理：从任意窗口打开的子窗口会自动注册到父窗口的控制器中
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

        // 属性
        MenuItem propertiesItem = new MenuItem(I18nUtil.getString("geo.menu.properties"));
        propertiesItem.setOnAction(e -> showPropertiesDialog(shape, canvas));

        // 删除
        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> deleteObject(shape, canvas, controller));

        menu.getItems().addAll(propertiesItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    /**
     * 为画布创建右键菜单
     */
    public static ContextMenu createCanvasMenu(
            GridChartView canvas,
            DrawingController controller
    ) {
        return createCanvasMenu(canvas, controller, null);
    }

    /**
     * 为画布创建右键菜单（带父窗口引用）
     *
     * @param canvas 画布视图
     * @param controller 绘制控制器
     * @param parentWindow 父窗口（如果在独立窗口中则传入，否则为null）
     */
    public static ContextMenu createCanvasMenu(
            GridChartView canvas,
            DrawingController controller,
            DetachedCanvasWindow parentWindow
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
            DetachedCanvasWindow detachedWindow = new DetachedCanvasWindow(canvas, parentWindow);
            // 将新窗口注册到当前控制器的子窗口列表中
            controller.addChildWindow(detachedWindow);
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
     * 显示几何图形属性对话框
     */
    private static void showPropertiesDialog(WorldObject shape, GridChartView canvas) {
        ShapePropertiesDialog dialog;
        
        // 根据不同的图形类型创建对话框
        if (shape instanceof CircleGeo circle) {
            // 圆形：支持颜色和半径修改
            dialog = new ShapePropertiesDialog(circle.getColor(), circle.getR());
            
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                circle.setColor(props.getColor());
                circle.setR(props.getRadius());
                canvas.redraw();
            });
        } else if (shape instanceof LineGeo line) {
            // 线段：仅支持颜色修改
            dialog = new ShapePropertiesDialog(line.getColor());
            
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                line.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof InfiniteLineGeo infiniteLine) {
            // 直线：仅支持颜色修改
            dialog = new ShapePropertiesDialog(infiniteLine.getColor());
            
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                infiniteLine.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof PathGeo path) {
            // 手绘路径：仅支持颜色修改
            dialog = new ShapePropertiesDialog(path.getColor());
            
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                path.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof PolygonGeo polygon) {
            // 多边形：仅支持颜色修改
            dialog = new ShapePropertiesDialog(polygon.getColor());
            
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                polygon.setColor(props.getColor());
                canvas.redraw();
            });
        }
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
