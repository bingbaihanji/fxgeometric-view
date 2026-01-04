package com.bingbaihanji.model;

/**
 * 函数参数描述类
 * <p>
 * 用于描述函数参数的元信息（名称、标签、默认值、说明）
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class FunctionParameter {
    /**
     * 参数名称（用于代码中引用）
     */
    private final String name;

    /**
     * 参数标签（显示给用户）
     */
    private final String label;

    /**
     * 默认值
     */
    private final String defaultValue;

    /**
     * 参数说明
     */
    private final String description;

    public FunctionParameter(String name, String label, String defaultValue, String description) {
        this.name = name;
        this.label = label;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }
}
