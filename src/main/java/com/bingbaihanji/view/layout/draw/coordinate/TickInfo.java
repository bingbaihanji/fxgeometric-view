package com.bingbaihanji.view.layout.draw.coordinate;

/**
 * 刻度信息 —— 描述一个刻度线及其标签
 * <p>
 * 纯数据对象，由 CoordinateSystem.calculateTicks() 产出，
 * 由 TickLineRenderer 和 TickLabelRenderer 消费。
 *
 * @param worldPos   刻度在世界坐标系中的位置
 * @param screenPos  刻度在屏幕上的位置（x 或 y 坐标，取决于轴方向）
 * @param label      刻度标签文本
 * @param isMinor    是否为次刻度
 * @author bingbaihanji
 */
public record TickInfo(double worldPos, double screenPos, String label, boolean isMinor) {}
