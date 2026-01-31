package com.bingbaihanji.util;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 点复用管理器
 * <p>
 * 用于查找位置附近是否已存在点对象，实现点的复用逻辑
 * 避免在同一位置创建多个点
 * <p>
 * 还支持复用组功能：当多个点重合时，可以启用复用使它们同步移动
 *
 * @author bingbaihanji
 * @date 2025-01-01
 */
public class PointReuseManager {

    /**
     * 查找位置附近的已存在点
     *
     * @param x         世界坐标X
     * @param y         世界坐标Y
     * @param objects   所有对象列表
     * @param threshold 检测阈值（世界坐标距离）
     * @return 找到的点对象，如果没有则返回null
     */
    public static PointGeo findExistingPoint(double x, double y, List<WorldObject> objects, double threshold) {
        PointGeo nearest = null;
        double minDist = threshold;

        for (WorldObject obj : objects) {
            if (obj instanceof PointGeo point) {
                double dist = MathCalculationUtils.hypot(point.getX() - x, point.getY() - y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = point;
                }
            }
        }

        return nearest;
    }

    /**
     * 检测位置是否已有点存在（包括图形的顶点/端点/圆心等）
     *
     * @param x         世界坐标X
     * @param y         世界坐标Y
     * @param objects   所有对象列表
     * @param threshold 检测阈值
     * @return true 如果该位置已有点
     */
    public static boolean hasPointAt(double x, double y, List<WorldObject> objects, double threshold) {
        return findExistingPoint(x, y, objects, threshold) != null;
    }

    /**
     * 获取或创建点
     * <p>
     * 如果位置附近已有点，返回该点；否则返回null表示需要创建新点
     *
     * @param x       世界坐标X
     * @param y       世界坐标Y
     * @param objects 所有对象列表
     * @param scale   当前缩放比例
     * @return 已存在的点，或null
     */
    public static PointGeo getExistingPointOrNull(double x, double y, List<WorldObject> objects, double scale) {
        double threshold = 10.0 / scale; // 10像素的检测范围
        return findExistingPoint(x, y, objects, threshold);
    }

    /**
     * 查找位置附近的特殊位置（点、端点、顶点、圆心等）
     *
     * @param x         世界坐标X
     * @param y         世界坐标Y
     * @param objects   所有对象列表
     * @param threshold 检测阈值
     * @return 最近的特殊位置坐标，如果没有则返回null
     */
    public static Point2D findNearestSpecialPosition(double x, double y, List<WorldObject> objects, double threshold) {
        Point2D nearest = null;
        double minDist = threshold;

        for (WorldObject obj : objects) {
            // 检查独立点
            if (obj instanceof PointGeo point) {
                double dist = MathCalculationUtils.hypot(point.getX() - x, point.getY() - y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = new Point2D(point.getX(), point.getY());
                }
            }
            // 检查线段端点
            else if (obj instanceof LineGeo line) {
                double dist1 = MathCalculationUtils.hypot(line.getStartX() - x, line.getStartY() - y);
                double dist2 = MathCalculationUtils.hypot(line.getEndX() - x, line.getEndY() - y);
                if (dist1 < minDist) {
                    minDist = dist1;
                    nearest = new Point2D(line.getStartX(), line.getStartY());
                }
                if (dist2 < minDist) {
                    minDist = dist2;
                    nearest = new Point2D(line.getEndX(), line.getEndY());
                }
            }
            // 检查圆心
            else if (obj instanceof CircleGeo circle) {
                double dist = MathCalculationUtils.hypot(circle.getCx() - x, circle.getCy() - y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = new Point2D(circle.getCx(), circle.getCy());
                }
            }
            // 检查多边形顶点
            else if (obj instanceof PolygonGeo polygon) {
                for (int i = 0; i < polygon.getVertexCount(); i++) {
                    Point2D vertex = polygon.getVertex(i);
                    double dist = MathCalculationUtils.hypot(vertex.getX() - x, vertex.getY() - y);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = vertex;
                    }
                }
            }
            // 检查无限直线的定义点
            else if (obj instanceof InfiniteLineGeo infLine) {
                double dist1 = MathCalculationUtils.hypot(infLine.getPoint1X() - x, infLine.getPoint1Y() - y);
                double dist2 = MathCalculationUtils.hypot(infLine.getPoint2X() - x, infLine.getPoint2Y() - y);
                if (dist1 < minDist) {
                    minDist = dist1;
                    nearest = new Point2D(infLine.getPoint1X(), infLine.getPoint1Y());
                }
                if (dist2 < minDist) {
                    minDist = dist2;
                    nearest = new Point2D(infLine.getPoint2X(), infLine.getPoint2Y());
                }
            }
        }

