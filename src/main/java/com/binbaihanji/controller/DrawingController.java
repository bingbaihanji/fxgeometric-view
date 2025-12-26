package com.binbaihanji.controller;

import com.binbaihanji.constant.DrawMode;
import com.binbaihanji.util.*;
import com.binbaihanji.util.SpecialPointManager.SpecialPoint;
import com.binbaihanji.view.layout.core.GridChartView;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import com.binbaihanji.view.layout.draw.geometry.impl.*;
import com.binbaihanji.view.layout.draw.tools.CircleDrawingTool;
import com.binbaihanji.view.layout.draw.tools.FreehandDrawingTool;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
     * 已选中的线段或直线（用于作图工具）
     */
    private WorldObject selectedLine = null;

    /**
     * 已选中的圆（用于作图工具）
     */
    private CircleGeo selectedCircle = null;

    /**
     * 已选中的要旋转的图形
     */
    private WorldObject selectedRotateShape = null;
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
        // 清除选中的对象
        selectedLine = null;
        selectedCircle = null;
        selectedRotateShape = null;
        // 如果是旋转模式，进入选择图形状态
        if (mode == DrawMode.ROTATE) {
            state = DrawingState.ROTATE_SELECT_SHAPE;
        }
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

        // 处理中点模式
        if (drawMode == DrawMode.MIDPOINT) {
            handleMidpointClick(worldX, worldY);
            e.consume();
            return;
        }

        // 处理垂线模式
        if (drawMode == DrawMode.PERPENDICULAR) {
            handlePerpendicularClick(worldX, worldY);
            e.consume();
            return;
        }

        // 处理垂直平分线模式
        if (drawMode == DrawMode.PERPENDICULAR_BISECTOR) {
            handlePerpendicularBisectorClick(worldX, worldY);
            e.consume();
            return;
        }

        // 处理平行线模式
        if (drawMode == DrawMode.PARALLEL) {
            handleParallelClick(worldX, worldY);
            e.consume();
            return;
        }

        // 处理切线模式
        if (drawMode == DrawMode.TANGENT) {
            handleTangentClick(worldX, worldY);
            e.consume();
            return;
        }

        // 处理旋转模式
        if (drawMode == DrawMode.ROTATE) {
            handleRotateClick(worldX, worldY);
            e.consume();
            return;
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
        } else if (selectedLine != null && (drawMode == DrawMode.PERPENDICULAR ||
                drawMode == DrawMode.PARALLEL)) {
            // 作图工具模式下，鼠标移动时更新预览（垂直平分线不需要预览）
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
        } else if (drawMode == DrawMode.TANGENT) {
            // 切线模式下，鼠标移动时更新预览
            gridChartPane.redraw();
        } else if (drawMode == DrawMode.ROTATE) {
            // 旋转模式下，鼠标移动时更新预览
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
            // 作图工具预览：垂线、平行线（垂直平分线不需要预览，点击即绘制）
            if (selectedLine != null && (drawMode == DrawMode.PERPENDICULAR ||
                    drawMode == DrawMode.PARALLEL)) {
                // 高亮显示已选中的线段/直线
                selectedLine.setHover(true);
                selectedLine.paint(gc, transform, gridChartPane.getWidth(), gridChartPane.getHeight());
                selectedLine.setHover(false);

                // 根据不同的作图模式绘制预览线
                double x1, y1, x2, y2;
                if (selectedLine instanceof LineGeo line) {
                    x1 = line.getStartX();
                    y1 = line.getStartY();
                    x2 = line.getEndX();
                    y2 = line.getEndY();
                } else if (selectedLine instanceof InfiniteLineGeo line) {
                    x1 = line.getPoint1X();
                    y1 = line.getPoint1Y();
                    x2 = line.getPoint2X();
                    y2 = line.getPoint2Y();
                } else {
                    return;
                }

                Point2D[] previewLine = null;
                if (drawMode == DrawMode.PERPENDICULAR) {
                    previewLine = IntersectionUtils.getPerpendicularLine(x1, y1, x2, y2, currentMouseX, currentMouseY);
                } else if (drawMode == DrawMode.PARALLEL) {
                    previewLine = IntersectionUtils.getParallelLine(x1, y1, x2, y2, currentMouseX, currentMouseY);
                }

                if (previewLine != null) {
                    // 扩展为无限直线的屏幕坐标
                    double px1 = previewLine[0].getX();
                    double py1 = previewLine[0].getY();
                    double px2 = previewLine[1].getX();
                    double py2 = previewLine[1].getY();

                    // 计算方向向量并扩展
                    double dx = px2 - px1;
                    double dy = py2 - py1;
                    double len = Math.hypot(dx, dy);
                    if (len > 1e-10) {
                        double extendScale = 10000.0;
                        double ux = dx / len;
                        double uy = dy / len;
                        double extX1 = px1 - ux * extendScale;
                        double extY1 = py1 - uy * extendScale;
                        double extX2 = px1 + ux * extendScale;
                        double extY2 = py1 + uy * extendScale;

                        double sx1 = transform.worldToScreenX(extX1);
                        double sy1 = transform.worldToScreenY(extY1);
                        double sx2 = transform.worldToScreenX(extX2);
                        double sy2 = transform.worldToScreenY(extY2);

                        // 绘制虚线预览
                        gc.setStroke(Color.valueOf("#759eb2"));
                        gc.setLineWidth(2);
                        gc.setLineDashes(6);
                        gc.strokeLine(sx1, sy1, sx2, sy2);
                        gc.setLineDashes(null);
                    }

                    // 绘制鼠标位置的点
                    double mouseScreenX = transform.worldToScreenX(currentMouseX);
                    double mouseScreenY = transform.worldToScreenY(currentMouseY);
                    gc.setFill(Color.LIGHTGRAY);
                    double pointRadius = 3;
                    gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
                }
            } else if (drawMode == DrawMode.CIRCLE) {
                // 圆形预览
                circleTool.paintPreview(gc, transform);
            } else if (drawMode == DrawMode.LINE || drawMode == DrawMode.INFINITE_LINE) {
                // 绘制线段/直线的预览
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
        } else if (state == DrawingState.FIRST_CLICK && selectedLine != null) {
            // 作图工具预览：高亮显示已选中的线段/直线并显示预览线
            selectedLine.setHover(true);
            selectedLine.paint(gc, transform, gridChartPane.getWidth(), gridChartPane.getHeight());
            selectedLine.setHover(false);

            // 根据不同的作图模式绘制预览线
            double x1, y1, x2, y2;
            if (selectedLine instanceof LineGeo line) {
                x1 = line.getStartX();
                y1 = line.getStartY();
                x2 = line.getEndX();
                y2 = line.getEndY();
            } else if (selectedLine instanceof InfiniteLineGeo line) {
                x1 = line.getPoint1X();
                y1 = line.getPoint1Y();
                x2 = line.getPoint2X();
                y2 = line.getPoint2Y();
            } else {
                return;
            }

            Point2D[] previewLine = null;
            if (drawMode == DrawMode.PERPENDICULAR) {
                previewLine = IntersectionUtils.getPerpendicularLine(x1, y1, x2, y2, currentMouseX, currentMouseY);
            } else if (drawMode == DrawMode.PERPENDICULAR_BISECTOR) {
                previewLine = IntersectionUtils.getPerpendicularBisector(x1, y1, x2, y2, currentMouseX, currentMouseY);
            } else if (drawMode == DrawMode.PARALLEL) {
                previewLine = IntersectionUtils.getParallelLine(x1, y1, x2, y2, currentMouseX, currentMouseY);
            }

            if (previewLine != null) {
                // 扩展为无限直线的屏幕坐标
                double px1 = previewLine[0].getX();
                double py1 = previewLine[0].getY();
                double px2 = previewLine[1].getX();
                double py2 = previewLine[1].getY();

                // 计算方向向量并扩展
                double dx = px2 - px1;
                double dy = py2 - py1;
                double len = Math.hypot(dx, dy);
                if (len > 1e-10) {
                    double extendScale = 10000.0;
                    double ux = dx / len;
                    double uy = dy / len;
                    double extX1 = px1 - ux * extendScale;
                    double extY1 = py1 - uy * extendScale;
                    double extX2 = px1 + ux * extendScale;
                    double extY2 = py1 + uy * extendScale;

                    double sx1 = transform.worldToScreenX(extX1);
                    double sy1 = transform.worldToScreenY(extY1);
                    double sx2 = transform.worldToScreenX(extX2);
                    double sy2 = transform.worldToScreenY(extY2);

                    // 绘制虚线预览
                    gc.setStroke(Color.valueOf("#759eb2"));
                    gc.setLineWidth(2);
                    gc.setLineDashes(6);
                    gc.strokeLine(sx1, sy1, sx2, sy2);
                    gc.setLineDashes(null);
                }

                // 绘制鼠标位置的点
                double mouseScreenX = transform.worldToScreenX(currentMouseX);
                double mouseScreenY = transform.worldToScreenY(currentMouseY);
                gc.setFill(Color.LIGHTGRAY);
                double pointRadius = 3;
                gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
            }
        } else if (drawMode == DrawMode.TANGENT) {
            // 切线模式预览：显示鼠标附近的圆和切线预览
            double scale = transform.getScale();
            double tolerance = 15.0 / scale;

            for (WorldObject obj : gridChartPane.getObjects()) {
                if (obj instanceof CircleGeo circle) {
                    // 检查鼠标是否靠近圆
                    double distance = Math.hypot(currentMouseX - circle.getCx(), currentMouseY - circle.getCy());
                    if (Math.abs(distance - circle.getR()) <= tolerance) {
                        // 高亮显示圆
                        circle.setHover(true);
                        circle.paint(gc, transform, gridChartPane.getWidth(), gridChartPane.getHeight());
                        circle.setHover(false);

                        // 计算离鼠标位置最近的圆周上的点（将鼠标位置投影到圆周上）
                        double dx = currentMouseX - circle.getCx();
                        double dy = currentMouseY - circle.getCy();
                        double len = Math.hypot(dx, dy);

                        // 归一化方向向量并乘以半径，得到圆周上的点
                        double tangentPointX = circle.getCx() + (dx / len) * circle.getR();
                        double tangentPointY = circle.getCy() + (dy / len) * circle.getR();

                        // 计算并绘制切线预览
                        Point2D[] tangentLine = IntersectionUtils.getTangentLine(
                                circle.getCx(), circle.getCy(), tangentPointX, tangentPointY
                        );

                        // 扩展为无限直线
                        double px1 = tangentLine[0].getX();
                        double py1 = tangentLine[0].getY();
                        double px2 = tangentLine[1].getX();
                        double py2 = tangentLine[1].getY();

                        double tdx = px2 - px1;
                        double tdy = py2 - py1;
                        double tlen = Math.hypot(tdx, tdy);
                        if (tlen > 1e-10) {
                            double extendScale = 10000.0;
                            double ux = tdx / tlen;
                            double uy = tdy / tlen;
                            double extX1 = px1 - ux * extendScale;
                            double extY1 = py1 - uy * extendScale;
                            double extX2 = px1 + ux * extendScale;
                            double extY2 = py1 + uy * extendScale;

                            double sx1 = transform.worldToScreenX(extX1);
                            double sy1 = transform.worldToScreenY(extY1);
                            double sx2 = transform.worldToScreenX(extX2);
                            double sy2 = transform.worldToScreenY(extY2);

                            gc.setStroke(Color.valueOf("#759eb2"));
                            gc.setLineWidth(2);
                            gc.setLineDashes(6);
                            gc.strokeLine(sx1, sy1, sx2, sy2);
                            gc.setLineDashes(null);
                        }

                        // 绘制切点（投影后的圆周上的点）
                        double tangentScreenX = transform.worldToScreenX(tangentPointX);
                        double tangentScreenY = transform.worldToScreenY(tangentPointY);
                        gc.setFill(Color.ORANGE);
                        double pointRadius = 4;
                        gc.fillOval(tangentScreenX - pointRadius, tangentScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
                        break;
                    }
                }
            }
        } else if (drawMode == DrawMode.ROTATE) {
            // 旋转模式预览
            if (state == DrawingState.ROTATE_SELECT_SHAPE) {
                // 高亮鼠标悬停的图形
                double scale = transform.getScale();
                double tolerance = 10.0 / scale;

                for (WorldObject obj : gridChartPane.getObjects()) {
                    if (obj instanceof PointGeo) continue;

                    if (obj.hitTest(currentMouseX, currentMouseY, tolerance)) {
                        obj.setHover(true);
                        obj.paint(gc, transform, gridChartPane.getWidth(), gridChartPane.getHeight());
                        obj.setHover(false);
                        break;
                    }
                }
            } else if (state == DrawingState.ROTATE_SELECT_CENTER && selectedRotateShape != null) {
                // 高亮显示已选中的图形
                selectedRotateShape.setHover(true);
                selectedRotateShape.paint(gc, transform, gridChartPane.getWidth(), gridChartPane.getHeight());
                selectedRotateShape.setHover(false);

                // 显示当前鼠标位置作为旋转中心点预览
                double mouseScreenX = transform.worldToScreenX(currentMouseX);
                double mouseScreenY = transform.worldToScreenY(currentMouseY);

                // 绘制旋转中心点预览（十字准星）
                gc.setStroke(Color.MAGENTA);
                gc.setLineWidth(2);
                double crossSize = 8;
                gc.strokeLine(mouseScreenX - crossSize, mouseScreenY, mouseScreenX + crossSize, mouseScreenY);
                gc.strokeLine(mouseScreenX, mouseScreenY - crossSize, mouseScreenX, mouseScreenY + crossSize);

                // 绘制旋转中心圆圈
                gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);
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
     * 处理中点模式的点击事件
     * 点击线段或直线，立即绘制其中点
     */
    private void handleMidpointClick(double worldX, double worldY) {
        // 查找点击位置附近的线段或直线
        double scale = gridChartPane.getTransform().getScale();
        double tolerance = 10.0 / scale;

        for (WorldObject obj : gridChartPane.getObjects()) {
            if (obj instanceof LineGeo line) {
                if (line.hitTest(worldX, worldY, tolerance)) {
                    // 计算中点
                    Point2D midpoint = IntersectionUtils.getMidpoint(
                            line.getStartX(), line.getStartY(),
                            line.getEndX(), line.getEndY()
                    );

                    // 绘制中点
                    PointGeo newPoint = new PointGeo(midpoint.getX(), midpoint.getY());
                    newPoint.setColor(Color.GREEN);
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
                    return;
                }
            } else if (obj instanceof InfiniteLineGeo line) {
                if (line.hitTest(worldX, worldY, tolerance)) {
                    // 直线的中点是定义点的中点
                    Point2D midpoint = IntersectionUtils.getMidpoint(
                            line.getPoint1X(), line.getPoint1Y(),
                            line.getPoint2X(), line.getPoint2Y()
                    );

                    // 绘制中点
                    PointGeo newPoint = new PointGeo(midpoint.getX(), midpoint.getY());
                    newPoint.setColor(Color.GREEN);
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
                    return;
                }
            }
        }
    }

    /**
     * 处理垂线模式的点击事件
     * 第一次点击：选择线段或直线
     * 第二次点击：选择一点，过此点绘制垂线
     */
    private void handlePerpendicularClick(double worldX, double worldY) {
        if (state == DrawingState.IDLE) {
            // 第一次点击：选择线段或直线
            double scale = gridChartPane.getTransform().getScale();
            double tolerance = 10.0 / scale;

            for (WorldObject obj : gridChartPane.getObjects()) {
                if (obj instanceof LineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        state = DrawingState.FIRST_CLICK;
                        gridChartPane.redraw();
                        return;
                    }
                } else if (obj instanceof InfiniteLineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        state = DrawingState.FIRST_CLICK;
                        gridChartPane.redraw();
                        return;
                    }
                }
            }
        } else if (state == DrawingState.FIRST_CLICK && selectedLine != null) {
            // 第二次点击：过此点绘制垂线
            double x1, y1, x2, y2;
            if (selectedLine instanceof LineGeo line) {
                x1 = line.getStartX();
                y1 = line.getStartY();
                x2 = line.getEndX();
                y2 = line.getEndY();
            } else if (selectedLine instanceof InfiniteLineGeo line) {
                x1 = line.getPoint1X();
                y1 = line.getPoint1Y();
                x2 = line.getPoint2X();
                y2 = line.getPoint2Y();
            } else {
                return;
            }

            // 计算垂线：过 (worldX, worldY) 点
            Point2D[] perpLine = IntersectionUtils.getPerpendicularLine(x1, y1, x2, y2, worldX, worldY);

            // 创建垂线（使用鼠标点和垂线上的另一点）
            InfiniteLineGeo newLine = new InfiniteLineGeo(
                    worldX, worldY,
                    perpLine[0].getX(), perpLine[0].getY()
            );

            // 计算交点
            List<PointGeo> intersectionPoints = checkIntersections(newLine);
            commandHistory.execute(new CommandHistory.Command() {
                @Override
                public void execute() {
                    gridChartPane.addObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        gridChartPane.addObject(point);
                    }
                }

                @Override
                public void undo() {
                    gridChartPane.removeObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        gridChartPane.removeObject(point);
                    }
                }
            });

            // 重置状态
            selectedLine = null;
            state = DrawingState.IDLE;
            gridChartPane.redraw();
        }
    }

    /**
     * 处理垂直平分线模式的点击事件
     * 点击线段，立即绘制其垂直平分线（直线没有垂直平分线）
     */
    private void handlePerpendicularBisectorClick(double worldX, double worldY) {
        // 查找点击位置附近的线段（注意：只有线段才有垂直平分线，直线没有）
        double scale = gridChartPane.getTransform().getScale();
        double tolerance = 10.0 / scale;

        for (WorldObject obj : gridChartPane.getObjects()) {
            if (obj instanceof LineGeo line) {
                if (line.hitTest(worldX, worldY, tolerance)) {
                    // 计算线段的中点
                    double midX = (line.getStartX() + line.getEndX()) / 2.0;
                    double midY = (line.getStartY() + line.getEndY()) / 2.0;

                    // 计算垂直平分线（过中点，垂直于线段）
                    Point2D[] bisectorLine = IntersectionUtils.getPerpendicularLine(
                            line.getStartX(), line.getStartY(),
                            line.getEndX(), line.getEndY(),
                            midX, midY
                    );

                    // 创建垂直平分线（使用中点和垂直方向点）
                    InfiniteLineGeo newLine = new InfiniteLineGeo(
                            midX, midY,
                            bisectorLine[0].getX(), bisectorLine[0].getY()
                    );

                    // 计算交点
                    List<PointGeo> intersectionPoints = checkIntersections(newLine);
                    commandHistory.execute(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            gridChartPane.addObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            gridChartPane.removeObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.removeObject(point);
                            }
                        }
                    });
                    return;
                }
            }
            // 注意：不处理 InfiniteLineGeo，因为直线没有垂直平分线
        }
    }

    /**
     * 处理平行线模式的点击事件
     * 第一次点击：选择线段或直线
     * 第二次点击：选择一点，过此点绘制平行线
     */
    private void handleParallelClick(double worldX, double worldY) {
        if (state == DrawingState.IDLE) {
            // 第一次点击：选择线段或直线
            double scale = gridChartPane.getTransform().getScale();
            double tolerance = 10.0 / scale;

            for (WorldObject obj : gridChartPane.getObjects()) {
                if (obj instanceof LineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        state = DrawingState.FIRST_CLICK;
                        gridChartPane.redraw();
                        return;
                    }
                } else if (obj instanceof InfiniteLineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        state = DrawingState.FIRST_CLICK;
                        gridChartPane.redraw();
                        return;
                    }
                }
            }
        } else if (state == DrawingState.FIRST_CLICK && selectedLine != null) {
            // 第二次点击：过此点绘制平行线
            double x1, y1, x2, y2;
            if (selectedLine instanceof LineGeo line) {
                x1 = line.getStartX();
                y1 = line.getStartY();
                x2 = line.getEndX();
                y2 = line.getEndY();
            } else if (selectedLine instanceof InfiniteLineGeo line) {
                x1 = line.getPoint1X();
                y1 = line.getPoint1Y();
                x2 = line.getPoint2X();
                y2 = line.getPoint2Y();
            } else {
                return;
            }

            // 计算平行线
            Point2D[] parallelLine = IntersectionUtils.getParallelLine(x1, y1, x2, y2, worldX, worldY);

            // 创建平行线（使用鼠标点和平行线上的另一点）
            InfiniteLineGeo newLine = new InfiniteLineGeo(
                    worldX, worldY,
                    parallelLine[0].getX(), parallelLine[0].getY()
            );

            // 计算交点
            List<PointGeo> intersectionPoints = checkIntersections(newLine);
            commandHistory.execute(new CommandHistory.Command() {
                @Override
                public void execute() {
                    gridChartPane.addObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        gridChartPane.addObject(point);
                    }
                }

                @Override
                public void undo() {
                    gridChartPane.removeObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        gridChartPane.removeObject(point);
                    }
                }
            });

            // 重置状态
            selectedLine = null;
            state = DrawingState.IDLE;
            gridChartPane.redraw();
        }
    }

    /**
     * 处理切线模式的点击事件
     * 点击靠近圆的位置，自动吸附到圆周上最近的点，过该点绘制切线
     */
    private void handleTangentClick(double worldX, double worldY) {
        // 查找点击位置附近的圆
        double scale = gridChartPane.getTransform().getScale();
        double tolerance = 15.0 / scale;

        for (WorldObject obj : gridChartPane.getObjects()) {
            if (obj instanceof CircleGeo circle) {
                // 计算点击位置到圆心的距离
                double distance = Math.hypot(worldX - circle.getCx(), worldY - circle.getCy());

                // 检查是否靠近圆（允许一定容差）
                if (Math.abs(distance - circle.getR()) <= tolerance) {
                    // 计算离点击位置最近的圆周上的点（将点击位置投影到圆周上）
                    double dx = worldX - circle.getCx();
                    double dy = worldY - circle.getCy();
                    double len = Math.hypot(dx, dy);

                    // 归一化方向向量并乘以半径，得到圆周上的点
                    double tangentPointX = circle.getCx() + (dx / len) * circle.getR();
                    double tangentPointY = circle.getCy() + (dy / len) * circle.getR();

                    // 计算并绘制切线（过圆周上的切点）
                    Point2D[] tangentLine = IntersectionUtils.getTangentLine(
                            circle.getCx(), circle.getCy(), tangentPointX, tangentPointY
                    );

                    // 创建切线（使用切点和切线上的另一点）
                    InfiniteLineGeo newLine = new InfiniteLineGeo(
                            tangentPointX, tangentPointY,
                            tangentLine[0].getX(), tangentLine[0].getY()
                    );

                    // 计算交点
                    List<PointGeo> intersectionPoints = checkIntersections(newLine);
                    commandHistory.execute(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            gridChartPane.addObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            gridChartPane.removeObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                gridChartPane.removeObject(point);
                            }
                        }
                    });
                    return;
                }
            }
        }
    }

    /**
     * 处理旋转模式的点击事件
     * 第一次点击：选择要旋转的几何图形
     * 第二次点击：选择旋转中心点，然后弹出对话框输入角度和方向
     */
    private void handleRotateClick(double worldX, double worldY) {
        if (state == DrawingState.ROTATE_SELECT_SHAPE) {
            // 第一次点击：选择要旋转的图形
            double scale = gridChartPane.getTransform().getScale();
            double tolerance = 10.0 / scale;

            for (WorldObject obj : gridChartPane.getObjects()) {
                // 排除点对象（点旋转没有意义）
                if (obj instanceof PointGeo) continue;

                if (obj.hitTest(worldX, worldY, tolerance)) {
                    selectedRotateShape = obj;
                    state = DrawingState.ROTATE_SELECT_CENTER;
                    gridChartPane.redraw();
                    return;
                }
            }
        } else if (state == DrawingState.ROTATE_SELECT_CENTER && selectedRotateShape != null) {
            // 第二次点击：选择旋转中心点，弹出对话框
            final double rotateCenterX = worldX;
            final double rotateCenterY = worldY;

            // 创建旋转参数对话框
            Optional<Pair<Double, Boolean>> result = showRotateDialog();

            if (result.isPresent()) {
                double angleDegrees = result.get().getKey();
                boolean clockwise = result.get().getValue();

                // 将角度转换为弧度，并根据方向调整
                double angleRadians = Math.toRadians(angleDegrees);
                if (clockwise) {
                    angleRadians = -angleRadians; // 顺时针方向为负角度
                }

                final double finalAngle = angleRadians;
                final WorldObject shapeToRotate = selectedRotateShape;

                // 使用命令历史执行旋转，支持撤销/恢复
                commandHistory.execute(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        shapeToRotate.rotateAroundPoint(rotateCenterX, rotateCenterY, finalAngle);
                    }

                    @Override
                    public void undo() {
                        // 反向旋转
                        shapeToRotate.rotateAroundPoint(rotateCenterX, rotateCenterY, -finalAngle);
                    }
                });

                // 重新计算交点
                recalculateAllIntersections();
            }

            // 重置状态
            selectedRotateShape = null;
            state = DrawingState.ROTATE_SELECT_SHAPE;
            gridChartPane.redraw();
        }
    }

    /**
     * 显示旋转参数对话框
     *
     * @return 旋转角度和方向（true为顺时针，false为逆时针）
     */
    private Optional<Pair<Double, Boolean>> showRotateDialog() {
        Dialog<Pair<Double, Boolean>> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("rotating.windows.title"));
        dialog.setHeaderText(I18nUtil.getString("rotating.windows.header"));

        // 显示对话框后获取Stage并设置图标
        dialog.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // 对话框正在显示
                Window window = dialog.getDialogPane().getScene().getWindow();
                if (window instanceof Stage stage) {
                    stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/icon/rotating.png")).toExternalForm()));
                }
            }
        });

        // 设置按钮
        ButtonType confirmButtonType = new ButtonType(I18nUtil.getString("rotating.windows.okButton"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 角度输入框
        TextField angleField = new TextField();
        angleField.setPromptText(I18nUtil.getString("rotating.windows.angle"));
        angleField.setText("90");

        // 方向选择
        ToggleGroup directionGroup = new ToggleGroup();
        RadioButton clockwiseBtn = new RadioButton(I18nUtil.getString("rotating.windows.clockwise"));
        clockwiseBtn.setToggleGroup(directionGroup);
        clockwiseBtn.setSelected(true);
        RadioButton counterclockwiseBtn = new RadioButton(I18nUtil.getString("rotating.windows.counterclockwise"));
        counterclockwiseBtn.setToggleGroup(directionGroup);

        grid.add(new Label(I18nUtil.getString("rotating.windows.rotationAngle")), 0, 0);
        grid.add(angleField, 1, 0);
        grid.add(new Label(I18nUtil.getString("rotating.windows.rotationDirection")), 0, 1);
        grid.add(clockwiseBtn, 1, 1);
        grid.add(counterclockwiseBtn, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                try {
                    double angle = Double.parseDouble(angleField.getText());
                    boolean clockwise = clockwiseBtn.isSelected();
                    return new Pair<>(angle, clockwise);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
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
        POLYGON_DRAWING,   // 多边形绘制中（依次选择顶点）
        ROTATE_SELECT_SHAPE,  // 旋转模式：选择要旋转的图形
        ROTATE_SELECT_CENTER  // 旋转模式：选择旋转中心点
    }
}