package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.util.Optional;

/**
 * 旋转处理器
 * <p>
 * 处理图形的旋转操作
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class RotationHandler extends AbstractDrawingHandler {

    /**
     * 已选中的要旋转的图形
     */
    private WorldObject selectedRotateShape = null;

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.ROTATE;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, IDrawingContext context) {
        // 只处理左键
        if (e.getButton() != MouseButton.PRIMARY || !canHandle(context.getDrawMode())) {
            return false;
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用特殊点磁性吸附
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        handleRotateClick(worldX, worldY, context);

        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, IDrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用特殊点磁性吸附
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        // 保存当前鼠标位置用于预览
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 重绘以显示预览
        context.redraw();
        return true;
    }

    @Override
    public void paintPreview(javafx.scene.canvas.GraphicsContext gc, com.bingbaihanji.view.layout.core.WorldTransform transform, IDrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        if (context.getState() == DrawingState.ROTATE_SELECT_SHAPE) {
            // 在选择图形状态,高亮显示鼠标悬停的可旋转图形
            double scale = context.getTransform().getScale();
            double tolerance = GeometryConfig.Tolerance.VERTEX_HIT_TEST_PIXELS / scale;
            double worldX = context.getCurrentMouseX();
            double worldY = context.getCurrentMouseY();

            for (WorldObject obj : context.getObjects()) {
                // 排除点对象(点旋转没有意义)
                if (obj instanceof PointGeo) continue;

                if (obj.hitTest(worldX, worldY, tolerance)) {
                    // 高亮显示可选择的图形
                    obj.setHover(true);
                    obj.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
                    obj.setHover(false);
                    break; // 只高亮最上层的一个图形
                }
            }
        } else if (context.getState() == DrawingState.ROTATE_SELECT_CENTER && selectedRotateShape != null) {
            // 如果已选中旋转图形,高亮显示
            selectedRotateShape.setHover(true);
            selectedRotateShape.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
            selectedRotateShape.setHover(false);

            // 绘制旋转中心点预览
            double centerScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double centerScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            gc.setFill(GeometryConfig.Colors.CONSTRUCTION_HIGHLIGHT);
            double pointRadius = 5;
            gc.fillOval(centerScreenX - pointRadius, centerScreenY - pointRadius, pointRadius * 2, pointRadius * 2);

            // 绘制中心点标记(十字线)
            gc.setStroke(GeometryConfig.Colors.CONSTRUCTION_HIGHLIGHT);
            gc.setLineWidth(1);
            double crossSize = 10;
            gc.strokeLine(centerScreenX - crossSize, centerScreenY, centerScreenX + crossSize, centerScreenY);
            gc.strokeLine(centerScreenX, centerScreenY - crossSize, centerScreenX, centerScreenY + crossSize);
        }
    }

    @Override
    public void reset() {
        selectedRotateShape = null;
    }

    /**
     * 处理旋转模式的点击事件
     */
    private void handleRotateClick(double worldX, double worldY, IDrawingContext context) {
        if (context.getState() == DrawingState.ROTATE_SELECT_SHAPE) {
            // 第一次点击：选择要旋转的图形
            double scale = context.getTransform().getScale();
            double tolerance = GeometryConfig.Tolerance.VERTEX_HIT_TEST_PIXELS / scale;

            for (WorldObject obj : context.getObjects()) {
                // 排除点对象(点旋转没有意义)
                if (obj instanceof PointGeo) continue;

                if (obj.hitTest(worldX, worldY, tolerance)) {
                    selectedRotateShape = obj;
                    context.setState(DrawingState.ROTATE_SELECT_CENTER);
                    context.redraw();
                    return;
                }
            }
        } else if (context.getState() == DrawingState.ROTATE_SELECT_CENTER && selectedRotateShape != null) {
            // 第二次点击：选择旋转中心点,弹出对话框
            final double rotateCenterX = worldX;
            final double rotateCenterY = worldY;

            // 创建旋转参数对话框
            Optional<Pair<Double, Boolean>> result = showRotateDialog();

            if (result.isPresent()) {
                double angleDegrees = result.get().getKey();
                boolean clockwise = result.get().getValue();

                // 将角度转换为弧度,并根据方向调整
                double angleRadians = Math.toRadians(angleDegrees);
                if (clockwise) {
                    angleRadians = -angleRadians; // 顺时针方向为负角度
                }

                final double finalAngle = angleRadians;
                final WorldObject shapeToRotate = selectedRotateShape;

                // 使用命令历史执行旋转,支持撤销/恢复
                context.executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        shapeToRotate.rotateAroundPoint(rotateCenterX, rotateCenterY, finalAngle);
                    }

                    @Override
                    public void undo() {
                        // 反向旋转
                        shapeToRotate.rotateAroundPoint(rotateCenterX, rotateCenterY, -finalAngle);
                    }
                });

                // 重新计算交点
                context.getIntersectionHandler().recalculateAllIntersections(context);
            }

            // 重置状态
            selectedRotateShape = null;
            context.setState(DrawingState.ROTATE_SELECT_SHAPE);
            context.redraw();
        }
    }

    /**
     * 显示旋转参数对话框
     *
     * @return 旋转角度和方向(true为顺时针,false为逆时针)
     */
    private Optional<Pair<Double, Boolean>> showRotateDialog() {
        Dialog<Pair<Double, Boolean>> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("rotating.windows.title"));
        dialog.setHeaderText(I18nUtil.getString("rotating.windows.header"));

        // 设置对话框图标
        FxTools.setDialogIcon(dialog, "/icon/rotating.png");

        // 设置按钮
        ButtonType confirmButtonType = new ButtonType(I18nUtil.getString("rotating.windows.okButton"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 角度输入框
        TextField angleField = new TextField();
        angleField.setPromptText(I18nUtil.getString("rotating.windows.angle"));
        angleField.setText("90");

        // 方向选择
        ToggleGroup directionGroup = new ToggleGroup();
        RadioButton clockwiseBtn = new RadioButton(I18nUtil.getString("rotating.windows.clockwise"));
        clockwiseBtn.setToggleGroup(directionGroup);
        clockwiseBtn.setSelected(true);
        RadioButton counterclockwiseBtn = new RadioButton(I18nUtil.getString("rotating.windows.counterclockwise"));
        counterclockwiseBtn.setToggleGroup(directionGroup);

        grid.add(new Label(I18nUtil.getString("rotating.windows.rotationAngle")), 0, 0);
        grid.add(angleField, 1, 0);
        grid.add(new Label(I18nUtil.getString("rotating.windows.rotationDirection")), 0, 1);
        grid.add(clockwiseBtn, 1, 1);
        grid.add(counterclockwiseBtn, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    double angle = Double.parseDouble(angleField.getText());
                    boolean clockwise = clockwiseBtn.isSelected();
                    return new Pair<>(angle, clockwise);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
