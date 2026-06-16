package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.ParabolaConicFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import javafx.geometry.Point2D;

/**
 * 抛物线(圆锥曲线)几何对象
 * <p>
 * 表示抛物线 y² = 2px(标准形式)
 * 使用参数方程：x = pt²/2, y = pt
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ParabolaConicFunctionGeo extends FunctionGeo {

    /**
     * 焦参数(焦点到准线的距离的一半)
     */
    private double p;

    /**
     * 构造函数
     *
     * @param p 焦参数(必须 > 0)
     */
    public ParabolaConicFunctionGeo(double p) {
        super();
        if (p <= 0) {
            throw new IllegalArgumentException("焦参数p必须大于0");
        }
        this.p = p;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        sampledPoints.clear();

        // 使用参数方程：x = pt²/2, y = pt
        // 参数t的范围需要根据视图确定

        // 从y的范围推导t的范围
        double tMin = viewMinY / p;
        double tMax = viewMaxY / p;

        int numSamples = calculateSampleCount(scale, tMax - tMin);
        double dt = (tMax - tMin) / numSamples;

        for (int i = 0; i <= numSamples; i++) {
            double t = tMin + i * dt;
            double x = p * t * t / 2;
            double y = p * t;

            if (Double.isFinite(x) && isDrawableFiniteY(y, viewMinY, viewMaxY)) {
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
        return new ParabolaConicFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        this.expression = String.format("y² = %.2fx", 2 * p);
        this.label = expression;
    }

    /**
     * 根据参数t计算点坐标
     *
     * @param t 参数
     * @return 点坐标
     */
    public Point2D evaluateParametric(double t) {
        double x = p * t * t / 2;
        double y = p * t;
        return new Point2D(x, y);
    }

    // Getter/Setter

    public double getP() {
        return p;
    }

    public void setP(double p) {
        if (p <= 0) {
            throw new IllegalArgumentException("焦参数p必须大于0");
        }
        this.p = p;
        onParameterChanged();
    }
}
