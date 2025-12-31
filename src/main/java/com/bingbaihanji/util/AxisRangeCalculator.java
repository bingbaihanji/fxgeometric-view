package com.bingbaihanji.util;

import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 坐标轴范围计算器
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 提供坐标轴范围的自动计算和调整功能
 */
public class AxisRangeCalculator {

    /**
     * 根据所有对象计算合适的视图范围
     *
     * @param objects   所有几何对象
     * @param transform 坐标变换对象
     * @param viewWidth 视图宽度
     * @param viewHeight 视图高度
     * @return 包含xMin, xMax, yMin, yMax的数组
     */
    public static double[] fitAllObjects(List<WorldObject> objects,
                                          WorldTransform transform,
                                          double viewWidth,
                                          double viewHeight) {
        if (objects.isEmpty()) {
            // 没有对象时，返回标准范围
            return getStandardRange();
        }

        // 初始化边界值
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        // 遍历所有对象，计算边界框
        for (WorldObject obj : objects) {
            double[] bounds = obj.getBoundingBox();
            if (bounds != null && bounds.length == 4) {
                minX = Math.min(minX, bounds[0]);
                maxX = Math.max(maxX, bounds[1]);
                minY = Math.min(minY, bounds[2]);
                maxY = Math.max(maxY, bounds[3]);
            }
        }

        // 检查是否找到有效边界
        if (Double.isInfinite(minX) || Double.isInfinite(maxX) ||
            Double.isInfinite(minY) || Double.isInfinite(maxY)) {
            return getStandardRange();
        }

        // 添加10%的边距
        double xRange = maxX - minX;
        double yRange = maxY - minY;
        double xMargin = xRange * 0.1;
        double yMargin = yRange * 0.1;

        minX -= xMargin;
        maxX += xMargin;
        minY -= yMargin;
        maxY += yMargin;

        // 确保最小范围（避免过小的范围）
        if (xRange < 2.0) {
            double center = (minX + maxX) / 2;
            minX = center - 1.0;
            maxX = center + 1.0;
        }
        if (yRange < 2.0) {
            double center = (minY + maxY) / 2;
            minY = center - 1.0;
            maxY = center + 1.0;
        }

        return new double[]{minX, maxX, minY, maxY};
    }

    /**
     * 获取标准视图范围（原点居中，-10到10）
     *
     * @return 包含xMin, xMax, yMin, yMax的数组
     */
    public static double[] getStandardRange() {
        return new double[]{-10.0, 10.0, -10.0, 10.0};
    }

    /**
     * 根据视图尺寸自动计算合适的范围
     *
     * @param scale      当前缩放比例
     * @param viewWidth  视图宽度
     * @param viewHeight 视图高度
     * @return 包含xMin, xMax, yMin, yMax的数组
     */
    public static double[] calculateAutoRange(double scale, double viewWidth, double viewHeight) {
        double xRange = viewWidth / scale;
        double yRange = viewHeight / scale;

        return new double[]{
            -xRange / 2,  // xMin
            xRange / 2,   // xMax
            -yRange / 2,  // yMin
            yRange / 2    // yMax
        };
    }

    /**
     * 根据缩放百分比计算新的缩放比例
     *
     * @param currentScale 当前缩放比例
     * @param percent      目标百分比（如100表示100%）
     * @return 新的缩放比例
     */
    public static double getScaleFromPercent(double currentScale, double percent) {
        // 假设50像素/单位为100%
        final double STANDARD_SCALE = 50.0;
        return STANDARD_SCALE * (percent / 100.0);
    }

    /**
     * 计算当前缩放百分比
     *
     * @param currentScale 当前缩放比例
     * @return 缩放百分比
     */
    public static double getPercentFromScale(double currentScale) {
        final double STANDARD_SCALE = 50.0;
        return (currentScale / STANDARD_SCALE) * 100.0;
    }
}
