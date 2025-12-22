package com.binbaihanji.controller;

import com.binbaihanji.constant.DrawMode;
import com.binbaihanji.view.layout.core.GridChartPane;
import com.binbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.LineGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.RectangleGeo;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.canvas.GraphicsContext;
import com.binbaihanji.view.layout.core.WorldTransform;

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
     * 多边形边数（用于POLYGON模式）
     */
    private int polygonSides = 5;

    /**
     * 绘制状态
     */
    private enum DrawingState {
        IDLE,           // 空闲状态
        FIRST_CLICK     // 已点击第一个点，等待第二次点击
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

    public DrawingController(GridChartPane gridChartPane) {
        this.gridChartPane = gridChartPane;
        initMouseHandlers();
    }

    /**
     * 初始化鼠标事件处理器
     */
    private void initMouseHandlers() {
        gridChartPane.setOnMouseClicked(this::handleMouseClicked);
        gridChartPane.setOnMouseMoved(this::handleMouseMoved);
    }

    /**
     * 设置绘制模式
     */
    public void setDrawMode(DrawMode mode) {
        this.drawMode = mode;
        this.state = DrawingState.IDLE;
        // 清除预览
        gridChartPane.redraw();
    }

    /**
     * 设置多边形边数
     */
    public void setPolygonSides(int sides) {
        this.polygonSides = Math.max(3, Math.min(20, sides));
    }

    /**
     * 鼠标点击事件
     */
    public void handleMouseClicked(MouseEvent e) {
        // 只处理左键
        if (e.getButton() != MouseButton.PRIMARY || drawMode == DrawMode.NONE) {
            return;
        }

        double worldX = gridChartPane.screenToWorldX(e.getX());
        double worldY = gridChartPane.screenToWorldY(e.getY());

        if (drawMode == DrawMode.POINT) {
            // 点模式：直接绘制
            gridChartPane.addObject(new PointGeo(worldX, worldY));
            state = DrawingState.IDLE;
            // 消费点绘制事件
            e.consume();
        } else if (state == DrawingState.IDLE) {
            // 第一次点击：记录起点，进入预览状态
            firstPointX = worldX;
            firstPointY = worldY;
            state = DrawingState.FIRST_CLICK;
            // 消费第一次点击事件
            e.consume();
        } else if (state == DrawingState.FIRST_CLICK) {
            // 第二次点击：完成绘制
            switch (drawMode) {
                case CIRCLE -> {
                    double radius = Math.sqrt(
                            Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
                    );
                    gridChartPane.addObject(new CircleGeo(firstPointX, firstPointY, radius));
                }
                case LINE -> {
                    // 添加起点和终点的点
                    gridChartPane.addObject(new PointGeo(firstPointX, firstPointY));
                    gridChartPane.addObject(new PointGeo(worldX, worldY));
                    // 创建线段对象并添加到画布
                    gridChartPane.addObject(new LineGeo(firstPointX, firstPointY, worldX, worldY));
                }
                case TRIANGLE -> {
                    // 三角形绘制可以后续实现
                }
                case RECTANGLE -> {
                    // 计算矩形的左下角坐标和宽高
                    double rectX = Math.min(firstPointX, worldX);
                    double rectY = Math.min(firstPointY, worldY);
                    double rectWidth = Math.abs(worldX - firstPointX);
                    double rectHeight = Math.abs(worldY - firstPointY);
                    
                    // 创建矩形对象并添加到画布
                    gridChartPane.addObject(new RectangleGeo(rectX, rectY, rectWidth, rectHeight));
                }
                case POLYGON -> {
                    // 多边形绘制可以后续实现
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
     * 鼠标移动事件（实时预览）
     */
    public void handleMouseMoved(MouseEvent e) {
        if (state == DrawingState.FIRST_CLICK) {
            double worldX = gridChartPane.screenToWorldX(e.getX());
            double worldY = gridChartPane.screenToWorldY(e.getY());
            
            // 保存当前鼠标位置用于预览
            currentMouseX = worldX;
            currentMouseY = worldY;
            
            if (drawMode == DrawMode.CIRCLE) {
                // 计算预览半径
                previewRadius = Math.sqrt(
                        Math.pow(worldX - firstPointX, 2) + Math.pow(worldY - firstPointY, 2)
                );
            }
            
            // 重绘以显示预览
            gridChartPane.redraw();
        }
    }

    /**
     * 绘制预览图形
     */
    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        if (state == DrawingState.FIRST_CLICK && previewRadius > 0) {
            if (drawMode == DrawMode.CIRCLE) {
                double sx = transform.worldToScreenX(firstPointX);
                double sy = transform.worldToScreenY(firstPointY);
                double sr = previewRadius * transform.getScale();

                // 设置浅色虚线样式用于预览
                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineWidth(1);
                gc.setLineDashes(6);

                gc.strokeOval(
                        sx - sr,
                        sy - sr,
                        sr * 2,
                        sr * 2
                );

                // 清除虚线设置
                gc.setLineDashes(null);
            }
        } else if (state == DrawingState.FIRST_CLICK) {
            // 绘制线段或矩形的预览
            if (drawMode == DrawMode.LINE) {
                double sx1 = transform.worldToScreenX(firstPointX);
                double sy1 = transform.worldToScreenY(firstPointY);
                double sx2 = transform.worldToScreenX(currentMouseX);
                double sy2 = transform.worldToScreenY(currentMouseY);
                
                // 设置浅色虚线样式用于预览
                gc.setStroke(Color.LIGHTGRAY);
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
            } else if (drawMode == DrawMode.RECTANGLE) {
                double rectX = Math.min(firstPointX, currentMouseX);
                double rectY = Math.min(firstPointY, currentMouseY);
                double rectWidth = Math.abs(currentMouseX - firstPointX);
                double rectHeight = Math.abs(currentMouseY - firstPointY);
                
                double sx = transform.worldToScreenX(rectX);
                double sy = transform.worldToScreenY(rectY);
                double sw = rectWidth * transform.getScale();
                double sh = rectHeight * transform.getScale();
                
                // 矩形需要根据坐标系方向调整绘制方式
                double screenY = sy - sh; // 调整Y坐标
                
                // 设置浅色虚线样式用于预览
                gc.setStroke(Color.LIGHTGRAY);
                gc.setLineWidth(1);
                gc.setLineDashes(6);
                
                gc.strokeRect(sx, screenY, sw, sh);
                
                // 清除虚线设置
                gc.setLineDashes(null);
            }
        }
    }

    /**
     * 清除所有图形
     */
    public void clearAll() {
        gridChartPane.clearAllObjects();
    }
}