package com.bingbaihanji.util;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 特殊点管理器
 * 用于收集和管理需要磁性吸附的特殊点(圆心、线段端点、交点等)
 */
public class SpecialPointManager {

    /**
     * 从现有的图形对象中提取所有特殊点
     *
     * @param objects 图形对象列表
     * @return 特殊点列表
     */
    public static List<SpecialPoint> extractSpecialPoints(List<WorldObject> objects) {
        Set<SpecialPoint> specialPointsSet = new HashSet<>();

        // 收集圆、线段端点、多边形顶点、手绘路径端点和独立点
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
            } else if (obj instanceof PolygonGeo polygon) {
                // 添加多边形的所有顶点
                int vertexCount = polygon.getVertexCount();
                for (int i = 0; i < vertexCount; i++) {
                    Point2D vertex = polygon.getVertex(i);
                    specialPointsSet.add(new SpecialPoint(vertex.getX(), vertex.getY(), "VERTEX"));
                }
            } else if (obj instanceof RegularPolygonGeo regularPolygon) {
                // 添加正多边形的所有顶点
                List<Point2D> vertices = regularPolygon.getVertices();
                for (Point2D vertex : vertices) {
                    specialPointsSet.add(new SpecialPoint(vertex.getX(), vertex.getY(), "VERTEX"));
                }
                // 添加正多边形的中心点
                specialPointsSet.add(new SpecialPoint(
                        regularPolygon.getCenterX(),
                        regularPolygon.getCenterY(),
                        "CENTER"
                ));
            } else if (obj instanceof PathGeo path) {
                // 添加手绘路径的起点和终点
                List<LineGeo> edges = path.getEdges();
                if (!edges.isEmpty()) {
                    LineGeo firstEdge = edges.get(0);
                    LineGeo lastEdge = edges.get(edges.size() - 1);
                    specialPointsSet.add(new SpecialPoint(firstEdge.getStartX(), firstEdge.getStartY(), "ENDPOINT"));
                    specialPointsSet.add(new SpecialPoint(lastEdge.getEndX(), lastEdge.getEndY(), "ENDPOINT"));
                }
            } else if (obj instanceof PointGeo point) {
                // 添加点对象的坐标(包括交点)
                specialPointsSet.add(new SpecialPoint(point.getX(), point.getY(), "INTERSECTION"));
            } else if (obj instanceof FunctionGeo function) {
                // 添加函数与坐标轴的交点
                specialPointsSet.addAll(extractFunctionAxisIntersections(function));
            }
        }

        // 计算并添加所有交点
        // 注意：这里我们只添加通过计算得到的交点,不重复添加已经作为PointGeo存在的交点
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
     * 提取函数与坐标轴的交点
     * <p>
     * 通过遍历采样点近似查找与x轴(y=0)和y轴(x=0)的交点
     *
     * @param function 函数对象
     * @return 与坐标轴的交点列表
     */
    private static List<SpecialPoint> extractFunctionAxisIntersections(FunctionGeo function) {
        List<SpecialPoint> intersections = new ArrayList<>();
        List<Point2D> sampledPoints = function.getSampledPoints();

        if (sampledPoints == null || sampledPoints.size() < 2) {
            return intersections;
        }

        // 遍历采样点,查找与x轴的交点(y=0)
        for (int i = 0; i < sampledPoints.size() - 1; i++) {
            Point2D p1 = sampledPoints.get(i);
            Point2D p2 = sampledPoints.get(i + 1);

            if (!isValidPoint(p1) || !isValidPoint(p2)) {
                continue;
            }

            // 检查y值符号变化(穿过x轴)
            if (p1.getY() * p2.getY() < 0) {
                // 线性插值找到交点
                double t = Math.abs(p1.getY()) / (Math.abs(p1.getY()) + Math.abs(p2.getY()));
                double xIntersect = p1.getX() + t * (p2.getX() - p1.getX());
                intersections.add(new SpecialPoint(xIntersect, 0, "AXIS_INTERSECTION"));
            }
            // 特殊情况：某个点恰好在x轴上
            else if (Math.abs(p1.getY()) < 1e-6) {
                intersections.add(new SpecialPoint(p1.getX(), 0, "AXIS_INTERSECTION"));
            }
        }

        // 对于显函数,查找与y轴的交点(x=0)
        double yAtZero = function.evaluate(0);
        if (Double.isFinite(yAtZero)) {
            // 检查x=0是否在定义域内
            double domainMin = function.getDomainMin();
            double domainMax = function.getDomainMax();
            if (0 >= domainMin && 0 <= domainMax) {
                intersections.add(new SpecialPoint(0, yAtZero, "AXIS_INTERSECTION"));
            }
        }

        return intersections;
    }

    /**
     * 检查点是否有效
     */
    private static boolean isValidPoint(Point2D p) {
        return p != null && Double.isFinite(p.getX()) && Double.isFinite(p.getY());
    }

    /**
     * 计算两个几何对象之间的交点
     *
     * @param obj1 第一个几何对象
     * @param obj2 第二个几何对象
     * @return 交点列表
     */
    private static List<Point2D> calculateIntersections(WorldObject obj1, WorldObject obj2) {
        List<Point2D> intersections = new ArrayList<>();

        // 如果涉及函数对象,先检查采样点是否有效
        if (obj1 instanceof FunctionGeo) {
            FunctionGeo func = (FunctionGeo) obj1;
            if (func.getSampledPoints() == null || func.getSampledPoints().isEmpty()) {
                return intersections; // 采样点未生成,跳过
            }
        }
        if (obj2 instanceof FunctionGeo) {
            FunctionGeo func = (FunctionGeo) obj2;
            if (func.getSampledPoints() == null || func.getSampledPoints().isEmpty()) {
                return intersections; // 采样点未生成,跳过
            }
        }

        // 线段与线段的交点
        if (obj1 instanceof LineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineLineIntersections((LineGeo) obj1, (LineGeo) obj2));
        }
        // 线段与圆的交点
        else if (obj1 instanceof LineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj2, (CircleGeo) obj1));
        }
        // 圆与圆的交点
        else if (obj1 instanceof CircleGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getCircleCircleIntersections((CircleGeo) obj1, (CircleGeo) obj2));
        }
        // 无限直线与线段的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj1, (LineGeo) obj2));
        } else if (obj1 instanceof LineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj2, (LineGeo) obj1));
        }
        // 无限直线与圆的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj2, (CircleGeo) obj1));
        }
        // 无限直线与无限直线的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineInfiniteLineIntersections((InfiniteLineGeo) obj1, (InfiniteLineGeo) obj2));
        }
        // 线段与函数的交点
        else if (obj1 instanceof LineGeo && obj2 instanceof FunctionGeo) {
            intersections.addAll(IntersectionUtils.getLineFunctionIntersections((LineGeo) obj1, (FunctionGeo) obj2));
        } else if (obj1 instanceof FunctionGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineFunctionIntersections((LineGeo) obj2, (FunctionGeo) obj1));
        }
        // 无限直线与函数的交点
        else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof FunctionGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineFunctionIntersections((InfiniteLineGeo) obj1, (FunctionGeo) obj2));
        } else if (obj1 instanceof FunctionGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineFunctionIntersections((InfiniteLineGeo) obj2, (FunctionGeo) obj1));
        }
        // 圆与函数的交点
        else if (obj1 instanceof CircleGeo && obj2 instanceof FunctionGeo) {
            intersections.addAll(IntersectionUtils.getCircleFunctionIntersections((CircleGeo) obj1, (FunctionGeo) obj2));
        } else if (obj1 instanceof FunctionGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getCircleFunctionIntersections((CircleGeo) obj2, (FunctionGeo) obj1));
        }
        // 多边形与函数的交点(将多边形的每条边与函数求交点)
        else if (obj1 instanceof PolygonGeo && obj2 instanceof FunctionGeo) {
            PolygonGeo polygon = (PolygonGeo) obj1;
            FunctionGeo function = (FunctionGeo) obj2;
            for (LineGeo edge : polygon.getEdges()) {
                intersections.addAll(IntersectionUtils.getLineFunctionIntersections(edge, function));
            }
        } else if (obj1 instanceof FunctionGeo && obj2 instanceof PolygonGeo) {
            PolygonGeo polygon = (PolygonGeo) obj2;
            FunctionGeo function = (FunctionGeo) obj1;
            for (LineGeo edge : polygon.getEdges()) {
                intersections.addAll(IntersectionUtils.getLineFunctionIntersections(edge, function));
            }
        }
        // 正多边形与函数的交点(将正多边形的每条边与函数求交点)
        else if (obj1 instanceof RegularPolygonGeo && obj2 instanceof FunctionGeo) {
            RegularPolygonGeo regularPolygon = (RegularPolygonGeo) obj1;
            FunctionGeo function = (FunctionGeo) obj2;
            for (LineGeo edge : regularPolygon.getEdges()) {
                intersections.addAll(IntersectionUtils.getLineFunctionIntersections(edge, function));
            }
        } else if (obj1 instanceof FunctionGeo && obj2 instanceof RegularPolygonGeo) {
            RegularPolygonGeo regularPolygon = (RegularPolygonGeo) obj2;
            FunctionGeo function = (FunctionGeo) obj1;
            for (LineGeo edge : regularPolygon.getEdges()) {
                intersections.addAll(IntersectionUtils.getLineFunctionIntersections(edge, function));
            }
        }
        // 函数与函数的交点
        else if (obj1 instanceof FunctionGeo && obj2 instanceof FunctionGeo) {
            intersections.addAll(IntersectionUtils.getFunctionFunctionIntersections((FunctionGeo) obj1, (FunctionGeo) obj2));
        }

        return intersections;
    }

    /**
     * 查找最近的特殊点
     *
     * @param x             当前鼠标x坐标(世界坐标)
     * @param y             当前鼠标y坐标(世界坐标)
     * @param specialPoints 特殊点列表
     * @param threshold     吸附阈值(世界坐标距离)
     * @return 最近的特殊点,如果没有找到则返回null
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
            // 考虑到浮点数精度问题,使用一个小的容差值进行比较
            return MathCalculationUtils.equals(that.x, x) && MathCalculationUtils.equals(that.y, y);
        }

        @Override
        public int hashCode() {
            // 使用与equals一致的精度进行舍入,保证equals为true的两个对象hashCode相同
            double eps = GeometryConfig.Performance.EPSILON;
            long xBits = Double.doubleToLongBits(Math.round(x / eps) * eps);
            long yBits = Double.doubleToLongBits(Math.round(y / eps) * eps);
            return (int) (xBits * 31 + yBits);
        }
    }
}