package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.ParabolaConicFunctionGeo;
import javafx.geometry.Point2D;

/**
 * 抛物线(圆锥曲线)函数约束
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ParabolaConicFunctionConstraint implements PointConstraint {

    private final ParabolaConicFunctionGeo parabola;
    private double parameter;

    public ParabolaConicFunctionConstraint(ParabolaConicFunctionGeo parabola) {
        this.parabola = parabola;
        this.parameter = 0.0;
    }

    @Override
    public Point2D getPointFromParameter() {
        return parabola.evaluateParametric(parameter);
    }

    @Override
    public double calculateParameter(double x, double y) {
        // 从 y = pt 求解 t
        return y / parabola.getP();
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
        return parabola;
    }

    @Override
    public String getConstraintType() {
        return "ParabolaConicFunctionConstraint";
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
