package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * 无限直线几何图形
 * <p>
 * 通过两个点定义一条无限直线,直线会延伸至屏幕边界
 * 支持点复用：如果定义点位置已有PointGeo,直接引用而不是创建新点
 */
public class InfiniteLineGeo extends AbstractWorldObject {

    private final ReusableCoordinate point1;
    private final ReusableCoordinate point2;

    /**
     * 基础构造函数(坐标方式)
     */
    public InfiniteLineGeo(double point1X, double point1Y, double point2X, double point2Y) {
        this(point1X, point1Y, point2X, point2Y, true);
    }

    /**
     * 构造函数(坐标方式,可选自动命名)
     */
    public InfiniteLineGeo(double point1X, double point1Y, double point2X, double point2Y, boolean autoName) {
        super(ObjectType.INFINITE_LINE);
        PointNameManager manager = PointNameManager.getInstance();
        this.point1 = new ReusableCoordinate(point1X, point1Y,
                autoName ? manager.assignName(point1X, point1Y) : null);
        this.point2 = new ReusableCoordinate(point2X, point2Y,
                autoName ? manager.assignName(point2X, point2Y) : null);
        this.color = StyleManager.defaultLineColor;
    }

    /**
     * 构造函数(点引用方式)- 复用已有点
     */
    public InfiniteLineGeo(PointGeo point1Ref, double point1X, double point1Y,
                           PointGeo point2Ref, double point2X, double point2Y) {
        super(ObjectType.INFINITE_LINE);
        PointNameManager manager = PointNameManager.getInstance();
        this.point1 = new ReusableCoordinate(point1Ref, point1X, point1Y);
        if (point1Ref == null) {
            this.point1.setName(manager.assignName(point1X, point1Y));
        }
        this.point2 = new ReusableCoordinate(point2Ref, point2X, point2Y);
        if (point2Ref == null) {
            this.point2.setName(manager.assignName(point2X, point2Y));
        }
        this.color = StyleManager.defaultLineColor;
    }

    public double getPoint1X() {
        return point1.getX();
    }

    public double getPoint1Y() {
        return point1.getY();
    }

    public double getPoint2X() {
        return point2.getX();
    }

    public double getPoint2Y() {
        return point2.getY();
    }

    public String getPoint1Name() {
        return point1.getName();
    }

    public void setPoint1Name(String name) {
        point1.setName(name);
    }

    public String getPoint2Name() {
        return point2.getName();
    }

    public void setPoint2Name(String name) {
        point2.setName(name);
    }

    public PointGeo getPoint1Ref() {
        return point1.getRef();
    }

    public PointGeo getPoint2Ref() {
        return point2.getRef();
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (!visible) return;
        double sx1 = transform.worldToScreenX(getPoint1X());
        double sy1 = transform.worldToScreenY(getPoint1Y());
        double sx2 = transform.worldToScreenX(getPoint2X());
        double sy2 = transform.worldToScreenY(getPoint2Y());

        double[] endpoints = calculateLineScreenIntersection(sx1, sy1, sx2, sy2, w, h);

        // 发光通道（稍宽、半透明、实线）
        if (StyleManager.GLOW_ENABLED) {
            gc.save();
            LineStyleUtil.resetLineStyle(gc);
            gc.setGlobalAlpha(StyleManager.GLOW_ALPHA);
            gc.setLineWidth(getEffectiveLineWidth() + StyleManager.GLOW_WIDTH_BONUS);
            gc.setStroke(getEffectiveColor());
            gc.strokeLine(endpoints[0], endpoints[1], endpoints[2], endpoints[3]);
            gc.restore();
        }

        // 主描边
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokeLine(endpoints[0], endpoints[1], endpoints[2], endpoints[3]);
        LineStyleUtil.resetLineStyle(gc);

        gc.setFill(getEffectiveColor());
        double pointRadius = hover ? 5 : 4;

        if (point1.isInternal()) {
            gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
            if (point1.getName() != null && !point1.getName().isEmpty()) {
                gc.setFill(GeometryConfig.Colors.LABEL_TEXT);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(point1.getName(), sx1 + 8, sy1 - 8);
                gc.setFill(getEffectiveColor());
            }
        }

        if (point2.isInternal()) {
            gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
            if (point2.getName() != null && !point2.getName().isEmpty()) {
                gc.setFill(GeometryConfig.Colors.LABEL_TEXT);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(point2.getName(), sx2 + 8, sy2 - 8);
                gc.setFill(getEffectiveColor());
            }
        }
    }

    private double[] calculateLineScreenIntersection(double sx1, double sy1, double sx2, double sy2, double w, double h) {
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;

        if (Math.abs(dx) < 1e-10 && Math.abs(dy) < 1e-10) {
            return new double[]{sx1, sy1, sx1, sy1};
        }

        double scale = Math.max(w, h) * 2;

        if (Math.abs(dx) < 1e-10) {
            return new double[]{sx1, -scale, sx1, scale};
        } else if (Math.abs(dy) < 1e-10) {
            return new double[]{-scale, sy1, scale, sy1};
        } else {
            double t = scale / Math.hypot(dx, dy);
            return new double[]{sx1 - t * dx, sy1 - t * dy, sx1 + t * dx, sy1 + t * dy};
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double p1X = getPoint1X();
        double p1Y = getPoint1Y();
        double p2X = getPoint2X();
        double p2Y = getPoint2Y();

        double dx = p2X - p1X;
        double dy = p2Y - p1Y;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return Math.hypot(x - p1X, y - p1Y) <= tolerance;
        }

        double distance = Math.abs(dy * x - dx * y + p2X * p1Y - p2Y * p1X) / length;
        return distance <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        return List.of(
                new DraggablePoint(getPoint1X(), getPoint1Y(), (newX, newY) -> point1.updatePosition(newX, newY)),
                new DraggablePoint(getPoint2X(), getPoint2Y(), (newX, newY) -> point2.updatePosition(newX, newY))
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        point1.rotateAround(centerX, centerY, cos, sin);
        point2.rotateAround(centerX, centerY, cos, sin);
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitInfiniteLine(this);
    }
}