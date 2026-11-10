package com.bingbaihanji.geometry.style;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.LabelPosition;
import javafx.scene.paint.Color;

/**
 * 标签样式
 * <p>
 * 集中保存标签文本、可见性、位置、字号与颜色, 取值校验一并收敛在这里.
 */
public class LabelStyle {

    /**
     * 标签文本
     */
    private String text = "";

    /**
     * 标签是否可见
     */
    private boolean visible = true;

    /**
     * 标签位置
     */
    private LabelPosition position = LabelPosition.AUTO;

    /**
     * 标签字体大小
     */
    private double fontSize = 12.0;

    /**
     * 标签颜色
     */
    private Color color = GeometryConfig.Colors.LABEL_TEXT;

    /**
     * 获取标签文本
     */
    public String getText() {
        return text;
    }

    /**
     * 设置标签文本, null 时置为空串
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * 标签是否可见
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 设置标签可见性
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * 获取标签位置
     */
    public LabelPosition getPosition() {
        return position;
    }

    /**
     * 设置标签位置, null 时回落到自动
     */
    public void setPosition(LabelPosition position) {
        this.position = position != null ? position : LabelPosition.AUTO;
    }

    /**
     * 获取标签字号
     */
    public double getFontSize() {
        return fontSize;
    }

    /**
     * 设置标签字号, 限制在 6-72
     */
    public void setFontSize(double fontSize) {
        this.fontSize = Math.max(6, Math.min(fontSize, 72));
    }

    /**
     * 获取标签颜色
     */
    public Color getColor() {
        return color;
    }

    /**
     * 设置标签颜色, null 时回落到默认文字色
     */
    public void setColor(Color color) {
        this.color = color != null ? color : GeometryConfig.Colors.LABEL_TEXT;
    }
}
