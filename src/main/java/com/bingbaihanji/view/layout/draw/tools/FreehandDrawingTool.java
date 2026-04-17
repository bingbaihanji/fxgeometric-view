package com.bingbaihanji.view.layout.draw.tools;

import com.bingbaihanji.constant.HandDrawnParameters;
import com.bingbaihanji.util.CurveSmoothing;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.impl.PathGeo;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class FreehandDrawingTool {

    public static final boolean DEFAULT_ENABLE_SMOOTHING = true;    // 是否启用平滑


    private final List<Point2D> points = new ArrayList<>();
    private boolean isDrawing = false;
    // 当前值(可动态修改)
    private double simplifyEpsilon = HandDrawnParameters.DEFAULT_SIMPLIFY_EPSILON.getValue().doubleValue();
    private int smoothSegments = HandDrawnParameters.DEFAULT_SMOOTH_SEGMENTS.getValue().intValue();
    private double tension = HandDrawnParameters.DEFAULT_TENSION.getValue().doubleValue();
    private double minPointDistance = HandDrawnParameters.DEFAULT_MIN_POINT_DISTANCE.getValue().doubleValue();
    private boolean enableSmoothing = DEFAULT_ENABLE_SMOOTHING;

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
            // 不在这里调用redraw,由DrawingController统一处理
        }
    }

    private void addPoint(GridChartView pane, MouseEvent e) {
        double wx = pane.screenToWorldX(e.getX());
        double wy = pane.screenToWorldY(e.getY());
        Point2D newPoint = new Point2D(wx, wy);

        // 过滤过于密集的点(最小采样距离)
        if (!points.isEmpty()) {
            Point2D lastPoint = points.get(points.size() - 1);
            double distance = newPoint.distance(lastPoint);
            if (distance < minPointDistance) {
                return; // 跳过过近的点
            }
        }

        points.add(newPoint);
    }

    private void createLines(GridChartView pane) {
        // 创建一个手绘路径对象(保留完整曲线形状,但只显示起点和终点)
        if (points.size() >= 2) {
            PathGeo path = new PathGeo(new ArrayList<>(points));
            pane.addObject(path);
        }
    }

    /**
     * 获取当前绘制的路径点(应用优化后的平滑处理)
     */
    public List<Point2D> getPoints() {
        if (!enableSmoothing || points.size() < 3) {
            return new ArrayList<>(points);
        }

        // 使用手绘专用平滑方法：高斯去噪 -> 去尖刺 -> 简化 -> Catmull-Rom平滑
        return CurveSmoothing.smoothHandDrawnCurve(points, simplifyEpsilon, smoothSegments, tension);
    }

    /**
     * 获取原始未平滑的点集
     */
    public List<Point2D> getRawPoints() {
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

        // 获取平滑后的点(使用手绘专用平滑)
        List<Point2D> previewPoints = enableSmoothing && points.size() >= 3
                ? CurveSmoothing.smoothHandDrawnCurve(points, simplifyEpsilon, smoothSegments, tension)
                : points;

        // 绘制预览曲线(使用 Path 一次性绘制,更平滑)
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1.5);
        gc.setLineDashes(3);

        // 使用 Path API 绘制,避免线段间的断开
        gc.beginPath();
        Point2D first = previewPoints.get(0);
        gc.moveTo(transform.worldToScreenX(first.getX()), transform.worldToScreenY(first.getY()));

        for (int i = 1; i < previewPoints.size(); i++) {
            Point2D p = previewPoints.get(i);
            gc.lineTo(transform.worldToScreenX(p.getX()), transform.worldToScreenY(p.getY()));
        }
        gc.stroke();

        gc.setLineDashes(null);
    }

    //   Getter 和 Setter 方法

    public double getSimplifyEpsilon() {
        return simplifyEpsilon;
    }

    public void setSimplifyEpsilon(double simplifyEpsilon) {
        this.simplifyEpsilon = Math.max(0.01, Math.min(5.0, simplifyEpsilon));
    }

    public int getSmoothSegments() {
        return smoothSegments;
    }

    public void setSmoothSegments(int smoothSegments) {
        this.smoothSegments = Math.max(2, Math.min(50, smoothSegments));
    }

    public double getTension() {
        return tension;
    }

    public void setTension(double tension) {
        this.tension = Math.max(0.0, Math.min(1.0, tension));
    }

    public double getMinPointDistance() {
        return minPointDistance;
    }

    public void setMinPointDistance(double minPointDistance) {
        this.minPointDistance = Math.max(0.001, Math.min(1.0, minPointDistance));
    }

    public boolean isEnableSmoothing() {
        return enableSmoothing;
    }

    public void setEnableSmoothing(boolean enableSmoothing) {
        this.enableSmoothing = enableSmoothing;
    }

    /**
     * 重置为默认值
     */
    public void resetToDefaults() {
        this.simplifyEpsilon = HandDrawnParameters.DEFAULT_SIMPLIFY_EPSILON.getValue().doubleValue();
        this.smoothSegments = HandDrawnParameters.DEFAULT_SMOOTH_SEGMENTS.getValue().intValue();
        this.tension = HandDrawnParameters.DEFAULT_TENSION.getValue().doubleValue();
        this.minPointDistance = HandDrawnParameters.DEFAULT_MIN_POINT_DISTANCE.getValue().doubleValue();
        this.enableSmoothing = DEFAULT_ENABLE_SMOOTHING;
    }
}
