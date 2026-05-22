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

    private double r;

    // 圆心引用(如果复用已有点)
    private PointGeo centerPointRef;

    // 内部坐标(当没有引用时使用)
    private double cx;
    private double cy;

    private String centerName; // 圆心名称

    // 标记圆心是否是内部创建的(需要由圆绘制)
    private boolean centerIsInternal = true;

    /**
     * 基础构造函数(坐标方式)
     */
    public CircleGeo(double cx, double cy, double r) {
        super(ObjectType.CIRCLE);
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.color = StyleManager.GEOMETRY_LINE;
        this.centerName = PointNameManager.getInstance().assignCenterName(cx, cy);
    }

    /**
     * 构造函数(坐标方式,可选自动命名)
     */
    public CircleGeo(double cx, double cy, double r, boolean autoNameCenter) {
        super(ObjectType.CIRCLE);
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.color = StyleManager.GEOMETRY_LINE;
        if (autoNameCenter) {
            this.centerName = PointNameManager.getInstance().assignCenterName(cx, cy);
        }
    }

    /**
     * 构造函数(点引用方式)- 复用已有点作为圆心
     *
     * @param centerPoint 圆心点引用(可为null,表示内部创建)
     * @param cx          圆心X坐标
     * @param cy          圆心Y坐标
     * @param r           半径
     */
    public CircleGeo(PointGeo centerPoint, double cx, double cy, double r) {
        super(ObjectType.CIRCLE);
        this.centerPointRef = centerPoint;
        this.cx = cx;
        this.cy = cy;
        this.r = r;
        this.color = StyleManager.GEOMETRY_LINE;

        if (centerPoint != null) {
            this.centerName = centerPoint.getName();
            this.centerIsInternal = false; // 复用外部点,不由圆绘制
        } else {
            this.centerName = PointNameManager.getInstance().assignCenterName(cx, cy);
            this.centerIsInternal = true;
        }
    }

    // Getter methods
    public double getCx() {
        return centerPointRef != null ? centerPointRef.getX() : cx;
    }

    public double getCy() {
        return centerPointRef != null ? centerPointRef.getY() : cy;
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

    public PointGeo getCenterPointRef() {
        return centerPointRef;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double w,
                      double h) {

        if (!visible) return;
        double sx = transform.worldToScreenX(getCx());
        double sy = transform.worldToScreenY(getCy());
        double sr = r * transform.getScale();

        // 先绘制填充
        FillRenderer.fillOval(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, sx, sy, sr);

        // 再绘制边框
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

        // 只绘制内部创建的圆心,复用的外部点由它们自己绘制
        if (centerIsInternal) {
            gc.setFill(getEffectiveColor());
            double pointRadius = hover ? 4 : 3;
            gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);

            // 使用LabelRenderer绘制圆心名称
            if (centerName != null && !centerName.isEmpty()) {
                LabelRenderer.renderLabel(gc, centerName, sx, sy);
            }
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double d = Math.hypot(x - getCx(), y - getCy());
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
                new DraggablePoint(getCx(), getCy(), (newX, newY) -> {
                    if (centerPointRef != null) {
                        // 复用的外部点,更新其位置
                        centerPointRef.updatePosition(newX, newY);
                    } else {
                        // 内部坐标,更新坐标并同步复用组
                        double oldX = cx;
                        double oldY = cy;
                        cx = newX;
                        cy = newY;
                        // 更新名称映射
                        if (centerName != null && !centerName.isEmpty()) {
                            PointNameManager.getInstance().updatePosition(oldX, oldY, cx, cy);
                        }
                    }
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        // 旋转圆心
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        if (centerPointRef != null && !centerPointRef.isConstrained()) {
            double dx = centerPointRef.getX() - centerX;
            double dy = centerPointRef.getY() - centerY;
            centerPointRef.updatePosition(
                    centerX + dx * cos - dy * sin,
                    centerY + dx * sin + dy * cos
            );
        } else if (centerPointRef == null) {
            double dx = cx - centerX;
            double dy = cy - centerY;
            cx = centerX + dx * cos - dy * sin;
            cy = centerY + dx * sin + dy * cos;
        }
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