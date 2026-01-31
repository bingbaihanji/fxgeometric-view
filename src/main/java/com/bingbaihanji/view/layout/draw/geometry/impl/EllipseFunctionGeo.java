package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.EllipseFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import javafx.geometry.Point2D;

/**
 * 椭圆函数几何对象
 * <p>
 * 表示椭圆 (x-cx)²/a² + (y-cy)²/b² = 1
 * 使用参数方程：x = cx + a·cos(t), y = cy + b·sin(t)
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class EllipseFunctionGeo extends FunctionGeo {

    /**
     * 中心x坐标
     */
    private double cx;

    /**
     * 中心y坐标
     */
    private double cy;

    /**
     * 长半轴
     */
    private double a;

    /**
     * 短半轴
     */
    private double b;

    /**
     * 构造函数
     *
     * @param cx 中心x坐标
     * @param cy 中心y坐标
     * @param a  长半轴（必须 > 0）
     * @param b  短半轴（必须 > 0）
     */
    public EllipseFunctionGeo(double cx, double cy, double a, double b) {
        super();
        if (a <= 0 || b <= 0) {
            throw new IllegalArgumentException("长半轴和短半轴必须大于0");
        }
        this.cx = cx;
        this.cy = cy;
        this.a = a;
        this.b = b;
        updateExpression();
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        sampledPoints.clear();

        // 使用参数方程采样
        // 根据椭圆大小和缩放级别调整采样点数
        int numSamples = Math.max(MIN_SAMPLES, (int) (scale * Math.max(a, b) * 10));
        numSamples = Math.min(numSamples, MAX_SAMPLES);

        for (int i = 0; i <= numSamples; i++) {
            double t = 2 * Math.PI * i / numSamples;
            double x = cx + a * Math.cos(t);
            double y = cy + b * Math.sin(t);

            sampledPoints.add(new Point2D(x, y));
        }
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new EllipseFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        if (Math.abs(cx) < 1e-10 && Math.abs(cy) < 1e-10) {
            this.expression = String.format("x²/%.2f² + y²/%.2f² = 1", a, b);
        } else {
            this.expression = String.format("(x-%.2f)²/%.2f² + (y-%.2f)²/%.2f² = 1",
                    cx, a, cy, b);
        }
        this.label = expression;
    }

    /**
     * 根据参数t计算点坐标
     *
     * @param t 参数（0到2π）
     * @return 点坐标
     */
    public Point2D evaluateParametric(double t) {
        double x = cx + a * Math.cos(t);
        double y = cy + b * Math.sin(t);
        return new Point2D(x, y);
    }

    // Getter/Setter

    public double getCx() {
        return cx;
    }

    public void setCx(double cx) {
        this.cx = cx;
        onParameterChanged();
    }

    public double getCy() {
        return cy;
    }

    public void setCy(double cy) {
        this.cy = cy;
        onParameterChanged();
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        if (a <= 0) {
            throw new IllegalArgumentException("长半轴a必须大于0");
        }
        this.a = a;
        onParameterChanged();
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        if (b <= 0) {
            throw new IllegalArgumentException("短半轴b必须大于0");
        }
        this.b = b;
        onParameterChanged();
    }
}
