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
     * 拖动状态管理对象
     * <p>
     * 封装所有拖动相关的状态变量，简化状态管理
     */
    private final DragState state = new DragState();

    /**
     * 拖动状态类型枚举
     */
    public enum DragType {
        NONE,                   // 无拖动
        SINGLE_POINT,           // 单个控制点拖动
        MULTIPLE_OBJECTS,       // 多选对象拖动
        BOUNDING_BOX_HANDLE     // BoundingBox句柄拖动（已禁用）
    }

    /**
     * 拖动状态封装类
     * <p>
     * 将所有拖动相关的状态变量封装在一起，便于管理和重置
     */
    private static class DragState {
        // 拖动类型
        DragType type = DragType.NONE;

        // 单点拖动相关
        WorldObject.DraggablePoint point = null;
        WorldObject owner = null;

        // 圆心拖动相关
        boolean isDraggingCircleCenter = false;
        CircleGeo draggingCircle = null;

        // 句柄拖动相关（已禁用但保留结构）
        ResizeHandle handle = null;
        Map<WorldObject.DraggablePoint, double[]> boundingBoxInitialPositions = new HashMap<>();
        Map<CircleGeo, Double> circleInitialRadii = new HashMap<>();

        // 多选拖动相关
        List<WorldObject> objects = new ArrayList<>();
        List<WorldObject.DraggablePoint> allPoints = new ArrayList<>();
        Map<WorldObject.DraggablePoint, double[]> initialPositions = new HashMap<>();

        // 通用拖动信息
        double offsetX = 0;
        double offsetY = 0;
        double startX = 0;
        double startY = 0;
        double endX = 0;
        double endY = 0;

        /**
         * 重置所有状态
         */
        void reset() {
            type = DragType.NONE;
            point = null;
            owner = null;
            isDraggingCircleCenter = false;
            draggingCircle = null;
            handle = null;
            boundingBoxInitialPositions.clear();
            circleInitialRadii.clear();
            objects.clear();
            allPoints.clear();
            initialPositions.clear();
            offsetX = 0;
            offsetY = 0;
            startX = 0;
            startY = 0;
            endX = 0;
            endY = 0;
        }

        /**
         * 是否正在拖动
         */
        boolean isDragging() {
            return type != DragType.NONE;
        }

        /**
         * 开始单点拖动
         */
        void startSinglePointDrag(WorldObject.DraggablePoint point, WorldObject owner,
                                  double offsetX, double offsetY, double startX, double startY) {
            this.type = DragType.SINGLE_POINT;
            this.point = point;
            this.owner = owner;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.startX = startX;
            this.startY = startY;
            this.endX = startX;
            this.endY = startY;
        }

        /**
         * 开始多选对象拖动
         */
        void startMultipleObjectsDrag(List<WorldObject> selectedObjects,
                                      double startX, double startY) {
            this.type = DragType.MULTIPLE_OBJECTS;
            this.objects = new ArrayList<>(selectedObjects);
            this.allPoints.clear();
            this.initialPositions.clear();

            // 收集所有控制点并保存初始位置
            for (WorldObject obj : this.objects) {
                for (WorldObject.DraggablePoint p : obj.getDraggablePoints()) {
                    this.allPoints.add(p);
                    this.initialPositions.put(p, new double[]{p.getX(), p.getY()});
                }
            }

            this.startX = startX;
            this.startY = startY;
            this.endX = startX;
            this.endY = startY;
        }

        /**
         * 设置圆心拖动信息
         */
        void setCircleCenterDrag(CircleGeo circle) {
            this.isDraggingCircleCenter = true;
            this.draggingCircle = circle;
        }
    }

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

        // 优先级1：检查所有控制点（最高优先级）
        Hits.HitPoint hitPoint = Hits.performPointHitTest(context.getObjects(), worldX, worldY, tolerance);
        if (hitPoint != null) {
            // 命中控制点，进入单控制点拖动模式
            WorldObject.DraggablePoint point = hitPoint.getPoint();
            WorldObject owner = hitPoint.getOwner();

            // 重置 SnapController 状态机
            context.getSnapController().reset();

            // 初始化拖动状态
            state.startSinglePointDrag(
                    point, owner,
                    worldX - point.getX(),
                    worldY - point.getY(),
                    point.getX(),
                    point.getY()
            );

            // 检查是否在拖动圆心
            if (owner instanceof CircleGeo circle) {
                if (Math.abs(point.getX() - circle.getCx()) < 1e-6 &&
                        Math.abs(point.getY() - circle.getCy()) < 1e-6) {
                    state.setCircleCenterDrag(circle);
                }
            }

            // 设置 MoveMode
            context.setMoveMode(MoveMode.MOVE_POINT);

            e.consume();
            return true;
        }

        // 优先级2：检查已选中对象（用于多选拖动整体）
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (!selectedObjects.isEmpty()) {
            Hits hits = Hits.performHitTest(selectedObjects, worldX, worldY, tolerance);
            if (!hits.isEmpty()) {
                // 点击了选中对象的边缘/内部，开始多选拖动
                context.getSnapController().reset();

                // 初始化多选拖动状态
                state.startMultipleObjectsDrag(selectedObjects, worldX, worldY);

                // 设置 MoveMode
                context.setMoveMode(MoveMode.MOVE_MULTIPLE_OBJECTS);

                e.consume();
                return true;
            }
        }

        // 优先级3：未命中任何对象
        context.setMoveMode(MoveMode.MOVE_NONE);
        return false;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || state.isDragging()) {
            return false;
        }

        // 保存鼠标位置用于预览
        double worldX = context.getGridChartPane().screenToWorldX(e.getX());
        double worldY = context.getGridChartPane().screenToWorldY(e.getY());
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale;

        // 检查鼠标是否靠近顶点，如果是则显示十字光标
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
        if (!state.isDragging()) {
            return false;
        }

        // 拖动顶点时保持十字光标
        if (state.type == DragType.SINGLE_POINT) {
            context.getGridChartPane().setCursor(javafx.scene.Cursor.CROSSHAIR);
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用吸附逻辑
        double worldX = rawX;
        double worldY = rawY;

        // 圆心拖动时的特殊吸附处理
        boolean circleSnapApplied = false;
        if (state.isDraggingCircleCenter) {
            double newCenterX = rawX - state.offsetX;
            double newCenterY = rawY - state.offsetY;
            double scale = context.getTransform().getScale();
            double circleRadius = state.draggingCircle.getR();

            // 优先级1：圆心到边吸附
            double centerToEdgeThreshold = 12.0 / scale;
            EdgeSnapManager.CircleCenterToLineResult centerToEdgeResult =
                    EdgeSnapManager.findCircleCenterToAllEdgesSnap(newCenterX, newCenterY,
                            context.getObjects(), centerToEdgeThreshold, state.owner);

            if (centerToEdgeResult != null) {
                worldX = centerToEdgeResult.getCenterX() + state.offsetX;
                worldY = centerToEdgeResult.getCenterY() + state.offsetY;
                circleSnapApplied = true;
            }

            // 优先级2：圆与圆相切吸附
            if (!circleSnapApplied) {
                double circleToCircleThreshold = 15.0 / scale;
                EdgeSnapManager.CircleToCircleSnapResult circleSnapResult =
                        EdgeSnapManager.findCircleToCircleTangentSnap(newCenterX, newCenterY, circleRadius,
                                context.getObjects(), circleToCircleThreshold, state.owner);

                if (circleSnapResult != null) {
                    worldX = circleSnapResult.getCenterX() + state.offsetX;
                    worldY = circleSnapResult.getCenterY() + state.offsetY;
                    circleSnapApplied = true;
                }
            }

            // 优先级3：圆边缘与边相切吸附
            if (!circleSnapApplied) {
                double tangentThreshold = 15.0 / scale;
                EdgeSnapManager.LineTangentResult tangentResult =
                        EdgeSnapManager.findCircleToAllEdgesTangentSnap(newCenterX, newCenterY, circleRadius,
                                context.getObjects(), tangentThreshold, state.owner);

                if (tangentResult != null) {
                    worldX = tangentResult.getCenterX() + state.offsetX;
                    worldY = tangentResult.getCenterY() + state.offsetY;
                    circleSnapApplied = true;
                }
            }
        }

        // 如果没有应用圆的特殊吸附，则尝试一般吸附
        if (!circleSnapApplied) {
            double snapCheckX = rawX;
            double snapCheckY = rawY;
            if (state.isDraggingCircleCenter) {
                snapCheckX = rawX - state.offsetX;
                snapCheckY = rawY - state.offsetY;
            }

            // 点吸附
            SpecialPoint nearestPoint = context.getSnappingHandler()
                    .findNearestSpecialPoint(snapCheckX, snapCheckY, context, state.owner);
            if (nearestPoint != null) {
                if (state.isDraggingCircleCenter) {
                    worldX = nearestPoint.getX() + state.offsetX;
                    worldY = nearestPoint.getY() + state.offsetY;
                } else {
                    worldX = nearestPoint.getX();
                    worldY = nearestPoint.getY();
                }
            } else {
                // 边吸附（仅在非圆心拖动时）
                if (!state.isDraggingCircleCenter) {
                    EdgeSnapManager.EdgeSnapResult edgeSnap = context.getSnappingHandler()
                            .findNearestEdge(rawX, rawY, context, state.owner);
                    if (edgeSnap != null) {
                        worldX = edgeSnap.getX();
                        worldY = edgeSnap.getY();
                    }
                }

                // 网格吸附
                if (worldX == rawX && worldY == rawY && context.getGridChartPane().getSettings().isGridSnapEnabled()) {
                    double[] snapped = context.getSnappingHandler().applySnapping(snapCheckX, snapCheckY, context);
                    if (state.isDraggingCircleCenter) {
                        worldX = snapped[0] + state.offsetX;
                        worldY = snapped[1] + state.offsetY;
                    } else {
                        worldX = snapped[0];
                        worldY = snapped[1];
                    }
                }
            }
        }

        if (state.type == DragType.MULTIPLE_OBJECTS) {
            // 多选对象拖动
            double deltaX = worldX - state.startX;
            double deltaY = worldY - state.startY;

            for (WorldObject.DraggablePoint point : state.allPoints) {
                double[] initial = state.initialPositions.get(point);
                if (initial != null) {
                    point.updatePosition(initial[0] + deltaX, initial[1] + deltaY);
                }
            }

            state.endX = worldX;
            state.endY = worldY;

        } else if (state.type == DragType.SINGLE_POINT) {
            // 单个控制点拖动
            double newX = worldX - state.offsetX;
            double newY = worldY - state.offsetY;

            state.point.updatePosition(newX, newY);

            // 同步移动点复用
            syncReusePointsPosition(context, state.startX, state.startY, newX, newY, state.owner);

            state.endX = newX;
            state.endY = newY;
        }

        // 更新约束点和交点
        context.getConstraintHandler().updateAllConstrainedPoints(context);
        context.getIntersectionHandler().recalculateAllIntersections(context);

        context.redraw();
        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, DrawingContext context) {
        if (!state.isDragging()) {
            return false;
        }

        if (state.type == DragType.MULTIPLE_OBJECTS) {
            // 多选对象拖动结束
            double deltaX = state.endX - state.startX;
            double deltaY = state.endY - state.startY;

            if (!MathCalculationUtils.isZero(deltaX) || !MathCalculationUtils.isZero(deltaY)) {
                final Map<WorldObject.DraggablePoint, double[]> savedInitialPositions = new HashMap<>(state.initialPositions);
                final double finalDeltaX = deltaX;
                final double finalDeltaY = deltaY;

                context.addCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        for (Map.Entry<WorldObject.DraggablePoint, double[]> entry : savedInitialPositions.entrySet()) {
                            WorldObject.DraggablePoint point = entry.getKey();
                            double[] initial = entry.getValue();
                            point.updatePosition(initial[0] + finalDeltaX, initial[1] + finalDeltaY);
                        }
                        context.getConstraintHandler().updateAllConstrainedPoints(context);
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                        context.invalidateSnapCache();
                    }

                    @Override
                    public void undo() {
                        for (Map.Entry<WorldObject.DraggablePoint, double[]> entry : savedInitialPositions.entrySet()) {
                            WorldObject.DraggablePoint point = entry.getKey();
                            double[] initial = entry.getValue();
                            point.updatePosition(initial[0], initial[1]);
                        }
                        context.getConstraintHandler().updateAllConstrainedPoints(context);
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                        context.invalidateSnapCache();
                    }
                });
            }

        } else if (state.type == DragType.SINGLE_POINT) {
            // 单个控制点拖动结束
            if (!MathCalculationUtils.isZero(state.startX - state.endX) ||
                    !MathCalculationUtils.isZero(state.startY - state.endY)) {

                final WorldObject.DraggablePoint pointRef = state.point;
                final double startX = state.startX;
                final double startY = state.startY;
                final double endX = state.endX;
                final double endY = state.endY;

                context.addCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        pointRef.updatePosition(endX, endY);
                        context.getConstraintHandler().updateAllConstrainedPoints(context);
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                        context.invalidateSnapCache();
                    }

                    @Override
                    public void undo() {
                        pointRef.updatePosition(startX, startY);
                        context.getConstraintHandler().updateAllConstrainedPoints(context);
                        context.getIntersectionHandler().recalculateAllIntersections(context);
                        context.invalidateSnapCache();
                    }
                });
            }
        }

        // 重置拖动状态
        state.reset();

        // 重新计算交点和使缓存失效
        context.getIntersectionHandler().recalculateAllIntersections(context);
        context.invalidateSnapCache();

        // 重绘并恢复光标
        context.redraw();
        context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());
        context.setMoveMode(MoveMode.MOVE_NONE);

        e.consume();
        return true;
    }

    @Override
    public void reset() {
        state.reset();
    }

    /**
     * 获取当前拖动状态（用于预览显示位移信息）
     */
    public DragState getDragState() {
        return state;
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
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        // 拖动时显示位移提示
        if (state.isDragging()) {
            paintDragDisplacementHint(gc, transform, context);
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
                    double screenX = transform.worldToScreenX(point.getX());
                    double screenY = transform.worldToScreenY(point.getY());

                    // 绘制外圈（高亮效果）
                    gc.setFill(Color.rgb(117, 158, 178, 0.3));
                    double outerRadius = 8;
                    gc.fillOval(screenX - outerRadius, screenY - outerRadius, outerRadius * 2, outerRadius * 2);

                    // 绘制内圈
                    gc.setFill(Color.valueOf("#759eb2"));
                    double innerRadius = 5;
                    gc.fillOval(screenX - innerRadius, screenY - innerRadius, innerRadius * 2, innerRadius * 2);

                    return;
                }
            }
        }
    }

    /**
     * 绘制拖动位移提示
     * <p>
     * 在拖动过程中显示位移量（Δx, Δy）
     */
    private void paintDragDisplacementHint(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        double dx, dy;
        double screenX, screenY;

        if (state.type == DragType.SINGLE_POINT && state.point != null) {
            // 单点拖动：显示点的位移
            dx = state.endX - state.startX;
            dy = state.endY - state.startY;
            screenX = transform.worldToScreenX(state.endX);
            screenY = transform.worldToScreenY(state.endY);
        } else if (state.type == DragType.MULTIPLE_OBJECTS) {
            // 多选拖动：显示整体位移
            dx = state.endX - state.startX;
            dy = state.endY - state.startY;
            screenX = transform.worldToScreenX(state.endX);
            screenY = transform.worldToScreenY(state.endY);
        } else {
            return;
        }

        // 只有在有位移时才显示提示
        if (MathCalculationUtils.isZero(dx) && MathCalculationUtils.isZero(dy)) {
            return;
        }

        // 格式化位移信息
        String info = String.format("Δx: %.2f, Δy: %.2f", dx, dy);

        // 绘制提示背景
        gc.save();
        gc.setFont(javafx.scene.text.Font.font("Microsoft YaHei", 12));
        double textWidth = info.length() * 7; // 估算文字宽度
        double textHeight = 16;
        double padding = 4;
        double bgX = screenX + 15;
        double bgY = screenY - 25;

        // 背景圆角矩形
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRoundRect(bgX - padding, bgY - textHeight, textWidth + padding * 2, textHeight + padding, 4, 4);

        // 文字
        gc.setFill(Color.WHITE);
        gc.fillText(info, bgX, bgY - 3);

        gc.restore();
    }
}
