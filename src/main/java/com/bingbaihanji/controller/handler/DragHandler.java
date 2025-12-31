package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拖动处理器
 * <p>
 * 处理控制点的拖动操作（在非绘制模式下）
 * 支持多选对象的同时拖动
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class DragHandler extends AbstractDrawingHandler {

    /**
     * 当前拖动的控制点
     */
    private WorldObject.DraggablePoint draggingPoint = null;

    /**
     * 多选对象拖动模式
     */
    private boolean draggingMultipleObjects = false;

    /**
     * 被拖动的多个对象及其所有控制点
     */
    private List<WorldObject> draggingObjects = new ArrayList<>();
    private List<WorldObject.DraggablePoint> allDraggingPoints = new ArrayList<>();

    /**
     * 拖动开始时的鼠标偏移量
     */
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    /**
     * 拖动开始时的坐标（用于撤销）
     */
    private double dragStartX = 0;
    private double dragStartY = 0;

    /**
     * 拖动结束时的实际坐标（用于恢复）
     */
    private double dragEndX = 0;
    private double dragEndY = 0;

    /**
     * 多选对象的初始位置（用于撤销）
     */
    private Map<WorldObject.DraggablePoint, double[]> initialPositions = new HashMap<>();

    @Override
    public boolean canHandle(DrawMode mode) {
        // 拖动只在非绘制模式（NONE）下生效
        return mode == DrawMode.NONE;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || e.getButton() != MouseButton.PRIMARY) {
            return false;
        }

        // 非绘制模式下，尝试选中控制点进行拖动
        double worldX = context.getGridChartPane().screenToWorldX(e.getX());
        double worldY = context.getGridChartPane().screenToWorldY(e.getY());

        // 计算容差
        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale; // 10像素的点击范围

        // ========== 优先级1：检查所有顶点（最高优先级）==========
        // 无论对象是否被选中，顶点始终具有最高优先级
        for (WorldObject obj : context.getObjects()) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, tolerance)) {
                    // 命中顶点，进入单顶点拖动模式
                    draggingPoint = point;
                    draggingMultipleObjects = false;
                    dragOffsetX = worldX - point.getX();
                    dragOffsetY = worldY - point.getY();

                    // 保存拖动前的坐标，用于撤销
                    dragStartX = point.getX();
                    dragStartY = point.getY();
                    // 初始化结束位置为起始位置
                    dragEndX = dragStartX;
                    dragEndY = dragStartY;

                    e.consume();
                    return true;
                }
            }
        }

        // ========== 优先级2：检查已选中对象（用于多选拖动整体）==========
        // 只有在没有命中顶点的情况下，才检查是否点击了对象的边缘/内部
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (!selectedObjects.isEmpty()) {
            // 检查是否点击了某个选中对象的边缘/内部（非顶点）
            for (WorldObject selectedObj : selectedObjects) {
                if (selectedObj.hitTest(worldX, worldY, tolerance)) {
                    // 点击了选中对象的边缘/内部，开始多选拖动
                    draggingMultipleObjects = true;
                    draggingObjects = new ArrayList<>(selectedObjects);
                    allDraggingPoints.clear();
                    initialPositions.clear();

                    // 收集所有选中对象的控制点
                    for (WorldObject obj : draggingObjects) {
                        for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                            allDraggingPoints.add(point);
                            // 保存初始位置
                            initialPositions.put(point, new double[]{point.getX(), point.getY()});
                        }
                    }

                    // 记录拖动起始位置
                    dragStartX = worldX;
                    dragStartY = worldY;
                    dragEndX = worldX;
                    dragEndY = worldY;

                    e.consume();
                    return true;
                }
            }
        }

        // ========== 优先级3：未命中任何对象 ==========
        return false;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || draggingPoint != null || draggingMultipleObjects) {
            return false;
        }

        // 保存鼠标位置用于预览
        double worldX = context.getGridChartPane().screenToWorldX(e.getX());
        double worldY = context.getGridChartPane().screenToWorldY(e.getY());
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 检查鼠标是否靠近顶点，如果是则显示十字光标
        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale; // 10像素的容差

        boolean nearVertex = false;
        for (WorldObject obj : context.getObjects()) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, tolerance)) {
                    nearVertex = true;
                    break;
                }
            }
            if (nearVertex) break;
        }

        // 设置光标样式
        if (nearVertex) {
            context.getGridChartPane().setCursor(javafx.scene.Cursor.CROSSHAIR);
        } else {
            context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());
        }

        // 重绘以显示悬停高亮
        context.redraw();
        return true;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e, DrawingContext context) {
        if (draggingPoint == null && !draggingMultipleObjects) {
            return false;
        }

        // 拖动顶点时保持十字光标
        if (draggingPoint != null) {
            context.getGridChartPane().setCursor(javafx.scene.Cursor.CROSSHAIR);
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用特殊点磁性吸附
        double worldX = rawX;
        double worldY = rawY;
        SpecialPoint nearestPoint = context.getSnappingHandler().findNearestSpecialPoint(rawX, rawY, context);
        if (nearestPoint != null) {
            worldX = nearestPoint.getX();
            worldY = nearestPoint.getY();
        }

        if (draggingMultipleObjects) {
            // 多选对象拖动：计算位移量，移动所有控制点
            double deltaX = worldX - dragStartX;
            double deltaY = worldY - dragStartY;

            // 更新所有控制点位置
            for (WorldObject.DraggablePoint point : allDraggingPoints) {
                double[] initial = initialPositions.get(point);
                if (initial != null) {
                    point.updatePosition(initial[0] + deltaX, initial[1] + deltaY);
                }
            }

            // 记录当前位置
            dragEndX = worldX;
            dragEndY = worldY;

        } else if (draggingPoint != null) {
            // 单个控制点拖动
            double newX = worldX - dragOffsetX;
            double newY = worldY - dragOffsetY;

            // 更新控制点位置
            draggingPoint.updatePosition(newX, newY);

            // 实时记录当前拖动位置（用于撤销/恢复）
            dragEndX = newX;
            dragEndY = newY;
        }

        // 实时更新所有约束点（关键！）
        context.getConstraintHandler().updateAllConstrainedPoints(context);

        // 重绘
        context.redraw();
        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, DrawingContext context) {
        if (draggingPoint == null && !draggingMultipleObjects) {
            return false;
        }

        if (draggingMultipleObjects) {
            // 多选对象拖动结束
            double deltaX = dragEndX - dragStartX;
            double deltaY = dragEndY - dragStartY;

            // 只有位置实际改变才记录命令
            if (Math.abs(deltaX) > 1e-10 || Math.abs(deltaY) > 1e-10) {
                // 保存对控制点的引用和位置
                final Map<WorldObject.DraggablePoint, double[]> savedInitialPositions = new HashMap<>(initialPositions);
                final double finalDeltaX = deltaX;
                final double finalDeltaY = deltaY;

                context.addCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        // 恢复操作：移动到结束位置
                        for (Map.Entry<WorldObject.DraggablePoint, double[]> entry : savedInitialPositions.entrySet()) {
                            WorldObject.DraggablePoint point = entry.getKey();
                            double[] initial = entry.getValue();
                            point.updatePosition(initial[0] + finalDeltaX, initial[1] + finalDeltaY);
                        }
                        context.getConstraintHandler().updateAllConstrainedPoints(context);
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                    }

                    @Override
                    public void undo() {
                        // 撤销操作：移动回起始位置
                        for (Map.Entry<WorldObject.DraggablePoint, double[]> entry : savedInitialPositions.entrySet()) {
                            WorldObject.DraggablePoint point = entry.getKey();
                            double[] initial = entry.getValue();
                            point.updatePosition(initial[0], initial[1]);
                        }
                        context.getConstraintHandler().updateAllConstrainedPoints(context);
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                    }
                });
            }

            // 重置多选拖动状态
            draggingMultipleObjects = false;
            draggingObjects.clear();
            allDraggingPoints.clear();
            initialPositions.clear();

        } else if (draggingPoint != null) {
            // 单个控制点拖动结束
            // 只有位置实际改变才记录命令
            if (Math.abs(dragStartX - dragEndX) > 1e-10 ||
                    Math.abs(dragStartY - dragEndY) > 1e-10) {

                // 保存对点的持久引用和坐标
                final WorldObject.DraggablePoint pointRef = draggingPoint;
                final double startX = dragStartX;
                final double startY = dragStartY;
                final double endX = dragEndX;
                final double endY = dragEndY;

                // 使用addCommand而不是execute，因为拖动已经完成了
                context.addCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        // 恢复操作：移动到结束位置
                        pointRef.updatePosition(endX, endY);
                        context.getConstraintHandler().updateAllConstrainedPoints(context);  // 更新约束点
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                    }

                    @Override
                    public void undo() {
                        // 撤销操作：移动回起始位置
                        pointRef.updatePosition(startX, startY);
                        context.getConstraintHandler().updateAllConstrainedPoints(context);  // 更新约束点
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                    }
                });
            }

            // 重置单点拖动状态
            draggingPoint = null;
            dragOffsetX = 0;
            dragOffsetY = 0;
        }

        // 重置通用拖动状态
        dragStartX = 0;
        dragStartY = 0;
        dragEndX = 0;
        dragEndY = 0;

        // 重新计算所有交点
        context.getIntersectionHandler().recalculateAllIntersections(context);

        // 恢复默认光标
        context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());

        e.consume();
        return true;
    }

    @Override
    public void reset() {
        draggingPoint = null;
        draggingMultipleObjects = false;
        draggingObjects.clear();
        allDraggingPoints.clear();
        initialPositions.clear();
        dragOffsetX = 0;
        dragOffsetY = 0;
        dragStartX = 0;
        dragStartY = 0;
        dragEndX = 0;
        dragEndY = 0;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || draggingPoint != null || draggingMultipleObjects) {
            return;
        }

        // 高亮显示鼠标悬停的可拖动控制点
        double worldX = context.getCurrentMouseX();
        double worldY = context.getCurrentMouseY();
        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale;

        for (WorldObject obj : context.getObjects()) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, tolerance)) {
                    // 高亮显示控制点
                    double screenX = transform.worldToScreenX(point.getX());
                    double screenY = transform.worldToScreenY(point.getY());

                    // 绘制外圈（高亮效果）
                    gc.setFill(Color.rgb(117, 158, 178, 0.3)); // #759eb2 with transparency
                    double outerRadius = 8;
                    gc.fillOval(screenX - outerRadius, screenY - outerRadius, outerRadius * 2, outerRadius * 2);

                    // 绘制内圈（控制点本身）
                    gc.setFill(Color.valueOf("#759eb2"));
                    double innerRadius = 5;
                    gc.fillOval(screenX - innerRadius, screenY - innerRadius, innerRadius * 2, innerRadius * 2);

                    return; // 只高亮一个控制点
                }
            }
        }
    }
}
