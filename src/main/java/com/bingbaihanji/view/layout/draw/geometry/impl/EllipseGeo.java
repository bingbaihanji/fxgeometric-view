package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.*;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 椭圆几何图形
 * <p>
 * 由两个焦点 F1、F2 以及椭圆上一点 P 定义：
 * 2a = |PF1| + |PF2|，a 为半长轴，b = sqrt(a² - c²) 为半短轴。
 * 绘制为旋转椭圆（屏幕坐标下的多边形近似）。
 * <p>
 * 参考 GeoGebra 的 Ellipse 绘制逻辑：
 * 依次点击两个焦点，然后根据鼠标移动距离确定椭圆上一点。
 *
 * @author bingbaihanji
 */
public class EllipseGeo extends AbstractWorldObject {

    /** 焦点1（支持 PointGeo 引用复用） */
    private final ReusableCoordinate focus1;
    /** 焦点2（支持 PointGeo 引用复用） */
    private final ReusableCoordinate focus2;
    /** 2a = 椭圆上任意点到两焦点的距离之和（常量） */
    private double twoA;

    /** 椭圆绘制的近似线段数 */
    private static final int ELLIPSE_SEGMENTS = 128;

    /**
     * 基础构造函数（坐标方式）
     *
     * @param f1x  焦点1 X
     * @param f1y  焦点1 Y
     * @param f2x  焦点2 X
     * @param f2y  焦点2 Y
     * @param twoA 2a（椭圆上一点到两焦点距离之和）
     */
    public EllipseGeo(double f1x, double f1y, double f2x, double f2y, double twoA) {
        super(ObjectType.ELLIPSE);
        this.focus1 = new ReusableCoordinate(f1x, f1y, "F₁");
        this.focus2 = new ReusableCoordinate(f2x, f2y, "F₂");
        this.twoA = twoA;
        this.color = StyleManager.defaultLineColor;
    }

    /**
     * 点引用方式构造函数 — 复用已有点作为焦点
     */
    public EllipseGeo(PointGeo f1Ref, double f1x, double f1y,
                      PointGeo f2Ref, double f2x, double f2y, double twoA) {
        super(ObjectType.ELLIPSE);
        this.focus1 = new ReusableCoordinate(f1Ref, f1x, f1y);
        if (f1Ref == null) {
            this.focus1.setName("F₁");
        }
        this.focus2 = new ReusableCoordinate(f2Ref, f2x, f2y);
        if (f2Ref == null) {
            this.focus2.setName("F₂");
        }
        this.twoA = twoA;
        this.color = StyleManager.defaultLineColor;
    }

    // ---- 访问器 ----

    public double getF1x() { return focus1.getX(); }
    public double getF1y() { return focus1.getY(); }
    public double getF2x() { return focus2.getX(); }
    public double getF2y() { return focus2.getY(); }
    public double getTwoA() { return twoA; }
    public void setTwoA(double twoA) { this.twoA = twoA; }

    /** 半长轴 a */
    public double getA() { return twoA / 2.0; }
    /** 焦距一半 c */
    public double getC() { return Math.hypot(getF2x() - getF1x(), getF2y() - getF1y()) / 2.0; }
    /** 半短轴 b */
    public double getB() {
        double a = getA();
        double c = getC();
        double b2 = a * a - c * c;
        return b2 > 0 ? Math.sqrt(b2) : 0;
    }
    /** 椭圆中心 X */
    public double getCx() { return (getF1x() + getF2x()) / 2.0; }
    /** 椭圆中心 Y */
    public double getCy() { return (getF1y() + getF2y()) / 2.0; }
    /** 椭圆旋转角（弧度，焦点连线方向） */
    public double getRotationAngle() { return Math.atan2(getF2y() - getF1y(), getF2x() - getF1x()); }

    public PointGeo getF1Ref() { return focus1.getRef(); }
    public PointGeo getF2Ref() { return focus2.getRef(); }

    // ---- 绘制 ----

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (!visible) return;

        double a = getA();
        double b = getB();
        if (a <= 0 || b <= 0) return;

        double cx = getCx();
        double cy = getCy();
        double cos = Math.cos(getRotationAngle());
        double sin = Math.sin(getRotationAngle());

        // 生成屏幕坐标点
        int n = ELLIPSE_SEGMENTS;
        double[] sx = new double[n];
        double[] sy = new double[n];
        for (int i = 0; i < n; i++) {
            double t = 2 * Math.PI * i / n;
            double xt = a * Math.cos(t);
            double yt = b * Math.sin(t);
            double wx = cx + xt * cos - yt * sin;
            double wy = cy + xt * sin + yt * cos;
            sx[i] = transform.worldToScreenX(wx);
            sy[i] = transform.worldToScreenY(wy);
        }

        FillRenderer.fillPolygon(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, sx, sy, n);

        // 发光通道（稍宽、半透明、实线）
        if (StyleManager.GLOW_ENABLED) {
        gc.save();
        LineStyleUtil.resetLineStyle(gc);
        gc.setGlobalAlpha(StyleManager.GLOW_ALPHA);
        gc.setLineWidth(getEffectiveLineWidth() + StyleManager.GLOW_WIDTH_BONUS);
        gc.setStroke(getEffectiveColor());
        gc.strokePolygon(sx, sy, n);
        gc.restore();
        }

        // 主描边
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokePolygon(sx, sy, n);
        LineStyleUtil.resetLineStyle(gc);

        // 绘制焦点
        drawFocus(gc, transform, focus1);
        drawFocus(gc, transform, focus2);
    }

    /** 绘制单个焦点（小圆点 + 标签） */
    private void drawFocus(GraphicsContext gc, WorldTransform transform, ReusableCoordinate focus) {
        if (!focus.isInternal()) return; // 引用外部点时由外部点自行绘制
        double sx = transform.worldToScreenX(focus.getX());
        double sy = transform.worldToScreenY(focus.getY());
        double r = hover ? 4 : 3;
        gc.setFill(getEffectiveColor());
        gc.fillOval(sx - r, sy - r, r * 2, r * 2);
        if (focus.getName() != null && !focus.getName().isEmpty()) {
            LabelRenderer.renderLabel(gc, focus.getName(), sx, sy);
        }
    }

    // ---- 碰撞检测 ----

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double d1 = Math.hypot(x - getF1x(), y - getF1y());
        double d2 = Math.hypot(x - getF2x(), y - getF2y());
        return Math.abs(d1 + d2 - twoA) <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
    }

    // ---- 拖拽 ----

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        List<DraggablePoint> points = new ArrayList<>();
        points.add(new DraggablePoint(getF1x(), getF1y(), (newX, newY) -> {
            focus1.updatePosition(newX, newY);
        }));
        points.add(new DraggablePoint(getF2x(), getF2y(), (newX, newY) -> {
            focus2.updatePosition(newX, newY);
        }));
        return points;
    }

    // ---- 旋转 ----

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        focus1.rotateAround(centerX, centerY, cos, sin);
        focus2.rotateAround(centerX, centerY, cos, sin);
    }

    // ---- 包围盒 ----

    @Override
    public double[] getBoundingBox() {
        double cx = getCx();
        double cy = getCy();
        double a = getA();
        double b = getB();
        double cos = Math.cos(getRotationAngle());
        double sin = Math.sin(getRotationAngle());
        double hw = Math.sqrt(a * a * cos * cos + b * b * sin * sin); // 半宽
        double hh = Math.sqrt(a * a * sin * sin + b * b * cos * cos); // 半高
        return new double[]{cx - hw, cx + hw, cy - hh, cy + hh};
    }

    // ---- 访问者 ----

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitEllipse(this);
    }
}
