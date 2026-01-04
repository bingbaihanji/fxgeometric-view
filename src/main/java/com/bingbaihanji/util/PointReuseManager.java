package com.bingbaihanji.util;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * 点复用管理器
 * <p>
 * 用于查找位置附近是否已存在点对象，实现点的复用逻辑
 * 避免在同一位置创建多个点
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
                double dist = Math.hypot(point.getX() - x, point.getY() - y);
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
                double dist = Math.hypot(point.getX() - x, point.getY() - y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = new Point2D(point.getX(), point.getY());
                }
            }
            // 检查线段端点
            else if (obj instanceof LineGeo line) {
                double dist1 = Math.hypot(line.getStartX() - x, line.getStartY() - y);
                double dist2 = Math.hypot(line.getEndX() - x, line.getEndY() - y);
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
                double dist = Math.hypot(circle.getCx() - x, circle.getCy() - y);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = new Point2D(circle.getCx(), circle.getCy());
                }
            }
            // 检查多边形顶点
            else if (obj instanceof PolygonGeo polygon) {
                for (int i = 0; i < polygon.getVertexCount(); i++) {
                    Point2D vertex = polygon.getVertex(i);
                    double dist = Math.hypot(vertex.getX() - x, vertex.getY() - y);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = vertex;
                    }
                }
            }
            // 检查无限直线的定义点
            else if (obj instanceof InfiniteLineGeo infLine) {
                double dist1 = Math.hypot(infLine.getPoint1X() - x, infLine.getPoint1Y() - y);
                double dist2 = Math.hypot(infLine.getPoint2X() - x, infLine.getPoint2Y() - y);
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
}
