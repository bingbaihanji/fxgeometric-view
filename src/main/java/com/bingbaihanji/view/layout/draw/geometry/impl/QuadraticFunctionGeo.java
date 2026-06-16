package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.util.constraint.QuadraticFunctionConstraint;
import javafx.geometry.Point2D;

/**
 * 二次函数几何对象
 * <p>
 * 表示二次函数 y = ax² + bx + c
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class QuadraticFunctionGeo extends FunctionGeo {

    /**
     * 二次项系数
     */
    private double a;

    /**
     * 一次项系数
     */
    private double b;

    /**
     * 常数项
     */
    private double c;

    /**
     * 构造函数
     *
     * @param a 二次项系数(不能为0)
     * @param b 一次项系数
     * @param c 常数项
     */
    public QuadraticFunctionGeo(double a, double b, double c) {
        super();
        if (Math.abs(a) < 1e-10) {
            throw new IllegalArgumentException("二次项系数a不能为0");
        }
        this.a = a;
        this.b = b;
        this.c = c;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        sampledPoints.clear();

        // 使用基类方法应用定义域限制
        double[] domain = applyDomainLimits(viewMinX, viewMaxX);
        double x1 = domain[0];
        double x2 = domain[1];

        // 使用基类方法计算采样点数
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
        return new QuadraticFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        StringBuilder sb = new StringBuilder("y = ");

        // 二次项
        if (Math.abs(a - 1.0) < 1e-10) {
            sb.append("x²");
        } else if (Math.abs(a + 1.0) < 1e-10) {
            sb.append("-x²");
        } else {
            sb.append(String.format("%.2fx²", a));
        }

        // 一次项
        if (Math.abs(b) > 1e-10) {
            if (b > 0) {
                if (Math.abs(b - 1.0) < 1e-10) {
                    sb.append(" + x");
                } else {
                    sb.append(String.format(" + %.2fx", b));
                }
            } else {
                if (Math.abs(b + 1.0) < 1e-10) {
                    sb.append(" - x");
                } else {
                    sb.append(String.format(" - %.2fx", -b));
                }
            }
        }

        // 常数项
        if (Math.abs(c) > 1e-10) {
            if (c > 0) {
                sb.append(String.format(" + %.2f", c));
            } else {
                sb.append(String.format(" - %.2f", -c));
            }
        }

        this.expression = sb.toString();
        this.label = expression;
    }

    @Override
    public double evaluate(double x) {
        return a * x * x + b * x + c;
    }

    // Getter/Setter

    public double getA() {
        return a;
    }

    public void setA(double a) {
        if (Math.abs(a) < 1e-10) {
            throw new IllegalArgumentException("二次项系数a不能为0");
        }
        this.a = a;
        onParameterChanged();
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
        onParameterChanged();
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
        onParameterChanged();
    }
}
