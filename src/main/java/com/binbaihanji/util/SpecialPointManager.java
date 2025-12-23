package com.binbaihanji.util;

import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import com.binbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.InfiniteLineGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.LineGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 特殊点管理器
 * 用于收集和管理需要磁性吸附的特殊点（圆心、线段端点、交点等）
 */
public class SpecialPointManager {
    
    /**
     * 特殊点类
     */
    public static class SpecialPoint {
        private final double x;
        private final double y;
        private final String type; // 点的类型：CENTER(圆心)、ENDPOINT(端点)、INTERSECTION(交点)等
        
        public SpecialPoint(double x, double y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SpecialPoint that = (SpecialPoint) obj;
            // 考虑到浮点数精度问题，使用一个小的容差值进行比较
            return Math.abs(that.x - x) < 1e-10 && Math.abs(that.y - y) < 1e-10;
        }
        
        @Override
        public int hashCode() {
            // 为了处理浮点数精度问题，我们将坐标四舍五入到一定精度
            long xBits = Double.doubleToLongBits(Math.round(x * 1e10) / 1e10);
            long yBits = Double.doubleToLongBits(Math.round(y * 1e10) / 1e10);
            return (int) (xBits * 31 + yBits);
        }
    }
    
    /**
     * 从现有的图形对象中提取所有特殊点
     * @param objects 图形对象列表
     * @return 特殊点列表
     */
    public static List<SpecialPoint> extractSpecialPoints(List<WorldObject> objects) {
        Set<SpecialPoint> specialPointsSet = new HashSet<>();
        
        // 收集圆、线段端点和独立点
        for (WorldObject obj : objects) {
            if (obj instanceof CircleGeo circle) {
                // 添加圆心点
                specialPointsSet.add(new SpecialPoint(circle.getCx(), circle.getCy(), "CENTER"));
            } else if (obj instanceof LineGeo line) {
                // 添加线段的两个端点
                specialPointsSet.add(new SpecialPoint(line.getStartX(), line.getStartY(), "ENDPOINT"));
                specialPointsSet.add(new SpecialPoint(line.getEndX(), line.getEndY(), "ENDPOINT"));
            } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                // 添加无限直线的两个定义点
                specialPointsSet.add(new SpecialPoint(infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(), "ENDPOINT"));
                specialPointsSet.add(new SpecialPoint(infiniteLine.getPoint2X(), infiniteLine.getPoint2Y(), "ENDPOINT"));
            } else if (obj instanceof PointGeo point) {
                // 添加点对象的坐标（包括交点）
                specialPointsSet.add(new SpecialPoint(point.getX(), point.getY(), "INTERSECTION"));
            }
        }
        
        // 计算并添加所有交点
        // 注意：这里我们只添加通过计算得到的交点，不重复添加已经作为PointGeo存在的交点
        List<WorldObject> objectList = new ArrayList<>(objects);
        for (int i = 0; i < objectList.size(); i++) {
            WorldObject obj1 = objectList.get(i);
            for (int j = i + 1; j < objectList.size(); j++) {
                WorldObject obj2 = objectList.get(j);
                
                // 计算交点并添加到特殊点集合
                List<Point2D> intersections = calculateIntersections(obj1, obj2);
                for (Point2D point : intersections) {
                    specialPointsSet.add(new SpecialPoint(point.getX(), point.getY(), "INTERSECTION"));
                }
            }
        }
        
        // 转换为列表并返回
        return new ArrayList<>(specialPointsSet);
    }
    
    /**
     * 计算两个几何对象之间的交点
     * @param obj1 第一个几何对象
     * @param obj2 第二个几何对象
     * @return 交点列表
     */
    private static List<Point2D> calculateIntersections(WorldObject obj1, WorldObject obj2) {
        List<Point2D> intersections = new ArrayList<>();
        
        // 线段与线段的交点
        if (obj1 instanceof LineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineLineIntersections((LineGeo) obj1, (LineGeo) obj2));
        }
        // 线段与圆的交点
        else if (obj1 instanceof LineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj1, (CircleGeo) obj2));
        }
        else if (obj1 instanceof CircleGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj2, (CircleGeo) obj1));
        }
        // 圆与圆的交点
        else if (obj1 instanceof CircleGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getCircleCircleIntersections((CircleGeo) obj1, (CircleGeo) obj2));
        }
        // 无限直线与线段的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj1, (LineGeo) obj2));
        }
        else if (obj1 instanceof LineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj2, (LineGeo) obj1));
        }
        // 无限直线与圆的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj1, (CircleGeo) obj2));
        }
        else if (obj1 instanceof CircleGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj2, (CircleGeo) obj1));
        }
        // 无限直线与无限直线的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineInfiniteLineIntersections((InfiniteLineGeo) obj1, (InfiniteLineGeo) obj2));
        }
        
        return intersections;
    }
    
    /**
     * 查找最近的特殊点
     * @param x 当前鼠标x坐标（世界坐标）
     * @param y 当前鼠标y坐标（世界坐标）
     * @param specialPoints 特殊点列表
     * @param threshold 吸附阈值（世界坐标距离）
     * @return 最近的特殊点，如果没有找到则返回null
     */
    public static SpecialPoint findNearestSpecialPoint(double x, double y, List<SpecialPoint> specialPoints, double threshold) {
        SpecialPoint nearestPoint = null;
        double minDistance = Double.MAX_VALUE;
        
        for (SpecialPoint point : specialPoints) {
            double distance = Math.hypot(point.getX() - x, point.getY() - y);
            if (distance <= threshold && distance < minDistance) {
                minDistance = distance;
                nearestPoint = point;
            }
        }
        
        return nearestPoint;
    }
}