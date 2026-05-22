package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.util.Optional;

/**
 * 上下文菜单对话框工具类
 * <p>
 * 提供重命名、颜色选择、位置设置、属性编辑、背景色等对话框
 *
 * @author bingbaihanji
 * @date 2026-05-23
 */
public final class ContextMenuDialogs {

    private ContextMenuDialogs() {
    }

    /**
     * 显示重命名对话框
     */
    public static void showRenameDialog(PointGeo point, GridChartView canvas) {
        TextInputDialog dialog = new TextInputDialog(point.getName());
        dialog.setTitle(I18nUtil.getString("geo.dialog.rename.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.rename.header"));
        dialog.setContentText(I18nUtil.getString("geo.dialog.rename.content"));
        FxTools.addEscapeKeyHandler(dialog);

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
    public static void showColorPickerDialog(PointGeo point, GridChartView canvas) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.color.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.color.header"));

        ColorPicker picker = new ColorPicker(point.getColor());
        picker.setPrefWidth(200);

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        FxTools.addEscapeKeyHandler(dialog);

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
    public static void showBackgroundColorPicker(GridChartView canvas) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.bgColor.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.bgColor.header"));
        FxTools.setDialogIcon(dialog, "/icon/rgb.png");

        ColorPicker picker = new ColorPicker(canvas.getBackgroundColor());
        picker.setPrefWidth(200);

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        FxTools.addEscapeKeyHandler(dialog);

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
     * 显示位置设置对话框
     */
    public static void showPositionDialog(PointGeo point, GridChartView canvas, DrawingController controller) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.position.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.position.header"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField xField = new TextField(String.format("%.2f", point.getX()));
        TextField yField = new TextField(String.format("%.2f", point.getY()));

        grid.add(new Label(I18nUtil.getString("geo.dialog.position.x")), 0, 0);
        grid.add(xField, 1, 0);
        grid.add(new Label(I18nUtil.getString("geo.dialog.position.y")), 0, 1);
        grid.add(yField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        FxTools.addEscapeKeyHandler(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double newX = Double.parseDouble(xField.getText());
                double newY = Double.parseDouble(yField.getText());
                final double oldX = point.getX();
                final double oldY = point.getY();

                controller.getContext().executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        point.updatePosition(newX, newY);
                        canvas.redraw();
                    }

                    @Override
                    public void undo() {
                        point.updatePosition(oldX, oldY);
                        canvas.redraw();
                    }
                });
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.getString("geo.dialog.position.error.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nUtil.getString("geo.dialog.position.error.invalid"));
                FxTools.addEscapeKeyHandler(alert);
                alert.showAndWait();
            }
        }
    }

    /**
     * 显示顶点位置设置对话框
     */
    public static void showVertexPositionDialog(
            WorldObject.DraggablePoint vertex,
            WorldObject parentShape,
            GridChartView canvas,
            DrawingController controller) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.vertex.position.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.vertex.position.header"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField xField = new TextField(String.format("%.2f", vertex.getX()));
        TextField yField = new TextField(String.format("%.2f", vertex.getY()));

        grid.add(new Label(I18nUtil.getString("geo.dialog.position.x")), 0, 0);
        grid.add(xField, 1, 0);
        grid.add(new Label(I18nUtil.getString("geo.dialog.position.y")), 0, 1);
        grid.add(yField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        FxTools.addEscapeKeyHandler(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double newX = Double.parseDouble(xField.getText());
                double newY = Double.parseDouble(yField.getText());
                final double oldX = vertex.getX();
                final double oldY = vertex.getY();

                controller.getContext().executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        vertex.updatePosition(newX, newY);
                        controller.getContext().getConstraintHandler().updateAllConstrainedPoints(controller.getContext());
                        controller.getContext().getIntersectionHandler().recalculateAllIntersections(controller.getContext());
                        canvas.redraw();
                    }

                    @Override
                    public void undo() {
                        vertex.updatePosition(oldX, oldY);
                        controller.getContext().getConstraintHandler().updateAllConstrainedPoints(controller.getContext());
                        controller.getContext().getIntersectionHandler().recalculateAllIntersections(controller.getContext());
                        canvas.redraw();
                    }
                });
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.getString("geo.dialog.position.error.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nUtil.getString("geo.dialog.position.error.invalid"));
                FxTools.addEscapeKeyHandler(alert);
                alert.showAndWait();
            }
        }
    }

    /**
     * 显示几何图形属性对话框
     */
    public static void showPropertiesDialog(WorldObject shape, GridChartView canvas) {
        ShapePropertiesDialog dialog;

        if (shape instanceof CircleGeo circle) {
            dialog = new ShapePropertiesDialog(circle.getColor(), circle.getR(), circle.getCenterName());
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                circle.setColor(props.getColor());
                circle.setR(props.getRadius());
                if (props.getCenterName() != null && !props.getCenterName().isEmpty()) {
                    circle.setCenterName(props.getCenterName());
                }
                canvas.redraw();
            });
        } else if (shape instanceof LineGeo line) {
            dialog = new ShapePropertiesDialog(line.getColor());
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                line.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof InfiniteLineGeo infiniteLine) {
            dialog = new ShapePropertiesDialog(infiniteLine.getColor());
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                infiniteLine.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof PathGeo path) {
            dialog = new ShapePropertiesDialog(path.getColor());
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                path.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof PolygonGeo polygon) {
            dialog = new ShapePropertiesDialog(polygon.getColor());
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                polygon.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof FunctionGeo function) {
            dialog = new ShapePropertiesDialog(function.getColor());
            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                function.setColor(props.getColor());
                canvas.redraw();
            });
        }
    }

    /**
     * 删除对象(自动移除关联约束)
     */
    public static void deleteObject(WorldObject obj, GridChartView canvas, DrawingController controller) {
        for (WorldObject o : canvas.getObjects()) {
            if (o instanceof PointGeo point && point.isConstrained()) {
                if (point.getConstraint().getConstrainedShape() == obj) {
                    point.setConstraint(null);
                }
            }
        }
        canvas.removeObject(obj);
        canvas.redraw();
    }
}