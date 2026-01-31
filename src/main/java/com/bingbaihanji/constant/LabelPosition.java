package com.bingbaihanji.constant;

/**
 * 标签位置枚举
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 定义标签相对于对象的显示位置
 */
public enum LabelPosition {
    /**
     * 自动位置（默认）
     */
    AUTO("自动"),

    /**
     * 右上方
     */
    TOP_RIGHT("右上"),

    /**
     * 右下方
     */
    BOTTOM_RIGHT("右下"),

    /**
     * 左上方
     */
    TOP_LEFT("左上"),

    /**
     * 左下方
     */
    BOTTOM_LEFT("左下"),

    /**
     * 正上方
     */
    TOP("上方"),

    /**
     * 正下方
     */
    BOTTOM("下方"),

    /**
     * 正左方
     */
    LEFT("左方"),

    /**
     * 正右方
     */
    RIGHT("右方"),

    /**
     * 中心
     */
    CENTER("中心");

    private final String displayName;

    LabelPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 计算标签的偏移量（像素）
     *
     * @param baseX 基准X坐标（屏幕坐标）
     * @param baseY 基准Y坐标（屏幕坐标）
     * @return [offsetX, offsetY]
     */
    public double[] getOffset(double baseX, double baseY) {
        // 使用 GeometryConfig 中的常量
        final double OFFSET = com.bingbaihanji.config.GeometryConfig.Label.LABEL_OFFSET_X;
        final double FONT_HEIGHT = com.bingbaihanji.config.GeometryConfig.Label.DEFAULT_FONT_SIZE;

        return switch (this) {
            case TOP_RIGHT, AUTO -> new double[]{OFFSET, -OFFSET};
            case BOTTOM_RIGHT -> new double[]{OFFSET, OFFSET + FONT_HEIGHT};
            case TOP_LEFT -> new double[]{-OFFSET - 20, -OFFSET};
            case BOTTOM_LEFT -> new double[]{-OFFSET - 20, OFFSET + FONT_HEIGHT};
            case TOP -> new double[]{0, -OFFSET - FONT_HEIGHT};
            case BOTTOM -> new double[]{0, OFFSET + FONT_HEIGHT};
            case LEFT -> new double[]{-OFFSET - 20, 4};
            case RIGHT -> new double[]{OFFSET, 4};
            case CENTER -> new double[]{0, 4};
        };
    }
}
