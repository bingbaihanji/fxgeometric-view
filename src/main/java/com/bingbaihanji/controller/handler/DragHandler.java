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

/**
 * 拖动处理器
 * <p>
 * 处理控制点的拖动操作（在非绘制模式下）
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

        // 遍历所有图形的控制点，找到最近的控制点
        for (WorldObject obj : context.getObjects()) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, tolerance)) {
                    draggingPoint = point;
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

        return false;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || draggingPoint != null) {
            return false;
        }

        // 保存鼠标位置用于预览
        double worldX = context.getGridChartPane().screenToWorldX(e.getX());
        double worldY = context.getGridChartPane().screenToWorldY(e.getY());
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 重绘以显示悬停高亮
        context.redraw();
        return true;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e, DrawingContext context) {
        if (draggingPoint == null) {
            return false;
        }

        // 拖动控制点
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

        // 计算实际更新后的位置
        double newX = worldX - dragOffsetX;
        double newY = worldY - dragOffsetY;

        // 更新控制点位置
        draggingPoint.updatePosition(newX, newY);

        // 实时更新所有约束点（关键！）
        context.getConstraintHandler().updateAllConstrainedPoints(context);

        // 实时记录当前拖动位置（用于撤销/恢复）
        dragEndX = newX;
        dragEndY = newY;

        // 重绘
        context.redraw();
        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, DrawingContext context) {
        if (draggingPoint == null) {
            return false;
        }

        // 结束拖动
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

        // 重置拖动状态
        draggingPoint = null;
        dragOffsetX = 0;
        dragOffsetY = 0;
        dragStartX = 0;
        dragStartY = 0;
        dragEndX = 0;
        dragEndY = 0;

        // 重新计算所有交点
        context.getIntersectionHandler().recalculateAllIntersections(context);

        e.consume();
        return true;
    }

    @Override
    public void reset() {
        draggingPoint = null;
        dragOffsetX = 0;
        dragOffsetY = 0;
        dragStartX = 0;
        dragStartY = 0;
        dragEndX = 0;
        dragEndY = 0;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || draggingPoint != null) {
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
