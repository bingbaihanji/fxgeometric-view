package com.binbaihanji.view.layout.draw.geometry;

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

    default void setHover(boolean hover) {}
}
