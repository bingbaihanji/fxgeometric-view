package com.binbaihanji.view.layout.draw.tools;

import com.binbaihanji.view.layout.core.GridChartView;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class CircleDrawingTool {

    private boolean hasCenter = false;
    private double cx, cy;
    private double previewRadius = 0;

    public void onMouseClicked(GridChartView pane, MouseEvent e) {

        double wx = pane.screenToWorldX(e.getX());
        double wy = pane.screenToWorldY(e.getY());

        if (!hasCenter) {
            cx = wx;
            cy = wy;
            hasCenter = true;
        } else {
            double r = Math.hypot(wx - cx, wy - cy);
            pane.addObject(new CircleGeo(cx, cy, r));
            hasCenter = false;
        }

        pane.redraw();
    }

    public void onMouseMoved(GridChartView pane, MouseEvent e) {

        if (!hasCenter) return;

        double wx = pane.screenToWorldX(e.getX());
        double wy = pane.screenToWorldY(e.getY());

        previewRadius = Math.hypot(wx - cx, wy - cy);
        pane.redraw();
    }

    // 添加公共方法，允许外部设置预览参数
    public void setPreviewParams(double centerX, double centerY, double radius) {
        this.cx = centerX;
        this.cy = centerY;
        this.previewRadius = radius;
        // 确保预览可以正确显示
        this.hasCenter = true;
    }

    // 添加公共方法，重置工具状态
    public void reset() {
        this.hasCenter = false;
        this.previewRadius = 0;
    }

    public void paintPreview(GraphicsContext gc, WorldTransform transform) {

        if (!hasCenter) return;

        double sx = transform.worldToScreenX(cx);
        double sy = transform.worldToScreenY(cy);
        double sr = previewRadius * transform.getScale();

        gc.setStroke(Color.GRAY);
        gc.setLineDashes(6);
        gc.strokeOval(sx - sr, sy - sr, sr * 2, sr * 2);
        gc.setLineDashes(null);

        // 添加圆心点的预览显示，与线段绘制保持一致
        gc.setFill(Color.LIGHTGRAY);
        double pointRadius = 3;
        gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);
    }
}