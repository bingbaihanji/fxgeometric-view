package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.controller.GeometryCommand;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.geometry.PointReuseManager;
import com.bingbaihanji.geometry.WorldTransform;
import com.bingbaihanji.geometry.impl.PointGeo;
import com.bingbaihanji.geometry.impl.RegularPolygonGeo;
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
    /**
     * 正多边形预览是否激活
     */
    private boolean polygonPreviewActive;
    /**
     * 预览半径(世界坐标), 由中心到鼠标顶点距离决定
     */
    private double previewRadius;
    /**
     * 预览边数
     */
    private int previewSideCount = 6;
    /**
     * 预览鼠标位置(决定第一个顶点的方向)
     */
    private double previewMouseX;
    private double previewMouseY;

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

        if (context.getState() == DrawingState.IDLE) {
            handleFirstClick(worldX, worldY, context);
        } else if (context.getState() == DrawingState.FIRST_CLICK) {
            handleSecondClick(worldX, worldY, context);
        }

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

        // 应用网格吸附
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        // 保存当前鼠标位置用于预览
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 如果在预览状态,更新预览(鼠标位置就是第一个顶点的位置)
        if (context.getState() == DrawingState.FIRST_CLICK && polygonPreviewActive) {
            previewMouseX = worldX;
            previewMouseY = worldY;
            previewRadius = Math.hypot(worldX - centerX, worldY - centerY);
        }

        // 重绘以显示预览
        context.redraw();
        return true;
    }

    /**
     * 处理第一次点击(确定中心)
     */
    private void handleFirstClick(double worldX, double worldY, IDrawingContext context) {
        // 记录中心点
        centerX = worldX;
        centerY = worldY;
        context.setState(DrawingState.FIRST_CLICK);

        // 检查中心点位置是否已有点对象(用于复用)
        double scale = context.getTransform().getScale();
        centerPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        // 进入预览状态
        polygonPreviewActive = true;
        previewSideCount = Math.max(3, Math.min(10, selectedSides));
        previewMouseX = centerX;
        previewMouseY = centerY;
        previewRadius = 0;
    }

    /**
     * 处理第二次点击(确定半径)
     */
    private void handleSecondClick(double worldX, double worldY, IDrawingContext context) {
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

        context.executeCommand(new GeometryCommand(context, newPolygon, vertexPoints));

        // 清理状态
        cleanupPreview(context);
        centerPointRef = null;
        context.setState(DrawingState.IDLE);
    }

    /**
     * 清理预览对象
     */
    private void cleanupPreview(IDrawingContext context) {
        polygonPreviewActive = false;
        previewRadius = 0;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, IDrawingContext context) {
        // 只在正多边形模式下绘制预览
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        double pointRadius = 4;

        // 在 IDLE 状态下,显示吸附预览点
        if (context.getState() == DrawingState.IDLE) {
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            gc.setStroke(GeometryConfig.Colors.PREVIEW);
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);

            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
            return;
        }

        // 在 FIRST_CLICK 状态下,绘制中心点和鼠标位置点(顶点预览)
        if (context.getState() == DrawingState.FIRST_CLICK) {
            // 正多边形轮廓与半径线
            if (polygonPreviewActive && previewRadius > 1e-6) {
                paintRegularPolygonOutline(gc, transform);
            }

            // 绘制中心点
            double centerScreenX = transform.worldToScreenX(centerX);
            double centerScreenY = transform.worldToScreenY(centerY);
            gc.setFill(GeometryConfig.Colors.PREVIEW);
            gc.fillOval(centerScreenX - pointRadius, centerScreenY - pointRadius, pointRadius * 2, pointRadius * 2);

            // 绘制鼠标位置点(第一个顶点位置)
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            gc.setFill(GeometryConfig.Colors.PREVIEW);
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }
    }

    /**
     * 绘制正多边形预览轮廓与半径线
     *
     * @param gc        图形上下文
     * @param transform 坐标变换
     */
    private void paintRegularPolygonOutline(GraphicsContext gc, WorldTransform transform) {
        // 第一个顶点指向鼠标方向
        double angleToMouse = Math.atan2(previewMouseY - centerY, previewMouseX - centerX);
        double angleStep = 2 * Math.PI / previewSideCount;

        double[] xPoints = new double[previewSideCount];
        double[] yPoints = new double[previewSideCount];
        for (int i = 0; i < previewSideCount; i++) {
            double angle = angleToMouse + i * angleStep;
            double worldX = centerX + previewRadius * Math.cos(angle);
            double worldY = centerY + previewRadius * Math.sin(angle);
            xPoints[i] = transform.worldToScreenX(worldX);
            yPoints[i] = transform.worldToScreenY(worldY);
        }

        gc.save();
        gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
        gc.setLineWidth(1.5);
        gc.setLineDashes(6);
        gc.strokePolygon(xPoints, yPoints, previewSideCount);

        // 半径线: 中心指向鼠标顶点
        gc.setLineDashes(2);
        gc.strokeLine(transform.worldToScreenX(centerX), transform.worldToScreenY(centerY),
                transform.worldToScreenX(previewMouseX), transform.worldToScreenY(previewMouseY));
        gc.restore();
    }
}
