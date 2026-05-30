package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.EllipseGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Point2D;

/**
 * 参数化椭圆约束
 * <p>
 * 使用参数 t（0到2π）表示点在椭圆上的位置。
 * 当焦点移动时，约束点根据参数 t 重新计算位置。
 *
 * @author bingbaihanji
 */
public class EllipseConstraint implements PointConstraint {

    private final EllipseGeo ellipse;
    private double parameter; // 参数t, 角度(弧度), 范围[0, 2π]
    private boolean isVertexConstraint = false;
    private boolean isFocus1Constraint = false;
    private boolean isFocus2Constraint = false;

    public EllipseConstraint(EllipseGeo ellipse) {
        this(ellipse, 0.0);
    }

    public EllipseConstraint(EllipseGeo ellipse, double parameter) {
        this.ellipse = ellipse;
        this.parameter = normalizeAngle(parameter);
    }

    private double normalizeAngle(double angle) {
        angle = angle % (2 * Math.PI);
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }

    @Override
    public Point2D getPointFromParameter() {
        if (isFocus1Constraint) {
            return new Point2D(ellipse.getF1x(), ellipse.getF1y());
        }
        if (isFocus2Constraint) {
            return new Point2D(ellipse.getF2x(), ellipse.getF2y());
        }

        double a = ellipse.getA();
        double b = ellipse.getB();
        double cx = ellipse.getCx();
        double cy = ellipse.getCy();
        double cos = Math.cos(ellipse.getRotationAngle());
        double sin = Math.sin(ellipse.getRotationAngle());

        double xt = a * Math.cos(parameter);
        double yt = b * Math.sin(parameter);
        double x = cx + xt * cos - yt * sin;
        double y = cy + xt * sin + yt * cos;

        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        if (isFocus1Constraint || isFocus2Constraint) {
            return parameter;
        }

        // 将点转换到椭圆局部坐标
        double cx = ellipse.getCx();
        double cy = ellipse.getCy();
        double cos = Math.cos(ellipse.getRotationAngle());
        double sin = Math.sin(ellipse.getRotationAngle());
        double dx = x - cx;
        double dy = y - cy;
        double localX = dx * cos + dy * sin;
        double localY = -dx * sin + dy * cos;

        // 计算角度参数
        double a = ellipse.getA();
        double b = ellipse.getB();
        if (a > 0 && b > 0) {
            double t = Math.atan2(localY / b, localX / a);
            return normalizeAngle(t);
        }
        return 0;
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = normalizeAngle(parameter);
    }

    @Override
    public WorldObject getConstrainedShape() {
        return ellipse;
    }

    @Override
    public String getConstraintType() {
        return "EllipseConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double d1 = Math.hypot(x - ellipse.getF1x(), y - ellipse.getF1y());
        double d2 = Math.hypot(x - ellipse.getF2x(), y - ellipse.getF2y());
        return Math.abs(d1 + d2 - ellipse.getTwoA());
    }

    @Override
    public boolean isVertexConstraint() {
        return isVertexConstraint;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(PointGeo point) {
        final double EPSILON = 1e-6;

        PointGeo f1Ref = ellipse.getF1Ref();
        PointGeo f2Ref = ellipse.getF2Ref();

        if ((f1Ref != null && f1Ref == point) ||
                Math.hypot(point.getX() - ellipse.getF1x(), point.getY() - ellipse.getF1y()) < EPSILON) {
            isVertexConstraint = true;
            isFocus1Constraint = true;
            parameter = 0.0;
            return;
        }
        if ((f2Ref != null && f2Ref == point) ||
                Math.hypot(point.getX() - ellipse.getF2x(), point.getY() - ellipse.getF2y()) < EPSILON) {
            isVertexConstraint = true;
            isFocus2Constraint = true;
            parameter = 0.0;
            return;
        }

        isVertexConstraint = false;
        isFocus1Constraint = false;
        isFocus2Constraint = false;
    }
}
