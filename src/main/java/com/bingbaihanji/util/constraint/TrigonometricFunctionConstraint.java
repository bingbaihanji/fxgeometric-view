package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.TrigonometricFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 三角函数约束
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class TrigonometricFunctionConstraint implements PointConstraint {

    private final TrigonometricFunctionGeo function;
    private double parameter;

    public TrigonometricFunctionConstraint(TrigonometricFunctionGeo function) {
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
        return "TrigonometricFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double yOnCurve = function.evaluate(x);
        if (!Double.isFinite(yOnCurve)) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.abs(y - yOnCurve);
    }
}
