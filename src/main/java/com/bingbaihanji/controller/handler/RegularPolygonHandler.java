package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.controller.PreviewManager;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.RegularPolygonGeo;
import com.bingbaihanji.view.menu.RegularPolygonSidesDialog;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.Optional;

/**
 * 正多边形绘制处理器
 * <p>
 * 处理正多边形的绘制：第一次点击确定中心,第二次点击确定半径
 * 点击工具时先弹出边数选择对话框
 *
 * @author bingbaihanji
 * @date 2026-01-10
 */
public class RegularPolygonHandler extends AbstractDrawingHandler {

    /**
     * 正多边形预览对象
     */
    private PreviewManager.RegularPolygonPreview polygonPreview = null;

    /**
     * 中心点的世界坐标
     */
    private double centerX;
    private double centerY;

    /**
     * 第一次点击时复用的点引用
     */
    private PointGeo centerPointRef = null;

    /**
     * 用户选择的边数(3-10)
     */
    private int selectedSides = 6;

    /**
     * 显示边数选择对话框(由外部调用,比如按钮点击时)
     */
    public void showSidesSelectionDialog() {
        RegularPolygonSidesDialog dialog = new RegularPolygonSidesDialog(selectedSides);
        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(sides -> selectedSides = sides);
    }

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.REGULAR_POLYGON;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, DrawingContext context) {
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

        if (context.getState() == DrawingState.IDLE) {
            handleFirstClick(worldX, worldY, context);
        } else if (context.getState() == DrawingState.FIRST_CLICK) {
            handleSecondClick(worldX, worldY, context);
        }

        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用网格吸附
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        // 保存当前鼠标位置用于预览
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 如果在预览状态,更新预览
        if (context.getState() == DrawingState.FIRST_CLICK && polygonPreview != null) {
            // 鼠标位置就是第一个顶点的位置
            // 更新正多边形预览(鼠标位置作为顶点位置)
            polygonPreview.updatePreviewWithVertex(worldX, worldY);
        }

        // 重绘以显示预览
        context.redraw();
        return true;
    }

    /**
     * 处理第一次点击(确定中心)
     */
    private void handleFirstClick(double worldX, double worldY, DrawingContext context) {
        // 记录中心点
        centerX = worldX;
        centerY = worldY;
        context.setState(DrawingState.FIRST_CLICK);

        // 检查中心点位置是否已有点对象(用于复用)
        double scale = context.getTransform().getScale();
        centerPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        // 创建预览对象并注册到 PreviewManager
        polygonPreview = new PreviewManager.RegularPolygonPreview();
        polygonPreview.setCenterPoint(centerX, centerY);
        polygonPreview.setSideCount(selectedSides);
        context.getPreviewManager().addPreviewable(polygonPreview);
    }

    /**
     * 处理第二次点击(确定半径)
     */
    private void handleSecondClick(double worldX, double worldY, DrawingContext context) {
        // 鼠标位置是第一个顶点的位置,计算半径
        double radius = MathCalculationUtils.sqrt(
                MathCalculationUtils.pow(worldX - centerX, 2) + MathCalculationUtils.pow(worldY - centerY, 2)
        );

        // 计算第一个顶点的角度
        double startAngle = Math.atan2(worldY - centerY, worldX - centerX);

        // 创建正多边形,复用已有的中心点,并指定起始角度
        RegularPolygonGeo newPolygon = new RegularPolygonGeo(centerPointRef, centerX, centerY, radius, selectedSides, startAngle);

        // 获取顶点点对象列表
        List<PointGeo> vertexPoints = newPolygon.getVertexPoints();

        // 计算此正多边形产生的所有交点
        List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                .checkIntersections(newPolygon, context);

        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                context.addObject(newPolygon);
                // 添加顶点点对象到场景
                for (PointGeo vertexPoint : vertexPoints) {
                    context.addObject(vertexPoint);
                }
                // 添加交点
                for (PointGeo point : intersectionPoints) {
                    context.addObject(point);
                }
            }

            @Override
            public void undo() {
                context.removeObject(newPolygon);
                // 移除顶点点对象
                for (PointGeo vertexPoint : vertexPoints) {
                    context.removeObject(vertexPoint);
                }
                // 移除交点
                for (PointGeo point : intersectionPoints) {
                    context.removeObject(point);
                }
            }
        });

        // 清理状态
        cleanupPreview(context);
        centerPointRef = null;
        context.setState(DrawingState.IDLE);
    }

    /**
     * 清理预览对象
     */
    private void cleanupPreview(DrawingContext context) {
        if (polygonPreview != null) {
            context.getPreviewManager().removePreviewable(polygonPreview);
            polygonPreview = null;
        }
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        // 只在正多边形模式下绘制预览
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        double pointRadius = 4;

        // 在 IDLE 状态下,显示吸附预览点
        if (context.getState() == DrawingState.IDLE) {
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            gc.setStroke(javafx.scene.paint.Color.valueOf("#759eb2"));
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);

            gc.setFill(javafx.scene.paint.Color.valueOf("#759eb2").deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
            return;
        }

        // 在 FIRST_CLICK 状态下,绘制中心点和鼠标位置点(顶点预览)
        if (context.getState() == DrawingState.FIRST_CLICK) {
            // 绘制中心点
            double centerScreenX = transform.worldToScreenX(centerX);
            double centerScreenY = transform.worldToScreenY(centerY);
            gc.setFill(javafx.scene.paint.Color.valueOf("#759eb2"));
            gc.fillOval(centerScreenX - pointRadius, centerScreenY - pointRadius, pointRadius * 2, pointRadius * 2);

            // 绘制鼠标位置点(第一个顶点位置)
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            gc.setFill(javafx.scene.paint.Color.valueOf("#759eb2"));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }
    }
}
