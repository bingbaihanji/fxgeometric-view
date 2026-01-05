package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.MoveMode;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.EdgeSnapManager;
import com.bingbaihanji.util.Hits;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.view.ResizeHandle;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
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
     * 拥有当前拖动点的对象
     */
    private WorldObject draggingPointOwner = null;

    /**
     * 当前拖动的句柄
     */
    private ResizeHandle draggingHandle = null;

    /**
     * BoundingBox变换开始时所有控制点的初始位置（用于缩放和旋转）
     */
    private Map<WorldObject.DraggablePoint, double[]> boundingBoxInitialPositions = new HashMap<>();

    /**
     * BoundingBox变换开始时圆的初始半径（用于缩放）
     */
    private Map<CircleGeo, Double> circleInitialRadii = new HashMap<>();

    /**
     * 是否正在拖动圆心（在mousePressed时确定，整个拖动过程保持不变）
     */
    private boolean isDraggingCircleCenter = false;

    /**
     * 被拖动的圆（如果正在拖动圆心）
     */
    private CircleGeo draggingCircle = null;

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
        // double handleTolerance = 15.0 / scale; // 句柄使用更大的容差（15像素）

        // 已禁用：优先级1：检查BoundingBox句柄（最高优先级）
        /*
        if (context.getSelectionManager().hasBoundingBox()) {
            ResizeHandle handle = context.getSelectionManager()
                .getBoundingBox().hitTestHandles(worldX, worldY, handleTolerance);
            if (handle != null) {
                // 命中句柄，进入句柄拖动模式
                draggingHandle = handle;
                dragOffsetX = worldX - handle.getWorldX();
                dragOffsetY = worldY - handle.getWorldY();
                dragStartX = worldX; // 使用鼠标位置而不是句柄位置
                dragStartY = worldY;
                dragEndX = dragStartX;
                dragEndY = dragStartY;
                
                // 保存所有选中对象的初始位置
                boundingBoxInitialPositions.clear();
                circleInitialRadii.clear();
                List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
                for (WorldObject obj : selectedObjects) {
                    // 对于圆，保存初始半径
                    if (obj instanceof CircleGeo circle) {
                        circleInitialRadii.put(circle, circle.getR());
                    }
                    // 保存所有控制点的初始位置
                    for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                        boundingBoxInitialPositions.put(point, new double[]{point.getX(), point.getY()});
                    }
                }
                
                // 设置 MoveMode 和光标
                context.setMoveMode(MoveMode.RESIZE_BOUNDING_BOX);
                context.getGridChartPane().setCursor(handle.getCursor());
                
                // 消费事件，防止SelectionHandler清除选择
                e.consume();
                return true;
            }
        }
        */

        // 优先级2：检查所有控制点（最高优先级）
        // 无论对象是否被选中，控制点始终具有最高优先级
        Hits.HitPoint hitPoint = Hits.performPointHitTest(context.getObjects(), worldX, worldY, tolerance);
        if (hitPoint != null) {
            // 命中控制点，进入单控制点拖动模式
            draggingPoint = hitPoint.getPoint();
            draggingPointOwner = hitPoint.getOwner();
            draggingMultipleObjects = false;
            dragOffsetX = worldX - draggingPoint.getX();
            dragOffsetY = worldY - draggingPoint.getY();

            // 重置 SnapController 状态机
            context.getSnapController().reset();

            // 保存拖动前的坐标，用于撤销
            dragStartX = draggingPoint.getX();
            dragStartY = draggingPoint.getY();

            // 在这里确定是否在拖动圆心（只判断一次，整个拖动过程保持不变）
            isDraggingCircleCenter = false;
            draggingCircle = null;
            if (draggingPointOwner instanceof CircleGeo circle) {
                // 检查点击的点是否是圆心（通过比较坐标）
                if (Math.abs(draggingPoint.getX() - circle.getCx()) < 1e-6 &&
                        Math.abs(draggingPoint.getY() - circle.getCy()) < 1e-6) {
                    isDraggingCircleCenter = true;
                    draggingCircle = circle;
                }
            }
            // 初始化结束位置为起始位置
            dragEndX = dragStartX;
            dragEndY = dragStartY;

            // 设置 MoveMode
            context.setMoveMode(MoveMode.MOVE_POINT);

            e.consume();
            return true;
        }

        // 优先级3：检查已选中对象（用于多选拖动整体） 
        // 只有在没有命中控制点的情况下，才检查是否点击了对象的边缘/内部
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (!selectedObjects.isEmpty()) {
            // 使用 Hits 进行命中检测
            Hits hits = Hits.performHitTest(selectedObjects, worldX, worldY, tolerance);
            if (!hits.isEmpty()) {
                // 点击了选中对象的边缘/内部，开始多选拖动
                draggingMultipleObjects = true;
                draggingObjects = new ArrayList<>(selectedObjects);
                allDraggingPoints.clear();
                initialPositions.clear();

                // 重置 SnapController 状态机
                context.getSnapController().reset();

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

                // 设置 MoveMode
                context.setMoveMode(MoveMode.MOVE_MULTIPLE_OBJECTS);

                e.consume();
                return true;
            }
        }

        // 优先级4：未命中任何对象  
        context.setMoveMode(MoveMode.MOVE_NONE);
        return false;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || draggingPoint != null || draggingMultipleObjects || draggingHandle != null) {
            return false;
        }

        // 保存鼠标位置用于预览
        double worldX = context.getGridChartPane().screenToWorldX(e.getX());
        double worldY = context.getGridChartPane().screenToWorldY(e.getY());
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale; // 10像素的容差

        // 已禁用：优先级1：检查鼠标是否悬停在BoundingBox句柄上
        /*
        if (context.getSelectionManager().hasBoundingBox()) {
            ResizeHandle handle = context.getSelectionManager()
                .getBoundingBox().hitTestHandles(worldX, worldY, tolerance);
            if (handle != null) {
                // 悬停在句柄上，显示相应的光标
                context.getGridChartPane().setCursor(handle.getCursor());
                context.redraw();
                return true;
            }
        }
        */

        // 优先级2：检查鼠标是否靠近顶点，如果是则显示十字光标
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
        if (draggingPoint == null && !draggingMultipleObjects && draggingHandle == null) {
            return false;
        }

        // 已禁用：拖动句柄时实现缩放或旋转（类似Word中的图片操作）
        /*
        if (draggingHandle != null) {
            double worldX = context.getGridChartPane().screenToWorldX(e.getX());
            double worldY = context.getGridChartPane().screenToWorldY(e.getY());
            
            context.getGridChartPane().setCursor(draggingHandle.getCursor());
            
            if (draggingHandle.getPosition() == ResizeHandle.HandlePosition.ROTATE) {
                // 旋转变换（相对于初始位置）
                BoundingBoxTransform.applyRotation(
                    context.getSelectionManager().getBoundingBox(),
                    worldX, worldY,
                    dragStartX, dragStartY,
                    boundingBoxInitialPositions
                );
            } else {
                // 缩放变换（Shift键保持宽高比，相对于初始位置）
                boolean maintainAspect = e.isShiftDown();
                BoundingBoxTransform.applyResize(
                    context.getSelectionManager().getBoundingBox(),
                    draggingHandle,
                    worldX, worldY,
                    boundingBoxInitialPositions,
                    circleInitialRadii,
                    maintainAspect
                );
            }
            
            // 更新所有约束点和交点（关键！）
            context.getConstraintHandler().updateAllConstrainedPoints(context);
            context.getIntersectionHandler().recalculateAllIntersections(context);
            
            e.consume();
            context.redraw();
            return true;
        }
        */

        // 拖动顶点时保持十字光标
        if (draggingPoint != null) {
            context.getGridChartPane().setCursor(javafx.scene.Cursor.CROSSHAIR);
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用吸附逻辑，优先级：圆心到直线吸附 > 圆-直线切线吸附 > 点吸附 > 边吸附 > 网格吸附
        double worldX = rawX;
        double worldY = rawY;

        // 圆心拖动时的特殊吸附处理（使用mousePressed时确定的状态）
        boolean circleSnapApplied = false;
        if (isDraggingCircleCenter) {
            // 计算假设的新圆心位置（无任何吸附）
            double newCenterX = rawX - dragOffsetX;
            double newCenterY = rawY - dragOffsetY;
            double scale = context.getTransform().getScale();
            double circleRadius = draggingCircle.getR();

            // 优先级1（最高）：圆心到边吸附（线段、多边形边）
            // 当圆心本身非常靠近边时，将圆心直接吸附到边上
            double centerToEdgeThreshold = 12.0 / scale;
            EdgeSnapManager.CircleCenterToLineResult centerToEdgeResult =
                    EdgeSnapManager.findCircleCenterToAllEdgesSnap(newCenterX, newCenterY,
                            context.getObjects(), centerToEdgeThreshold, draggingPointOwner);

            if (centerToEdgeResult != null) {
                // 圆心直接吸附到边上
                worldX = centerToEdgeResult.getCenterX() + dragOffsetX;
                worldY = centerToEdgeResult.getCenterY() + dragOffsetY;
                circleSnapApplied = true;
            }

            // 优先级2：圆与圆相切吸附
            if (!circleSnapApplied) {
                double circleToCircleThreshold = 15.0 / scale;
                EdgeSnapManager.CircleToCircleSnapResult circleSnapResult =
                        EdgeSnapManager.findCircleToCircleTangentSnap(newCenterX, newCenterY, circleRadius,
                                context.getObjects(), circleToCircleThreshold, draggingPointOwner);

                if (circleSnapResult != null) {
                    // 应用圆与圆相切吸附
                    worldX = circleSnapResult.getCenterX() + dragOffsetX;
                    worldY = circleSnapResult.getCenterY() + dragOffsetY;
                    circleSnapApplied = true;
                }
            }

            // 优先级3：圆边缘与边相切吸附（线段、多边形边）
            if (!circleSnapApplied) {
                double tangentThreshold = 15.0 / scale;
                EdgeSnapManager.LineTangentResult tangentResult =
                        EdgeSnapManager.findCircleToAllEdgesTangentSnap(newCenterX, newCenterY, circleRadius,
                                context.getObjects(), tangentThreshold, draggingPointOwner);

                if (tangentResult != null) {
                    // 应用切线吸附：调整圆心位置使圆与边相切
                    worldX = tangentResult.getCenterX() + dragOffsetX;
                    worldY = tangentResult.getCenterY() + dragOffsetY;
                    circleSnapApplied = true;
                }
            }
        }

        // 如果没有应用圆的特殊吸附，则尝试一般的点吸附和边吸附
        if (!circleSnapApplied) {
            // 对于圆心拖动，使用圆心位置进行吸附检测
            double snapCheckX = rawX;
            double snapCheckY = rawY;
            if (isDraggingCircleCenter) {
                snapCheckX = rawX - dragOffsetX;
                snapCheckY = rawY - dragOffsetY;
            }

            // 优先级3：点吸附（排除正在拖动的对象）
            SpecialPoint nearestPoint = context.getSnappingHandler()
                    .findNearestSpecialPoint(snapCheckX, snapCheckY, context, draggingPointOwner);
            if (nearestPoint != null) {
                if (isDraggingCircleCenter) {
                    worldX = nearestPoint.getX() + dragOffsetX;
                    worldY = nearestPoint.getY() + dragOffsetY;
                } else {
                    worldX = nearestPoint.getX();
                    worldY = nearestPoint.getY();
                }
            } else {
                // 优先级4：边吸附（仅在非圆心拖动时生效）
                if (!isDraggingCircleCenter) {
                    EdgeSnapManager.EdgeSnapResult edgeSnap = context.getSnappingHandler()
                            .findNearestEdge(rawX, rawY, context, draggingPointOwner);
                    if (edgeSnap != null) {
                        worldX = edgeSnap.getX();
                        worldY = edgeSnap.getY();
                    }
                }

                // 优先级5：网格吸附（仅在启用且没有其他吸附时）
                if (worldX == rawX && worldY == rawY && context.getGridChartPane().getSettings().isGridSnapEnabled()) {
                    double[] snapped = context.getSnappingHandler().applySnapping(snapCheckX, snapCheckY, context);
                    if (isDraggingCircleCenter) {
                        worldX = snapped[0] + dragOffsetX;
                        worldY = snapped[1] + dragOffsetY;
                    } else {
                        worldX = snapped[0];
                        worldY = snapped[1];
                    }
                }
            }
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

            // 同步移动所有在同一位置的几何图形关键点（点复用增强）
            syncReusePointsPosition(context, dragStartX, dragStartY, newX, newY, draggingPointOwner);

            // 实时记录当前拖动位置（用于撤销/恢复）
            dragEndX = newX;
            dragEndY = newY;
        }

        // 实时更新所有约束点（关键！）
        context.getConstraintHandler().updateAllConstrainedPoints(context);

        // 实时更新所有交点（拖动时实时显示交点变化）
        context.getIntersectionHandler().recalculateAllIntersections(context);

        // 重绘
        context.redraw();
        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, DrawingContext context) {
        if (draggingPoint == null && !draggingMultipleObjects && draggingHandle == null) {
            return false;
        }

        // 句柄拖动结束（缩放或旋转）
        if (draggingHandle != null) {
            // 检查是否有实际变化
            boolean hasChanged = false;
            if (!boundingBoxInitialPositions.isEmpty()) {
                // 保存命令用于撤销/恢复
                final Map<WorldObject.DraggablePoint, double[]> savedInitialPositions = new HashMap<>(boundingBoxInitialPositions);
                final Map<WorldObject.DraggablePoint, double[]> savedFinalPositions = new HashMap<>();

                // 保存最终位置
                for (WorldObject.DraggablePoint point : boundingBoxInitialPositions.keySet()) {
                    savedFinalPositions.put(point, new double[]{point.getX(), point.getY()});

                    // 检查是否有变化
                    double[] initial = boundingBoxInitialPositions.get(point);
                    if (!MathCalculationUtils.isZero(point.getX() - initial[0]) ||
                            !MathCalculationUtils.isZero(point.getY() - initial[1])) {
                        hasChanged = true;
                    }
                }

                // 只有实际发生变化时才记录命令
                if (hasChanged) {
                    context.addCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            // 恢复操作：移动到结束位置
                            for (Map.Entry<WorldObject.DraggablePoint, double[]> entry : savedFinalPositions.entrySet()) {
                                WorldObject.DraggablePoint point = entry.getKey();
                                double[] finalPos = entry.getValue();
                                point.updatePosition(finalPos[0], finalPos[1]);
                            }
                            // 已禁用：更新BoundingBox
                            // context.getSelectionManager().getBoundingBox().setObjects(
                            //     context.getSelectionManager().getSelectedObjects());
                            context.getConstraintHandler().updateAllConstrainedPoints(context);
                            context.getIntersectionHandler().recalculateAllIntersections(context);
                            context.getSnappingHandler().invalidateCache();
                        }

                        @Override
                        public void undo() {
                            // 撤销操作：移动回起始位置
                            for (Map.Entry<WorldObject.DraggablePoint, double[]> entry : savedInitialPositions.entrySet()) {
                                WorldObject.DraggablePoint point = entry.getKey();
                                double[] initial = entry.getValue();
                                point.updatePosition(initial[0], initial[1]);
                            }
                            // 已禁用：更新BoundingBox
                            // context.getSelectionManager().getBoundingBox().setObjects(
                            //     context.getSelectionManager().getSelectedObjects());
                            context.getConstraintHandler().updateAllConstrainedPoints(context);
                            context.getIntersectionHandler().recalculateAllIntersections(context);
                            context.getSnappingHandler().invalidateCache();
                        }
                    });
                }
            }

            // 清理状态
            draggingHandle = null;
            boundingBoxInitialPositions.clear();
            dragOffsetX = 0;
            dragOffsetY = 0;
            dragStartX = 0;
            dragStartY = 0;
            dragEndX = 0;
            dragEndY = 0;

            // 恢复默认光标
            context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());

            // 重置 MoveMode
            context.setMoveMode(MoveMode.MOVE_NONE);

            e.consume();
            context.redraw();
            return true;
        }

        if (draggingMultipleObjects) {
            // 多选对象拖动结束
            double deltaX = dragEndX - dragStartX;
            double deltaY = dragEndY - dragStartY;

            // 只有位置实际改变才记录命令
            if (!MathCalculationUtils.isZero(deltaX) || !MathCalculationUtils.isZero(deltaY)) {
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
                        context.getSnappingHandler().invalidateCache(); // 使吸附缓存失效
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
                        context.getSnappingHandler().invalidateCache(); // 使吸附缓存失效
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
            if (!MathCalculationUtils.isZero(dragStartX - dragEndX) ||
                    !MathCalculationUtils.isZero(dragStartY - dragEndY)) {

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
                        context.getSnappingHandler().invalidateCache(); // 使吸附缓存失效
                    }

                    @Override
                    public void undo() {
                        // 撤销操作：移动回起始位置
                        pointRef.updatePosition(startX, startY);
                        context.getConstraintHandler().updateAllConstrainedPoints(context);  // 更新约束点
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                        context.getSnappingHandler().invalidateCache(); // 使吸附缓存失效
                    }
                });
            }

            // 重置单点拖动状态
            draggingPoint = null;
            draggingPointOwner = null;
            dragOffsetX = 0;
            dragOffsetY = 0;
        }

        // 重置通用拖动状态
        dragStartX = 0;
        dragStartY = 0;
        dragEndX = 0;
        dragEndY = 0;

        // 重新计算所有交点（最终清理步骤）
        context.getIntersectionHandler().recalculateAllIntersections(context);

        // 使吸附缓存失效（因为位置已经改变）
        context.getSnappingHandler().invalidateCache();

        // 最终重绘确保交点更新显示
        context.redraw();

        // 恢复默认光标
        context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());

        // 重置 MoveMode
        context.setMoveMode(MoveMode.MOVE_NONE);

        e.consume();
        return true;
    }

    @Override
    public void reset() {
        draggingPoint = null;
        draggingPointOwner = null;
        draggingHandle = null;
        isDraggingCircleCenter = false;
        draggingCircle = null;
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

    /**
     * 同步移动所有在同一位置的几何图形关键点
     * <p>
     * 当拖动一个点时，查找所有与该点位置重合的几何图形的关键点（圆心、顶点等），
     * 并同步移动它们。这实现了点复用的核心逻辑。
     *
     * @param context      绘图上下文
     * @param oldX         原始位置X
     * @param oldY         原始位置Y
     * @param newX         新位置X
     * @param newY         新位置Y
     * @param excludeOwner 排除的对象（正在拖动的对象）
     */
    private void syncReusePointsPosition(DrawingContext context, double oldX, double oldY,
                                         double newX, double newY, WorldObject excludeOwner) {
        double scale = context.getTransform().getScale();
        double threshold = 10.0 / scale; // 位置重合判定阈值

        // 遍历所有对象，查找关键点与 oldX, oldY 重合的几何图形
        for (WorldObject obj : context.getObjects()) {
            if (obj == excludeOwner) {
                continue; // 跳过正在拖动的对象
            }

            // 处理圆的圆心
            if (obj instanceof CircleGeo circle) {
                double cx = circle.getCx();
                double cy = circle.getCy();

                // 检查圆心是否与原始位置重合
                if (MathCalculationUtils.hypot(cx - oldX, cy - oldY) < threshold) {
                    // 移动圆心到新位置
                    PointGeo centerRef = circle.getCenterPointRef();
                    if (centerRef != null) {
                        // 如果圆心是引用点，更新引用点位置
                        centerRef.setPositionDirectly(newX, newY);
                    } else {
                        // 如果是内部坐标，直接更新（通过反射或者直接访问内部字段）
                        // 这里需要获取圆的 DraggablePoint 并更新
                        for (WorldObject.DraggablePoint point : circle.getDraggablePoints()) {
                            if (MathCalculationUtils.hypot(point.getX() - oldX, point.getY() - oldY) < threshold) {
                                point.updatePosition(newX, newY);
                                break;
                            }
                        }
                    }
                }
            }
            // 处理线段的端点
            else if (obj instanceof LineGeo line) {
                PointGeo startRef = line.getStartPointRef();
                PointGeo endRef = line.getEndPointRef();

                // 检查起点
                if (startRef != null) {
                    double sx = startRef.getX();
                    double sy = startRef.getY();
                    if (MathCalculationUtils.hypot(sx - oldX, sy - oldY) < threshold) {
                        startRef.setPositionDirectly(newX, newY);
                    }
                }

                // 检查终点
                if (endRef != null) {
                    double ex = endRef.getX();
                    double ey = endRef.getY();
                    if (MathCalculationUtils.hypot(ex - oldX, ey - oldY) < threshold) {
                        endRef.setPositionDirectly(newX, newY);
                    }
                }

                // 如果没有引用点，更新 DraggablePoint
                for (WorldObject.DraggablePoint point : line.getDraggablePoints()) {
                    if (MathCalculationUtils.hypot(point.getX() - oldX, point.getY() - oldY) < threshold) {
                        point.updatePosition(newX, newY);
                    }
                }
            }
            // 处理多边形的顶点
            else if (obj instanceof PolygonGeo polygon) {
                for (PointGeo vertex : polygon.getVertexPoints()) {
                    double vx = vertex.getX();
                    double vy = vertex.getY();
                    if (MathCalculationUtils.hypot(vx - oldX, vy - oldY) < threshold) {
                        vertex.setPositionDirectly(newX, newY);
                    }
                }
            }
            // 处理无限直线的定义点
            else if (obj instanceof InfiniteLineGeo infLine) {
                PointGeo point1Ref = infLine.getPoint1Ref();
                PointGeo point2Ref = infLine.getPoint2Ref();

                if (point1Ref != null) {
                    double p1x = point1Ref.getX();
                    double p1y = point1Ref.getY();
                    if (MathCalculationUtils.hypot(p1x - oldX, p1y - oldY) < threshold) {
                        point1Ref.setPositionDirectly(newX, newY);
                    }
                }

                if (point2Ref != null) {
                    double p2x = point2Ref.getX();
                    double p2y = point2Ref.getY();
                    if (MathCalculationUtils.hypot(p2x - oldX, p2y - oldY) < threshold) {
                        point2Ref.setPositionDirectly(newX, newY);
                    }
                }

                // 如果没有引用点，更新 DraggablePoint
                for (WorldObject.DraggablePoint point : infLine.getDraggablePoints()) {
                    if (MathCalculationUtils.hypot(point.getX() - oldX, point.getY() - oldY) < threshold) {
                        point.updatePosition(newX, newY);
                    }
                }
            }
        }
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
