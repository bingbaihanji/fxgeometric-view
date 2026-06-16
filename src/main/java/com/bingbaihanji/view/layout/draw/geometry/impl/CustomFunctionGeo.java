package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.util.constraint.CustomFunctionConstraint;
import com.bingbaihanji.util.constraint.PointConstraint;
import javafx.geometry.Point2D;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * 自定义表达式函数几何对象
 * <p>
 * 支持用户输入任意数学表达式,如 sin(x) + x^2、2*x^3 - 1 等
 *
 * @author bingbaihanji
 * @date 2026-04-13
 */
public class CustomFunctionGeo extends FunctionGeo {

    /**
     * 原始表达式字符串(用户输入)
     */
    private String expressionStr;

    /**
     * 已编译的 exp4j 表达式对象
     */
    private Expression compiledExpression;

    /**
     * 表达式是否有效
     */
    private boolean expressionValid;

    /**
     * 构造函数
     *
     * @param expressionStr 数学表达式字符串,使用 x 作为变量
     */
    public CustomFunctionGeo(String expressionStr) {
        super();
        compile(expressionStr);
        updateExpression();
    }

    /**
     * 校验表达式是否合法(静态工具方法,供对话框调用)
     *
     * @param expr 待校验的表达式
     * @return null 表示合法,否则返回错误提示
     */
    public static String validate(String expr) {
        if (expr == null || expr.isBlank()) {
            return "表达式不能为空";
        }
        try {
            Expression e = new ExpressionBuilder(expr).variable("x").build();
            e.setVariable("x", 1.0);
            e.evaluate();
            return null;
        } catch (Exception ex) {
            return "表达式语法错误: " + ex.getMessage();
        }
    }

    /**
     * 编译表达式
     */
    private void compile(String expr) {
        this.expressionStr = expr;
        try {
            this.compiledExpression = new ExpressionBuilder(expr)
                    .variable("x")
                    .build();
            this.expressionValid = true;
        } catch (Exception e) {
            this.compiledExpression = null;
            this.expressionValid = false;
        }
    }

    @Override
    public double evaluate(double x) {
        if (!expressionValid || compiledExpression == null) {
            return Double.NaN;
        }
        try {
            compiledExpression.setVariable("x", x);
            double result = compiledExpression.evaluate();
            return Double.isFinite(result) ? result : Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    @Override
    protected void samplePoints(double viewMinX, double viewMaxX,
                                double viewMinY, double viewMaxY,
                                double scale) {
        clearSampleCache();
        if (!expressionValid) {
            return;
        }

        double[] domain = applyDomainLimits(viewMinX, viewMaxX);
        double x1 = domain[0];
        double x2 = domain[1];
        if (x1 >= x2) {
            return;
        }

        int numSamples = calculateSampleCount(scale, x2 - x1);
        double dx = (x2 - x1) / numSamples;

        Point2D previousRawPoint = null;
        boolean previousVisible = false;
        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;
            double y = evaluate(x);
            if (!Double.isFinite(y)) {
                startNewSampleSegment();
                previousRawPoint = null;
                previousVisible = false;
                continue;
            }

            Point2D currentRawPoint = new Point2D(x, y);
            boolean currentVisible = isDrawableFiniteY(y, viewMinY, viewMaxY);

            if (previousRawPoint != null && hasDiscontinuityBetween(previousRawPoint, currentRawPoint)) {
                startNewSampleSegment();
                previousRawPoint = null;
                previousVisible = false;
            }

            if (currentVisible) {
                if (previousRawPoint != null && !previousVisible) {
                    Point2D entryPoint = interpolateViewBoundaryPoint(
                            previousRawPoint, currentRawPoint, viewMinY, viewMaxY);
                    if (entryPoint != null) {
                        addSamplePoint(entryPoint);
                    }
                }
                addSamplePoint(currentRawPoint);
            } else if (previousRawPoint != null && previousVisible) {
                Point2D exitPoint = interpolateViewBoundaryPoint(
                        previousRawPoint, currentRawPoint, viewMinY, viewMaxY);
                if (exitPoint != null) {
                    addSamplePoint(exitPoint);
                }
                startNewSampleSegment();
            }

            previousRawPoint = currentRawPoint;
            previousVisible = currentVisible;
        }
    }

    /**
     * 在两个相邻采样点之间估算曲线穿过视图垂直边界的位置。
     */
    private Point2D interpolateViewBoundaryPoint(Point2D p1, Point2D p2,
                                                 double viewMinY, double viewMaxY) {
        if (p1 == null || p2 == null) {
            return null;
        }

        double y1 = p1.getY();
        double y2 = p2.getY();
        if (!Double.isFinite(y1) || !Double.isFinite(y2) || Math.abs(y2 - y1) < 1e-12) {
            return null;
        }

        double boundaryY;
        if (y1 < viewMinY && y2 >= viewMinY) {
            boundaryY = viewMinY;
        } else if (y1 > viewMaxY && y2 <= viewMaxY) {
            boundaryY = viewMaxY;
        } else if (y1 >= viewMinY && y2 < viewMinY) {
            boundaryY = viewMinY;
        } else if (y1 <= viewMaxY && y2 > viewMaxY) {
            boundaryY = viewMaxY;
        } else {
            return null;
        }

        double t = (boundaryY - y1) / (y2 - y1);
        if (t < 0 || t > 1 || !Double.isFinite(t)) {
            return null;
        }

        double x = p1.getX() + t * (p2.getX() - p1.getX());
        return new Point2D(x, boundaryY);
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    protected void updateExpression() {
        this.expression = "y = " + expressionStr;
        this.label = expression;
    }

    @Override
    public PointConstraint createConstraint() {
        return new CustomFunctionConstraint(this);
    }

    public String getExpressionStr() {
        return expressionStr;
    }

    /**
     * 更新表达式字符串(不重新编译,用于运行时替换)
     */
    public void setExpressionStr(String expr) {
        compile(expr);
        onParameterChanged();
    }

    public boolean isExpressionValid() {
        return expressionValid;
    }
}
