package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.EdgeSnapManager;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import com.bingbaihanji.view.layout.draw.tools.CircleDrawingTool;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 基础图形绘制处理器
 * <p>
 * 处理点、线段、无限直线、圆的绘制
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class BasicShapeHandler extends AbstractDrawingHandler {

    /**
     * 圆形绘制工具
     */
    private final CircleDrawingTool circleTool = new CircleDrawingTool();
    /**
     * 第一个点的世界坐标（用于绘制圆、线段等）
     */
    private double firstPointX;
    private double firstPointY;
    /**
     * 第一次点击时的现有特殊点（用于圆心吸附）
     */
    private SpecialPoint firstClickExistingPoint = null;
    /**
     * 第一次点击时复用的点引用
     */
    private PointGeo firstPointRef = null;
    /**
     * 预览半径
     */
    private double previewRadius = 0;

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.POINT || mode == DrawMode.LINE ||
                mode == DrawMode.INFINITE_LINE || mode == DrawMode.CIRCLE;
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

        if (context.getDrawMode() == DrawMode.POINT) {
            handlePointDrawing(worldX, worldY, context);
        } else if (context.getState() == DrawingState.IDLE) {
            handleFirstClick(rawX, rawY, worldX, worldY, context);
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

        // 点模式：检测是否靠近可约束的图形，改变光标
        if (context.getDrawMode() == DrawMode.POINT) {
            double rawX = context.getGridChartPane().screenToWorldX(e.getX());
            double rawY = context.getGridChartPane().screenToWorldY(e.getY());
            double scale = context.getTransform().getScale();
            double snapDistance = 15.0 / scale; // 吸附距离阈值

            // 查找是否靠近可约束图形
            boolean nearConstrainableShape = false;
            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo || obj instanceof InfiniteLineGeo ||
                        obj instanceof CircleGeo || obj instanceof PolygonGeo || obj instanceof PathGeo) {
                    PointConstraint tempConstraint = context.getConstraintHandler().createConstraint(obj);
                    double distance = tempConstraint.distanceToShape(rawX, rawY);

                    if (distance < snapDistance) {
                        nearConstrainableShape = true;
                        break;
                    }
                }
            }

            // 根据是否靠近图形设置光标
            if (nearConstrainableShape) {
                context.getGridChartPane().setCursor(javafx.scene.Cursor.HAND);
            } else {
                context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());
            }

            return true;
        }

        // 其他模式：预览逻辑
        if (context.getState() != DrawingState.FIRST_CLICK) {
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

        if (context.getDrawMode() == DrawMode.CIRCLE) {
            // 计算预览半径
            previewRadius = Math.sqrt(
                    Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
            );
            
            // 检测圆相切吸附
            double tangentThreshold = 15.0 / context.getTransform().getScale();
            EdgeSnapManager.CircleTangentResult tangentResult = 
                    EdgeSnapManager.findCircleTangentSnap(firstPointX, firstPointY, previewRadius, 
                            context.getObjects(), tangentThreshold);
            
            // 如果找到相切吸附，使用调整后的半径预览
            if (tangentResult != null) {
                previewRadius = tangentResult.getRadius();
            }
            
            // 更新CircleDrawingTool的预览参数
            circleTool.setPreviewParams(firstPointX, firstPointY, previewRadius);
        }

        // 重绘以显示预览
        context.redraw();
        return true;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || context.getState() != DrawingState.FIRST_CLICK) {
            return;
        }

        // 绘制第一个点（所有模式都需要显示）
        double firstPointScreenX = transform.worldToScreenX(firstPointX);
        double firstPointScreenY = transform.worldToScreenY(firstPointY);
        gc.setFill(Color.valueOf("#759eb2"));
        double pointRadius = 4;
        gc.fillOval(firstPointScreenX - pointRadius, firstPointScreenY - pointRadius, pointRadius * 2, pointRadius * 2);

        if (context.getDrawMode() == DrawMode.CIRCLE) {
            // 圆形预览
            circleTool.paintPreview(gc, transform);
        } else if (context.getDrawMode() == DrawMode.LINE || context.getDrawMode() == DrawMode.INFINITE_LINE) {
            // 绘制线段/直线的预览
            double sx1 = transform.worldToScreenX(firstPointX);
            double sy1 = transform.worldToScreenY(firstPointY);
            double sx2 = transform.worldToScreenX(context.getCurrentMouseX());
            double sy2 = transform.worldToScreenY(context.getCurrentMouseY());

            // 设置浅色虚线样式用于预览
            gc.setStroke(Color.valueOf("#759eb2"));
            gc.setLineWidth(1);
            gc.setLineDashes(6);

            if (context.getDrawMode() == DrawMode.INFINITE_LINE) {
                // 为无限直线扩展端点
                double dx = sx2 - sx1;
                double dy = sy2 - sy1;
                double scale = 10000; // 一个足够高的扩展值

                if (Math.abs(dx) < 1e-10) {
                    // 竖直线
                    sx2 = sx1;
                    sy1 = -scale;
                    sy2 = scale;
                } else if (Math.abs(dy) < 1e-10) {
                    // 水平线
                    sx1 = -scale;
                    sx2 = scale;
                } else {
                    // 一般情况
                    double t = scale / Math.hypot(dx, dy);
                    double p1x = sx1 - t * dx;
                    double p1y = sy1 - t * dy;
                    double p2x = sx1 + t * dx;
                    double p2y = sy1 + t * dy;
                    sx1 = p1x;
                    sy1 = p1y;
                    sx2 = p2x;
                    sy2 = p2y;
                }
            }

            gc.strokeLine(sx1, sy1, sx2, sy2);
            gc.setLineDashes(null);

            // 绘制当前鼠标位置的点
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            gc.setFill(Color.LIGHTGRAY);
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }
    }

    @Override
    public void reset() {
        firstPointX = 0;
        firstPointY = 0;
        firstClickExistingPoint = null;
        firstPointRef = null;
        previewRadius = 0;
        circleTool.reset();
    }

    /**
     * 处理点模式的绘制
     */
    private void handlePointDrawing(double worldX, double worldY, DrawingContext context) {
        // 点模式：检测是否靠近图形（吸附）
        double scale = context.getTransform().getScale();
        double snapDistance = 15.0 / scale; // 吸附距离阈值

        // 查找最近的可约束图形
        WorldObject nearestShape = null;
        double minDistance = snapDistance;

        for (WorldObject obj : context.getObjects()) {
            if (obj instanceof LineGeo || obj instanceof InfiniteLineGeo ||
                    obj instanceof CircleGeo || obj instanceof PolygonGeo || obj instanceof PathGeo) {
                // 创建临时约束来计算距离
                PointConstraint tempConstraint = context.getConstraintHandler().createConstraint(obj);
                double distance = tempConstraint.distanceToShape(worldX, worldY);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestShape = obj;
                }
            }
        }

        PointGeo newPoint;
        if (nearestShape != null) {
            // 创建约束点
            PointConstraint constraint = context.getConstraintHandler().createConstraint(nearestShape);
            double parameter = constraint.calculateParameter(worldX, worldY);
            constraint.setParameter(parameter);

            Point2D constrainedPos = constraint.getPointFromParameter();
            newPoint = new PointGeo(constrainedPos.getX(), constrainedPos.getY());
            newPoint.setConstraint(constraint);
        } else {
            // 创建普通点
            newPoint = new PointGeo(worldX, worldY);
        }

        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                context.addObject(newPoint);
            }

            @Override
            public void undo() {
                context.removeObject(newPoint);
            }
        });

        context.setState(DrawingState.IDLE);
    }

    /**
     * 处理第一次点击
     */
    private void handleFirstClick(double rawX, double rawY, double worldX, double worldY, DrawingContext context) {
        // 第一次点击：记录起点，进入预览状态
        firstPointX = worldX;
        firstPointY = worldY;
        context.setState(DrawingState.FIRST_CLICK);

        // 检查第一次点击是否靠近现有特殊点（用于圆心吸附）
        firstClickExistingPoint = context.getSnappingHandler().findNearestSpecialPoint(rawX, rawY, context);
        
        // 检查第一次点击位置是否已有点对象（用于复用）
        double scale = context.getTransform().getScale();
        firstPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        // 对于圆形绘制，初始化预览参数
        if (context.getDrawMode() == DrawMode.CIRCLE) {
            circleTool.setPreviewParams(firstPointX, firstPointY, 0);
        }
    }

    /**
     * 处理第二次点击
     */
    private void handleSecondClick(double worldX, double worldY, DrawingContext context) {
        double scale = context.getTransform().getScale();
        
        // 检查第二次点击位置是否已有点对象（用于复用）
        PointGeo secondPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);
        
        switch (context.getDrawMode()) {
            case CIRCLE -> {
                double radius = Math.sqrt(
                        Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
                );
                
                // 检测圆相切吸附
                double tangentThreshold = 15.0 / scale;
                EdgeSnapManager.CircleTangentResult tangentResult = 
                        EdgeSnapManager.findCircleTangentSnap(firstPointX, firstPointY, radius, 
                                context.getObjects(), tangentThreshold);
                
                double finalRadius = (tangentResult != null) ? tangentResult.getRadius() : radius;

                // 创建圆，复用已有的圆心点
                CircleGeo newCircle = new CircleGeo(firstPointRef, firstPointX, firstPointY, finalRadius);

                // 计算此圆产生的所有交点
                List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                        .checkIntersections(newCircle, context);

                context.executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        context.addObject(newCircle);
                        for (PointGeo point : intersectionPoints) {
                            context.addObject(point);
                        }
                    }

                    @Override
                    public void undo() {
                        context.removeObject(newCircle);
                        for (PointGeo point : intersectionPoints) {
                            context.removeObject(point);
                        }
                    }
                });

                circleTool.reset();
                firstClickExistingPoint = null;
                firstPointRef = null;
            }
            case LINE -> {
                // 创建线段，复用已有的端点
                LineGeo newLine = new LineGeo(firstPointRef, firstPointX, firstPointY, 
                                              secondPointRef, worldX, worldY);
                
                List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                        .checkIntersections(newLine, context);

                context.executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        context.addObject(newLine);
                        for (PointGeo point : intersectionPoints) {
                            context.addObject(point);
                        }
                    }

                    @Override
                    public void undo() {
                        context.removeObject(newLine);
                        for (PointGeo point : intersectionPoints) {
                            context.removeObject(point);
                        }
                    }
                });
                
                firstPointRef = null;
            }
            case INFINITE_LINE -> {
                // 创建无限直线，复用已有的端点
                InfiniteLineGeo newLine = new InfiniteLineGeo(firstPointRef, firstPointX, firstPointY,
                                                               secondPointRef, worldX, worldY);
                
                List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                        .checkIntersections(newLine, context);

                context.executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        context.addObject(newLine);
                        for (PointGeo point : intersectionPoints) {
                            context.addObject(point);
                        }
                    }

                    @Override
                    public void undo() {
                        context.removeObject(newLine);
                        for (PointGeo point : intersectionPoints) {
                            context.removeObject(point);
                        }
                    }
                });
                
                firstPointRef = null;
            }
        }

        context.setState(DrawingState.IDLE);
        previewRadius = 0;
        context.redraw();
    }
}
