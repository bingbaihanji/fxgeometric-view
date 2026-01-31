package com.bingbaihanji.util;

import com.bingbaihanji.constant.LabelPosition;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 标签渲染工具类
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 提供统一的标签绘制功能，支持可配置的位置、字体和颜色
 */
public class LabelRenderer {

    /**
     * 绘制对象的标签
     *
     * @param gc      GraphicsContext对象
     * @param object  世界对象
     * @param screenX 标签基准点X坐标（屏幕坐标）
     * @param screenY 标签基准点Y坐标（屏幕坐标）
     */
    public static void renderLabel(GraphicsContext gc, WorldObject object,
                                   double screenX, double screenY) {
        // 检查标签是否可见且非空
        if (!object.isLabelVisible() || object.getLabel() == null || object.getLabel().isEmpty()) {
            return;
        }

        // 获取标签配置
        String label = object.getLabel();
        LabelPosition position = object.getLabelPosition();
        double fontSize = object.getLabelFontSize();
        Color labelColor = object.getLabelColor();

        // 计算标签位置偏移
        double[] offset = position.getOffset(screenX, screenY);
        double labelX = screenX + offset[0];
        double labelY = screenY + offset[1];

        // 设置字体和颜色
        gc.setFill(labelColor);
        gc.setFont(Font.font(fontSize));
        gc.setTextAlign(TextAlignment.LEFT);

        // 绘制标签
        gc.fillText(label, labelX, labelY);
    }

    /**
     * 绘制标签（使用默认位置AUTO）
     *
     * @param gc      GraphicsContext对象
     * @param label   标签文本
     * @param screenX 基准点X坐标（屏幕坐标）
     * @param screenY 基准点Y坐标（屏幕坐标）
     */
    public static void renderLabel(GraphicsContext gc, String label,
                                   double screenX, double screenY) {
        if (label == null || label.isEmpty()) {
            return;
        }

        // 使用默认配置
        double[] offset = LabelPosition.AUTO.getOffset(screenX, screenY);
        double labelX = screenX + offset[0];
        double labelY = screenY + offset[1];

        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(label, labelX, labelY);
    }

    /**
     * 绘制标签（带自定义配置）
     *
     * @param gc       GraphicsContext对象
     * @param label    标签文本
     * @param screenX  基准点X坐标（屏幕坐标）
     * @param screenY  基准点Y坐标（屏幕坐标）
     * @param position 标签位置
     * @param fontSize 字体大小
     * @param color    标签颜色
     */
    public static void renderLabel(GraphicsContext gc, String label,
                                   double screenX, double screenY,
                                   LabelPosition position, double fontSize, Color color) {
        if (label == null || label.isEmpty()) {
            return;
        }

        // 计算标签位置偏移
        double[] offset = position.getOffset(screenX, screenY);
        double labelX = screenX + offset[0];
        double labelY = screenY + offset[1];

        // 设置字体和颜色
        gc.setFill(color);
        gc.setFont(Font.font(fontSize));
        gc.setTextAlign(TextAlignment.LEFT);

        // 绘制标签
        gc.fillText(label, labelX, labelY);
    }
}
