package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.controller.PreviewManager;
import com.bingbaihanji.util.GeometryCommand;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PolygonGeo;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形绘制处理器
 * <p>
 * 处理多边形的逐点选择和闭合绘制
 * 支持点复用：如果点击位置已有点,直接复用该点
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class PolygonHandler extends AbstractDrawingHandler {

    /**
     * 多边形顶点点对象列表
     * 直接存储PointGeo引用,实现点复用
     */
    private final List<PointGeo> vertexPoints = new ArrayList<>();

    /**
     * 多边形内部创建的新点列表(用于撤销时删除)
     */
    private final List<PointGeo> newCreatedPoints = new ArrayList<>();

    /**
     * 多边形预览对象
     */
    private PreviewManager.PolygonPreview polygonPreview = null;

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.POLYGON;
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

        handlePolygonClick(worldX, worldY, context);

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

        // 更新多边形预览
        if (polygonPreview != null && polygonPreview.isActive()) {
            polygonPreview.updatePreview(worldX, worldY);
        }

        // 重绘以显示预览(IDLE 和 POLYGON_DRAWING 状态都需要重绘)
        context.redraw();
        return true;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, IDrawingContext context) {
        // PreviewManager 统一管理,此处只绘制补充效果(吸附预览点、顶点高亮)
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        double pointRadius = 3;

        // IDLE 状态：显示吸附预览点(第一次点击前)
        if (context.getState() == DrawingState.IDLE) {
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            // 绘制吸附预览点(浅色圆圈 + 中心点)
            gc.setStroke(GeometryConfig.Colors.PREVIEW);
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);

            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
            return;
        }

        // POLYGON_DRAWING 状态：绘制顶点和高亮效果
        if (context.getState() != DrawingState.POLYGON_DRAWING) {
            return;
        }

        if (vertexPoints.isEmpty()) {
            return;
        }

        // 绘制顶点(小圆点)
        gc.setFill(GeometryConfig.Colors.PREVIEW);
        for (PointGeo vertex : vertexPoints) {
            double vx = transform.worldToScreenX(vertex.getX());
            double vy = transform.worldToScreenY(vertex.getY());
            gc.fillOval(vx - pointRadius, vy - pointRadius, pointRadius * 2, pointRadius * 2);
        }

        // 高亮第一个顶点(可以闭合多边形)
        boolean nearFirstVertex = false;
        if (vertexPoints.size() >= 3) {
            PointGeo firstVertex = vertexPoints.get(0);
            double fx = transform.worldToScreenX(firstVertex.getX());
            double fy = transform.worldToScreenY(firstVertex.getY());

            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            // 检查鼠标是否靠近第一个顶点
            double screenDistance = MathCalculationUtils.hypot(mouseScreenX - fx, mouseScreenY - fy);
            double threshold = 15.0; // 屏幕像素阈值

            if (screenDistance < threshold) {
                nearFirstVertex = true;
                // 鼠标靠近第一个顶点,显示更明显的闭合提示
                gc.setFill(GeometryConfig.Colors.CLOSE_HIGHLIGHT);
                gc.fillOval(fx - pointRadius * 2, fy - pointRadius * 2, pointRadius * 4, pointRadius * 4);

                // 绘制闭合线(从最后一个顶点到第一个顶点)
                PointGeo lastVertex = vertexPoints.get(vertexPoints.size() - 1);
                double lastScreenX = transform.worldToScreenX(lastVertex.getX());
                double lastScreenY = transform.worldToScreenY(lastVertex.getY());

                gc.setStroke(GeometryConfig.Colors.CLOSE_HIGHLIGHT);
                gc.setLineWidth(2);
                gc.setLineDashes(4);
                gc.strokeLine(lastScreenX, lastScreenY, fx, fy);
                gc.setLineDashes(null);
            } else {
                // 正常高亮第一个顶点
                gc.setFill(GeometryConfig.Colors.CLOSE_HIGHLIGHT);
                gc.fillOval(fx - pointRadius * 1.5, fy - pointRadius * 1.5, pointRadius * 3, pointRadius * 3);
            }
        }

        // 绘制当前鼠标位置的吸附预览点(如果不是靠近第一个顶点)
        if (!nearFirstVertex) {
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            gc.setStroke(GeometryConfig.Colors.PREVIEW);
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);

            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }
    }

    @Override
    public void reset() {
        vertexPoints.clear();
        newCreatedPoints.clear();

        // 清除预览对象
        polygonPreview = null;
    }

    /**
     * 处理多边形点击事件
     */
    private void handlePolygonClick(double worldX, double worldY, IDrawingContext context) {
        double scale = context.getTransform().getScale();
        double threshold = GeometryConfig.Tolerance.POLYGON_CLOSE_THRESHOLD_PIXELS / scale; // 多边形闭合阈值

        // 检查是否与起点重合(闭合多边形)
        if (!vertexPoints.isEmpty()) {
            PointGeo firstVertex = vertexPoints.get(0);
            double distance = MathCalculationUtils.hypot(worldX - firstVertex.getX(), worldY - firstVertex.getY());

            if (distance < threshold && vertexPoints.size() >= 3) {
                // 完成多边形绘制
                finishPolygon(context);
                return;
            }
        }

        // 检查点击位置是否已有点对象(复用已有点)
        PointGeo existingPoint = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        if (existingPoint != null) {
            // 复用已有的点
            vertexPoints.add(existingPoint);
        } else {
            // 创建新的点对象(多边形内部顶点)
            PointGeo newPoint = new PointGeo(worldX, worldY);
            newPoint.setPolygonVertex(true); // 标记为多边形内部顶点
            vertexPoints.add(newPoint);
            newCreatedPoints.add(newPoint); // 记录新创建的点,用于撤销
        }

        // 创建或更新预览对象
        if (polygonPreview == null) {
            // 第一个点：创建预览对象并注册
            polygonPreview = new PreviewManager.PolygonPreview();
            context.getPreviewManager().addPreviewable(polygonPreview);
        }
        // 添加点到预览对象
        polygonPreview.addPoint(worldX, worldY);

        // 进入多边形绘制状态
        context.setState(DrawingState.POLYGON_DRAWING);

        // 重绘以显示预览
        context.redraw();
    }

    /**
     * 完成多边形绘制
     */
    private void finishPolygon(IDrawingContext context) {
        if (vertexPoints.size() < 3) {
            return;
        }

        // 创建多边形对象,直接传入点引用列表
        PolygonGeo polygon = new PolygonGeo(new ArrayList<>(vertexPoints));

        context.executeCommand(new GeometryCommand(context, polygon, newCreatedPoints));

        // 清除预览对象
        if (polygonPreview != null) {
            context.getPreviewManager().removePreviewable(polygonPreview);
            polygonPreview = null;
        }

        // 重置状态
        vertexPoints.clear();
        newCreatedPoints.clear();
        context.setState(DrawingState.IDLE);
        context.redraw();
    }
}
