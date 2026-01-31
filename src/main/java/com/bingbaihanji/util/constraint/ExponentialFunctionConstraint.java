package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.ExponentialFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 指数函数约束
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ExponentialFunctionConstraint implements PointConstraint {

    private final ExponentialFunctionGeo function;
    private double parameter;

    public ExponentialFunctionConstraint(ExponentialFunctionGeo function) {
        this.function = function;
        this.parameter = 0.0;
    }

    @Override
    public Point2D getPointFromParameter() {
        double y = function.evaluate(parameter);
        return new Point2D(parameter, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        return x;
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = parameter;
    }

    @Override
    public WorldObject getConstrainedShape() {
        return function;
    }

    @Override
    public String getConstraintType() {
        return "ExponentialFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double yOnCurve = function.evaluate(x);
        return Math.abs(y - yOnCurve);
    }

    @Override
    public boolean isVertexConstraint() {
        return false;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
        // 函数约束不支持顶点
    }
}
