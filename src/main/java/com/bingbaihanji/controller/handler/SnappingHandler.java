package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.EdgeSnapManager;
import com.bingbaihanji.util.SpecialPointManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;

import java.util.List;

/**
 * 磁性吸附处理器
 * <p>
 * 处理特殊点和几何图形边的磁性吸附功能
 * - 点吸附：阈值为 15 像素（优先级高）
 * - 边吸附：阈值为 10 像素（优先级低）
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
        double threshold = POINT_SNAP_THRESHOLD_PIXELS / scale;

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, specialPoints, threshold);
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
        // 计算吸附阈值（像素距离转换为世界坐标距离）
        double scale = context.getTransform().getScale();
        double threshold = EDGE_SNAP_THRESHOLD_PIXELS / scale;
        
        // 查找最近的边
        return EdgeSnapManager.findNearestEdge(x, y, context.getObjects(), threshold);
    }

    /**
     * 应用吸附逻辑（点优先，其次边）
     * <p>
     * 1. 如果在阈值范围内有特殊点，返回该特殊点的坐标（优先级高）
     * 2. 如果没有点，尝试吸附到最近的边（优先级低）
     * 3. 都没有则返回原坐标
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
        
        // 3. 如果都没有，返回原始坐标
        return new double[]{x, y};
    }
}
