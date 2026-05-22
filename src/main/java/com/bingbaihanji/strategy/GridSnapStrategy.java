package com.bingbaihanji.strategy;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.AxisTickCalculator;

/**
 * 网格吸附策略
 * <p>
 * 低优先级的吸附策略,吸附到网格交点。
 * 网格精度随坐标轴刻度动态变化。
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class GridSnapStrategy implements SnapStrategy {

    @Override
    public double[] snap(double x, double y, IDrawingContext context) {
        double scale = context.getTransform().getScale();
        double threshold = GeometryConfig.Snapping.GRID_SNAP_THRESHOLD_PIXELS / scale;

        // 使用统一的刻度计算器,确保网格吸附精度与坐标轴刻度一致
        double step = AxisTickCalculator.calculateAxisTickDistance(scale, false);

        // 计算最近的网格点
        double gridX = Math.round(x / step) * step;
        double gridY = Math.round(y / step) * step;

        // 检查距离是否在阈值内
        double distance = Math.hypot(gridX - x, gridY - y);
        if (distance < threshold) {
            // 消除浮点误差
            if (Math.abs(gridX - Math.round(gridX)) < 1e-9) gridX = Math.round(gridX);
            if (Math.abs(gridY - Math.round(gridY)) < 1e-9) gridY = Math.round(gridY);
            return new double[]{gridX, gridY};
        }

        return null;
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
