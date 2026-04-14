package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.util.constraint.ReciprocalFunctionConstraint;
import javafx.geometry.Point2D;

/**
 * 反比例函数几何对象
 * <p>
 * 表示反比例函数 y = k/x
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ReciprocalFunctionGeo extends FunctionGeo {

    /**
     * 断点容差(x=0附近的排除范围)
     */
    private static final double DISCONTINUITY_EPSILON = 1e-3;
    /**
     * 系数k
     */
    private double k;

    /**
     * 构造函数
     *
     * @param k 系数(不能为0)
     */
    public ReciprocalFunctionGeo(double k) {
        super();
        if (Math.abs(k) < 1e-10) {
            throw new IllegalArgumentException("系数k不能为0");
        }
        this.k = k;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        sampledPoints.clear();

        // 应用定义域限制
        double[] domain = applyDomainLimits(viewMinX, viewMaxX);
        double x1 = domain[0];
        double x2 = domain[1];

        // 分段采样：x < -ε 和 x > ε 两段
        // 左分支：x < -ε
        if (x1 < -DISCONTINUITY_EPSILON) {
            double leftEnd = Math.min(-DISCONTINUITY_EPSILON, x2);
            sampleSegment(x1, leftEnd, scale, viewMinY, viewMaxY);
        }

        // 右分支：x > ε
        if (x2 > DISCONTINUITY_EPSILON) {
            double rightStart = Math.max(DISCONTINUITY_EPSILON, x1);
            sampleSegment(rightStart, x2, scale, viewMinY, viewMaxY);
        }
    }

    /**
     * 采样一个连续区间
     */
    private void sampleSegment(double x1, double x2, double scale,
                               double viewMinY, double viewMaxY) {
        int numSamples = calculateSampleCount(scale, Math.abs(x2 - x1));
        double dx = (x2 - x1) / numSamples;

        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;

            // 确保不在断点附近
            if (Math.abs(x) < DISCONTINUITY_EPSILON) {
                continue;
            }

            double y = evaluate(x);

            // 使用基类方法检查y值范围
            if (isYInViewRange(y, viewMinY, viewMaxY)) {
                sampledPoints.add(new Point2D(x, y));
            }
        }
    }

    @Override
    protected boolean hasDiscontinuity(double x) {
        // 在x=0附近有断点
        return Math.abs(x) < DISCONTINUITY_EPSILON;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new ReciprocalFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        if (Math.abs(k - 1.0) < 1e-10) {
            this.expression = "y = 1/x";
        } else if (Math.abs(k + 1.0) < 1e-10) {
            this.expression = "y = -1/x";
        } else {
            this.expression = String.format("y = %.2f/x", k);
        }
        this.label = expression;
    }

    @Override
    public double evaluate(double x) {
        if (Math.abs(x) < 1e-10) {
            return Double.NaN;
        }
        return k / x;
    }

    // Getter/Setter

    public double getK() {
        return k;
    }

    public void setK(double k) {
        if (Math.abs(k) < 1e-10) {
            throw new IllegalArgumentException("系数k不能为0");
        }
        this.k = k;
        onParameterChanged();
    }
}
