package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.FillRenderer;
import com.bingbaihanji.util.LabelRenderer;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class CircleGeo extends AbstractWorldObject {

    private double r;
    private double cx;
    private double cy;
    private String centerName; // 圆心名称


    public CircleGeo(double cx, double cy, double r) {
        super(ObjectType.CIRCLE);
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.color = StyleManager.GEOMETRY_LINE;
        // 自动为圆心分配名称
        this.centerName = PointNameManager.getInstance().assignName(cx, cy);
    }

    public CircleGeo(double cx, double cy, double r, boolean autoNameCenter) {
        super(ObjectType.CIRCLE);
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.color = StyleManager.GEOMETRY_LINE;
        // 根据参数决定是否为圆心自动命名
        if (autoNameCenter) {
            this.centerName = PointNameManager.getInstance().assignName(cx, cy);
        }
    }

    // Getter methods for intersection calculations
    public double getCx() {
        return cx;
    }

    public double getCy() {
        return cy;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double w,
                      double h) {

        double sx = transform.worldToScreenX(cx);
        double sy = transform.worldToScreenY(cy);
        double sr = r * transform.getScale();

        // 先绘制填充
        FillRenderer.fillOval(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, sx, sy, sr);

        // 再绘制边框
        // 应用线型
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());

        gc.strokeOval(
                sx - sr,
                sy - sr,
                sr * 2,
                sr * 2
        );

        // 重置线型
        LineStyleUtil.resetLineStyle(gc);

        // 根据项目规范要求,绘制圆形时显示圆心点
        // 绘制圆心点以便提供明确的几何定位反馈
        gc.setFill(getEffectiveColor());
        double pointRadius = hover ? 4 : 3;
        gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);

        // 使用LabelRenderer绘制圆心名称
        if (centerName != null && !centerName.isEmpty()) {
            LabelRenderer.renderLabel(gc, centerName, sx, sy);
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double d = Math.hypot(x - cx, y - cy);
        return Math.abs(d - r) <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
        // 圆本身暂时不响应点击
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 圆心可拖动
        return List.of(
                new DraggablePoint(cx, cy, (newX, newY) -> {
                    cx = newX;
                    cy = newY;
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        // 旋转圆心
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = cx - centerX;
        double dy = cy - centerY;
        cx = centerX + dx * cos - dy * sin;
        cy = centerY + dx * sin + dy * cos;
    }

    @Override
    public double[] getBoundingBox() {
        // 圆的边界框
        return new double[]{cx - r, cx + r, cy - r, cy + r};
    }
}