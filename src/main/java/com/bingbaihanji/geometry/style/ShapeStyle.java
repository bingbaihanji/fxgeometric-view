package com.bingbaihanji.geometry.style;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.LineType;
import com.bingbaihanji.util.StyleManager;
import javafx.scene.paint.Color;

/**
 * 图形样式
 * <p>
 * 集中保存描边与填充相关的可变样式. 所有取值校验都收敛在这里,
 * 因此直接持有本对象的代码也能得到同样的约束.
 */
public class ShapeStyle {

    /**
     * 描边颜色
     */
    private Color color = GeometryConfig.Colors.LABEL_TEXT;

    /**
     * 线型
     */
    private LineType lineType = LineType.FULL;

    /**
     * 线宽
     */
    private double lineWidth = StyleManager.getDefaultLineWidth();

    /**
     * 透明度 (0.0-1.0)
     */
    private double opacity = 1.0;

    /**
     * 填充类型
     */
    private FillType fillType = FillType.NONE;

    /**
     * 填充颜色
     */
    private Color fillColor = GeometryConfig.Colors.SELECTION_FILL;

    /**
     * 填充透明度 (0.0-1.0)
     */
    private double fillOpacity = 0.3;

    /**
     * 填充角度 (度, 用于线条填充)
     */
    private int hatchAngle = 45;

    /**
     * 填充间距 (像素, 用于线条填充)
     */
    private int hatchDistance = 10;

    /**
     * 获取描边颜色
     */
    public Color getColor() {
        return color;
    }

    /**
     * 设置描边颜色, null 时回落到默认文字色
     */
    public void setColor(Color color) {
        this.color = color != null ? color : GeometryConfig.Colors.LABEL_TEXT;
    }

    /**
     * 获取线型
     */
    public LineType getLineType() {
        return lineType;
    }

    /**
     * 设置线型, null 时回落到实线
     */
    public void setLineType(LineType lineType) {
        this.lineType = lineType != null ? lineType : LineType.FULL;
    }

    /**
     * 获取线宽
     */
    public double getLineWidth() {
        return lineWidth;
    }

    /**
     * 设置线宽, 限制在 0.5-20
     */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = Math.max(0.5, Math.min(lineWidth, 20.0));
    }

    /**
     * 获取透明度
     */
    public double getOpacity() {
        return opacity;
    }

    /**
     * 设置透明度, 限制在 0.0-1.0
     */
    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(opacity, 1.0));
    }

    /**
     * 获取填充类型
     */
    public FillType getFillType() {
        return fillType;
    }

    /**
     * 设置填充类型, null 时回落到无填充
     */
    public void setFillType(FillType fillType) {
        this.fillType = fillType != null ? fillType : FillType.NONE;
    }

    /**
     * 获取填充颜色
     */
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * 设置填充颜色, null 时回落到当前的描边颜色
     */
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor != null ? fillColor : color;
    }

    /**
     * 获取填充透明度
     */
    public double getFillOpacity() {
        return fillOpacity;
    }

    /**
     * 设置填充透明度, 限制在 0.0-1.0
     */
    public void setFillOpacity(double fillOpacity) {
        this.fillOpacity = Math.max(0.0, Math.min(fillOpacity, 1.0));
    }

    /**
     * 获取填充角度
     */
    public int getHatchAngle() {
        return hatchAngle;
    }

    /**
     * 设置填充角度, 按 360 归一化
     */
    public void setHatchAngle(int hatchAngle) {
        this.hatchAngle = hatchAngle % 360;
    }

    /**
     * 获取填充间距
     */
    public int getHatchDistance() {
        return hatchDistance;
    }

    /**
     * 设置填充间距, 限制在 1-50
     */
    public void setHatchDistance(int hatchDistance) {
        this.hatchDistance = Math.max(1, Math.min(hatchDistance, 50));
    }
}
