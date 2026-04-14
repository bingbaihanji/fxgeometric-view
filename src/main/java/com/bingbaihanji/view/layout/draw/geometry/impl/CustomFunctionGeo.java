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
        sampledPoints.clear();
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

        for (int i = 0; i <= numSamples; i++) {
            double x = x1 + i * dx;
            double y = evaluate(x);
            if (isYInViewRange(y, viewMinY, viewMaxY)) {
                sampledPoints.add(new Point2D(x, y));
            }
        }
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
