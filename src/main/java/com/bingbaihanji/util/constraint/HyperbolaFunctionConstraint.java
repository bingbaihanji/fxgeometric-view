package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.HyperbolaFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 双曲线函数约束
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class HyperbolaFunctionConstraint implements PointConstraint {

    private final HyperbolaFunctionGeo hyperbola;
    private double parameter;

    public HyperbolaFunctionConstraint(HyperbolaFunctionGeo hyperbola) {
        this.hyperbola = hyperbola;
        this.parameter = 0.0;
    }

    @Override
    public Point2D getPointFromParameter() {
        // x = cx + a*cosh(t), y = cy + b*sinh(t) (右分支)
        double x = hyperbola.getCx() + hyperbola.getA() * Math.cosh(parameter);
        double y = hyperbola.getCy() + hyperbola.getB() * Math.sinh(parameter);
        return new Point2D(x, y);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 简化处理：返回当前参数
        return parameter;
    }

    @Override
    public double getParameter() {
        return parameter;
    }

    @Override
    public void setParameter(double parameter) {
        this.parameter = Math.max(-3.0, Math.min(3.0, parameter));
    }

    @Override
    public WorldObject getConstrainedShape() {
        return hyperbola;
    }

    @Override
    public String getConstraintType() {
        return "HyperbolaFunctionConstraint";
    }

    @Override
    public double distanceToShape(double x, double y) {
        Point2D pointOnCurve = getPointFromParameter();
        return Math.hypot(x - pointOnCurve.getX(), y - pointOnCurve.getY());
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
