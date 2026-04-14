package com.bingbaihanji.strategy;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.SpecialPointManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;

import java.util.List;

/**
 * 点吸附策略
 * <p>
 * 优先级最高的吸附策略,吸附到特殊点(端点、顶点、圆心、交点等)
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class PointSnapStrategy implements SnapStrategy {

    @Override
    public double[] snap(double x, double y, DrawingContext context) {
        // 获取所有特殊点
        List<SpecialPoint> specialPoints = SpecialPointManager.extractSpecialPoints(context.getObjects());

        // 计算吸附阈值
        double scale = context.getTransform().getScale();
        double threshold = GeometryConfig.Snapping.POINT_SNAP_THRESHOLD_PIXELS / scale;

        // 查找最近的特殊点
        SpecialPoint nearestPoint = SpecialPointManager.findNearestSpecialPoint(x, y, specialPoints, threshold);

        if (nearestPoint != null) {
            return new double[]{nearestPoint.getX(), nearestPoint.getY()};
        }

        return null; // 未找到可吸附的点
    }

    @Override
    public int getPriority() {
        return 100; // 最高优先级
    }

    @Override
    public String getName() {
        return "PointSnap";
    }
}
