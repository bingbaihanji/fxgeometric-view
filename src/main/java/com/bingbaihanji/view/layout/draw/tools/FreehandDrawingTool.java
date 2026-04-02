package com.bingbaihanji.view.layout.draw.tools;

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
    private boolean isDrawing = false;
    private final List<Point2D> points = new ArrayList<>();

    // ========== 可配置的平滑参数 ==========

    // 默认值
    //    小 (0.1-0.3)   保留更多点，曲线更贴近原始形状，但可能有尖刺
    //    中 (0.5-1.0)   平衡，适合大多数手绘
    //    大 (2.0-3.0)   大幅简化，曲线更平滑，但可能丢失细节
    public static final double DEFAULT_SIMPLIFY_EPSILON = 0.25;      // 简化容差: 算法会找出曲线上"最弯曲"的点保留下来,平坦部分会被简化成直线

    // 小 (5-8)     点数少，曲线可能有棱角
    // 中 (12-20)   平滑且性能好
    // 大 (25-30)   非常平滑，但点太多可能影响性能
    public static final int DEFAULT_SMOOTH_SEGMENTS = 20;           // 平滑细分数: 每两个控制点之间，生成多少个中间点,越多点 = 曲线越平滑细腻

    // 0.0 = 直线连接
    // 0.5 = 默认平滑（推荐）
    // 1.0 = 最紧，最贴近原始折线
    public static final double DEFAULT_TENSION = 0.2;               // 张力: 控制曲线的"紧绷"程度

    //   新点与前一个点距离小于此值时跳过
    //   减少数据量，降低抖动影响
    //    小 (0.01-0.02)   几乎不过滤，保留所有鼠标移动
    //    中 (0.05)        适当过滤，去除手抖
    //    大 (0.1-0.2)     大幅过滤，可能丢失快速绘制细节

    public static final double DEFAULT_MIN_POINT_DISTANCE = 0.04;   // 最小点间距: 过滤鼠标移动时产生的过密点

    public static final boolean DEFAULT_ENABLE_SMOOTHING = true;    // 是否启用平滑

    // 当前值（可动态修改）
    private double simplifyEpsilon = DEFAULT_SIMPLIFY_EPSILON;
    private int smoothSegments = DEFAULT_SMOOTH_SEGMENTS;
    private double tension = DEFAULT_TENSION;
    private double minPointDistance = DEFAULT_MIN_POINT_DISTANCE;
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
            // 不在这里调用redraw，由DrawingController统一处理
        }
    }

    private void addPoint(GridChartView pane, MouseEvent e) {
        double wx = pane.screenToWorldX(e.getX());
        double wy = pane.screenToWorldY(e.getY());
        Point2D newPoint = new Point2D(wx, wy);

        // 过滤过于密集的点（最小采样距离）
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
        // 创建一个手绘路径对象（保留完整曲线形状，但只显示起点和终点）
        if (points.size() >= 2) {
            PathGeo path = new PathGeo(new ArrayList<>(points));
            pane.addObject(path);
        }
    }

    /**
     * 获取当前绘制的路径点（应用优化后的平滑处理）
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

        // 获取平滑后的点（使用手绘专用平滑）
        List<Point2D> previewPoints = enableSmoothing && points.size() >= 3
                ? CurveSmoothing.smoothHandDrawnCurve(points, simplifyEpsilon, smoothSegments, tension)
                : points;

        // 绘制预览曲线（使用 Path 一次性绘制，更平滑）
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1.5);
        gc.setLineDashes(3);

        // 使用 Path API 绘制，避免线段间的断开
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
        this.simplifyEpsilon = DEFAULT_SIMPLIFY_EPSILON;
        this.smoothSegments = DEFAULT_SMOOTH_SEGMENTS;
        this.tension = DEFAULT_TENSION;
        this.minPointDistance = DEFAULT_MIN_POINT_DISTANCE;
        this.enableSmoothing = DEFAULT_ENABLE_SMOOTHING;
    }
}
