package com.bingbaihanji.geometry;

import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.LabelPosition;
import com.bingbaihanji.constant.LineType;
import javafx.scene.paint.Color;

/**
 * 几何数据接口
 * <p>
 * 定义几何对象的所有数据属性(getter/setter),
 * 与行为接口({@link WorldPainter})分离, 便于序列化, 属性编辑等场景使用.
 * <p>
 * 这里只声明契约, 不提供默认实现: 默认值统一由 {@code AbstractWorldObject}
 * 及其样式对象(ShapeStyle/LabelStyle)决定, 避免两处默认值各自漂移.
 *
 * @author bingbaihanji
 * @date 2026-05-22
 */
public interface GeometryData {

    // === 标识 ===

    /**
     * 对象唯一 ID
     */
    long getId();

    // === 标签 ===

    /**
     * 获取标签文本
     */
    String getLabel();

    /**
     * 设置标签文本
     */
    void setLabel(String label);

    /**
     * 标签是否可见
     */
    boolean isLabelVisible();

    /**
     * 设置标签可见性
     */
    void setLabelVisible(boolean visible);

    /**
     * 获取标签位置
     */
    LabelPosition getLabelPosition();

    /**
     * 设置标签位置
     */
    void setLabelPosition(LabelPosition position);

    /**
     * 获取标签字号
     */
    double getLabelFontSize();

    /**
     * 设置标签字号
     */
    void setLabelFontSize(double fontSize);

    /**
     * 获取标签颜色
     */
    Color getLabelColor();

    /**
     * 设置标签颜色
     */
    void setLabelColor(Color color);

    // === 交互状态 ===

    /**
     * 是否处于悬停状态
     */
    boolean isHover();

    /**
     * 设置悬停状态
     */
    void setHover(boolean hover);

    /**
     * 是否处于选中状态
     */
    boolean isSelected();

    /**
     * 设置选中状态
     */
    void setSelected(boolean selected);

    // === 视觉属性 ===

    /**
     * 获取描边颜色
     */
    Color getColor();

    /**
     * 设置描边颜色
     */
    void setColor(Color color);

    /**
     * 获取线型
     */
    LineType getLineType();

    /**
     * 设置线型
     */
    void setLineType(LineType lineType);

    /**
     * 获取线宽
     */
    double getLineWidth();

    /**
     * 设置线宽
     */
    void setLineWidth(double lineWidth);

    /**
     * 获取透明度
     */
    double getOpacity();

    /**
     * 设置透明度
     */
    void setOpacity(double opacity);

    /**
     * 获取图层
     */
    int getLayer();

    /**
     * 设置图层
     */
    void setLayer(int layer);

    // === 填充属性 ===

    /**
     * 获取填充类型
     */
    FillType getFillType();

    /**
     * 设置填充类型
     */
    void setFillType(FillType fillType);

    /**
     * 获取填充颜色
     */
    Color getFillColor();

    /**
     * 设置填充颜色
     */
    void setFillColor(Color fillColor);

    /**
     * 获取填充透明度
     */
    double getFillOpacity();

    /**
     * 设置填充透明度
     */
    void setFillOpacity(double fillOpacity);

    /**
     * 获取填充角度
     */
    int getHatchAngle();

    /**
     * 设置填充角度
     */
    void setHatchAngle(int angle);

    /**
     * 获取填充间距
     */
    int getHatchDistance();

    /**
     * 设置填充间距
     */
    void setHatchDistance(int distance);

    // === 可见性和锁定 ===

    /**
     * 是否可见
     */
    boolean isVisible();

    /**
     * 设置可见性
     */
    void setVisible(boolean visible);

    /**
     * 是否锁定
     */
    boolean isLocked();

    /**
     * 设置锁定状态
     */
    void setLocked(boolean locked);

    /**
     * 是否固定
     */
    boolean isFixed();

    /**
     * 设置固定状态
     */
    void setFixed(boolean fixed);

    /**
     * 是否可选择
     */
    boolean isSelectable();

    /**
     * 设置可选择状态
     */
    void setSelectable(boolean selectable);
}
