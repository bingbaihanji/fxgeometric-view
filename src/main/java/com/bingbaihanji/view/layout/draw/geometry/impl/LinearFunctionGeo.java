package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.LinearFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import javafx.geometry.Point2D;

/**
 * 一次函数几何对象
 * <p>
 * 表示一次函数 y = kx + b
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class LinearFunctionGeo extends FunctionGeo {

    /**
     * 斜率
     */
    private double k;

    /**
     * 截距
     */
    private double b;

    /**
     * 构造函数
     *
     * @param k 斜率
     * @param b 截距
     */
    public LinearFunctionGeo(double k, double b) {
        super();
        this.k = k;
        this.b = b;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        sampledPoints.clear();

        // 一次函数只需要两个点(视图边界)
        double x1 = Double.isFinite(domainMin) ? Math.max(viewMinX, domainMin) : viewMinX;
        double x2 = Double.isFinite(domainMax) ? Math.min(viewMaxX, domainMax) : viewMaxX;

        double y1 = evaluate(x1);
        double y2 = evaluate(x2);

        sampledPoints.add(new Point2D(x1, y1));
        sampledPoints.add(new Point2D(x2, y2));
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new LinearFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        StringBuilder sb = new StringBuilder("y = ");

        // 斜率部分
        if (Math.abs(k - 1.0) < 1e-10) {
            sb.append("x");
        } else if (Math.abs(k + 1.0) < 1e-10) {
            sb.append("-x");
        } else {
            sb.append(String.format("%.2fx", k));
        }

        // 截距部分
        if (Math.abs(b) > 1e-10) {
            if (b > 0) {
                sb.append(String.format(" + %.2f", b));
            } else {
                sb.append(String.format(" - %.2f", -b));
            }
        }

        this.expression = sb.toString();
        this.label = expression;
    }

    @Override
    public double evaluate(double x) {
        return k * x + b;
    }

    // Getter/Setter

    public double getK() {
        return k;
    }

    public void setK(double k) {
        this.k = k;
        onParameterChanged();
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
        onParameterChanged();
    }
}
