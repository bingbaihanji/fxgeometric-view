package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.HyperbolaFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 双曲线函数几何对象
 * <p>
 * 表示双曲线 (x-cx)²/a² - (y-cy)²/b² = 1
 * 使用参数方程：x = cx + a·cosh(t), y = cy + b·sinh(t) 和对称分支
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class HyperbolaFunctionGeo extends FunctionGeo {

    private double cx;
    private double cy;
    private double a; // 实半轴
    private double b; // 虚半轴

    /**
     * 左分支采样点(用于分段绘制,避免两个分支连线)
     */
    private List<Point2D> leftBranchPoints = new ArrayList<>();

    /**
     * 右分支采样点
     */
    private List<Point2D> rightBranchPoints = new ArrayList<>();

    /**
     * 构造函数
     *
     * @param cx 中心x坐标
     * @param cy 中心y坐标
     * @param a  实半轴(必须 > 0)
     * @param b  虚半轴(必须 > 0)
     */
    public HyperbolaFunctionGeo(double cx, double cy, double a, double b) {
        super();
        if (a <= 0 || b <= 0) {
            throw new IllegalArgumentException("实半轴和虚半轴必须大于0");
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
        leftBranchPoints.clear();
        rightBranchPoints.clear();

        // 双曲线有两个分支
        // 使用参数方程：x = cx ± a·cosh(t), y = cy + b·sinh(t)

        int numSamples = Math.max(MIN_SAMPLES / 2, (int) (scale * Math.max(a, b) * 5));
        numSamples = Math.min(numSamples, MAX_SAMPLES / 2);

        // 确定参数范围(避免无穷远)
        double tMax = 3.0; // 这会给出较大但有限的值

        // 右分支：x = cx + a·cosh(t)
        for (int i = 0; i <= numSamples; i++) {
            double t = -tMax + 2 * tMax * i / numSamples;
            double x = cx + a * Math.cosh(t);
            double y = cy + b * Math.sinh(t);

            // 检查是否在视图范围内
            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
                rightBranchPoints.add(new Point2D(x, y));
            }
        }

        // 左分支：x = cx - a·cosh(t)
        for (int i = 0; i <= numSamples; i++) {
            double t = -tMax + 2 * tMax * i / numSamples;
            double x = cx - a * Math.cosh(t);
            double y = cy + b * Math.sinh(t);

            // 检查是否在视图范围内
            if (x >= viewMinX && x <= viewMaxX && y >= viewMinY && y <= viewMaxY) {
                leftBranchPoints.add(new Point2D(x, y));
            }
        }

        // 合并两个分支(保持顺序,先右分支再左分支)
        sampledPoints.addAll(rightBranchPoints);
        sampledPoints.addAll(leftBranchPoints);
    }

    @Override
    protected void drawCurve(GraphicsContext gc, WorldTransform transform) {
        // 分别绘制两个分支,避免连接两个分支
        drawBranch(gc, transform, rightBranchPoints);
        drawBranch(gc, transform, leftBranchPoints);
    }

    /**
     * 绘制单个分支
     */
    private void drawBranch(GraphicsContext gc, WorldTransform transform, List<Point2D> points) {
        if (points.isEmpty()) {
            return;
        }

        Point2D prevPoint = null;
        for (Point2D point : points) {
            if (prevPoint != null) {
                double sx1 = transform.worldToScreenX(prevPoint.getX());
                double sy1 = transform.worldToScreenY(prevPoint.getY());
                double sx2 = transform.worldToScreenX(point.getX());
                double sy2 = transform.worldToScreenY(point.getY());

                gc.strokeLine(sx1, sy1, sx2, sy2);
            }
            prevPoint = point;
        }
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new HyperbolaFunctionConstraint(this);
    }

    @Override
    protected void updateExpression() {
        if (Math.abs(cx) < 1e-10 && Math.abs(cy) < 1e-10) {
            this.expression = String.format("x²/%.2f² - y²/%.2f² = 1", a, b);
        } else {
            this.expression = String.format("(x-%.2f)²/%.2f² - (y-%.2f)²/%.2f² = 1",
                    cx, a, cy, b);
        }
        this.label = expression;
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
            throw new IllegalArgumentException("实半轴a必须大于0");
        }
        this.a = a;
        onParameterChanged();
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        if (b <= 0) {
            throw new IllegalArgumentException("虚半轴b必须大于0");
        }
        this.b = b;
        onParameterChanged();
    }
}
