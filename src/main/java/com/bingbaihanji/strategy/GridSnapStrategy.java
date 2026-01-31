package com.bingbaihanji.strategy;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.DrawingContext;

/**
 * 网格吸附策略
 * <p>
 * 低优先级的吸附策略，吸附到网格交点
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class GridSnapStrategy implements SnapStrategy {

    private final double gridSize;

    /**
     * 构造函数
     *
     * @param gridSize 网格大小（世界坐标）
     */
    public GridSnapStrategy(double gridSize) {
        this.gridSize = gridSize;
    }

    @Override
    public double[] snap(double x, double y, DrawingContext context) {
        double scale = context.getTransform().getScale();
        double threshold = GeometryConfig.Snapping.GRID_SNAP_THRESHOLD_PIXELS / scale;

        // 计算最近的网格点
        double gridX = Math.round(x / gridSize) * gridSize;
        double gridY = Math.round(y / gridSize) * gridSize;

        // 检查距离是否在阈值内
        double distance = Math.hypot(gridX - x, gridY - y);
        if (distance < threshold) {
            return new double[]{gridX, gridY};
        }

        return null; // 距离网格点太远，不吸附
    }

    @Override
    public int getPriority() {
        return 10; // 低优先级
    }

    @Override
    public String getName() {
        return "GridSnap";
    }
}
