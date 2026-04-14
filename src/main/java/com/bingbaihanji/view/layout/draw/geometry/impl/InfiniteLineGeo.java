package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * 无限直线几何图形
 * <p>
 * 通过两个点定义一条无限直线,直线会延伸至屏幕边界
 * 支持点复用：如果定义点位置已有PointGeo,直接引用而不是创建新点
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class InfiniteLineGeo extends AbstractWorldObject {

    // 定义点引用(如果复用已有点)
    private PointGeo point1Ref;
    private PointGeo point2Ref;

    // 内部坐标(当没有引用时使用)
    private double point1X;
    private double point1Y;
    private double point2X;
    private double point2Y;

    private String point1Name; // 定义点1名称
    private String point2Name; // 定义点2名称

    // 标记定义点是否是内部创建的
    private boolean point1IsInternal = true;
    private boolean point2IsInternal = true;

    /**
     * 基础构造函数(坐标方式)
     */
    public InfiniteLineGeo(double point1X, double point1Y, double point2X, double point2Y) {
        super(ObjectType.LINE);
        this.point1X = point1X;
        this.point1Y = point1Y;
        this.point2X = point2X;
        this.point2Y = point2Y;
        this.color = StyleManager.GEOMETRY_LINE;
        PointNameManager manager = PointNameManager.getInstance();
        this.point1Name = manager.assignName(point1X, point1Y);
        this.point2Name = manager.assignName(point2X, point2Y);
    }

    /**
     * 构造函数(点引用方式)- 复用已有点
     */
    public InfiniteLineGeo(PointGeo point1, double point1X, double point1Y,
                           PointGeo point2, double point2X, double point2Y) {
        super(ObjectType.LINE);
        this.point1Ref = point1;
        this.point2Ref = point2;
        this.point1X = point1X;
        this.point1Y = point1Y;
        this.point2X = point2X;
        this.point2Y = point2Y;
        this.color = StyleManager.GEOMETRY_LINE;

        PointNameManager manager = PointNameManager.getInstance();
        if (point1 != null) {
            this.point1Name = point1.getName();
            this.point1IsInternal = false;
        } else {
            this.point1Name = manager.assignName(point1X, point1Y);
            this.point1IsInternal = true;
        }

        if (point2 != null) {
            this.point2Name = point2.getName();
            this.point2IsInternal = false;
        } else {
            this.point2Name = manager.assignName(point2X, point2Y);
            this.point2IsInternal = true;
        }
    }

    public double getPoint1X() {
        return point1Ref != null ? point1Ref.getX() : point1X;
    }

    public double getPoint1Y() {
        return point1Ref != null ? point1Ref.getY() : point1Y;
    }

    public double getPoint2X() {
        return point2Ref != null ? point2Ref.getX() : point2X;
    }

    public double getPoint2Y() {
        return point2Ref != null ? point2Ref.getY() : point2Y;
    }

    public String getPoint1Name() {
        return point1Name;
    }

    public String getPoint2Name() {
        return point2Name;
    }

    public PointGeo getPoint1Ref() {
        return point1Ref;
    }

    public PointGeo getPoint2Ref() {
        return point2Ref;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        double sx1 = transform.worldToScreenX(getPoint1X());
        double sy1 = transform.worldToScreenY(getPoint1Y());
        double sx2 = transform.worldToScreenX(getPoint2X());
        double sy2 = transform.worldToScreenY(getPoint2Y());

        // 计算直线与屏幕边界的交点
        double[] endpoints = calculateLineScreenIntersection(sx1, sy1, sx2, sy2, w, h);

        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokeLine(endpoints[0], endpoints[1], endpoints[2], endpoints[3]);
        LineStyleUtil.resetLineStyle(gc);

        // 只绘制内部创建的定义点
        gc.setFill(getEffectiveColor());
        double pointRadius = hover ? 5 : 4;

        if (point1IsInternal) {
            gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
            if (point1Name != null && !point1Name.isEmpty()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(point1Name, sx1 + 8, sy1 - 8);
                gc.setFill(getEffectiveColor());
            }
        }

        if (point2IsInternal) {
            gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
            if (point2Name != null && !point2Name.isEmpty()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(point2Name, sx2 + 8, sy2 - 8);
            }
        }
    }

    /**
     * 计算直线与屏幕边界的交点,使直线延伸至屏幕边界
     */
    private double[] calculateLineScreenIntersection(double sx1, double sy1, double sx2, double sy2, double w, double h) {
        double dx = sx2 - sx1;
        double dy = sy2 - sy1;

        // 如果两点重合,返回原点
        if (Math.abs(dx) < 1e-10 && Math.abs(dy) < 1e-10) {
            return new double[]{sx1, sy1, sx1, sy1};
        }

        // 扩展倍数(足够覆盖整个屏幕)
        double scale = Math.max(w, h) * 2;

        // 计算扩展后的端点
        double p1x, p1y, p2x, p2y;

        if (Math.abs(dx) < 1e-10) {
            // 垂直线
            p1x = sx1;
            p1y = -scale;
            p2x = sx1;
            p2y = scale;
        } else if (Math.abs(dy) < 1e-10) {
            // 水平线
            p1x = -scale;
            p1y = sy1;
            p2x = scale;
            p2y = sy1;
        } else {
            // 一般情况：使用参数化方程
            double t = scale / Math.hypot(dx, dy);
            p1x = sx1 - t * dx;
            p1y = sy1 - t * dy;
            p2x = sx1 + t * dx;
            p2y = sy1 + t * dy;
        }

        return new double[]{p1x, p1y, p2x, p2y};
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
        // 直线本身暂时不响应点击
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        return List.of(
                new DraggablePoint(getPoint1X(), getPoint1Y(), (newX, newY) -> {
                    if (point1Ref != null) {
                        point1Ref.updatePosition(newX, newY);
                    } else {
                        point1X = newX;
                        point1Y = newY;
                    }
                }),
                new DraggablePoint(getPoint2X(), getPoint2Y(), (newX, newY) -> {
                    if (point2Ref != null) {
                        point2Ref.updatePosition(newX, newY);
                    } else {
                        point2X = newX;
                        point2Y = newY;
                    }
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        if (point1Ref != null && !point1Ref.isConstrained()) {
            double dx1 = point1Ref.getX() - centerX;
            double dy1 = point1Ref.getY() - centerY;
            point1Ref.updatePosition(
                    centerX + dx1 * cos - dy1 * sin,
                    centerY + dx1 * sin + dy1 * cos
            );
        } else if (point1Ref == null) {
            double dx1 = point1X - centerX;
            double dy1 = point1Y - centerY;
            point1X = centerX + dx1 * cos - dy1 * sin;
            point1Y = centerY + dx1 * sin + dy1 * cos;
        }

        if (point2Ref != null && !point2Ref.isConstrained()) {
            double dx2 = point2Ref.getX() - centerX;
            double dy2 = point2Ref.getY() - centerY;
            point2Ref.updatePosition(
                    centerX + dx2 * cos - dy2 * sin,
                    centerY + dx2 * sin + dy2 * cos
            );
        } else if (point2Ref == null) {
            double dx2 = point2X - centerX;
            double dy2 = point2Y - centerY;
            point2X = centerX + dx2 * cos - dy2 * sin;
            point2Y = centerY + dx2 * sin + dy2 * cos;
        }
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitInfiniteLine(this);
    }
}
