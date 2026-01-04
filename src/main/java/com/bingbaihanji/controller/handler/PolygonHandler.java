package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PolygonGeo;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形绘制处理器
 * <p>
 * 处理多边形的逐点选择和闭合绘制
 * 支持点复用：如果点击位置已有点，直接复用该点
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class PolygonHandler extends AbstractDrawingHandler {

    /**
     * 多边形顶点点对象列表
     * 直接存储PointGeo引用，实现点复用
     */
    private final List<PointGeo> vertexPoints = new ArrayList<>();

    /**
     * 多边形内部创建的新点列表（用于撤销时删除）
     */
    private final List<PointGeo> newCreatedPoints = new ArrayList<>();

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.POLYGON;
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

        handlePolygonClick(worldX, worldY, context);

        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || context.getState() != DrawingState.POLYGON_DRAWING) {
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
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || context.getState() != DrawingState.POLYGON_DRAWING) {
            return;
        }

        if (vertexPoints.isEmpty()) {
            return;
        }

        // 绘制已有的边（浅色虚线）
        gc.setStroke(Color.valueOf("#759eb2"));
        gc.setLineWidth(1);
        gc.setLineDashes(6);

        for (int i = 0; i < vertexPoints.size() - 1; i++) {
            PointGeo p1 = vertexPoints.get(i);
            PointGeo p2 = vertexPoints.get(i + 1);

            double sx1 = transform.worldToScreenX(p1.getX());
            double sy1 = transform.worldToScreenY(p1.getY());
            double sx2 = transform.worldToScreenX(p2.getX());
            double sy2 = transform.worldToScreenY(p2.getY());

            gc.strokeLine(sx1, sy1, sx2, sy2);
        }

        // 绘制从最后一个顶点到当前鼠标位置的边
        PointGeo lastVertex = vertexPoints.get(vertexPoints.size() - 1);
        double lastScreenX = transform.worldToScreenX(lastVertex.getX());
        double lastScreenY = transform.worldToScreenY(lastVertex.getY());
        double currentScreenX = transform.worldToScreenX(context.getCurrentMouseX());
        double currentScreenY = transform.worldToScreenY(context.getCurrentMouseY());

        gc.strokeLine(lastScreenX, lastScreenY, currentScreenX, currentScreenY);

        gc.setLineDashes(null);

        // 绘制顶点（小圆点）
        gc.setFill(Color.valueOf("#759eb2"));
        double pointRadius = 3;
        for (PointGeo vertex : vertexPoints) {
            double vx = transform.worldToScreenX(vertex.getX());
            double vy = transform.worldToScreenY(vertex.getY());
            gc.fillOval(vx - pointRadius, vy - pointRadius, pointRadius * 2, pointRadius * 2);
        }

        // 高亮第一个顶点（可以闭合多边形）
        if (vertexPoints.size() >= 3) {
            PointGeo firstVertex = vertexPoints.get(0);
            double fx = transform.worldToScreenX(firstVertex.getX());
            double fy = transform.worldToScreenY(firstVertex.getY());

            // 检查鼠标是否靠近第一个顶点
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            double screenDistance = Math.hypot(mouseScreenX - fx, mouseScreenY - fy);

            double threshold = 15.0; // 屏幕像素阈值

            if (screenDistance < threshold) {
                // 鼠标靠近第一个顶点，显示更明显的闭合提示
                gc.setFill(Color.LIGHTGREEN);
                gc.fillOval(fx - pointRadius * 2, fy - pointRadius * 2, pointRadius * 4, pointRadius * 4);

                // 绘制闭合线（从最后一个顶点到第一个顶点）
                gc.setStroke(Color.LIGHTGREEN);
                gc.setLineWidth(2);
                gc.setLineDashes(4);
                gc.strokeLine(lastScreenX, lastScreenY, fx, fy);
                gc.setLineDashes(null);
            } else {
                // 正常高亮第一个顶点
                gc.setFill(Color.LIGHTGREEN);
                gc.fillOval(fx - pointRadius * 1.5, fy - pointRadius * 1.5, pointRadius * 3, pointRadius * 3);
            }
        }
    }

    @Override
    public void reset() {
        vertexPoints.clear();
        newCreatedPoints.clear();
    }

    /**
     * 处理多边形点击事件
     */
    private void handlePolygonClick(double worldX, double worldY, DrawingContext context) {
        double scale = context.getTransform().getScale();
        double threshold = 15.0 / scale; // 15像素的吸附范围

        // 检查是否与起点重合（闭合多边形）
        if (!vertexPoints.isEmpty()) {
            PointGeo firstVertex = vertexPoints.get(0);
            double distance = Math.hypot(worldX - firstVertex.getX(), worldY - firstVertex.getY());

            if (distance < threshold && vertexPoints.size() >= 3) {
                // 完成多边形绘制
                finishPolygon(context);
                return;
            }
        }

        // 检查点击位置是否已有点对象（复用已有点）
        PointGeo existingPoint = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        if (existingPoint != null) {
            // 复用已有的点
            vertexPoints.add(existingPoint);
        } else {
            // 创建新的点对象（多边形内部顶点）
            PointGeo newPoint = new PointGeo(worldX, worldY);
            newPoint.setPolygonVertex(true); // 标记为多边形内部顶点
            vertexPoints.add(newPoint);
            newCreatedPoints.add(newPoint); // 记录新创建的点，用于撤销
        }

        // 进入多边形绘制状态
        context.setState(DrawingState.POLYGON_DRAWING);

        // 重绘以显示预览
        context.redraw();
    }

    /**
     * 完成多边形绘制
     */
    private void finishPolygon(DrawingContext context) {
        if (vertexPoints.size() < 3) {
            return;
        }

        // 创建多边形对象，直接传入点引用列表
        PolygonGeo polygon = new PolygonGeo(new ArrayList<>(vertexPoints));

        // 复制新创建的点列表（用于撤销时删除）
        List<PointGeo> createdPoints = new ArrayList<>(newCreatedPoints);

        // 计算此多边形产生的所有交点
        List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                .checkIntersections(polygon, context);

        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                // 添加多边形内部创建的新点到对象列表
                for (PointGeo point : createdPoints) {
                    context.addObject(point);
                }
                // 添加多边形
                context.addObject(polygon);
                // 添加交点
                for (PointGeo point : intersectionPoints) {
                    context.addObject(point);
                }
            }

            @Override
            public void undo() {
                context.removeObject(polygon);
                // 移除多边形内部创建的新点
                for (PointGeo point : createdPoints) {
                    context.removeObject(point);
                }
                // 移除交点
                for (PointGeo point : intersectionPoints) {
                    context.removeObject(point);
                }
            }
        });

        // 重置状态
        vertexPoints.clear();
        newCreatedPoints.clear();
        context.setState(DrawingState.IDLE);
        context.redraw();
    }
}
