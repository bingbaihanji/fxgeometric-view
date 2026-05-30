package com.bingbaihanji.view.layout.draw.coordinate.render;

import com.bingbaihanji.view.layout.draw.coordinate.TickInfo;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * 刻度线渲染器
 * <p>
 * 绘制主刻度线（8px）和次刻度线（4px），垂直于轴线方向。
 * 迁移自 AxesPainter.drawXAxisTicks() 和 drawMinorTicks()。
 *
 * @author bingbaihanji
 */
public class TickLineRenderer {

    /**
     * 主刻度线长度（像素）
     */
    private static final double MAJOR_TICK_LENGTH = 8;

    /**
     * 次刻度线长度（像素）
     */
    private static final double MINOR_TICK_LENGTH = 4;

    /**
     * 绘制 X 轴方向的刻度线
     *
     * @param gc    画布上下文
     * @param ticks 刻度信息列表
     * @param axisY Y 轴在屏幕上的位置
     */
    public void drawXTickLines(GraphicsContext gc, List<TickInfo> ticks, double axisY) {
        for (TickInfo tick : ticks) {
            if (tick.isMinor()) {
                gc.setLineWidth(1);
                gc.strokeLine(tick.screenPos(), axisY - MINOR_TICK_LENGTH / 2,
                        tick.screenPos(), axisY + MINOR_TICK_LENGTH / 2);
            } else {
                gc.setLineWidth(2);
                gc.strokeLine(tick.screenPos(), axisY - MAJOR_TICK_LENGTH / 2,
                        tick.screenPos(), axisY + MAJOR_TICK_LENGTH / 2);
            }
        }
    }

    /**
     * 绘制 Y 轴方向的刻度线
     *
     * @param gc    画布上下文
     * @param ticks 刻度信息列表
     * @param axisX X 轴在屏幕上的位置
     */
    public void drawYTickLines(GraphicsContext gc, List<TickInfo> ticks, double axisX) {
        for (TickInfo tick : ticks) {
            if (tick.isMinor()) {
                gc.setLineWidth(1);
                gc.strokeLine(axisX - MINOR_TICK_LENGTH / 2, tick.screenPos(),
                        axisX + MINOR_TICK_LENGTH / 2, tick.screenPos());
            } else {
                gc.setLineWidth(2);
                gc.strokeLine(axisX - MAJOR_TICK_LENGTH / 2, tick.screenPos(),
                        axisX + MAJOR_TICK_LENGTH / 2, tick.screenPos());
            }
        }
    }
}
