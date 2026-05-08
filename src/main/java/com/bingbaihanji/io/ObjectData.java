package com.bingbaihanji.io;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 单个几何对象的数据载体
 * <p>
 * 使用 Map 存储类型相关的属性，灵活支持所有几何类型
 */
public class ObjectData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对象类型名称（对应 ObjectType 的 name）
     */
    private String type;

    /**
     * 通用属性 + 类型特有属性
     */
    private Map<String, Serializable> properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }
}
