package com.bingbaihanji.model;

import com.bingbaihanji.constant.FunctionType;

import java.util.Map;

/**
 * 函数输入结果类
 * <p>
 * 封装用户在对话框中输入的函数信息
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class FunctionInputResult {
    /**
     * 函数类型
     */
    private final FunctionType type;

    /**
     * 参数映射(参数名 -> 参数值)
     */
    private final Map<String, Double> parameters;

    /**
     * 定义域最小值
     */
    private final double domainMin;

    /**
     * 定义域最大值
     */
    private final double domainMax;

    /**
     * 是否使用自动范围
     */
    private final boolean autoRange;

    /**
     * 自定义函数表达式(仅 CUSTOM 类型有效)
     */
    private final String customExpression;

    public FunctionInputResult(FunctionType type, Map<String, Double> parameters,
                               double domainMin, double domainMax, boolean autoRange) {
        this(type, parameters, domainMin, domainMax, autoRange, null);
    }

    public FunctionInputResult(FunctionType type, Map<String, Double> parameters,
                               double domainMin, double domainMax, boolean autoRange,
                               String customExpression) {
        this.type = type;
        this.parameters = parameters;
        this.domainMin = domainMin;
        this.domainMax = domainMax;
        this.autoRange = autoRange;
        this.customExpression = customExpression;
    }

    public FunctionType getType() {
        return type;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }

    public double getDomainMin() {
        return domainMin;
    }

    public double getDomainMax() {
        return domainMax;
    }

    public boolean isAutoRange() {
        return autoRange;
    }

    public Double getParameter(String name) {
        return parameters.get(name);
    }

    public String getCustomExpression() {
        return customExpression;
    }
}
