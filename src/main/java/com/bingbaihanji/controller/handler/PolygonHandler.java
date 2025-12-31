package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PolygonGeo;
import javafx.geometry.Point2D;
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
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class PolygonHandler extends AbstractDrawingHandler {

    /**
     * 多边形顶点列表
     */
    private final List<Point2D> polygonVertices = new ArrayList<>();

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

        if (polygonVertices.isEmpty()) {
            return;
        }

        // 绘制已有的边（浅色虚线）
        gc.setStroke(Color.valueOf("#759eb2"));
        gc.setLineWidth(1);
        gc.setLineDashes(6);

        for (int i = 0; i < polygonVertices.size() - 1; i++) {
            Point2D p1 = polygonVertices.get(i);
            Point2D p2 = polygonVertices.get(i + 1);

            double sx1 = transform.worldToScreenX(p1.getX());
            double sy1 = transform.worldToScreenY(p1.getY());
            double sx2 = transform.worldToScreenX(p2.getX());
            double sy2 = transform.worldToScreenY(p2.getY());

            gc.strokeLine(sx1, sy1, sx2, sy2);
        }

        // 绘制从最后一个顶点到当前鼠标位置的边
        Point2D lastVertex = polygonVertices.get(polygonVertices.size() - 1);
        double lastScreenX = transform.worldToScreenX(lastVertex.getX());
        double lastScreenY = transform.worldToScreenY(lastVertex.getY());
        double currentScreenX = transform.worldToScreenX(context.getCurrentMouseX());
        double currentScreenY = transform.worldToScreenY(context.getCurrentMouseY());

        gc.strokeLine(lastScreenX, lastScreenY, currentScreenX, currentScreenY);

        gc.setLineDashes(null);

        // 绘制顶点（小圆点）
        gc.setFill(Color.valueOf("#759eb2"));
        double pointRadius = 3;
        for (Point2D vertex : polygonVertices) {
            double vx = transform.worldToScreenX(vertex.getX());
            double vy = transform.worldToScreenY(vertex.getY());
            gc.fillOval(vx - pointRadius, vy - pointRadius, pointRadius * 2, pointRadius * 2);
        }

        // 高亮第一个顶点（可以闭合多边形）
        if (polygonVertices.size() >= 3) {
            Point2D firstVertex = polygonVertices.get(0);
            double fx = transform.worldToScreenX(firstVertex.getX());
            double fy = transform.worldToScreenY(firstVertex.getY());

            // 检查鼠标是否靠近第一个顶点
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            double screenDistance = Math.hypot(mouseScreenX - fx, mouseScreenY - fy);

            double scale = context.getTransform().getScale();
            double threshold = 15.0; // 屏幕像素阈值

            if (screenDistance < threshold) {
                // 鼠标靠近第一个顶点，显示更明显的闭合提示
                gc.setFill(Color.LIGHTGREEN);
                gc.fillOval(fx - pointRadius * 2, fy - pointRadius * 2, pointRadius * 4, pointRadius * 4);

                // 绘制闭合线（从最后一个顶点到第一个顶点）
                // 重用之前定义的 lastScreenX 和 lastScreenY
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
        polygonVertices.clear();
    }

    /**
     * 处理多边形点击事件
     */
    private void handlePolygonClick(double worldX, double worldY, DrawingContext context) {
        // 检查是否与起点重合
        if (!polygonVertices.isEmpty()) {
            Point2D firstVertex = polygonVertices.get(0);
            double distance = Math.hypot(worldX - firstVertex.getX(), worldY - firstVertex.getY());

            // 如果与起点距离小于阈值，则完成多边形绘制
            double scale = context.getTransform().getScale();
            double threshold = 15.0 / scale; // 15像素的吸附范围

            if (distance < threshold && polygonVertices.size() >= 3) {
                // 完成多边形绘制
                finishPolygon(context);
                return;
            }
        }

        // 添加新顶点
        polygonVertices.add(new Point2D(worldX, worldY));

        // 进入多边形绘制状态
        context.setState(DrawingState.POLYGON_DRAWING);

        // 重绘以显示预览
        context.redraw();
    }

    /**
     * 完成多边形绘制
     */
    private void finishPolygon(DrawingContext context) {
        if (polygonVertices.size() < 3) {
            return;
        }

        // 只创建多边形对象，不创建独立的点和线段
        PolygonGeo polygon = new PolygonGeo(new ArrayList<>(polygonVertices));
        // 计算此多边形产生的所有交点
        List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                .checkIntersections(polygon, context);

        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                context.addObject(polygon);
                // 添加交点
                for (PointGeo point : intersectionPoints) {
                    context.addObject(point);
                }
            }

            @Override
            public void undo() {
                context.removeObject(polygon);
                // 移除交点
                for (PointGeo point : intersectionPoints) {
                    context.removeObject(point);
                }
            }
        });

        // 重置状态
        polygonVertices.clear();
        context.setState(DrawingState.IDLE);
        context.redraw();
    }
}
