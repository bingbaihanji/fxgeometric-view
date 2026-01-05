package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.util.EdgeSnapManager;
import com.bingbaihanji.util.SpecialPointManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 磁性吸附处理器
 * <p>
 * 处理特殊点、几何图形边和网格点的磁性吸附功能
 * - 点吸附：阈值为 15 像素（优先级高）
 * - 边吸附：阈值为 10 像素（优先级中）
 * - 网格吸附：阈值为 8 像素（优先级低）
 * <p>
 * 优化：使用缓存机制避免重复计算特殊点，提高拖动性能
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class SnappingHandler {

    /**
     * 点吸附阈值（像素）
     */
    private static final double POINT_SNAP_THRESHOLD_PIXELS = 15.0;

    /**
     * 边吸附阈值（像素）- 比点吸附弱一点
     */
    private static final double EDGE_SNAP_THRESHOLD_PIXELS = 10.0;

    /**
     * 网格吸附阈值（像素）
     */
    private static final double GRID_SNAP_THRESHOLD_PIXELS = 8.0;

    /**
     * 特殊点缓存
     */
    private List<SpecialPoint> cachedSpecialPoints = null;

    /**
     * 缓存是否有效
     */
    private boolean cacheValid = false;

    /**
     * 使缓存失效（当几何对象发生变化时调用）
     */
    public void invalidateCache() {
        cacheValid = false;
        cachedSpecialPoints = null;
    }

    /**
     * 查找最近的特殊点（带缓存优化）
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 最近的特殊点，如果在阈值范围内没有则返回 null
     */
    public SpecialPoint findNearestSpecialPoint(double x, double y, DrawingContext context) {
        return findNearestSpecialPoint(x, y, context, null);
    }

    /**
     * 查找最近的特殊点（带排除对象）
     *
     * @param x              世界坐标 X
     * @param y              世界坐标 Y
     * @param context        绘制上下文
     * @param excludedObject 要排除的对象（通常是正在拖动的对象）
     * @return 最近的特殊点，如果在阈值范围内没有则返回 null
     */
    public SpecialPoint findNearestSpecialPoint(double x, double y, DrawingContext context, WorldObject excludedObject) {
        // 如果缓存无效，重新提取特殊点
        if (!cacheValid || cachedSpecialPoints == null) {
            cachedSpecialPoints = SpecialPointManager.extractSpecialPoints(context.getObjects());
            cacheValid = true;
        }

        // 计算吸附阈值（像素距离转换为世界坐标距离）
        double scale = context.getTransform().getScale();
        double threshold = POINT_SNAP_THRESHOLD_PIXELS / scale;

        // 如果需要排除某个对象，过滤掉该对象的特殊点
        List<SpecialPoint> pointsToCheck = cachedSpecialPoints;
        if (excludedObject != null) {
            pointsToCheck = SpecialPointManager.extractSpecialPoints(context.getObjects());
            // 移除被排除对象的特殊点
            List<SpecialPoint> excludedPoints = SpecialPointManager.extractSpecialPoints(List.of(excludedObject));
            pointsToCheck.removeAll(excludedPoints);
        }

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, pointsToCheck, threshold);
    }

    /**
     * 查找最近的边吸附点
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 最近的边吸附结果，如果在阈值范围内没有则返回 null
     */
    public EdgeSnapManager.EdgeSnapResult findNearestEdge(double x, double y, DrawingContext context) {
        return findNearestEdge(x, y, context, null);
    }

    /**
     * 查找最近的边吸附点（带排除对象）
     *
     * @param x              世界坐标 X
     * @param y              世界坐标 Y
     * @param context        绘制上下文
     * @param excludedObject 要排除的对象（通常是正在拖动的对象）
     * @return 最近的边吸附结果，如果在阈值范围内没有则返回 null
     */
    public EdgeSnapManager.EdgeSnapResult findNearestEdge(double x, double y, DrawingContext context, WorldObject excludedObject) {
        // 计算吸附阈值（像素距离转换为世界坐标距离）
        double scale = context.getTransform().getScale();
        double threshold = EDGE_SNAP_THRESHOLD_PIXELS / scale;

        // 获取要检查的对象列表
        List<WorldObject> objectsToCheck = context.getObjects();
        if (excludedObject != null) {
            objectsToCheck = context.getObjects().stream()
                    .filter(obj -> obj != excludedObject)
                    .toList();
        }

        // 查找最近的边
        return EdgeSnapManager.findNearestEdge(x, y, objectsToCheck, threshold);
    }

    /**
     * 应用吸附逻辑（点优先，其次边，最后网格）
     * <p>
     * 1. 如果在阈值范围内有特殊点，返回该特殊点的坐标（优先级高）
     * 2. 如果没有点，尝试吸附到最近的边（优先级中）
     * 3. 如果启用了网格吸附，尝试吸附到网格点（优先级低）
     * 4. 都没有则返回原坐标
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 吸附后的坐标数组 [x, y]
     */
    public double[] applySnapping(double x, double y, DrawingContext context) {
        // 1. 优先尝试点吸附
        SpecialPoint nearestPoint = findNearestSpecialPoint(x, y, context);
        if (nearestPoint != null) {
            return new double[]{nearestPoint.getX(), nearestPoint.getY()};
        }

        // 2. 如果没有点吸附，尝试边吸附
        EdgeSnapManager.EdgeSnapResult edgeSnap = findNearestEdge(x, y, context);
        if (edgeSnap != null) {
            return new double[]{edgeSnap.getX(), edgeSnap.getY()};
        }

        // 3. 如果启用了网格吸附，尝试吸附到网格点
        EuclidianViewSettings settings = context.getGridChartPane().getSettings();
        if (settings.isGridSnapEnabled()) {
            double[] gridSnapped = snapToGrid(x, y, context);
            if (gridSnapped != null) {
                return gridSnapped;
            }
        }

        // 4. 如果都没有，返回原始坐标
        return new double[]{x, y};
    }

    /**
     * 吸附到网格点
     * <p>
     * 网格精度随坐标轴刻度动态变化
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 吸附后的坐标，如果距离太远则返回 null
     */
    private double[] snapToGrid(double x, double y, DrawingContext context) {
        double scale = context.getTransform().getScale();
        double threshold = GRID_SNAP_THRESHOLD_PIXELS / scale;

        // 使用统一的刻度计算器，确保网格吸附精度与坐标轴刻度一致
        double step = AxisTickCalculator.calculateAxisTickDistance(scale, false);

        // 计算最近的网格点
        double gridX = Math.round(x / step) * step;
        double gridY = Math.round(y / step) * step;

        // 检查距离是否在阈值内
        double distance = Math.hypot(gridX - x, gridY - y);
        if (distance < threshold) {
            // 消除浮点误差
            gridX = stabilize(gridX);
            gridY = stabilize(gridY);
            return new double[]{gridX, gridY};
        }

        return null;
    }

    /**
     * 消除浮点抖动
     */
    private double stabilize(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) {
            return Math.round(v);
        }
        return v;
    }
}
