package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.constant.AxisTickStyle;
import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.view.layout.draw.coordinate.*;
import com.bingbaihanji.view.layout.draw.coordinate.grid.GridElement;
import com.bingbaihanji.view.layout.draw.coordinate.render.*;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * 坐标系统统一渲染器
 * <p>
 * 编排背景绘制管线：
 * 1. 网格 → 2. 轴线+箭头 → 3. 刻度线 → 4. 刻度标签 → 5. 轴名
 * <p>
 * 根据 GridType 自动选择对应的 CoordinateSystem 实现。
 *
 * @author bingbaihanji
 */
public class CoordSystemRenderer {

    private final CartesianCoordinateSystem cartesianSystem = new CartesianCoordinateSystem();
    private final PolarCoordinateSystem polarSystem = new PolarCoordinateSystem();
    private final IsometricCoordinateSystem isometricSystem = new IsometricCoordinateSystem();

    private final GridElementPainter gridElementPainter = new GridElementPainter();
    private final AxesLineRenderer axesLineRenderer = new AxesLineRenderer();
    private final TickLineRenderer tickLineRenderer = new TickLineRenderer();
    private final TickLabelRenderer tickLabelRenderer = new TickLabelRenderer();
    private final AxisLabelRenderer axisLabelRenderer = new AxisLabelRenderer();

    /**
     * 当前使用的坐标系统
     */
    private CoordinateSystem currentSystem;

    /**
     * 渲染完整的坐标系统背景（网格 + 坐标轴）
     *
     * @param gc        画布上下文
     * @param transform 世界坐标变换
     * @param settings  视图配置
     * @param width     视口宽度
     * @param height    视口高度
     */
    public void render(GraphicsContext gc, WorldTransform transform,
                       EuclidianViewSettings settings, double width, double height) {
        // 选择坐标系统
        currentSystem = selectSystem(settings.getGridType());

        // 极坐标系统要求 X/Y 轴比例锁定为 1:1，确保极轴保持水平
        if (currentSystem.isAxesRatioLocked()) {
            double sx = transform.getScaleX();
            double sy = transform.getScaleY();
            if (Math.abs(sx - sy) > 0.001) {
                double avgScale = (sx + sy) / 2;
                transform.setScaleX(avgScale);
                transform.setScaleY(avgScale);
                // 同步回 settings，但不在此处触发布局通知（避免循环）
                settings.setXScale(avgScale);
                settings.setYScale(avgScale);
            }
        }

        // 1. 生成并绘制网格
        if (settings.isShowGrid()) {
            List<GridElement> gridElements = currentSystem.generateGrid(
                    transform, settings, width, height);
            gridElementPainter.paint(gc, gridElements, settings);
        }

        // 2. 绘制坐标轴线 + 箭头
        axesLineRenderer.paint(gc, transform, settings, width, height);

        // 3. 计算并绘制刻度
        if (settings.isShowAxesNumbers()) {
            List<TickInfo> xTicks = currentSystem.calculateXTicks(
                    transform, settings, width, height);
            List<TickInfo> yTicks = currentSystem.calculateYTicks(
                    transform, settings, width, height);

            double x0 = transform.worldToScreenX(0);
            double y0 = transform.worldToScreenY(0);
            boolean xAxisVisible = y0 >= 0 && y0 <= height && settings.isShowXAxis();
            boolean yAxisVisible = x0 >= 0 && x0 <= width && settings.isShowYAxis();

            gc.setStroke(settings.getAxesColor());
            gc.setFill(settings.getAxesColor());

            // X 轴刻度线和标签
            if (xAxisVisible && settings.getXTickStyle() != AxisTickStyle.NONE) {
                tickLineRenderer.drawXTickLines(gc, xTicks, y0);
                tickLabelRenderer.drawXLabels(gc, xTicks, y0,
                        settings.getUnitLabelType(), settings.getXTickDistance(), settings);
            }

            // Y 轴刻度线和标签
            if (yAxisVisible && settings.getYTickStyle() != AxisTickStyle.NONE) {
                tickLineRenderer.drawYTickLines(gc, yTicks, x0);
                tickLabelRenderer.drawYLabels(gc, yTicks, x0,
                        settings.getUnitLabelType(), settings.getYTickDistance(), settings);
            }
        }

        // 4. 绘制轴名标签（在刻度标签之上）
        axisLabelRenderer.paint(gc, transform, settings, width, height);
    }

    /**
     * 根据 GridType 选择 CoordinateSystem 实现
     */
    private CoordinateSystem selectSystem(GridType gridType) {
        if (gridType == GridType.POLAR) {
            return polarSystem;
        } else if (gridType == GridType.ISOMETRIC) {
            return isometricSystem;
        }
        return cartesianSystem; // CARTESIAN, CARTESIAN_WITH_SUBGRID, DOT
    }

    /**
     * 获取当前坐标系是否锁定轴比例
     *
     * @return true 如果轴比例不可自由调整
     */
    public boolean isAxesRatioLocked() {
        return currentSystem != null && currentSystem.isAxesRatioLocked();
    }
}
