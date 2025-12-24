package com.binbaihanji.util;

import com.binbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.InfiniteLineGeo;
import com.binbaihanji.view.layout.draw.geometry.impl.LineGeo;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 几何图形交点计算工具类
 * 提供线段与线段、线段与圆、圆与圆、无限直线与其他图形之间的交点计算功能
 */
public class IntersectionUtils {

    /**
     * 计算两个线段的交点
     *
     * @param line1 第一条线段
     * @param line2 第二条线段
     * @return 交点列表
     */
    public static List<Point2D> getLineLineIntersections(LineGeo line1, LineGeo line2) {
        List<Point2D> intersections = new ArrayList<>();

        double x1 = line1.getStartX();
        double y1 = line1.getStartY();
        double x2 = line1.getEndX();
        double y2 = line1.getEndY();

        double x3 = line2.getStartX();
        double y3 = line2.getStartY();
        double x4 = line2.getEndX();
        double y4 = line2.getEndY();

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) {
            // 线段平行或重合
            return intersections;
        }

        double tNum = (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4);
        double uNum = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3));

        double t = tNum / denom;
        double u = uNum / denom;

        // 检查交点是否在两条线段上
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            double ix = x1 + t * (x2 - x1);
            double iy = y1 + t * (y2 - y1);
            intersections.add(new Point2D(ix, iy));
        }

        return intersections;
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

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            // 没有实数解，线段与圆不相交
            return intersections;
        }

        if (Math.abs(discriminant) < 1e-10) {
            // 一个解，线段与圆相切
            double t = -b / (2 * a);
            if (t >= 0 && t <= 1) {
                double ix = x1 + t * dx;
                double iy = y1 + t * dy;
                intersections.add(new Point2D(ix, iy));
            }
        } else {
            // 两个解，线段与圆相交于两点
            double sqrtDiscriminant = Math.sqrt(discriminant);
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
        double d = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));

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
        double h = Math.sqrt(r1 * r1 - a * a);

        // 计算P2点坐标
        double x3 = x1 + a * (x2 - x1) / d;
        double y3 = y1 + a * (y2 - y1) / d;

        // 计算交点坐标
        double ix1 = x3 + h * (y2 - y1) / d;
        double iy1 = y3 - h * (x2 - x1) / d;

        double ix2 = x3 - h * (y2 - y1) / d;
        double iy2 = y3 + h * (x2 - x1) / d;

        intersections.add(new Point2D(ix1, iy1));

        // 如果两交点不重合，则添加第二个交点
        if (Math.abs(ix1 - ix2) > 1e-10 || Math.abs(iy1 - iy2) > 1e-10) {
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
        List<Point2D> intersections = new ArrayList<>();

        double x1 = infiniteLine.getPoint1X();
        double y1 = infiniteLine.getPoint1Y();
        double x2 = infiniteLine.getPoint2X();
        double y2 = infiniteLine.getPoint2Y();

        double x3 = line.getStartX();
        double y3 = line.getStartY();
        double x4 = line.getEndX();
        double y4 = line.getEndY();

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) {
            // 直线平行或重合
            return intersections;
        }

        double tNum = (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4);
        double uNum = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3));

        double u = uNum / denom;

        // 无限直线不需要检查t，只检查交点是否在线段上
        if (u >= 0 && u <= 1) {
            double t = tNum / denom;
            double ix = x1 + t * (x2 - x1);
            double iy = y1 + t * (y2 - y1);
            intersections.add(new Point2D(ix, iy));
        }

        return intersections;
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

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            // 没有实数解，直线与圆不相交
            return intersections;
        }

        if (Math.abs(discriminant) < 1e-10) {
            // 一个解，直线与圆相切
            double t = -b / (2 * a);
            double ix = x1 + t * dx;
            double iy = y1 + t * dy;
            intersections.add(new Point2D(ix, iy));
        } else {
            // 两个解，直线与圆相交于两点
            double sqrtDiscriminant = Math.sqrt(discriminant);
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
        List<Point2D> intersections = new ArrayList<>();

        double x1 = line1.getPoint1X();
        double y1 = line1.getPoint1Y();
        double x2 = line1.getPoint2X();
        double y2 = line1.getPoint2Y();

        double x3 = line2.getPoint1X();
        double y3 = line2.getPoint1Y();
        double x4 = line2.getPoint2X();
        double y4 = line2.getPoint2Y();

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) {
            // 直线平行或重合
            return intersections;
        }

        double tNum = (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4);
        double t = tNum / denom;

        // 无限直线不需要检查t和u的范围，直接计算交点
        double ix = x1 + t * (x2 - x1);
        double iy = y1 + t * (y2 - y1);
        intersections.add(new Point2D(ix, iy));

        return intersections;
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
        return new Point2D((x1 + x2) / 2, (y1 + y2) / 2);
    }

    /**
     * 计算过指定点垂直于给定直线的垂线的两个点（用于绘制无限直线）
     * 返回的两个点在垂线上，距离给定点足够远以绘制无限直线
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
        double length = Math.sqrt(perpDx * perpDx + perpDy * perpDy);
        if (length > 1e-10) {
            perpDx /= length;
            perpDy /= length;
        }

        // 生成垂线上的两个点（距离给定点足够远）
        double scale = 10000; // 扩展距离
        Point2D point1 = new Point2D(pointX + perpDx * scale, pointY + perpDy * scale);
        Point2D point2 = new Point2D(pointX - perpDx * scale, pointY - perpDy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算垂直平分线的两个点（用于绘制无限直线）
     * 垂直平分线过线段中点且垂直于线段
     *
     * @param lineX1 线段起点x坐标
     * @param lineY1 线段起点y坐标
     * @param lineX2 线段终点x坐标
     * @param lineY2 线段终点y坐标
     * @param pointX 给定点x坐标（垂直平分线将过此点）
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
        double length = Math.sqrt(perpDx * perpDx + perpDy * perpDy);
        if (length > 1e-10) {
            perpDx /= length;
            perpDy /= length;
        }

        // 生成垂直平分线上的两个点（从给定点出发）
        double scale = 10000; // 扩展距离
        Point2D point1 = new Point2D(pointX + perpDx * scale, pointY + perpDy * scale);
        Point2D point2 = new Point2D(pointX - perpDx * scale, pointY - perpDy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算过指定点平行于给定直线的平行线的两个点（用于绘制无限直线）
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
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 1e-10) {
            dx /= length;
            dy /= length;
        }

        // 生成平行线上的两个点（距离给定点足够远）
        double scale = 10000; // 扩展距离
        Point2D point1 = new Point2D(pointX + dx * scale, pointY + dy * scale);
        Point2D point2 = new Point2D(pointX - dx * scale, pointY - dy * scale);

        return new Point2D[]{point1, point2};
    }

    /**
     * 计算过圆上一点的切线的两个点（用于绘制无限直线）
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

        // 切线垂直于半径，方向向量为 (-dy, dx)
        double tangentDx = -dy;
        double tangentDy = dx;

        // 归一化方向向量
        double length = Math.sqrt(tangentDx * tangentDx + tangentDy * tangentDy);
        if (length > 1e-10) {
            tangentDx /= length;
            tangentDy /= length;
        }

        // 生成切线上的两个点（距离切点足够远）
        double scale = 10000; // 扩展距离
        Point2D point1 = new Point2D(pointX + tangentDx * scale, pointY + tangentDy * scale);
        Point2D point2 = new Point2D(pointX - tangentDx * scale, pointY - tangentDy * scale);

        return new Point2D[]{point1, point2};
    }
}