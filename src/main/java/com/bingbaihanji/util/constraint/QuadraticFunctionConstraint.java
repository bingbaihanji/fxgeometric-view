package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.QuadraticFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 二次函数约束
 * <p>
 * 约束点在二次函数曲线上移动
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class QuadraticFunctionConstraint implements PointConstraint {

    private final QuadraticFunctionGeo function;
    private double parameter; // 参数x

    public QuadraticFunctionConstraint(QuadraticFunctionGeo function) {
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
        // 对于显函数，参数就是x坐标
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
        return "QuadraticFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        // 计算点到抛物线的距离（近似方法）
        // 使用该x坐标上的函数值与给定y的差距
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
