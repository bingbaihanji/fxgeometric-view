package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.FunctionType;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.util.constraint.TrigonometricFunctionConstraint;
import javafx.geometry.Point2D;

/**
 * 三角函数几何对象
 * <p>
 * 表示三角函数 y = A·f(ωx + φ) + k
 * 其中 f 可以是 sin, cos, tan
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class TrigonometricFunctionGeo extends FunctionGeo {

    /**
     * tan函数的断点容差
     */
    private static final double TAN_DISCONTINUITY_EPSILON = 1e-2;
    /**
     * 函数类型
     */
    private TrigType trigType;

    /**
     * 振幅
     */
    private double A;

    /**
     * 角频率
     */
    private double omega;

    /**
     * 初相
     */
    private double phi;

    /**
     * 垂直偏移
     */
    private double k;

    /**
     * 构造函数
     *
     * @param functionType 函数类型(SINE、COSINE、TANGENT)
     * @param A            振幅
     * @param omega        角频率
     * @param phi          初相
     * @param k            垂直偏移
     */
    public TrigonometricFunctionGeo(FunctionType functionType, double A, double omega, double phi, double k) {
        super();

        // 根据FunctionType设置TrigType
        this.trigType = switch (functionType) {
            case SINE -> TrigType.SINE;
            case COSINE -> TrigType.COSINE;
            case TANGENT -> TrigType.TANGENT;
            default -> throw new IllegalArgumentException("Invalid function type for trigonometric function");
        };

        this.A = A;
        this.omega = omega;
        this.phi = phi;
        this.k = k;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        clearSampleCache();

        // 计算周期
        double period = 2 * Math.PI / Math.abs(omega);

        // 智能采样：每个周期至少100个点
        double numPeriods = (viewMaxX - viewMinX) / period;
        int totalSamples = Math.max(MIN_SAMPLES * 2, (int) (numPeriods * 100));
        totalSamples = Math.min(totalSamples, MAX_SAMPLES);

        double[] domain = applyDomainLimits(viewMinX, viewMaxX);
        double x1 = domain[0];
        double x2 = domain[1];

        // 对于tan函数,需要分段处理
        if (trigType == TrigType.TANGENT) {
            sampleTangentFunction(x1, x2, totalSamples, viewMinY, viewMaxY);
        } else {
            sampleSineOrCosine(x1, x2, totalSamples, viewMinY, viewMaxY);
        }
    }

    /**
     * 采样sin或cos函数(连续函数)
     */
    private void sampleSineOrCosine(double x1, double x2, int numSamples,
                                    double viewMinY, double viewMaxY) {
        double dx = (x2 - x1) / numSamples;

        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;
            double y = evaluate(x);

            if (Double.isFinite(y)) {
                addSamplePoint(new Point2D(x, y));
            } else {
                startNewSampleSegment();
            }
        }
    }

    /**
     * 采样tan函数(有断点)
     */
    private void sampleTangentFunction(double x1, double x2, int numSamples,
                                       double viewMinY, double viewMaxY) {
        double dx = (x2 - x1) / numSamples;

        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;

            // 检查是否在渐近线附近
            if (isNearAsymptote(x)) {
                startNewSampleSegment();
                continue;
            }

            double y = evaluate(x);

            if (isDrawableFiniteY(y, viewMinY, viewMaxY)) {
                addSamplePoint(new Point2D(x, y));
            } else {
                startNewSampleSegment();
            }
        }
    }

    /**
     * 检查x是否在tan函数的渐近线附近
     */
    private boolean isNearAsymptote(double x) {
        // tan的渐近线在 ωx + φ = (2n+1)π/2
        double argument = omega * x + phi;
        double normalizedArg = argument % Math.PI;

        // 检查是否接近 π/2 或 -π/2
        double distToAsymptote = Math.abs(normalizedArg - Math.PI / 2);
        double distToNegAsymptote = Math.abs(normalizedArg + Math.PI / 2);

        return distToAsymptote < TAN_DISCONTINUITY_EPSILON ||
                distToNegAsymptote < TAN_DISCONTINUITY_EPSILON;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new TrigonometricFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        String funcName = switch (trigType) {
            case SINE -> "sin";
            case COSINE -> "cos";
            case TANGENT -> "tan";
        };

        StringBuilder sb = new StringBuilder("y = ");

        // 振幅
        if (Math.abs(A - 1.0) > 1e-10) {
            sb.append(String.format("%.2f·", A));
        } else if (Math.abs(A + 1.0) < 1e-10) {
            sb.append("-");
        }

        // 函数名
        sb.append(funcName).append("(");

        // 角频率
        if (Math.abs(omega - 1.0) > 1e-10) {
            sb.append(String.format("%.2f", omega));
        }
        sb.append("x");

        // 初相
        if (Math.abs(phi) > 1e-10) {
            if (phi > 0) {
                sb.append(String.format(" + %.2f", phi));
            } else {
                sb.append(String.format(" - %.2f", -phi));
            }
        }

        sb.append(")");

        // 垂直偏移
        if (Math.abs(k) > 1e-10) {
            if (k > 0) {
                sb.append(String.format(" + %.2f", k));
            } else {
                sb.append(String.format(" - %.2f", -k));
            }
        }

        this.expression = sb.toString();
        this.label = expression;
    }

    @Override
    public double evaluate(double x) {
        double argument = omega * x + phi;

        return switch (trigType) {
            case SINE -> A * Math.sin(argument) + k;
            case COSINE -> A * Math.cos(argument) + k;
            case TANGENT -> {
                if (isNearAsymptote(x)) {
                    yield Double.NaN;
                }
                yield A * Math.tan(argument) + k;
            }
        };
    }

    public TrigType getTrigType() {
        return trigType;
    }

    // Getter/Setter

    public double getA() {
        return A;
    }

    public void setA(double a) {
        this.A = a;
        onParameterChanged();
    }

    public double getOmega() {
        return omega;
    }

    public void setOmega(double omega) {
        this.omega = omega;
        onParameterChanged();
    }

    public double getPhi() {
        return phi;
    }

    public void setPhi(double phi) {
        this.phi = phi;
        onParameterChanged();
    }

    public double getK() {
        return k;
    }

    public void setK(double k) {
        this.k = k;
        onParameterChanged();
    }

    /**
     * 三角函数类型
     */
    public enum TrigType {
        SINE, COSINE, TANGENT
    }
}
