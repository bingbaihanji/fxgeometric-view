package com.bingbaihanji.util;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.EllipseGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.FunctionGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.InfiniteLineGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.LineGeo;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 几何图形交点计算工具类
 * 提供线段与线段、线段与圆、圆与圆、函数与函数、无限直线与其他图形之间的交点计算功能
 */
public class IntersectionUtils {

    /**
     * 计算两条直线/线段的底层交点
     *
     * @param clipFirst  true 时约束 t ∈ [0,1]（第一段为线段）
     * @param clipSecond true 时约束 u ∈ [0,1]（第二段为线段）
     * @return 交点列表
     */
    private static List<Point2D> computeLineIntersection(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4,
            boolean clipFirst, boolean clipSecond) {
        List<Point2D> intersections = new ArrayList<>();

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (MathCalculationUtils.isZero(denom, GeometryConfig.Performance.MIN_VALID_DISTANCE)) {
            return intersections;
        }

        double tNum = (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4);
        double uNum = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3));

        double t = tNum / denom;
        double u = uNum / denom;

        if ((clipFirst && (t < 0 || t > 1)) || (clipSecond && (u < 0 || u > 1))) {
            return intersections;
        }

        double ix = x1 + t * (x2 - x1);
        double iy = y1 + t * (y2 - y1);
        intersections.add(new Point2D(ix, iy));

        return intersections;
    }

    /**
     * 计算两个线段的交点
     *
     * @param line1 第一条线段
     * @param line2 第二条线段
     * @return 交点列表
     */
    public static List<Point2D> getLineLineIntersections(LineGeo line1, LineGeo line2) {
        return computeLineIntersection(
                line1.getStartX(), line1.getStartY(), line1.getEndX(), line1.getEndY(),
                line2.getStartX(), line2.getStartY(), line2.getEndX(), line2.getEndY(),
                true, true);
    }

    /**
     * 计算线段与圆的交点
     *
     * @param line   线段
     * @param circle 圆
     * @return 交点列表
     */
    public static List<Point2D> getLineCircleIntersections(LineGeo line, CircleGeo circle) {
        List<Point2D> intersections = new ArrayList<>();

        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        double cx = circle.getCx();
        double cy = circle.getCy();
        double r = circle.getR();

        // 将线段转换为参数方程: P(t) = P1 + t(P2-P1)
        double dx = x2 - x1;
        double dy = y2 - y1;

        // 圆的方程: (x-cx)^2 + (y-cy)^2 = r^2
        // 线段的参数方程代入圆的方程得到关于t的二次方程: at^2 + bt + c = 0

        double a = dx * dx + dy * dy;
        double b = 2 * (dx * (x1 - cx) + dy * (y1 - cy));
        double c = (x1 - cx) * (x1 - cx) + (y1 - cy) * (y1 - cy) - r * r;

        // 退化线段(起点=终点): 当作点与圆的交点处理
        if (MathCalculationUtils.isZero(a, 1e-10)) {
            double dist = MathCalculationUtils.hypot(x1 - cx, y1 - cy);
            if (MathCalculationUtils.isZero(dist - r, GeometryConfig.Performance.MIN_VALID_DISTANCE)) {
                intersections.add(new Point2D(x1, y1));
            }
            return intersections;
        }

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            // 没有实数解,线段与圆不相交
            return intersections;
        }

        if (MathCalculationUtils.isZero(discriminant, GeometryConfig.Performance.MIN_VALID_DISTANCE)) {
            // 一个解,线段与圆相切
            double t = -b / (2 * a);
            if (t >= 0 && t <= 1) {
                double ix = x1 + t * dx;
                double iy = y1 + t * dy;
                intersections.add(new Point2D(ix, iy));
            }
        } else {
            // 两个解,线段与圆相交于两点
            double sqrtDiscriminant = MathCalculationUtils.sqrt(discriminant);
            double t1 = (-b + sqrtDiscriminant) / (2 * a);
            double t2 = (-b - sqrtDiscriminant) / (2 * a);

            if (t1 >= 0 && t1 <= 1) {
                double ix = x1 + t1 * dx;
                double iy = y1 + t1 * dy;
                intersections.add(new Point2D(ix, iy));
            }

            if (t2 >= 0 && t2 <= 1) {
                double ix = x1 + t2 * dx;
                double iy = y1 + t2 * dy;
                intersections.add(new Point2D(ix, iy));
            }
        }

        return intersections;
    }

    /**
     * 计算两个圆的交点
     *
     * @param circle1 第一个圆
     * @param circle2 第二个圆
     * @return 交点列表
     */
    public static List<Point2D> getCircleCircleIntersections(CircleGeo circle1, CircleGeo circle2) {
        List<Point2D> intersections = new ArrayList<>();

        double x1 = circle1.getCx();
        double y1 = circle1.getCy();
        double r1 = circle1.getR();

        double x2 = circle2.getCx();
        double y2 = circle2.getCy();
        double r2 = circle2.getR();

        // 计算两圆心之间的距离
        double d = MathCalculationUtils.distance(x1, y1, x2, y2);

        // 检查特殊情况
        if (d > r1 + r2) {
            // 两圆相离
            return intersections;
        }
        if (d < Math.abs(r1 - r2)) {
            // 一圆在另一圆内部
            return intersections;
        }
        if (d == 0 && r1 == r2) {
            // 同心圆
            return intersections;
        }

        // 计算交点
        double a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
        double h = MathCalculationUtils.sqrt(r1 * r1 - a * a);

        // 计算P2点坐标
        double x3 = x1 + a * (x2 - x1) / d;
        double y3 = y1 + a * (y2 - y1) / d;

        // 计算交点坐标
        double ix1 = x3 + h * (y2 - y1) / d;
        double iy1 = y3 - h * (x2 - x1) / d;

        double ix2 = x3 - h * (y2 - y1) / d;
        double iy2 = y3 + h * (x2 - x1) / d;

        intersections.add(new Point2D(ix1, iy1));

        // 如果两交点不重合,则添加第二个交点
        if (!MathCalculationUtils.equals(ix1, ix2) || !MathCalculationUtils.equals(iy1, iy2)) {
            intersections.add(new Point2D(ix2, iy2));
        }

        return intersections;
    }

    /**
     * 计算无限直线与线段的交点
     *
     * @param infiniteLine 无限直线
     * @param line         线段
     * @return 交点列表
     */
    public static List<Point2D> getInfiniteLineLineIntersections(InfiniteLineGeo infiniteLine, LineGeo line) {
        return computeLineIntersection(
                infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(), infiniteLine.getPoint2X(), infiniteLine.getPoint2Y(),
                line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(),
                false, true);
    }

    /**
     * 计算无限直线与圆的交点
     *
     * @param infiniteLine 无限直线
     * @param circle       圆
     * @return 交点列表
     */
    public static List<Point2D> getInfiniteLineCircleIntersections(InfiniteLineGeo infiniteLine, CircleGeo circle) {
        List<Point2D> intersections = new ArrayList<>();

        double x1 = infiniteLine.getPoint1X();
        double y1 = infiniteLine.getPoint1Y();
        double x2 = infiniteLine.getPoint2X();
        double y2 = infiniteLine.getPoint2Y();

        double cx = circle.getCx();
        double cy = circle.getCy();
        double r = circle.getR();

        // 将直线转换为参数方程: P(t) = P1 + t(P2-P1)
        double dx = x2 - x1;
        double dy = y2 - y1;

        // 圆的方程: (x-cx)^2 + (y-cy)^2 = r^2
        // 直线的参数方程代入圆的方程得到关于t的二次方程: at^2 + bt + c = 0

        double a = dx * dx + dy * dy;
        double b = 2 * (dx * (x1 - cx) + dy * (y1 - cy));
        double c = (x1 - cx) * (x1 - cx) + (y1 - cy) * (y1 - cy) - r * r;

        // 退化情况(定义点重合): 当作点与圆的交点处理
        if (MathCalculationUtils.isZero(a, 1e-10)) {
            double dist = MathCalculationUtils.hypot(x1 - cx, y1 - cy);
            if (MathCalculationUtils.isZero(dist - r, GeometryConfig.Performance.MIN_VALID_DISTANCE)) {
                intersections.add(new Point2D(x1, y1));
            }
            return intersections;
        }

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            // 没有实数解,直线与圆不相交
            return intersections;
        }

        if (MathCalculationUtils.isZero(discriminant, GeometryConfig.Performance.MIN_VALID_DISTANCE)) {
            // 一个解,直线与圆相切
            double t = -b / (2 * a);
            double ix = x1 + t * dx;
            double iy = y1 + t * dy;
            intersections.add(new Point2D(ix, iy));
        } else {
            // 两个解,直线与圆相交于两点
            double sqrtDiscriminant = MathCalculationUtils.sqrt(discriminant);
            double t1 = (-b + sqrtDiscriminant) / (2 * a);
            double t2 = (-b - sqrtDiscriminant) / (2 * a);

            double ix1 = x1 + t1 * dx;
            double iy1 = y1 + t1 * dy;
            intersections.add(new Point2D(ix1, iy1));

            double ix2 = x1 + t2 * dx;
            double iy2 = y1 + t2 * dy;
            intersections.add(new Point2D(ix2, iy2));
        }

        return intersections;
    }

    /**
     * 计算两条无限直线的交点
     *
     * @param line1 第一条无限直线
     * @param line2 第二条无限直线
     * @return 交点列表
     */
    public static List<Point2D> getInfiniteLineInfiniteLineIntersections(InfiniteLineGeo line1, InfiniteLineGeo line2) {
        return computeLineIntersection(
                line1.getPoint1X(), line1.getPoint1Y(), line1.getPoint2X(), line1.getPoint2Y(),
                line2.getPoint1X(), line2.getPoint1Y(), line2.getPoint2X(), line2.getPoint2Y(),
                false, false);
    }

    /**
     * 计算线段或直线的中点
     *
     * @param x1 起点x坐标
     * @param y1 起点y坐标
     * @param x2 终点x坐标
     * @param y2 终点y坐标
     * @return 中点坐标
     */
    public static Point2D getMidpoint(double x1, double y1, double x2, double y2) {
        return MathCalculationUtils.midpoint(x1, y1, x2, y2);
    }

    /**
     * 计算过指定点垂直于给定直线的垂线的两个点(用于绘制无限直线)
     * 返回的两个点在垂线上,距离给定点足够远以绘制无限直线
     *
     * @param lineX1 原直线的第一个点x坐标
     * @param lineY1 原直线的第一个点y坐标
     * @param lineX2 原直线的第二个点x坐标
     * @param lineY2 原直线的第二个点y坐标
     * @param pointX 给定点x坐标
     * @param pointY 给定点y坐标
     * @return 垂线上的两个点 [point1, point2]
     */
    public static Point2D[] getPerpendicularLine(double lineX1, double lineY1, double lineX2, double lineY2,
                                                 double pointX, double pointY) {
        // 计算原直线的方向向量
        double dx = lineX2 - lineX1;
        double dy = lineY2 - lineY1;

        // 垂线的方向向量是 (-dy, dx)
        double perpDx = -dy;
        double perpDy = dx;

        // 归一化方向向量
        double[] normalized = MathCalculationUtils.normalize(perpDx, perpDy);
        perpDx = normalized[0];
        perpDy = normalized[1];

        // 生成垂线上的两个点(距离给定点足够远)
        double scale = GeometryConfig.LineStyle.INFINITE_LINE_EXTENSION_FACTOR; // 扩展距离
        Point2D point1 = new Point2D(pointX + perpDx * scale, pointY + perpDy * scale);
        Point2D point2 = new Point2D(pointX - perpDx * scale, pointY - perpDy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算垂直平分线的两个点(用于绘制无限直线)
     * 垂直平分线过线段中点且垂直于线段
     *
     * @param lineX1 线段起点x坐标
     * @param lineY1 线段起点y坐标
     * @param lineX2 线段终点x坐标
     * @param lineY2 线段终点y坐标
     * @param pointX 给定点x坐标(垂直平分线将过此点)
     * @param pointY 给定点y坐标
     * @return 垂直平分线上的两个点 [point1, point2]
     */
    public static Point2D[] getPerpendicularBisector(double lineX1, double lineY1, double lineX2, double lineY2,
                                                     double pointX, double pointY) {
        // 计算线段的方向向量
        double dx = lineX2 - lineX1;
        double dy = lineY2 - lineY1;

        // 垂直平分线的方向向量是 (-dy, dx)
        double perpDx = -dy;
        double perpDy = dx;

        // 归一化方向向量
        double[] normalized = MathCalculationUtils.normalize(perpDx, perpDy);
        perpDx = normalized[0];
        perpDy = normalized[1];

        // 生成垂直平分线上的两个点(从给定点出发)
        double scale = GeometryConfig.LineStyle.INFINITE_LINE_EXTENSION_FACTOR; // 扩展距离
        Point2D point1 = new Point2D(pointX + perpDx * scale, pointY + perpDy * scale);
        Point2D point2 = new Point2D(pointX - perpDx * scale, pointY - perpDy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算过指定点平行于给定直线的平行线的两个点(用于绘制无限直线)
     *
     * @param lineX1 原直线的第一个点x坐标
     * @param lineY1 原直线的第一个点y坐标
     * @param lineX2 原直线的第二个点x坐标
     * @param lineY2 原直线的第二个点y坐标
     * @param pointX 给定点x坐标
     * @param pointY 给定点y坐标
     * @return 平行线上的两个点 [point1, point2]
     */
    public static Point2D[] getParallelLine(double lineX1, double lineY1, double lineX2, double lineY2,
                                            double pointX, double pointY) {
        // 计算原直线的方向向量
        double dx = lineX2 - lineX1;
        double dy = lineY2 - lineY1;

        // 归一化方向向量
        double[] normalized = MathCalculationUtils.normalize(dx, dy);
        dx = normalized[0];
        dy = normalized[1];

        // 生成平行线上的两个点(距离给定点足够远)
        double scale = GeometryConfig.LineStyle.INFINITE_LINE_EXTENSION_FACTOR; // 扩展距离
        Point2D point1 = new Point2D(pointX + dx * scale, pointY + dy * scale);
        Point2D point2 = new Point2D(pointX - dx * scale, pointY - dy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算过圆上一点的切线的两个点(用于绘制无限直线)
     * 切线垂直于圆心到该点的半径
     *
     * @param cx     圆心x坐标
     * @param cy     圆心y坐标
     * @param pointX 圆上的点x坐标
     * @param pointY 圆上的点y坐标
     * @return 切线上的两个点 [point1, point2]
     */
    public static Point2D[] getTangentLine(double cx, double cy, double pointX, double pointY) {
        // 计算从圆心到切点的半径向量
        double dx = pointX - cx;
        double dy = pointY - cy;

        // 切线垂直于半径,方向向量为 (-dy, dx)
        double tangentDx = -dy;
        double tangentDy = dx;

        // 归一化方向向量
        double[] normalized = MathCalculationUtils.normalize(tangentDx, tangentDy);
        tangentDx = normalized[0];
        tangentDy = normalized[1];

        // 生成切线上的两个点(距离切点足够远)
        double scale = GeometryConfig.LineStyle.INFINITE_LINE_EXTENSION_FACTOR; // 扩展距离
        Point2D point1 = new Point2D(pointX + tangentDx * scale, pointY + tangentDy * scale);
        Point2D point2 = new Point2D(pointX - tangentDx * scale, pointY - tangentDy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算两个函数图像的交点
     * <p>
     * 通过遍历采样点,检测函数值符号变化来近似查找交点
     * 使用线性插值精确定位交点位置
     *
     * @param function1 第一个函数
     * @param function2 第二个函数
     * @return 交点列表
     */
    public static List<Point2D> getFunctionFunctionIntersections(FunctionGeo function1, FunctionGeo function2) {
        List<Point2D> intersections = new ArrayList<>();

        List<Point2D> points1 = function1.getSampledPoints();
        List<Point2D> points2 = function2.getSampledPoints();

        if (points1 == null || points1.size() < 2 || points2 == null || points2.size() < 2) {
            return intersections;
        }

        // 遍历第一个函数的所有采样点段
        for (int i = 0; i < points1.size() - 1; i++) {
            Point2D p1a = points1.get(i);
            Point2D p1b = points1.get(i + 1);

            if (!isValidPoint(p1a) || !isValidPoint(p1b)) {
                continue;
            }

            // 遍历第二个函数的所有采样点段
            for (int j = 0; j < points2.size() - 1; j++) {
                Point2D p2a = points2.get(j);
                Point2D p2b = points2.get(j + 1);

                if (!isValidPoint(p2a) || !isValidPoint(p2b)) {
                    continue;
                }

                // 检查两个线段的x范围是否重叠
                double x1Min = Math.min(p1a.getX(), p1b.getX());
                double x1Max = Math.max(p1a.getX(), p1b.getX());
                double x2Min = Math.min(p2a.getX(), p2b.getX());
                double x2Max = Math.max(p2a.getX(), p2b.getX());

                // 如果x范围不重叠,跳过
                if (x1Max < x2Min || x2Max < x1Min) {
                    continue;
                }

                // 计算两条线段的交点
                List<Point2D> segmentIntersections = computeLineIntersection(
                        p1a.getX(), p1a.getY(), p1b.getX(), p1b.getY(),
                        p2a.getX(), p2a.getY(), p2b.getX(), p2b.getY(),
                        true, true
                );

                intersections.addAll(segmentIntersections);
            }
        }

        // 去重：移除非常接近的点
        return removeDuplicatePoints(intersections, GeometryConfig.Performance.MIN_VALID_DISTANCE * 10);
    }


    /**
     * 检查点是否有效(非null且坐标有限)
     */
    private static boolean isValidPoint(Point2D p) {
        return p != null && Double.isFinite(p.getX()) && Double.isFinite(p.getY());
    }

    /**
     * 去除重复的点(距离小于阈值的点视为重复)
     */
    private static List<Point2D> removeDuplicatePoints(List<Point2D> points, double threshold) {
        List<Point2D> result = new ArrayList<>();

        for (Point2D point : points) {
            boolean isDuplicate = false;
            for (Point2D existing : result) {
                double distance = Math.hypot(point.getX() - existing.getX(), point.getY() - existing.getY());
                if (distance < threshold) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                result.add(point);
            }
        }

        return result;
    }

    /**
     * 计算线段与函数的交点
     *
     * @param line     线段
     * @param function 函数
     * @return 交点列表
     */
    public static List<Point2D> getLineFunctionIntersections(LineGeo line, FunctionGeo function) {
        List<Point2D> intersections = new ArrayList<>();
        List<Point2D> sampledPoints = function.getSampledPoints();

        if (sampledPoints == null || sampledPoints.size() < 2) {
            return intersections;
        }

        double x1 = line.getStartX();
        double y1 = line.getStartY();
        double x2 = line.getEndX();
        double y2 = line.getEndY();

        // 遍历函数采样点,检测与线段的交点
        for (int i = 0; i < sampledPoints.size() - 1; i++) {
            Point2D p1 = sampledPoints.get(i);
            Point2D p2 = sampledPoints.get(i + 1);

            if (!isValidPoint(p1) || !isValidPoint(p2)) {
                continue;
            }

            // 计算两条线段的交点
            List<Point2D> segmentIntersections = computeLineIntersection(
                    x1, y1, x2, y2,
                    p1.getX(), p1.getY(), p2.getX(), p2.getY(),
                    true, true
            );

            intersections.addAll(segmentIntersections);
        }

        return removeDuplicatePoints(intersections, GeometryConfig.Performance.MIN_VALID_DISTANCE * 10);
    }

    /**
     * 计算无限直线与函数的交点
     *
     * @param infiniteLine 无限直线
     * @param function     函数
     * @return 交点列表
     */
    public static List<Point2D> getInfiniteLineFunctionIntersections(InfiniteLineGeo infiniteLine, FunctionGeo function) {
        List<Point2D> intersections = new ArrayList<>();
        List<Point2D> sampledPoints = function.getSampledPoints();

        if (sampledPoints == null || sampledPoints.size() < 2) {
            return intersections;
        }

        double x1 = infiniteLine.getPoint1X();
        double y1 = infiniteLine.getPoint1Y();
        double x2 = infiniteLine.getPoint2X();
        double y2 = infiniteLine.getPoint2Y();

        // 遍历函数采样点,检测与无限直线的交点
        for (int i = 0; i < sampledPoints.size() - 1; i++) {
            Point2D p1 = sampledPoints.get(i);
            Point2D p2 = sampledPoints.get(i + 1);

            if (!isValidPoint(p1) || !isValidPoint(p2)) {
                continue;
            }

            List<Point2D> segmentIntersections = computeLineIntersection(
                    x1, y1, x2, y2,
                    p1.getX(), p1.getY(), p2.getX(), p2.getY(),
                    false, true
            );

            intersections.addAll(segmentIntersections);
        }

        return removeDuplicatePoints(intersections, GeometryConfig.Performance.MIN_VALID_DISTANCE * 10);
    }

    /**
     * 计算圆与函数的交点
     *
     * @param circle   圆
     * @param function 函数
     * @return 交点列表
     */
    public static List<Point2D> getCircleFunctionIntersections(CircleGeo circle, FunctionGeo function) {
        List<Point2D> intersections = new ArrayList<>();
        List<Point2D> sampledPoints = function.getSampledPoints();

        if (sampledPoints == null || sampledPoints.size() < 2) {
            return intersections;
        }

        double cx = circle.getCx();
        double cy = circle.getCy();
        double r = circle.getR();

        // 遍历函数采样线段,检测与圆的交点
        for (int i = 0; i < sampledPoints.size() - 1; i++) {
            Point2D p1 = sampledPoints.get(i);
            Point2D p2 = sampledPoints.get(i + 1);

            if (!isValidPoint(p1) || !isValidPoint(p2)) {
                continue;
            }

            // 计算点到圆心的距离
            double d1 = Math.hypot(p1.getX() - cx, p1.getY() - cy);
            double d2 = Math.hypot(p2.getX() - cx, p2.getY() - cy);

            // 检查是否跨越圆(一个点在圆内,一个点在圆外,或者恰好在圆上)
            if ((d1 - r) * (d2 - r) <= 0 || Math.abs(d1 - r) < 1e-6 || Math.abs(d2 - r) < 1e-6) {
                // 创建临时线段,使用线段-圆交点算法
                LineGeo tempLine = new LineGeo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), false);
                List<Point2D> segmentIntersections = getLineCircleIntersections(tempLine, circle);
                intersections.addAll(segmentIntersections);
            }
        }

        return removeDuplicatePoints(intersections, GeometryConfig.Performance.MIN_VALID_DISTANCE * 10);
    }

    /**
     * 计算椭圆与线段的交点（近似方法：用多边形逼近椭圆）
     *
     * @param ellipse 椭圆
     * @param line    线段
     * @return 交点列表
     */
    public static List<Point2D> getEllipseLineIntersections(EllipseGeo ellipse, LineGeo line) {
        // 椭圆与线段的精确交点计算较复杂，使用数值方法近似
        List<Point2D> intersections = new ArrayList<>();
        double a = ellipse.getA();
        double b = ellipse.getB();
        double cx = ellipse.getCx();
        double cy = ellipse.getCy();
        double cos = Math.cos(ellipse.getRotationAngle());
        double sin = Math.sin(ellipse.getRotationAngle());

        // 在 0 到 2π 间采样，检测符号变化
        int samples = 128;
        double prevSign = 0;
        boolean prevValid = false;
        double prevT = 0;

        for (int i = 0; i <= samples; i++) {
            double t = 2 * Math.PI * i / samples;
            double xt = a * Math.cos(t);
            double yt = b * Math.sin(t);
            double wx = cx + xt * cos - yt * sin;
            double wy = cy + xt * sin + yt * cos;

            double dist = signedDistanceToLine(wx, wy, line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY());

            if (prevValid && prevSign * dist < 0) {
                double midT = (prevT + t) / 2;
                double mxt = a * Math.cos(midT);
                double myt = b * Math.sin(midT);
                double mwx = cx + mxt * cos - myt * sin;
                double mwy = cy + mxt * sin + myt * cos;
                double midDist = signedDistanceToLine(mwx, mwy, line.getStartX(), line.getStartY(),
                        line.getEndX(), line.getEndY());
                if (Math.abs(midDist) < GeometryConfig.Performance.MIN_VALID_DISTANCE) {
                    intersections.add(new Point2D(mwx, mwy));
                }
            }
            prevSign = Math.signum(dist);
            prevValid = true;
            prevT = t;
        }
        return intersections;
    }

    /** 点到线段的符号距离（正=线一侧，负=另一侧） */
    private static double signedDistanceToLine(double px, double py,
                                               double x1, double y1, double x2, double y2) {
        return (x2 - x1) * (py - y1) - (y2 - y1) * (px - x1);
    }

    /**
     * 计算椭圆与圆的交点（使用数值方法近似）
     *
     * @param ellipse 椭圆
     * @param circle  圆
     * @return 交点列表
     */
    public static List<Point2D> getEllipseCircleIntersections(EllipseGeo ellipse, CircleGeo circle) {
        // 在椭圆上采样，检测到圆心的距离符号变化
        List<Point2D> intersections = new ArrayList<>();
        double a = ellipse.getA();
        double b = ellipse.getB();
        double cx = ellipse.getCx();
        double cy = ellipse.getCy();
        double cos = Math.cos(ellipse.getRotationAngle());
        double sin = Math.sin(ellipse.getRotationAngle());
        double ccx = circle.getCx();
        double ccy = circle.getCy();
        double cr = circle.getR();

        int samples = 256;
        double prevDiff = 0;
        boolean prevValid = false;
        double prevT = 0;

        for (int i = 0; i <= samples; i++) {
            double t = 2 * Math.PI * i / samples;
            double xt = a * Math.cos(t);
            double yt = b * Math.sin(t);
            double wx = cx + xt * cos - yt * sin;
            double wy = cy + xt * sin + yt * cos;

            double distToCircle = Math.hypot(wx - ccx, wy - ccy) - cr;

            if (prevValid && prevDiff * distToCircle < 0) {
                double midT = (prevT + t) / 2;
                double mxt = a * Math.cos(midT);
                double myt = b * Math.sin(midT);
                double mwx = cx + mxt * cos - myt * sin;
                double mwy = cy + mxt * sin + myt * cos;
                double midDist = Math.hypot(mwx - ccx, mwy - ccy) - cr;
                if (Math.abs(midDist) < GeometryConfig.Performance.MIN_VALID_DISTANCE * 2) {
                    intersections.add(new Point2D(mwx, mwy));
                }
            }
            prevDiff = Math.signum(distToCircle);
            prevValid = true;
            prevT = t;
        }
        return intersections;
    }
}