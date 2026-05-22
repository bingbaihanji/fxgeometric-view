package com.bingbaihanji.view.layout.draw.geometry;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.LabelPosition;
import com.bingbaihanji.constant.LineType;
import javafx.scene.paint.Color;

/**
 * 几何数据接口
 * <p>
 * 定义几何对象的所有数据属性(getter/setter)，
 * 与行为接口({@link WorldPainter})分离，便于序列化、属性编辑等场景使用
 *
 * @author bingbaihanji
 * @date 2026-05-22
 */
public interface GeometryData {

    // === 标识 ===

    long getId();

    // === 标签 ===

    default String getLabel() {
        return "";
    }

    default void setLabel(String label) {
    }

    default boolean isLabelVisible() {
        return true;
    }

    default void setLabelVisible(boolean visible) {
    }

    default LabelPosition getLabelPosition() {
        return LabelPosition.AUTO;
    }

    default void setLabelPosition(LabelPosition position) {
    }

    default double getLabelFontSize() {
        return 12.0;
    }

    default void setLabelFontSize(double fontSize) {
    }

    default Color getLabelColor() {
        return GeometryConfig.Colors.LABEL_TEXT;
    }

    default void setLabelColor(Color color) {
    }

    // === 交互状态 ===

    default boolean isHover() {
        return false;
    }

    default void setHover(boolean hover) {
    }

    default boolean isSelected() {
        return false;
    }

    default void setSelected(boolean selected) {
    }

    // === 视觉属性 ===

    default Color getColor() {
        return GeometryConfig.Colors.LABEL_TEXT;
    }

    default void setColor(Color color) {
    }

    default LineType getLineType() {
        return LineType.FULL;
    }

    default void setLineType(LineType lineType) {
    }

    default double getLineWidth() {
        return 2.0;
    }

    default void setLineWidth(double lineWidth) {
    }

    default double getOpacity() {
        return 1.0;
    }

    default void setOpacity(double opacity) {
    }

    default int getLayer() {
        return 0;
    }

    default void setLayer(int layer) {
    }

    // === 填充属性 ===

    default FillType getFillType() {
        return FillType.NONE;
    }

    default void setFillType(FillType fillType) {
    }

    default Color getFillColor() {
        return getColor();
    }

    default void setFillColor(Color fillColor) {
    }

    default double getFillOpacity() {
        return 0.3;
    }

    default void setFillOpacity(double fillOpacity) {
    }

    default int getHatchAngle() {
        return 45;
    }

    default void setHatchAngle(int angle) {
    }

    default int getHatchDistance() {
        return 10;
    }

    default void setHatchDistance(int distance) {
    }

    // === 可见性和锁定 ===

    default boolean isVisible() {
        return true;
    }

    default void setVisible(boolean visible) {
    }

    default boolean isLocked() {
        return false;
    }

    default void setLocked(boolean locked) {
    }

    default boolean isFixed() {
        return false;
    }

    default void setFixed(boolean fixed) {
    }

    default boolean isSelectable() {
        return true;
    }

    default void setSelectable(boolean selectable) {
    }
}
