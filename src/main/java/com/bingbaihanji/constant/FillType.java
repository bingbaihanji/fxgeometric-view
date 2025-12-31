package com.bingbaihanji.constant;

/**
 * 填充类型枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义封闭图形的填充模式，参考 GeoGebra 的填充系统
 */
public enum FillType {

    /**
     * 无填充（仅描边）
     */
    NONE("无填充", "仅显示轮廓，不填充内部"),

    /**
     * 纯色填充
     */
    STANDARD("标准填充", "使用纯色填充"),

    /**
     * 水平线条填充
     */
    HATCH_HORIZONTAL("水平线条", "水平平行线填充"),

    /**
     * 垂直线条填充
     */
    HATCH_VERTICAL("垂直线条", "垂直平行线填充"),

    /**
     * 斜线填充（左上到右下）
     */
    HATCH_DIAGONAL("斜线填充", "对角线填充（\\）"),

    /**
     * 反斜线填充（左下到右上）
     */
    HATCH_CROSS_DIAGONAL("反斜线填充", "反对角线填充（/）"),

    /**
     * 交叉网格填充
     */
    HATCH_GRID("网格填充", "水平+垂直交叉线"),

    /**
     * 点状填充
     */
    DOTTED("点状填充", "点状图案填充"),

    /**
     * 蜂窝填充
     */
    HONEYCOMB("蜂窝填充", "六边形蜂窝图案"),

    /**
     * 砖块填充
     */
    BRICK("砖块填充", "砖块图案填充"),

    /**
     * 波浪填充
     */
    WEAVING("编织填充", "编织图案填充"),

    /**
     * 符号填充
     */
    SYMBOLS("符号填充", "重复符号图案");

    // ========== 属性 ==========

    /**
     * 填充类型名称（中文）
     */
    private final String displayName;

    /**
     * 填充类型描述
     */
    private final String description;

    // ========== 构造函数 ==========

    FillType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    // ========== Getter 方法 ==========

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    // ========== 工具方法 ==========

    /**
     * 判断是否为线条类填充（需要设置间距和角度）
     */
    public boolean isHatchFill() {
        return this == HATCH_HORIZONTAL
            || this == HATCH_VERTICAL
            || this == HATCH_DIAGONAL
            || this == HATCH_CROSS_DIAGONAL
            || this == HATCH_GRID;
    }

    /**
     * 判断是否需要填充颜色（非 NONE）
     */
    public boolean hasColor() {
        return this != NONE;
    }

    /**
     * 获取默认填充角度（度）
     *
     * @return 填充角度，如果不适用则返回 0
     */
    public int getDefaultAngle() {
        return switch (this) {
            case HATCH_HORIZONTAL -> 0;
            case HATCH_VERTICAL -> 90;
            case HATCH_DIAGONAL -> 45;
            case HATCH_CROSS_DIAGONAL -> -45;
            default -> 0;
        };
    }

    /**
     * 获取默认填充间距（像素）
     *
     * @return 填充间距
     */
    public int getDefaultDistance() {
        return switch (this) {
            case DOTTED -> 5;
            case HONEYCOMB, BRICK -> 15;
            case WEAVING, SYMBOLS -> 10;
            default -> 8; // 线条类填充默认间距
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
