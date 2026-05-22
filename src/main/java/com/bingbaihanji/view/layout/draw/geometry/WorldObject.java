package com.bingbaihanji.view.layout.draw.geometry;

import com.bingbaihanji.constant.ObjectType;

import java.util.List;

/**
 * 世界对象接口
 * <p>
 * 几何对象的统一接口，继承 {@link GeometryData}(数据属性) 和 {@link WorldPainter}(绘制行为)，
 * 并在此基础上定义几何对象特有的交互行为
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public interface WorldObject extends GeometryData, WorldPainter {

    // === 类型标识 ===

    /**
     * 获取对象类型
     */
    ObjectType getObjectType();

    // === 交互行为 ===

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

    // === 拖拽相关 ===

    /**
     * 获取可拖动的控制点列表
     *
     * @return 控制点列表，如果不支持拖动则返回空列表
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
    }

    /**
     * 获取对象的边界框
     *
     * @return 包含 [minX, maxX, minY, maxY] 的数组，如果无法计算则返回null
     */
    default double[] getBoundingBox() {
        return null;
    }

    // === 访问者模式 ===

    /**
     * 接受访问者(访问者模式)
     * <p>
     * 允许在不修改几何对象类的情况下，为它们添加新的操作
     *
     * @param visitor 访问者对象
     * @param <T>     返回值类型
     * @return 访问结果
     */
    default <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitOther(this);
    }

    // === 内部类型 ===

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
