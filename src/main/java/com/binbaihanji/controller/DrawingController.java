package com.binbaihanji.controller;

import com.binbaihanji.constant.DrawMode;
import com.binbaihanji.util.IntersectionUtils;
import com.binbaihanji.util.SpecialPointManager;
import com.binbaihanji.util.SpecialPointManager.SpecialPoint;
import com.binbaihanji.view.layout.core.GridChartPane;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import com.binbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.LineGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.PolygonGeo;
import com.binbaihanji.view.layout.draw.tools.CircleDrawingTool;
import com.binbaihanji.view.layout.draw.tools.FreehandDrawingTool;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;
import com.binbaihanji.view.layout.core.WorldTransform;

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
    private final GridChartPane gridChartPane;

    /**
     * 当前绘制模式
     */
    private DrawMode drawMode = DrawMode.NONE;

    /**
     * 多边形顶点列表（用于POLYGON模式）
     */
    private final List<Point2D> polygonVertices = new ArrayList<>();
    
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
     * 绘制状态
     */
    private enum DrawingState {
        IDLE,              // 空闲状态
        FIRST_CLICK,       // 已点击第一个点，等待第二次点击
        POLYGON_DRAWING    // 多边形绘制中（依次选择顶点）
    }

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

    public DrawingController(GridChartPane gridChartPane) {
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
            gridChartPane.addObject(newPoint);
            state = DrawingState.IDLE;
            // 消费点绘制事件
            e.consume();
            // 检查新点与其他图形的交点
            checkIntersections(newPoint);
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
                    gridChartPane.addObject(newCircle);
                    // 重置CircleDrawingTool状态
                    circleTool.reset();
                    // 检查新圆与其他图形的交点
                    checkIntersections(newCircle);
                }
                case LINE -> {
                    // 只创建线段对象，不创建独立的端点
                    LineGeo newLine = new LineGeo(firstPointX, firstPointY, worldX, worldY);
                    gridChartPane.addObject(newLine);
                    // 检查新线段与其他图形的交点
                    checkIntersections(newLine);
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
        gridChartPane.addObject(polygon);
        
        // 检查交点
        checkIntersections(polygon);
        
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
            
            // 更新控制点位置（减去偏移量）
            draggingPoint.updatePosition(worldX - dragOffsetX, worldY - dragOffsetY);
            
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
            e.consume();
        } else if (draggingPoint != null) {
            // 结束拖动
            draggingPoint = null;
            dragOffsetX = 0;
            dragOffsetY = 0;
            
            // 重新计算所有交点
            recalculateAllIntersections();
            
            e.consume();
        }
    }

    /**
     * 绘制预览图形
     */
    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        if (state == DrawingState.FIRST_CLICK) {
            // 修复：移除了previewRadius > 0的条件，确保在首次点击时也能显示预览
            // 这样可以在点击时立即显示圆心点，与线段绘制行为保持一致
            if (drawMode == DrawMode.CIRCLE) {
                // 圆形预览现在可以正确显示，包括首次点击时的圆心点
                circleTool.paintPreview(gc, transform);
            } else {
                // 绘制线段的预览
                if (drawMode == DrawMode.LINE) {
                    double sx1 = transform.worldToScreenX(firstPointX);
                    double sy1 = transform.worldToScreenY(firstPointY);
                    double sx2 = transform.worldToScreenX(currentMouseX);
                    double sy2 = transform.worldToScreenY(currentMouseY);
                    
                    // 设置浅色虚线样式用于预览
                    gc.setStroke(Color.valueOf("#759eb2"));
                    gc.setLineWidth(1);
                    gc.setLineDashes(6);
                    
                    gc.strokeLine(sx1, sy1, sx2, sy2);
                    
                    // 绘制端点
                    gc.setFill(Color.LIGHTGRAY);
                    double pointRadius = 3;
                    gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
                    gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
                    
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
        } else if (drawMode == DrawMode.FREEHAND) {
            freehandTool.paintPreview(gc, transform);
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
        gridChartPane.clearAllObjects();
    }

    /**
     * 检查新添加的图形与其他图形的交点，并绘制交点
     * @param newObject 新添加的图形对象
     */
    private void checkIntersections(Object newObject) {
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
                    // 使用特殊颜色绘制交点
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY());
                    intersectionPoint.setColor(Color.PURPLE); // 设置交点颜色
                    intersectionPoints.add(intersectionPoint); // 先收集起来
                }
            } else if (newObject instanceof LineGeo && obj instanceof CircleGeo) {
                // 线段与圆的交点
                List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections((LineGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY());
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof CircleGeo && obj instanceof LineGeo) {
                // 圆与线段的交点
                List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections((LineGeo) obj, (CircleGeo) newObject);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY());
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof CircleGeo && obj instanceof CircleGeo) {
                // 圆与圆的交点
                List<Point2D> intersections = IntersectionUtils.getCircleCircleIntersections((CircleGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY());
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            }
        }
        
        // 在遍历结束后统一添加所有交点
        for (PointGeo point : intersectionPoints) {
            gridChartPane.addObject(point);
        }
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
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY());
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
        // 这里需要一个方法来检查点的颜色
        // 由于PointGeo没有public的getColor方法，我们需要添加一个
        // 暂时使用反射或者添加一个标记
        try {
            java.lang.reflect.Field colorField = PointGeo.class.getDeclaredField("color");
            colorField.setAccessible(true);
            Color color = (Color) colorField.get(point);
            return Color.PURPLE.equals(color);
        } catch (Exception e) {
            return false;
        }
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
        }
        
        return intersections;
    }
    
    /**
     * 查找最近的特殊点（用于磁性吸附）
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
}