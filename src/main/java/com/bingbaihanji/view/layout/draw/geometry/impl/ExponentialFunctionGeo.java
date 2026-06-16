package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.ExponentialFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import javafx.geometry.Point2D;

/**
 * 指数函数几何对象
 * <p>
 * 表示指数函数 y = a^x
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ExponentialFunctionGeo extends FunctionGeo {

    /**
     * 底数
     */
    private double a;

    /**
     * 构造函数
     *
     * @param a 底数(必须 > 0 且 ≠ 1)
     */
    public ExponentialFunctionGeo(double a) {
        super();
        if (a <= 0 || Math.abs(a - 1.0) < 1e-10) {
            throw new IllegalArgumentException("底数a必须大于0且不等于1");
        }
        this.a = a;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        sampledPoints.clear();

        double[] domain = applyDomainLimits(viewMinX, viewMaxX);
        double x1 = domain[0];
        double x2 = domain[1];

        int numSamples = calculateSampleCount(scale, x2 - x1);
        double dx = (x2 - x1) / numSamples;

        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;
            double y = evaluate(x);

            if (isDrawableFiniteY(y, viewMinY, viewMaxY)) {
                sampledPoints.add(new Point2D(x, y));
            }
        }
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new ExponentialFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        if (Math.abs(a - Math.E) < 0.01) {
            this.expression = "y = e^x";
        } else {
            this.expression = String.format("y = %.2f^x", a);
        }
        this.label = expression;
    }

    @Override
    public double evaluate(double x) {
        return Math.pow(a, x);
    }

    // Getter/Setter

    public double getA() {
        return a;
    }

    public void setA(double a) {
        if (a <= 0 || Math.abs(a - 1.0) < 1e-10) {
            throw new IllegalArgumentException("底数a必须大于0且不等于1");
        }
        this.a = a;
        onParameterChanged();
    }
}