        return nearest;
    }

    // ==================== 复用组管理功能 ====================

    /**
     * 查找与指定点重合的其他点
     *
     * @param point     目标点
     * @param objects   所有对象列表
     * @param threshold 检测阈值
     * @return 重合的点列表（不包含目标点本身）
     */
    public static List<PointGeo> findOverlappingPoints(PointGeo point, List<WorldObject> objects, double threshold) {
        List<PointGeo> overlapping = new ArrayList<>();
        double px = point.getX();
        double py = point.getY();

        for (WorldObject obj : objects) {
            // 检查独立的 PointGeo 对象
            if (obj instanceof PointGeo other && other != point) {
                // 优化：跳过已经在同一复用组的点
                if (point.isInReuseGroup() && other.getReuseGroup() == point.getReuseGroup()) {
                    continue;
                }

                double dist = MathCalculationUtils.hypot(other.getX() - px, other.getY() - py);
                if (dist < threshold) {
                    overlapping.add(other);
                }
            }
            // 检查多边形的顶点
            else if (obj instanceof PolygonGeo polygon) {
                for (PointGeo vertex : polygon.getVertexPoints()) {
                    if (vertex != point) {
                        // 跳过已在同一复用组的点
                        if (point.isInReuseGroup() && vertex.getReuseGroup() == point.getReuseGroup()) {
                            continue;
                        }

                        double dist = MathCalculationUtils.hypot(vertex.getX() - px, vertex.getY() - py);
                        if (dist < threshold) {
                            overlapping.add(vertex);
                        }
                    }
                }
            }
            // 检查线段的端点（如果它们是独立的 PointGeo）
            else if (obj instanceof LineGeo line) {
                PointGeo startRef = line.getStartPointRef();
                PointGeo endRef = line.getEndPointRef();

                if (startRef != null && startRef != point) {
                    if (!(point.isInReuseGroup() && startRef.getReuseGroup() == point.getReuseGroup())) {
                        double dist = MathCalculationUtils.hypot(startRef.getX() - px, startRef.getY() - py);
                        if (dist < threshold && !overlapping.contains(startRef)) {
                            overlapping.add(startRef);
                        }
                    }
                }

                if (endRef != null && endRef != point) {
                    if (!(point.isInReuseGroup() && endRef.getReuseGroup() == point.getReuseGroup())) {
                        double dist = MathCalculationUtils.hypot(endRef.getX() - px, endRef.getY() - py);
                        if (dist < threshold && !overlapping.contains(endRef)) {
                            overlapping.add(endRef);
                        }
                    }
                }
            }
            // 检查圆的圆心（如果它是独立的 PointGeo）
            else if (obj instanceof CircleGeo circle) {
                PointGeo centerRef = circle.getCenterPointRef();

                if (centerRef != null && centerRef != point) {
                    if (!(point.isInReuseGroup() && centerRef.getReuseGroup() == point.getReuseGroup())) {
                        double dist = MathCalculationUtils.hypot(centerRef.getX() - px, centerRef.getY() - py);
                        if (dist < threshold && !overlapping.contains(centerRef)) {
                            overlapping.add(centerRef);
                        }
                    }
                }
            }
            // 检查无限直线的定义点（如果它们是独立的 PointGeo）
            else if (obj instanceof InfiniteLineGeo infLine) {
                PointGeo point1Ref = infLine.getPoint1Ref();
                PointGeo point2Ref = infLine.getPoint2Ref();

                if (point1Ref != null && point1Ref != point) {
                    if (!(point.isInReuseGroup() && point1Ref.getReuseGroup() == point.getReuseGroup())) {
                        double dist = MathCalculationUtils.hypot(point1Ref.getX() - px, point1Ref.getY() - py);
                        if (dist < threshold && !overlapping.contains(point1Ref)) {
                            overlapping.add(point1Ref);
                        }
                    }
                }

                if (point2Ref != null && point2Ref != point) {
                    if (!(point.isInReuseGroup() && point2Ref.getReuseGroup() == point.getReuseGroup())) {
                        double dist = MathCalculationUtils.hypot(point2Ref.getX() - px, point2Ref.getY() - py);
                        if (dist < threshold && !overlapping.contains(point2Ref)) {
                            overlapping.add(point2Ref);
                        }
                    }
                }
            }
        }

        return overlapping;
    }

    /**
     * 启用两个点之间的复用关系
     *
     * @param point1 点1
     * @param point2 点2
     * @return 创建或获取的复用组
     */
    public static PointReuseGroup enableReuse(PointGeo point1, PointGeo point2) {
        // 如果两个点已经在同一复用组，直接返回
        if (point1.getReuseGroup() == point2.getReuseGroup() && point1.getReuseGroup() != null) {
            return point1.getReuseGroup();
        }

        return PointReuseGroup.getManager().createGroup(point1, point2);
    }

    /**
     * 禁用点的复用关系
     *
     * @param point 要移除复用的点
     */
    public static void disableReuse(PointGeo point) {
        PointReuseGroup group = point.getReuseGroup();
        if (group != null) {
            group.removeMember(point);
        }
    }

    /**
     * 检查点是否可以启用复用（附近是否有其他点）
     *
     * @param point     目标点
     * @param objects   所有对象列表
     * @param threshold 检测阈值
     * @return 是否有可复用的重合点
     */
    public static boolean canEnableReuse(PointGeo point, List<WorldObject> objects, double threshold) {
        return !findOverlappingPoints(point, objects, threshold).isEmpty();
    }

    /**
     * 检查点是否已在复用组中
     *
     * @param point 目标点
     * @return 是否在复用组中
     */
    public static boolean isInReuseGroup(PointGeo point) {
        return point.getReuseGroup() != null;
    }
}
