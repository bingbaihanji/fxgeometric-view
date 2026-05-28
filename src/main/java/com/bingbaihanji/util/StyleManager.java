package com.bingbaihanji.util;

import javafx.scene.paint.Color;

/**
 * 样式管理器
 * <p>
 * 集中管理所有颜色常量和CSS样式字符串
 * 便于后续主题系统的实现
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class StyleManager {

    // 面板颜色
    public static final String PANEL_BACKGROUND = "#fafafa";
    public static final String BORDER_COLOR = "#e5e5e5";
    public static final String SCROLL_PANE_BACKGROUND = "#fafafa";

    // 按钮颜色
    public static final String BUTTON_DEFAULT_BG = "#ffffff";
    public static final String BUTTON_DEFAULT_BORDER = "#d0d0d0";
    public static final String BUTTON_HOVER_BG = "#f9f9f9";
    public static final String BUTTON_HOVER_BORDER = "#999";
    public static final String BUTTON_SELECTED_BG = "#eff6ff";
    public static final String BUTTON_SELECTED_BORDER = "#2563eb";

    // 文字颜色
    public static final String TEXT_PRIMARY = "#1f2937";
    public static final String TEXT_SECONDARY = "#333";

    // 几何元素颜色
    public static final Color GEOMETRY_DEFAULT = Color.RED;
    public static final Color GEOMETRY_HOVER = Color.ORANGE;
    public static final Color GEOMETRY_CONSTRAINED = Color.DARKBLUE;
    public static final Color GEOMETRY_REUSED = Color.PURPLE;        // 复用组点颜色
    public static final Color GEOMETRY_LINE = Color.LIGHTSLATEGRAY; // 默认线条颜色

    // 坐标轴和网格颜色
    public static final Color AXES_COLOR = Color.valueOf("#f7a707");
    public static final Color BOUNDARY_AXES_COLOR = Color.valueOf("#4287f5");
    public static final Color GRID_DOT_COLOR = Color.rgb(126, 126, 126);
    public static final Color GRID_LINE_COLOR = Color.rgb(153, 153, 153);

    // CSS样式生成方法

    /**
     * 获取面板样式
     */
    public static String getPanelStyle() {
        return """
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-width: 0 1 0 0;
                -fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
                """.formatted(PANEL_BACKGROUND, BORDER_COLOR);
    }

    /**
     * 获取分组标题样式
     */
    public static String getSectionTitleStyle() {
        return """
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-text-fill: %s;
                -fx-padding: 4 0 4 0;
                """.formatted(TEXT_PRIMARY);
    }

    /**
     * 获取按钮样式
     *
     * @param state 按钮状态：normal, hover, selected
     */
    public static String getButtonStyle(String state) {
        String bg, border;
        int borderWidth = 1;

        switch (state) {
            case "hover" -> {
                bg = BUTTON_HOVER_BG;
                border = BUTTON_HOVER_BORDER;
            }
            case "selected" -> {
                bg = BUTTON_SELECTED_BG;
                border = BUTTON_SELECTED_BORDER;
                borderWidth = 2;
            }
            default -> {
                bg = BUTTON_DEFAULT_BG;
                border = BUTTON_DEFAULT_BORDER;
            }
        }

        return """
                -fx-background-radius: 8;
                -fx-border-radius: 8;
                -fx-border-width: %d;
                -fx-border-color: %s;
                -fx-background-color: %s;
                -fx-padding: 4;
                -fx-font-size: 10px;
                """.formatted(borderWidth, border, bg);
    }

    /**
     * 获取ScrollPane样式
     */
    public static String getScrollPaneStyle() {
        return "-fx-control-inner-background: " + SCROLL_PANE_BACKGROUND + ";";
    }

    /**
     * 获取分隔线样式
     */
    public static String getSeparatorStyle() {
        return "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;";
    }

    /**
     * 获取Tooltip样式
     */
    public static String getTooltipStyle() {
        return "-fx-font-size: 11px;";
    }

    /**
     * 获取文本标签样式
     */
    public static String getTextLabelStyle() {
        return "-fx-font-size: 10px; -fx-text-alignment: center;";
    }

    /**
     * 获取备用文本标签样式(用于无图标情况)
     */
    public static String getFallbackLabelStyle() {
        return "-fx-font-size: 20px; -fx-text-fill: " + TEXT_SECONDARY + ";";
    }

    // 工具方法

    /**
     * 将JavaFX Color转换为十六进制字符串
     */
    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * 将JavaFX Color转换为RGB CSS字符串
     */
    public static String toRgb(Color color) {
        return String.format("rgb(%d, %d, %d)",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
