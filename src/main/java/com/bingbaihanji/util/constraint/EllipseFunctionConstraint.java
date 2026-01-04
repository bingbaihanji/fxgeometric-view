package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.EllipseFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 椭圆函数约束
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class EllipseFunctionConstraint implements PointConstraint {

    private final EllipseFunctionGeo ellipse;
    private double parameter; // 参数θ (0到2π)

    public EllipseFunctionConstraint(EllipseFunctionGeo ellipse) {
        this.ellipse = ellipse;
        this.parameter = 0.0;
    }

    @Override
    public Point2D getPointFromParameter() {
        return ellipse.evaluateParametric(parameter);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 计算从椭圆中心指向点的角度
        double dx = (x - ellipse.getCx()) / ellipse.getA();
        double dy = (y - ellipse.getCy()) / ellipse.getB();

        double angle = Math.atan2(dy, dx);
        return normalizeAngle(angle);
    }

    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
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
        return "EllipseFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        // 简化计算：找最近的参数角度
        double angle = calculateParameter(x, y);
        Point2D pointOnEllipse = ellipse.evaluateParametric(angle);

        return Math.hypot(x - pointOnEllipse.getX(), y - pointOnEllipse.getY());
    }
}
