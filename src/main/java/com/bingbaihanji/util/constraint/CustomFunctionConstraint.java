package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CustomFunctionGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Point2D;

/**
 * 自定义函数约束
 * <p>
 * 约束点在自定义表达式函数曲线上移动
 *
 * @author bingbaihanji
 * @date 2026-04-13
 */
public class CustomFunctionConstraint implements PointConstraint {

    private final CustomFunctionGeo function;
    /**
     * 参数即 x 坐标
     */
    private double parameter;

    public CustomFunctionConstraint(CustomFunctionGeo function) {
        this.function = function;
        this.parameter = 0.0;
    }

    @Override
    public Point2D getPointFromParameter() {
        double y = function.evaluate(parameter);
        if (!Double.isFinite(y)) {
            return new Point2D(parameter, 0.0);
        }
        return new Point2D(parameter, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        double domainMin = function.getDomainMin();
        double domainMax = function.getDomainMax();

        if (Double.isFinite(domainMin) && x < domainMin) return domainMin;
        if (Double.isFinite(domainMax) && x > domainMax) return domainMax;
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
        return "CustomFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        double fy = function.evaluate(x);
        if (!Double.isFinite(fy)) {
            return Double.MAX_VALUE;
        }
        return Math.abs(y - fy);
    }

    @Override
    public boolean isVertexConstraint() {
        return false;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(PointGeo point) {
        // 函数约束不支持顶点
    }
}
