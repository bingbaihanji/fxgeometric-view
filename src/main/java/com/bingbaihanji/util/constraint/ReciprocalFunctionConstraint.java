package com.bingbaihanji.util.constraint;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.ReciprocalFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 反比例函数约束
 * <p>
 * 约束点在反比例函数曲线上移动
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ReciprocalFunctionConstraint implements PointConstraint {

    private final ReciprocalFunctionGeo function;
    private double parameter; // 参数x

    public ReciprocalFunctionConstraint(ReciprocalFunctionGeo function) {
        this.function = function;
        this.parameter = 1.0; // 默认参数避开x=0
    }

    @Override
    public Point2D getPointFromParameter() {
        // 确保参数不在断点附近
        if (MathCalculationUtils.isZero(parameter, GeometryConfig.Mathematics.TINY_VALUE)) {
            parameter = parameter > 0 ? GeometryConfig.Mathematics.TINY_VALUE : -GeometryConfig.Mathematics.TINY_VALUE;
        }

        double y = function.evaluate(parameter);
        return new Point2D(parameter, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 对于显函数,参数就是x坐标
        // 但需要避开x=0附近
        if (MathCalculationUtils.isZero(x, GeometryConfig.Mathematics.TINY_VALUE)) {
            x = x > 0 ? GeometryConfig.Mathematics.TINY_VALUE : -GeometryConfig.Mathematics.TINY_VALUE;
        }

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
        // 确保参数不在断点附近
        if (MathCalculationUtils.isZero(parameter, GeometryConfig.Mathematics.TINY_VALUE)) {
            this.parameter = parameter > 0 ? GeometryConfig.Mathematics.TINY_VALUE : -GeometryConfig.Mathematics.TINY_VALUE;
        } else {
            this.parameter = parameter;
        }
    }

    @Override
    public WorldObject getConstrainedShape() {
        return function;
    }

    @Override
    public String getConstraintType() {
        return "ReciprocalFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        // 避开断点
        if (MathCalculationUtils.isZero(x, GeometryConfig.Mathematics.TINY_VALUE)) {
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
