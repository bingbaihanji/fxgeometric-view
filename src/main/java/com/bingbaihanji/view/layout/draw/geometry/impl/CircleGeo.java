package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.*;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * 圆几何图形
 * <p>
 * 支持圆心点复用：如果圆心位置已有PointGeo,直接引用而不是创建新点
 */
public class CircleGeo extends AbstractWorldObject {

    /**
     * 圆心坐标(支持 PointGeo 引用复用)
     */
    private final ReusableCoordinate center;
    private double r;

    /**
     * 基础构造函数(坐标方式)
     */
    public CircleGeo(double cx, double cy, double r) {
        super(ObjectType.CIRCLE);
        this.r = r;
        this.center = new ReusableCoordinate(cx, cy,
                PointNameManager.getInstance().assignCenterName(cx, cy));
        this.color = StyleManager.defaultLineColor;
    }

    /**
     * 构造函数(坐标方式,可选自动命名)
     */
    public CircleGeo(double cx, double cy, double r, boolean autoNameCenter) {
        super(ObjectType.CIRCLE);
        this.r = r;
        this.center = new ReusableCoordinate(cx, cy,
                autoNameCenter ? PointNameManager.getInstance().assignCenterName(cx, cy) : null);
        this.color = StyleManager.defaultLineColor;
    }

    /**
     * 构造函数(点引用方式)- 复用已有点作为圆心
     */
    public CircleGeo(PointGeo centerPoint, double cx, double cy, double r) {
        super(ObjectType.CIRCLE);
        this.r = r;
        this.center = new ReusableCoordinate(centerPoint, cx, cy);
        if (centerPoint == null) {
            this.center.setName(PointNameManager.getInstance().assignCenterName(cx, cy));
        }
        this.color = StyleManager.defaultLineColor;
    }

    public double getCx() {
        return center.getX();
    }

    public double getCy() {
        return center.getY();
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public String getCenterName() {
        return center.getName();
    }

    public void setCenterName(String centerName) {
        center.setName(centerName);
    }

    public PointGeo getCenterPointRef() {
        return center.getRef();
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (!visible) return;
        double sx = transform.worldToScreenX(getCx());
        double sy = transform.worldToScreenY(getCy());
        double sr = r * transform.getScale();

        FillRenderer.fillOval(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, sx, sy, sr);

        // 发光通道（稍宽、半透明、实线）
        if (StyleManager.GLOW_ENABLED) {
        gc.save();
        LineStyleUtil.resetLineStyle(gc);
        gc.setGlobalAlpha(StyleManager.GLOW_ALPHA);
        gc.setLineWidth(getEffectiveLineWidth() + StyleManager.GLOW_WIDTH_BONUS);
        gc.setStroke(getEffectiveColor());
        gc.strokeOval(sx - sr, sy - sr, sr * 2, sr * 2);
        gc.restore();
        }

        // 主描边
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokeOval(sx - sr, sy - sr, sr * 2, sr * 2);
        LineStyleUtil.resetLineStyle(gc);

        if (center.isInternal()) {
            gc.setFill(getEffectiveColor());
            double pointRadius = hover ? 4 : 3;
            gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);
            if (center.getName() != null && !center.getName().isEmpty()) {
                LabelRenderer.renderLabel(gc, center.getName(), sx, sy);
            }
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        return Math.abs(Math.hypot(x - getCx(), y - getCy()) - r) <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        return List.of(
                new DraggablePoint(getCx(), getCy(), (newX, newY) -> {
                    if (center.hasRef()) {
                        center.updatePosition(newX, newY);
                    } else {
                        double oldX = center.getX();
                        double oldY = center.getY();
                        center.updatePosition(newX, newY);
                        if (center.getName() != null && !center.getName().isEmpty()) {
                            PointNameManager.getInstance().updatePosition(oldX, oldY, getCx(), getCy());
                        }
                    }
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        center.rotateAround(centerX, centerY, cos, sin);
    }

    @Override
    public double[] getBoundingBox() {
        double centerX = getCx();
        double centerY = getCy();
        return new double[]{centerX - r, centerX + r, centerY - r, centerY + r};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitCircle(this);
    }
}