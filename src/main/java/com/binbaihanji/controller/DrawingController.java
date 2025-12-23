package com.binbaihanji.controller;

import com.binbaihanji.constant.DrawMode;
import com.binbaihanji.util.CommandHistory;
import com.binbaihanji.util.IntersectionUtils;
import com.binbaihanji.util.PointNameManager;
import com.binbaihanji.util.SpecialPointManager;
import com.binbaihanji.util.SpecialPointManager.SpecialPoint;
import com.binbaihanji.view.layout.core.GridChartView;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import com.binbaihanji.view.layout.draw.geometry.impl.*;
import com.binbaihanji.view.layout.draw.tools.CircleDrawingTool;
import com.binbaihanji.view.layout.draw.tools.FreehandDrawingTool;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 图形绘制控制器
 * <p>
 * 处理鼠标交互，控制图形的绘制流程
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public class DrawingController {

    /**
     * 坐标系面板
     */
    private final GridChartView gridChartPane;

    /**
     * 命令历史管理器
     */
    private final CommandHistory commandHistory = new CommandHistory();
    /**
     * 多边形顶点列表（用于POLYGON模式）
     */
    private final List<Point2D> polygonVertices = new ArrayList<>();
    /**
     * 当前绘制模式
     */
    private DrawMode drawMode = DrawMode.NONE;
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
    private DrawingState state = DrawingState.IDLE;
    /**
     * 第一个点的世界坐标（用于绘制圆、线段等）
     */
    private double firstPointX;
    private double firstPointY;
    /**
     * 预览半径
     */
    private double previewRadius = 0;
    /**
     * 当前鼠标位置的世界坐标（用于预览）
     */
    private double currentMouseX;
    private double currentMouseY;
    private CircleDrawingTool circleTool;
    private FreehandDrawingTool freehandTool;
    public DrawingController(GridChartView gridChartPane) {
        this.gridChartPane = gridChartPane;
        // CircleDrawingTool现在可以正确地与DrawingController协同工作
        // 通过添加setPreviewParams和reset方法，实现了与DrawingController的状态同步
        this.circleTool = new CircleDrawingTool();
        this.freehandTool = new FreehandDrawingTool();
        initMouseHandlers();
    }

    /**
     * 初始化鼠标事件处理器
     */
    private void initMouseHandlers() {
        gridChartPane.setOnMouseClicked(this::handleMouseClicked);
        gridChartPane.setOnMouseMoved(this::handleMouseMoved);
        gridChartPane.setOnMousePressed(this::handleMousePressed);
        gridChartPane.setOnMouseDragged(this::handleMouseDragged);
        gridChartPane.setOnMouseReleased(this::handleMouseReleased);
    }

    /**
     * 设置绘制模式
     */
    public void setDrawMode(DrawMode mode) {
        this.drawMode = mode;
        this.state = DrawingState.IDLE;
        // 清空多边形顶点列表
        polygonVertices.clear();
        // 清除预览
        gridChartPane.redraw();
    }

    /**
     * 鼠标点击事件
     */
    public void handleMouseClicked(MouseEvent e) {
        // 只处理左键
        if (e.getButton() != MouseButton.PRIMARY || drawMode == DrawMode.NONE) {
            return;
        }

        double rawX = gridChartPane.screenToWorldX(e.getX());
        double rawY = gridChartPane.screenToWorldY(e.getY());

        // 应用特殊点磁性吸附
        double worldX = rawX;
        double worldY = rawY;
        SpecialPoint nearestPoint = findNearestSpecialPoint(rawX, rawY);
        if (nearestPoint != null) {
            worldX = nearestPoint.getX();
            worldY = nearestPoint.getY();
        }

        if (drawMode == DrawMode.POINT) {
            // 点模式：直接绘制
            PointGeo newPoint = new PointGeo(worldX, worldY);
            commandHistory.execute(new CommandHistory.Command() {
                @Override
                public void execute() {
                    gridChartPane.addObject(newPoint);
                }

                @Override
                public void undo() {
                    gridChartPane.removeObject(newPoint);
                }
            });
            state = DrawingState.IDLE;
            // 消费点绘制事件
            e.consume();
            // 检查新点与其他图形的交点（点通常不产生新交点）
        } else if (drawMode == DrawMode.POLYGON) {
            // 多边形模式：依次选择顶点
            handlePolygonClick(worldX, worldY);
            e.consume();
        } else if (state == DrawingState.IDLE) {
            // 第一次点击：记录起点，进入预览状态
            firstPointX = worldX;
            firstPointY = worldY;
            state = DrawingState.FIRST_CLICK;

            // 对于圆形绘制，初始化预览参数
            if (drawMode == DrawMode.CIRCLE) {
                circleTool.setPreviewParams(firstPointX, firstPointY, 0);
            }

            // 消费第一次点击事件
            e.consume();
        } else if (state == DrawingState.FIRST_CLICK) {
            // 第二次点击：完成绘制
            switch (drawMode) {
                case CIRCLE -> {
                    double radius = Math.sqrt(
                            Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
                    );
                    CircleGeo newCircle = new CircleGeo(firstPointX, firstPointY, radius);
                    // 创建圆心点，使其拥有名称（如A1, B1等）
                    PointGeo centerPoint = new PointGeo(firstPointX, firstPointY);
                    // 计算此圆产生的所有交点
                    List<PointGeo> intersectionPoints = checkIntersections(newCircle);
                    commandHistory.execute(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            gridChartPane.addObject(centerPoint);
                            gridChartPane.addObject(newCircle);
                            // 添加交点
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            gridChartPane.removeObject(newCircle);
                            gridChartPane.removeObject(centerPoint);
                            // 移除交点
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.removeObject(point);
                            }
                        }
                    });
                    // 重置CircleDrawingTool状态
                    circleTool.reset();
                }
                case LINE -> {
                    // 只创建线段对象，不创建独立的端点
                    LineGeo newLine = new LineGeo(firstPointX, firstPointY, worldX, worldY);
                    // 计算此线段产生的所有交点
                    List<PointGeo> intersectionPoints = checkIntersections(newLine);
                    commandHistory.execute(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            gridChartPane.addObject(newLine);
                            // 添加交点
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            gridChartPane.removeObject(newLine);
                            // 移除交点
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.removeObject(point);
                            }
                        }
                    });
                }
                case INFINITE_LINE -> {
                    // 创建无限直线对象
                    InfiniteLineGeo newLine = new InfiniteLineGeo(firstPointX, firstPointY, worldX, worldY);
                    // 计算此无限直线产生的所有交点
                    List<PointGeo> intersectionPoints = checkIntersections(newLine);
                    commandHistory.execute(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            gridChartPane.addObject(newLine);
                            // 添加交点
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            gridChartPane.removeObject(newLine);
                            // 移除交点
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.removeObject(point);
                            }
                        }
                    });
                }
            }

            // 清除预览，回到空闲状态
            state = DrawingState.IDLE;
            previewRadius = 0;
            gridChartPane.redraw();
            // 消费第二次点击事件
            e.consume();
        }
    }

    /**
     * 处理多边形点击事件
     */
    private void handlePolygonClick(double worldX, double worldY) {
        // 检查是否与起点重合
        if (!polygonVertices.isEmpty()) {
            Point2D firstVertex = polygonVertices.get(0);
            double distance = Math.hypot(worldX - firstVertex.getX(), worldY - firstVertex.getY());

            // 如果与起点距离小于阈值，则完成多边形绘制
            double scale = gridChartPane.getTransform().getScale();
            double threshold = 15.0 / scale; // 15像素的吸附范围

            if (distance < threshold && polygonVertices.size() >= 3) {
                // 完成多边形绘制
                finishPolygon();
                return;
            }
        }

        // 添加新顶点
        polygonVertices.add(new Point2D(worldX, worldY));

        // 进入多边形绘制状态
        state = DrawingState.POLYGON_DRAWING;

        // 重绘以显示预览
        gridChartPane.redraw();
    }

    /**
     * 完成多边形绘制
     */
    private void finishPolygon() {
        if (polygonVertices.size() < 3) {
            return;
        }

        // 只创建多边形对象，不创建独立的点和线段
        PolygonGeo polygon = new PolygonGeo(new ArrayList<>(polygonVertices));
        // 计算此多边形产生的所有交点
        List<PointGeo> intersectionPoints = checkIntersections(polygon);
        commandHistory.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                gridChartPane.addObject(polygon);
                // 添加交点
                for (PointGeo point : intersectionPoints) {
                    gridChartPane.addObject(point);
                }
            }

            @Override
            public void undo() {
                gridChartPane.removeObject(polygon);
                // 移除交点
                for (PointGeo point : intersectionPoints) {
                    gridChartPane.removeObject(point);
                }
            }
        });

        // 重置状态
        polygonVertices.clear();
        state = DrawingState.IDLE;
        gridChartPane.redraw();
    }

    /**
     * 鼠标移动事件（实时预览）
     */
    public void handleMouseMoved(MouseEvent e) {
        double rawX = gridChartPane.screenToWorldX(e.getX());
        double rawY = gridChartPane.screenToWorldY(e.getY());

        // 更新当前鼠标位置
        currentMouseX = rawX;
        currentMouseY = rawY;

        if (state == DrawingState.FIRST_CLICK) {
            // 应用特殊点磁性吸附
            double worldX = rawX;
            double worldY = rawY;
            SpecialPoint nearestPoint = findNearestSpecialPoint(rawX, rawY);
            if (nearestPoint != null) {
                worldX = nearestPoint.getX();
                worldY = nearestPoint.getY();
            }

            // 保存当前鼠标位置用于预览
            currentMouseX = worldX;
            currentMouseY = worldY;

            if (drawMode == DrawMode.CIRCLE) {
                // 计算预览半径
                previewRadius = Math.sqrt(
                        Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
                );
                // 更新CircleDrawingTool的预览参数
                circleTool.setPreviewParams(firstPointX, firstPointY, previewRadius);
            }

            // 重绘以显示预览
            gridChartPane.redraw();
        } else if (state == DrawingState.POLYGON_DRAWING) {
            // 多边形绘制中，显示从最后一个顶点到当前鼠标位置的预览线
            // 应用特殊点磁性吸附
            double worldX = rawX;
            double worldY = rawY;
            SpecialPoint nearestPoint = findNearestSpecialPoint(rawX, rawY);
            if (nearestPoint != null) {
                worldX = nearestPoint.getX();
                worldY = nearestPoint.getY();
            }

            currentMouseX = worldX;
            currentMouseY = worldY;
            gridChartPane.redraw();
        } else if (drawMode == DrawMode.NONE) {
            // 非绘制模式下，重绘以显示控制点高亮
            gridChartPane.redraw();
        }
    }

    /**
     * 鼠标按下事件
     */
    public void handleMousePressed(MouseEvent e) {
        if (drawMode == DrawMode.FREEHAND) {
            freehandTool.onMousePressed(gridChartPane, e);
            e.consume();
        } else if (drawMode == DrawMode.NONE && e.getButton() == MouseButton.PRIMARY) {
            // 非绘制模式下，尝试选中控制点进行拖动
            double worldX = gridChartPane.screenToWorldX(e.getX());
            double worldY = gridChartPane.screenToWorldY(e.getY());

            // 计算容差
            double scale = gridChartPane.getTransform().getScale();
            double tolerance = 10.0 / scale; // 10像素的点击范围

            // 遍历所有图形的控制点，找到最近的控制点
            for (WorldObject obj : gridChartPane.getObjects()) {
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
                        return;
                    }
                }
            }
        }
    }

    /**
     * 鼠标拖拽事件
     */
    public void handleMouseDragged(MouseEvent e) {
        if (drawMode == DrawMode.FREEHAND) {
            freehandTool.onMouseDragged(gridChartPane, e);
            e.consume();
        } else if (draggingPoint != null) {
            // 拖动控制点
            double rawX = gridChartPane.screenToWorldX(e.getX());
            double rawY = gridChartPane.screenToWorldY(e.getY());

            // 应用特殊点磁性吸附
            double worldX = rawX;
            double worldY = rawY;
            SpecialPoint nearestPoint = findNearestSpecialPoint(rawX, rawY);
            if (nearestPoint != null) {
                worldX = nearestPoint.getX();
                worldY = nearestPoint.getY();
            }

            // 计算实际更新后的位置
            double newX = worldX - dragOffsetX;
            double newY = worldY - dragOffsetY;

            // 更新控制点位置
            draggingPoint.updatePosition(newX, newY);

            // 实时记录当前拖动位置（用于撤销/恢复）
            dragEndX = newX;
            dragEndY = newY;

            // 重绘
            gridChartPane.redraw();
            e.consume();
        } else if (state == DrawingState.FIRST_CLICK) {
            double rawX = gridChartPane.screenToWorldX(e.getX());
            double rawY = gridChartPane.screenToWorldY(e.getY());

            // 应用特殊点磁性吸附
            double worldX = rawX;
            double worldY = rawY;
            SpecialPoint nearestPoint = findNearestSpecialPoint(rawX, rawY);
            if (nearestPoint != null) {
                worldX = nearestPoint.getX();
                worldY = nearestPoint.getY();
            }

            // 保存当前鼠标位置用于预览
            currentMouseX = worldX;
            currentMouseY = worldY;

            if (drawMode == DrawMode.CIRCLE) {
                // 计算预览半径
                previewRadius = Math.sqrt(
                        Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
                );
                // 更新CircleDrawingTool的预览参数
                circleTool.setPreviewParams(firstPointX, firstPointY, previewRadius);
            }

            // 重绘以显示预览
            gridChartPane.redraw();
        }
    }

    /**
     * 鼠标释放事件
     */
    public void handleMouseReleased(MouseEvent e) {
        if (drawMode == DrawMode.FREEHAND) {
            freehandTool.onMouseReleased(gridChartPane, e);
            // 获取手绘路径点并创建 PathGeo 对象
            List<Point2D> points = freehandTool.getPoints();
            // 立即清空路径点，防止预览显示
            freehandTool.clearPoints();
            if (points.size() >= 2) {
                PathGeo newPath = new PathGeo(new ArrayList<>(points));
                // 计算此路径产生的所有交点
                List<PointGeo> intersectionPoints = checkIntersections(newPath);
                commandHistory.execute(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        gridChartPane.addObject(newPath);
                        // 添加交点
                        for (PointGeo point : intersectionPoints) {
                            gridChartPane.addObject(point);
                        }
                    }

                    @Override
                    public void undo() {
                        gridChartPane.removeObject(newPath);
                        // 移除交点
                        for (PointGeo point : intersectionPoints) {
                            gridChartPane.removeObject(point);
                        }
                    }
                });
            }
            gridChartPane.redraw();
            e.consume();
        } else if (draggingPoint != null) {
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
                commandHistory.addCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        // 恢复操作：移动到结束位置
                        pointRef.updatePosition(endX, endY);
                        recalculateAllIntersections();
                    }

                    @Override
                    public void undo() {
                        // 撤销操作：移动回起始位置
                        pointRef.updatePosition(startX, startY);
                        recalculateAllIntersections();
                    }
                });
            }

            draggingPoint = null;
            dragOffsetX = 0;
            dragOffsetY = 0;
            dragStartX = 0;
            dragStartY = 0;
            dragEndX = 0;
            dragEndY = 0;

            // 重新计算所有交点
            recalculateAllIntersections();

            e.consume();
        }
    }

    /**
     * 绘制预览图形
     */
    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        // 手绘线模式：不检查state，直接绘制
        if (drawMode == DrawMode.FREEHAND) {
            freehandTool.paintPreview(gc, transform);
            return;
        }

        if (state == DrawingState.FIRST_CLICK) {
            // 修复：移除了previewRadius > 0的条件，确保在首次点击时也能显示预览
            // 这样可以在点击时立即显示圆心点，与线段绘制行为保持一致
            if (drawMode == DrawMode.CIRCLE) {
                // 圆形预览现在可以正确显示，包括首次点击时的圆心点
                circleTool.paintPreview(gc, transform);
            } else {
                // 绘制线段/直线的预览
                if (drawMode == DrawMode.LINE || drawMode == DrawMode.INFINITE_LINE) {
                    double sx1 = transform.worldToScreenX(firstPointX);
                    double sy1 = transform.worldToScreenY(firstPointY);
                    double sx2 = transform.worldToScreenX(currentMouseX);
                    double sy2 = transform.worldToScreenY(currentMouseY);

                    // 设置浅色虚线样式用于预览
                    gc.setStroke(Color.valueOf("#759eb2"));
                    gc.setLineWidth(1);
                    gc.setLineDashes(6);

                    if (drawMode == DrawMode.INFINITE_LINE) {
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

                    // 绘制端点
                    gc.setFill(Color.LIGHTGRAY);
                    double pointRadius = 3;
                    double fsx1 = transform.worldToScreenX(firstPointX);
                    double fsy1 = transform.worldToScreenY(firstPointY);
                    gc.fillOval(fsx1 - pointRadius, fsy1 - pointRadius, pointRadius * 2, pointRadius * 2);
                    double fsx2 = transform.worldToScreenX(currentMouseX);
                    double fsy2 = transform.worldToScreenY(currentMouseY);
                    gc.fillOval(fsx2 - pointRadius, fsy2 - pointRadius, pointRadius * 2, pointRadius * 2);

                    // 清除虚线设置
                    gc.setLineDashes(null);
                }
            }
        } else if (state == DrawingState.POLYGON_DRAWING) {
            // 绘制多边形预览：显示已选择的顶点和边
            if (!polygonVertices.isEmpty()) {
                // 绘制已确定的顶点
                gc.setFill(Color.RED);
                double pointRadius = 4;
                for (Point2D vertex : polygonVertices) {
                    double sx = transform.worldToScreenX(vertex.getX());
                    double sy = transform.worldToScreenY(vertex.getY());
                    gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);
                }

                // 绘制已确定的边
                gc.setStroke(Color.DODGERBLUE);
                gc.setLineWidth(2);
                for (int i = 0; i < polygonVertices.size() - 1; i++) {
                    Point2D p1 = polygonVertices.get(i);
                    Point2D p2 = polygonVertices.get(i + 1);
                    double sx1 = transform.worldToScreenX(p1.getX());
                    double sy1 = transform.worldToScreenY(p1.getY());
                    double sx2 = transform.worldToScreenX(p2.getX());
                    double sy2 = transform.worldToScreenY(p2.getY());
                    gc.strokeLine(sx1, sy1, sx2, sy2);
                }

                // 绘制从最后一个顶点到鼠标的预览线
                Point2D lastVertex = polygonVertices.get(polygonVertices.size() - 1);
                Point2D firstVertex = polygonVertices.get(0);

                double sx1 = transform.worldToScreenX(lastVertex.getX());
                double sy1 = transform.worldToScreenY(lastVertex.getY());
                double sx2 = transform.worldToScreenX(currentMouseX);
                double sy2 = transform.worldToScreenY(currentMouseY);

                gc.setStroke(Color.valueOf("#759eb2"));
                gc.setLineWidth(1);
                gc.setLineDashes(6);
                gc.strokeLine(sx1, sy1, sx2, sy2);

                // 检查是否接近起点（用于显示闭合提示）
                double distance = Math.hypot(currentMouseX - firstVertex.getX(), currentMouseY - firstVertex.getY());
                double scale = transform.getScale();
                double threshold = 15.0 / scale;

                if (distance < threshold && polygonVertices.size() >= 3) {
                    // 显示闭合预览（高亮显示）
                    double sfx = transform.worldToScreenX(firstVertex.getX());
                    double sfy = transform.worldToScreenY(firstVertex.getY());

                    gc.setStroke(Color.GREEN);
                    gc.setLineWidth(2);
                    gc.strokeLine(sx1, sy1, sfx, sfy);

                    // 高亮起点
                    gc.setFill(Color.GREEN);
                    gc.fillOval(sfx - 6, sfy - 6, 12, 12);
                }

                gc.setLineDashes(null);
            }
        } else if (drawMode == DrawMode.NONE && draggingPoint == null) {
            // 非绘制模式下，高亮显示可拖动的控制点
            double mouseWorldX = currentMouseX;
            double mouseWorldY = currentMouseY;
            double scale = transform.getScale();
            double tolerance = 10.0 / scale;

            for (WorldObject obj : gridChartPane.getObjects()) {
                for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                    if (point.hitTest(mouseWorldX, mouseWorldY, tolerance)) {
                        // 绘制高亮圈
                        double sx = transform.worldToScreenX(point.getX());
                        double sy = transform.worldToScreenY(point.getY());

                        gc.setStroke(Color.ORANGE);
                        gc.setLineWidth(2);
                        gc.strokeOval(sx - 8, sy - 8, 16, 16);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 清除所有图形
     */
    public void clearAll() {
        // 保存当前所有对象，用于撤销
        List<WorldObject> objectsToClear = new ArrayList<>(gridChartPane.getObjects());
        commandHistory.execute(new CommandHistory.Command() {
            @Override
            public void execute() {
                gridChartPane.clearAllObjects();
                // 清除点命名管理器
                PointNameManager.getInstance().clear();
            }

            @Override
            public void undo() {
                for (WorldObject obj : objectsToClear) {
                    gridChartPane.addObject(obj);
                }
                // TODO: 恢复点名称映射（需要实现命名管理器的快照功能）
            }
        });
    }

    /**
     * 撤销操作
     */
    public void undo() {
        commandHistory.undo();
        gridChartPane.redraw();
    }

    /**
     * 恢复操作
     */
    public void redo() {
        commandHistory.redo();
        gridChartPane.redraw();
    }

    /**
     * 判断是否可以撤销
     */
    public boolean canUndo() {
        return commandHistory.canUndo();
    }

    /**
     * 判断是否可以恢复
     */
    public boolean canRedo() {
        return commandHistory.canRedo();
    }

    /**
     * 检查新添加的图形与其他图形的交点，并返回交点列表（不自动添加到画布）
     *
     * @param newObject 新添加的图形对象
     * @return 生成的交点列表
     */
    private List<PointGeo> checkIntersections(Object newObject) {
        List<WorldObject> allObjects = new ArrayList<>(gridChartPane.getObjects()); // 创建副本避免并发修改
        List<PointGeo> intersectionPoints = new ArrayList<>(); // 收集所有交点

        for (WorldObject obj : allObjects) {
            // 跳过自身
            if (obj == newObject) continue;

            // 检查不同类型的图形组合
            if (newObject instanceof LineGeo && obj instanceof LineGeo) {
                // 线段与线段的交点
                List<Point2D> intersections = IntersectionUtils.getLineLineIntersections((LineGeo) newObject, (LineGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof LineGeo && obj instanceof CircleGeo) {
                // 线段与圆的交点
                List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections((LineGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof CircleGeo && obj instanceof LineGeo) {
                // 圆与线段的交点
                List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections((LineGeo) obj, (CircleGeo) newObject);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof CircleGeo && obj instanceof CircleGeo) {
                // 圆与圆的交点
                List<Point2D> intersections = IntersectionUtils.getCircleCircleIntersections((CircleGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof InfiniteLineGeo && obj instanceof LineGeo) {
                // 无限直线与线段的交点
                List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) newObject, (LineGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof InfiniteLineGeo && obj instanceof CircleGeo) {
                // 无限直线与圆的交点
                List<Point2D> intersections = IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof InfiniteLineGeo && obj instanceof InfiniteLineGeo) {
                // 无限直线与无限直线的交点
                List<Point2D> intersections = IntersectionUtils.getInfiniteLineInfiniteLineIntersections((InfiniteLineGeo) newObject, (InfiniteLineGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof PolygonGeo polygon) {
                // 多边形与其他图形的交点：遍历多边形的每条边
                for (LineGeo edge : polygon.getEdges()) {
                    if (obj instanceof LineGeo line) {
                        List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, line);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof CircleGeo circle) {
                        List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections(edge, circle);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PolygonGeo otherPolygon) {
                        // 多边形与多边形的交点：遍历两个多边形的所有边
                        for (LineGeo otherEdge : otherPolygon.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, otherEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PathGeo path) {
                        // 多边形与手绘路径的交点
                        for (LineGeo pathEdge : path.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, pathEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    }
                }
            } else if (newObject instanceof PathGeo path) {
                // 手绘路径与其他图形的交点
                for (LineGeo edge : path.getEdges()) {
                    if (obj instanceof LineGeo line) {
                        List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, line);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof CircleGeo circle) {
                        List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections(edge, circle);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PolygonGeo polygon) {
                        for (LineGeo polyEdge : polygon.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, polyEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PathGeo otherPath) {
                        // 手绘路径与手绘路径的交点
                        for (LineGeo otherEdge : otherPath.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, otherEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    }
                }
            } else if (obj instanceof PolygonGeo polygon) {
                // 其他图形与多边形的交点
                for (LineGeo edge : polygon.getEdges()) {
                    if (newObject instanceof LineGeo line) {
                        List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(line, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (newObject instanceof CircleGeo circle) {
                        List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections(edge, circle);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (newObject instanceof InfiniteLineGeo infiniteLine) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    }
                }
            } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                // 其他图形与无限直线的交点
                if (newObject instanceof LineGeo line) {
                    List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, line);
                    for (Point2D point : intersections) {
                        PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                        intersectionPoint.setColor(Color.PURPLE);
                        intersectionPoints.add(intersectionPoint);
                    }
                } else if (newObject instanceof CircleGeo circle) {
                    List<Point2D> intersections = IntersectionUtils.getInfiniteLineCircleIntersections(infiniteLine, circle);
                    for (Point2D point : intersections) {
                        PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                        intersectionPoint.setColor(Color.PURPLE);
                        intersectionPoints.add(intersectionPoint);
                    }
                }
            } else if (newObject instanceof InfiniteLineGeo infiniteLine) {
                // 无限直线与多边形的交点
                if (obj instanceof PolygonGeo polygon) {
                    for (LineGeo edge : polygon.getEdges()) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    }
                } else if (obj instanceof PathGeo path) {
                    // 无限直线与手绘路径的交点
                    for (LineGeo edge : path.getEdges()) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    }
                }
            }
        }

        return intersectionPoints;
    }

    /**
     * 重新计算所有图形之间的交点
     * 用于拖动后更新交点位置
     */
    private void recalculateAllIntersections() {
        // 1. 删除所有旧的交点（紫色的PointGeo）
        List<WorldObject> allObjects = new ArrayList<>(gridChartPane.getObjects());
        for (WorldObject obj : allObjects) {
            if (obj instanceof PointGeo point) {
                // 检查是否为紫色交点
                if (isIntersectionPoint(point)) {
                    gridChartPane.removeObject(obj);
                }
            }
        }

        // 2. 重新计算所有图形之间的交点
        List<WorldObject> objects = new ArrayList<>(gridChartPane.getObjects());
        List<PointGeo> newIntersectionPoints = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            WorldObject obj1 = objects.get(i);
            // 跳过点对象
            if (obj1 instanceof PointGeo) continue;

            for (int j = i + 1; j < objects.size(); j++) {
                WorldObject obj2 = objects.get(j);
                // 跳过点对象
                if (obj2 instanceof PointGeo) continue;

                // 计算交点
                List<Point2D> intersections = calculateIntersections(obj1, obj2);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    newIntersectionPoints.add(intersectionPoint);
                }
            }
        }

        // 3. 添加新的交点
        for (PointGeo point : newIntersectionPoints) {
            gridChartPane.addObject(point);
        }

        // 4. 重绘
        gridChartPane.redraw();
    }

    /**
     * 判断点是否为交点（通过颜色判断）
     */
    private boolean isIntersectionPoint(PointGeo point) {
        // 使用新增的getColor方法
        Color color = point.getColor();
        return Color.PURPLE.equals(color);
    }

    /**
     * 计算两个几何对象之间的交点
     */
    private List<Point2D> calculateIntersections(WorldObject obj1, WorldObject obj2) {
        List<Point2D> intersections = new ArrayList<>();

        if (obj1 instanceof LineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineLineIntersections((LineGeo) obj1, (LineGeo) obj2));
        } else if (obj1 instanceof LineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj2, (CircleGeo) obj1));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getCircleCircleIntersections((CircleGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj1, (LineGeo) obj2));
        } else if (obj1 instanceof LineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj2, (LineGeo) obj1));
        } else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj2, (CircleGeo) obj1));
        } else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineInfiniteLineIntersections((InfiniteLineGeo) obj1, (InfiniteLineGeo) obj2));
        } else if (obj1 instanceof PolygonGeo polygon) {
            // 多边形与其他图形的交点
            for (LineGeo edge : polygon.getEdges()) {
                if (obj2 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, line));
                } else if (obj2 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj2 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                } else if (obj2 instanceof PolygonGeo otherPolygon) {
                    for (LineGeo otherEdge : otherPolygon.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, otherEdge));
                    }
                }
            }
        } else if (obj2 instanceof PolygonGeo polygon) {
            // 其他图形与多边形的交点
            for (LineGeo edge : polygon.getEdges()) {
                if (obj1 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(line, edge));
                } else if (obj1 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj1 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        } else if (obj1 instanceof InfiniteLineGeo infiniteLine) {
            // 无限直线与其他图形的交点
            if (obj2 instanceof PolygonGeo polygon) {
                for (LineGeo edge : polygon.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            } else if (obj2 instanceof PathGeo path) {
                for (LineGeo edge : path.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        } else if (obj2 instanceof InfiniteLineGeo infiniteLine) {
            // 其他图形与无限直线的交点
            if (obj1 instanceof PolygonGeo polygon) {
                for (LineGeo edge : polygon.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            } else if (obj1 instanceof PathGeo path) {
                for (LineGeo edge : path.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        } else if (obj1 instanceof PathGeo path) {
            // 手绘路径与其他图形的交点
            for (LineGeo edge : path.getEdges()) {
                if (obj2 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, line));
                } else if (obj2 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj2 instanceof PolygonGeo polygon) {
                    for (LineGeo polyEdge : polygon.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, polyEdge));
                    }
                } else if (obj2 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                } else if (obj2 instanceof PathGeo otherPath) {
                    for (LineGeo otherEdge : otherPath.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, otherEdge));
                    }
                }
            }
        } else if (obj2 instanceof PathGeo path) {
            // 其他图形与手绘路径的交点
            for (LineGeo edge : path.getEdges()) {
                if (obj1 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(line, edge));
                } else if (obj1 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj1 instanceof PolygonGeo polygon) {
                    for (LineGeo polyEdge : polygon.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(polyEdge, edge));
                    }
                } else if (obj1 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        }

        return intersections;
    }

    /**
     * 查找最近的特殊点（用于磁性吸附）
     *
     * @param x 当前鼠标x坐标（世界坐标）
     * @param y 当前鼠标y坐标（世界坐标）
     * @return 最近的特殊点，如果没有找到则返回null
     */
    private SpecialPoint findNearestSpecialPoint(double x, double y) {
        // 获取所有特殊点
        List<SpecialPoint> specialPoints = SpecialPointManager.extractSpecialPoints(gridChartPane.getObjects());

        // 计算吸附阈值（像素距离转换为世界坐标距离）
        double scale = gridChartPane.getTransform().getScale();
        double threshold = 15.0 / scale; // 15像素的吸附范围

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, specialPoints, threshold);
    }

    /**
     * 绘制状态
     */
    private enum DrawingState {
        IDLE,              // 空闲状态
        FIRST_CLICK,       // 已点击第一个点，等待第二次点击
        POLYGON_DRAWING    // 多边形绘制中（依次选择顶点）
    }
}