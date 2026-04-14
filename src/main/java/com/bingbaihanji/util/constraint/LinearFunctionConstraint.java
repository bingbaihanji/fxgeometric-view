package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.LinearFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 一次函数约束
 * <p>
 * 约束点在一次函数曲线上移动
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class LinearFunctionConstraint implements PointConstraint {

    private final LinearFunctionGeo function;
    private double parameter; // 参数x

    public LinearFunctionConstraint(LinearFunctionGeo function) {
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
        // 对于显函数,参数就是x坐标
        // 限制在定义域内
        double domainMin = function.getDomainMin();
        double domainMax = function.getDomainMax();

        if (Double.isFinite(domainMin) && x < domainMin) {
            return domainMin;
        }
        if (Double.isFinite(domainMax) && x > domainMax) {
            return domainMax;
        }

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
        return "LinearFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        // 计算点到直线的距离
        // 直线方程：kx - y + b = 0
        // 点到直线距离：|kx - y + b| / sqrt(k² + 1)
        double k = function.getK();
        double b = function.getB();

        return Math.abs(k * x - y + b) / Math.sqrt(k * k + 1);
    }

    @Override
    public boolean isVertexConstraint() {
        // 函数约束不支持顶点
        return false;
    }

    @Override
    public void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
        // 函数约束不支持顶点,不需要实现
    }
}
