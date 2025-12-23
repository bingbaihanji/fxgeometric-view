package com.binbaihanji.view.layout.draw.tools;

import com.binbaihanji.view.layout.core.GridChartView;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.impl.PathGeo;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class FreehandDrawingTool {
    private boolean isDrawing = false;
    private List<Point2D> points = new ArrayList<>();

    public void onMousePressed(GridChartView pane, MouseEvent e) {
        isDrawing = true;
        points.clear();
        addPoint(pane, e);
    }

    public void onMouseDragged(GridChartView pane, MouseEvent e) {
        if (isDrawing) {
            addPoint(pane, e);
            pane.redraw();
        }
    }

    public void onMouseReleased(GridChartView pane, MouseEvent e) {
        if (isDrawing) {
            addPoint(pane, e);
            isDrawing = false;
            // 不在这里调用redraw，由DrawingController统一处理
        }
    }

    private void addPoint(GridChartView pane, MouseEvent e) {
        double wx = pane.screenToWorldX(e.getX());
        double wy = pane.screenToWorldY(e.getY());
        points.add(new Point2D(wx, wy));
    }

    private void createLines(GridChartView pane) {
        // 创建一个手绘路径对象（保留完整曲线形状，但只显示起点和终点）
        if (points.size() >= 2) {
            PathGeo path = new PathGeo(new ArrayList<>(points));
            pane.addObject(path);
        }
    }

    /**
     * 获取当前绘制的路径点
     */
    public List<Point2D> getPoints() {
        return new ArrayList<>(points);
    }

    /**
     * 清空路径点
     */
    public void clearPoints() {
        points.clear();
    }

    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        // 检查是否正在绘制并且有足够的点
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