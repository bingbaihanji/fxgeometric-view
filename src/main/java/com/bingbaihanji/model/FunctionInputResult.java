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
 * @param type
函数类型
 * @param parameters
参数映射(参数名 -> 参数值)
 * @param domainMin
定义域最小值
 * @param domainMax
定义域最大值
 * @param autoRange
是否使用自动范围
 * @param customExpression
自定义函数表达式(仅 CUSTOM 类型有效)
 */
public record FunctionInputResult(FunctionType type, Map<String, Double> parameters,
                                  double domainMin, double domainMax,
                                  boolean autoRange, String customExpression) {


    public FunctionInputResult(FunctionType type, Map<String, Double> parameters,
                               double domainMin, double domainMax, boolean autoRange) {
        this(type, parameters, domainMin, domainMax, autoRange, null);
    }

    public Double getParameter(String name) {
        return parameters.get(name);
    }
}
