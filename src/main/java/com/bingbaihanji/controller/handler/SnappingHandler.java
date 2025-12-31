package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.SpecialPointManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;

import java.util.List;

/**
 * 磁性吸附处理器
 * <p>
 * 处理特殊点的磁性吸附功能（阈值为 15 像素）
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class SnappingHandler {

    /**
     * 吸附阈值（像素）
     */
    private static final double SNAP_THRESHOLD_PIXELS = 15.0;

    /**
     * 查找最近的特殊点
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 最近的特殊点，如果在阈值范围内没有则返回 null
     */
    public SpecialPoint findNearestSpecialPoint(double x, double y, DrawingContext context) {
        // 获取所有特殊点
        List<SpecialPoint> specialPoints = SpecialPointManager.extractSpecialPoints(context.getObjects());

        // 计算吸附阈值（像素距离转换为世界坐标距离）
        double scale = context.getTransform().getScale();
        double threshold = SNAP_THRESHOLD_PIXELS / scale;

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, specialPoints, threshold);
    }

    /**
     * 应用吸附逻辑
     * <p>
     * 如果在阈值范围内有特殊点，返回该特殊点的坐标；否则返回原坐标
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 吸附后的坐标数组 [x, y]
     */
    public double[] applySnapping(double x, double y, DrawingContext context) {
        SpecialPoint nearestPoint = findNearestSpecialPoint(x, y, context);
        if (nearestPoint != null) {
            return new double[]{nearestPoint.getX(), nearestPoint.getY()};
        }
        return new double[]{x, y};
    }
}
