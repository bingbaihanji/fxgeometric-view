package com.bingbaihanji.model;

/**
 * 函数参数描述类
 * <p>
 * 用于描述函数参数的元信息(名称、标签、默认值、说明)
 *
 * @param name         参数名称(用于代码中引用)
 * @param label        参数标签(显示给用户)
 * @param defaultValue 默认值
 * @param description  参数说明
 * @author bingbaihanji
 * @date 2026-01-04
 */
public record FunctionParameter(String name, String label, String defaultValue, String description) {
}
