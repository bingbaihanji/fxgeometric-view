package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.LogarithmicFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import javafx.geometry.Point2D;

/**
 * 对数函数几何对象
 * <p>
 * 表示对数函数 y = log_a(x)
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class LogarithmicFunctionGeo extends FunctionGeo {

    /**
     * 底数
     */
    private double a;

    /**
     * 构造函数
     *
     * @param a 底数(必须 > 0 且 ≠ 1)
     */
    public LogarithmicFunctionGeo(double a) {
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

        // 对数函数的定义域是 x > 0
        double x1 = Math.max(1e-6, viewMinX);
        double[] domain = applyDomainLimits(x1, viewMaxX);
        x1 = domain[0];
        double x2 = domain[1];

        // 确保起点在正区间
        if (x1 <= 0) {
            x1 = 1e-6;
        }
        if (x2 <= x1) {
            return; // 无效范围
        }

        int numSamples = calculateSampleCount(scale, x2 - x1);
        double dx = (x2 - x1) / numSamples;

        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;
            if (x <= 0) continue; // 跳过非正数

            double y = evaluate(x);

            if (Double.isFinite(y)) {
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
        return new LogarithmicFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        if (Math.abs(a - Math.E) < 0.01) {
            this.expression = "y = ln(x)";
        } else if (Math.abs(a - 10.0) < 0.01) {
            this.expression = "y = log(x)";
        } else {
            this.expression = String.format("y = log_%.2f(x)", a);
        }
        this.label = expression;
    }

    @Override
    public double evaluate(double x) {
        if (x <= 0) {
            return Double.NaN;
        }
        return Math.log(x) / Math.log(a);
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
