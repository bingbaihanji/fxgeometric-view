package com.bingbaihanji.constant;

/**
 * 几何对象类型枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义所有几何对象的类型, 参考 GeoGebra 的对象分类
 */
public enum ObjectType {

    //   点类型  

    /**
     * 自由点(可自由移动)
     */
    POINT_FREE("点", "自由点"),

    /**
     * 依赖点(由其他对象定义)
     */
    POINT_DEPENDENT("点", "依赖点"),

    /**
     * 约束点(在线或曲线上)
     */
    POINT_ON_PATH("点", "路径上的点"),

    /**
     * 交点(两个对象的交点)
     */
    POINT_INTERSECTION("点", "交点"),

    //   线类型  

    /**
     * 线段(有限线)
     */
    SEGMENT("线段", "线段"),

    /**
     * 直线(无限延伸)
     */
    LINE("直线", "直线"),

    /**
     * 射线(半无限线)
     */
    RAY("射线", "射线"),

    /**
     * 向量(带方向的线段)
     */
    VECTOR("向量", "向量"),

    //   圆锥曲线类型  

    /**
     * 圆
     */
    CIRCLE("圆", "圆"),

    /**
     * 圆弧
     */
    ARC("圆弧", "圆弧"),

    /**
     * 扇形
     */
    SECTOR("扇形", "扇形"),

    /**
     * 椭圆
     */
    ELLIPSE("椭圆", "椭圆"),

    /**
     * 双曲线
     */
    HYPERBOLA("双曲线", "双曲线"),

    /**
     * 抛物线
     */
    PARABOLA("抛物线", "抛物线"),

    //   多边形类型  

    /**
     * 多边形
     */
    POLYGON("多边形", "多边形"),

    /**
     * 三角形
     */
    TRIANGLE("三角形", "三角形"),

    /**
     * 矩形
     */
    RECTANGLE("矩形", "矩形"),

    /**
     * 正多边形
     */
    REGULAR_POLYGON("正多边形", "正多边形"),

    //   特殊对象类型  

    /**
     * 角度标注
     */
    ANGLE("角度", "角度"),

    /**
     * 手绘路径
     */
    PATH("路径", "手绘路径"),

    /**
     * 轨迹
     */
    LOCUS("轨迹", "轨迹"),

    /**
     * 函数曲线
     */
    FUNCTION("函数", "函数曲线"),

    /**
     * 文本标签
     */
    TEXT("文本", "文本标签"),

    /**
     * 图片
     */
    IMAGE("图片", "图片");

    //   属性  

    /**
     * 对象类别(中文)
     */
    private final String category;

    /**
     * 对象类型名称(中文)
     */
    private final String displayName;

    //   构造函数  

    ObjectType(String category, String displayName) {
        this.category = category;
        this.displayName = displayName;
    }

    //   Getter 方法  

    public String getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    //   工具方法  

    /**
     * 判断是否为点类型
     */
    public boolean isPoint() {
        return this == POINT_FREE
                || this == POINT_DEPENDENT
                || this == POINT_ON_PATH
                || this == POINT_INTERSECTION;
    }

    /**
     * 判断是否为线类型
     */
    public boolean isLine() {
        return this == SEGMENT
                || this == LINE
                || this == RAY
                || this == VECTOR;
    }

    /**
     * 判断是否为圆锥曲线类型
     */
    public boolean isConic() {
        return this == CIRCLE
                || this == ARC
                || this == SECTOR
                || this == ELLIPSE
                || this == HYPERBOLA
                || this == PARABOLA;
    }

    /**
     * 判断是否为多边形类型
     */
    public boolean isPolygon() {
        return this == POLYGON
                || this == TRIANGLE
                || this == RECTANGLE
                || this == REGULAR_POLYGON;
    }

    /**
     * 判断是否为封闭图形(需要填充)
     */
    public boolean isClosed() {
        return isConic() || isPolygon() || this == SECTOR;
    }

    /**
     * 判断对象是否可拖拽(默认行为)
     */
    public boolean isDraggableByDefault() {
        return this == POINT_FREE || this == POINT_ON_PATH;
    }
}
