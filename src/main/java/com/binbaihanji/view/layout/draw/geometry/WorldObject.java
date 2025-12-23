package com.binbaihanji.view.layout.draw.geometry;

import java.util.List;

public interface WorldObject extends WorldPainter {

    /**
     * 命中测试（世界坐标）
     *
     * @param worldX    世界 X
     * @param worldY    世界 Y
     * @param tolerance 世界单位下的容忍半径
     */
    boolean hitTest(double worldX, double worldY, double tolerance);

    default void onClick(double worldX, double worldY) {

    }

    default void setHover(boolean hover) {
    }

    /**
     * 获取可拖动的控制点列表
     *
     * @return 控制点列表，如果不支持拖动则返回空列表
     */
    default List<DraggablePoint> getDraggablePoints() {
        return List.of();
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
