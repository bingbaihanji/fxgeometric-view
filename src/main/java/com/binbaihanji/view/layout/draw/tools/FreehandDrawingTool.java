package com.binbaihanji.view.layout.draw.tools;

import com.binbaihanji.view.layout.core.GridChartPane;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.impl.LineGeo;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

public class FreehandDrawingTool {
    private boolean isDrawing = false;
    private List<Point2D> points = new ArrayList<>();
    
    public void onMousePressed(GridChartPane pane, MouseEvent e) {
        isDrawing = true;
        points.clear();
        addPoint(pane, e);
    }
    
    public void onMouseDragged(GridChartPane pane, MouseEvent e) {
        if (isDrawing) {
            addPoint(pane, e);
            pane.redraw();
        }
    }
    
    public void onMouseReleased(GridChartPane pane, MouseEvent e) {
        if (isDrawing) {
            addPoint(pane, e);
            // 创建线段连接所有点
            createLines(pane);
            isDrawing = false;
            points.clear();
            pane.redraw();
        }
    }
    
    private void addPoint(GridChartPane pane, MouseEvent e) {
        double wx = pane.screenToWorldX(e.getX());
        double wy = pane.screenToWorldY(e.getY());
        points.add(new Point2D(wx, wy));
    }
    
    private void createLines(GridChartPane pane) {
        // 创建连续的线段连接所有点
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D start = points.get(i);
            Point2D end = points.get(i + 1);
            pane.addObject(new LineGeo(start.getX(), start.getY(), end.getX(), end.getY()));
        }
    }
    
    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        if (!isDrawing || points.size() < 2) return;
        
        // 绘制预览线段
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);
        gc.setLineDashes(2);
        
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D start = points.get(i);
            Point2D end = points.get(i + 1);
            
            double sx1 = transform.worldToScreenX(start.getX());
            double sy1 = transform.worldToScreenY(start.getY());
            double sx2 = transform.worldToScreenX(end.getX());
            double sy2 = transform.worldToScreenY(end.getY());
            
            gc.strokeLine(sx1, sy1, sx2, sy2);
        }
        
        gc.setLineDashes(null);
    }
}