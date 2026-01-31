package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.LogarithmicFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 对数函数约束
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class LogarithmicFunctionConstraint implements PointConstraint {

    private final LogarithmicFunctionGeo function;
    private double parameter;

    public LogarithmicFunctionConstraint(LogarithmicFunctionGeo function) {
        this.function = function;
        this.parameter = 1.0; // 默认避开x=0
    }

    @Override
    public Point2D getPointFromParameter() {
        // 确保参数 > 0
        if (parameter <= 0) {
            parameter = 1e-6;
        }
        double y = function.evaluate(parameter);
        return new Point2D(parameter, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 对数函数定义域 x > 0
        if (x <= 0) {
            x = 1e-6;
        }
        return x;
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        // 确保参数 > 0
        this.parameter = Math.max(1e-6, parameter);
    }

    @Override
    public WorldObject getConstrainedShape() {
        return function;
    }

    @Override
    public String getConstraintType() {
        return "LogarithmicFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        if (x <= 0) {
            return Double.POSITIVE_INFINITY;
        }
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
