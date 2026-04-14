package com.bingbaihanji.view.layout.draw.geometry;

import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.LabelPosition;
import com.bingbaihanji.constant.LineType;
import com.bingbaihanji.constant.ObjectType;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 世界对象接口(扩展版)
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 几何对象的统一接口, 定义了对象的基本属性和行为
 */
public interface WorldObject extends WorldPainter {

    //   基本信息  

    /**
     * 获取对象唯一 ID
     */
    long getId();

    /**
     * 获取对象类型
     */
    ObjectType getObjectType();

    /**
     * 获取对象名称/标签
     */
    default String getLabel() {
        return "";
    }

    /**
     * 设置对象名称/标签
     */
    default void setLabel(String label) {
    }

    /**
     * 是否显示标签
     */
    default boolean isLabelVisible() {
        return true;
    }

    /**
     * 设置标签可见性
     */
    default void setLabelVisible(boolean visible) {
    }

    /**
     * 获取标签位置
     */
    default LabelPosition getLabelPosition() {
        return LabelPosition.AUTO;
    }

    /**
     * 设置标签位置
     */
    default void setLabelPosition(LabelPosition position) {
    }

    /**
     * 获取标签字体大小
     */
    default double getLabelFontSize() {
        return 12.0;
    }

    /**
     * 设置标签字体大小
     */
    default void setLabelFontSize(double fontSize) {
    }

    /**
     * 获取标签颜色
     */
    default Color getLabelColor() {
        return Color.BLACK;
    }

    /**
     * 设置标签颜色
     */
    default void setLabelColor(Color color) {
    }

    //   交互状态  

    /**
     * 命中测试(世界坐标)
     *
     * @param worldX    世界 X
     * @param worldY    世界 Y
     * @param tolerance 世界单位下的容忍半径
     */
    boolean hitTest(double worldX, double worldY, double tolerance);

    /**
     * 点击响应
     */
    default void onClick(double worldX, double worldY) {
    }

    /**
     * 获取悬停状态
     */
    default boolean isHover() {
        return false;
    }

    /**
     * 设置悬停状态
     */
    default void setHover(boolean hover) {
    }

    /**
     * 获取选中状态
     */
    default boolean isSelected() {
        return false;
    }

    /**
     * 设置选中状态
     */
    default void setSelected(boolean selected) {
    }

    //   视觉属性  

    /**
     * 获取对象颜色
     */
    default Color getColor() {
        return Color.BLACK;
    }

    /**
     * 设置对象颜色
     */
    default void setColor(Color color) {
    }

    /**
     * 获取线型
     */
    default LineType getLineType() {
        return LineType.FULL;
    }

    /**
     * 设置线型
     */
    default void setLineType(LineType lineType) {
    }

    /**
     * 获取线宽
     */
    default double getLineWidth() {
        return 2.0;
    }

    /**
     * 设置线宽
     */
    default void setLineWidth(double lineWidth) {
    }

    /**
     * 获取透明度 (0.0-1.0)
     */
    default double getOpacity() {
        return 1.0;
    }

    /**
     * 设置透明度 (0.0-1.0)
     */
    default void setOpacity(double opacity) {
    }

    /**
     * 获取图层 (0-9)
     */
    default int getLayer() {
        return 0;
    }

    /**
     * 设置图层 (0-9)
     */
    default void setLayer(int layer) {
    }

    //   填充属性 (仅封闭图形)  

    /**
     * 获取填充类型
     */
    default FillType getFillType() {
        return FillType.NONE;
    }

    /**
     * 设置填充类型
     */
    default void setFillType(FillType fillType) {
    }

    /**
     * 获取填充颜色
     */
    default Color getFillColor() {
        return getColor();
    }

    /**
     * 设置填充颜色
     */
    default void setFillColor(Color fillColor) {
    }

    /**
     * 获取填充透明度 (0.0-1.0)
     */
    default double getFillOpacity() {
        return 0.3;
    }

    /**
     * 设置填充透明度 (0.0-1.0)
     */
    default void setFillOpacity(double fillOpacity) {
    }

    /**
     * 获取填充角度(度,用于线条填充)
     */
    default int getHatchAngle() {
        return 45;
    }

    /**
     * 设置填充角度(度)
     */
    default void setHatchAngle(int angle) {
    }

    /**
     * 获取填充间距(像素,用于线条填充)
     */
    default int getHatchDistance() {
        return 10;
    }

    /**
     * 设置填充间距(像素)
     */
    default void setHatchDistance(int distance) {
    }

    //   可见性和锁定  

    /**
     * 是否可见
     */
    default boolean isVisible() {
        return true;
    }

    /**
     * 设置可见性
     */
    default void setVisible(boolean visible) {
    }

    /**
     * 是否锁定(锁定后不可编辑属性,但可选中)
     */
    default boolean isLocked() {
        return false;
    }

    /**
     * 设置锁定状态
     */
    default void setLocked(boolean locked) {
    }

    /**
     * 是否固定(固定后不可移动)
     */
    default boolean isFixed() {
        return false;
    }

    /**
     * 设置固定状态
     */
    default void setFixed(boolean fixed) {
    }

    /**
     * 是否可选择
     */
    default boolean isSelectable() {
        return true;
    }

    /**
     * 设置可选择性
     */
    default void setSelectable(boolean selectable) {
    }

    //   拖拽相关  

    /**
     * 获取可拖动的控制点列表
     *
     * @return 控制点列表,如果不支持拖动则返回空列表
     */
    default List<DraggablePoint> getDraggablePoints() {
        return List.of();
    }

    /**
     * 绕指定点旋转图形
     *
     * @param centerX 旋转中心X坐标(世界坐标)
     * @param centerY 旋转中心Y坐标(世界坐标)
     * @param angle   旋转角度(弧度)
     */
    default void rotateAroundPoint(double centerX, double centerY, double angle) {
        // 默认实现为空,子类可以覆写
    }

    /**
     * 获取对象的边界框
     *
     * @return 包含 [minX, maxX, minY, maxY] 的数组,如果无法计算则返回null
     */
    default double[] getBoundingBox() {
        // 默认实现返回null,子类应覆写此方法
        return null;
    }

    /**
     * 接受访问者(访问者模式)
     * <p>
     * 允许在不修改几何对象类的情况下,为它们添加新的操作
     *
     * @param visitor 访问者对象
     * @param <T>     返回值类型
     * @return 访问结果
     */
    default <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitOther(this);
    }

    /**
     * 点位置更新器
     */
    @FunctionalInterface
    interface PointUpdater {
        void update(double newX, double newY);
    }

    /**
     * 可拖动的控制点
     */
    class DraggablePoint {
        private final double x;
        private final double y;
        private final PointUpdater updater;

        public DraggablePoint(double x, double y, PointUpdater updater) {
            this.x = x;
            this.y = y;
            this.updater = updater;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        /**
         * 更新点的位置
         */
        public void updatePosition(double newX, double newY) {
            updater.update(newX, newY);
        }

        /**
         * 检查是否命中此控制点
         */
        public boolean hitTest(double worldX, double worldY, double tolerance) {
            return Math.hypot(worldX - x, worldY - y) < tolerance;
        }
    }
}
