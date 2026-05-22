package com.bingbaihanji.util;

import com.bingbaihanji.config.GeometryConfig;
import javafx.geometry.Point2D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;

/**
 * 数学工具类
 * <p>
 * 提供常用的数学计算和比较方法,使用 Apache Commons Math3 提供高性能数学计算
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class MathCalculationUtils {

    /**
     * 浮点数相等比较(使用默认精度)
     * 使用 Apache Commons Math3 的 Precision 工具进行精确比较
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 是否相等
     */
    public static boolean equals(double a, double b) {
        return Precision.equals(a, b, GeometryConfig.Performance.EPSILON);
    }

    /**
     * 浮点数相等比较(自定义精度)
     * 使用 Apache Commons Math3 的 Precision 工具进行精确比较
     *
     * @param a       第一个数
     * @param b       第二个数
     * @param epsilon 精度
     * @return 是否相等
     */
    public static boolean equals(double a, double b, double epsilon) {
        return Precision.equals(a, b, epsilon);
    }

    /**
     * 判断浮点数是否为零
     * 使用 Apache Commons Math3 的 Precision 工具进行精确比较
     *
     * @param value 数值
     * @return 是否为零
     */
    public static boolean isZero(double value) {
        return Precision.equals(value, 0.0, GeometryConfig.Performance.EPSILON);
    }

    /**
     * 判断浮点数是否为零(自定义精度)
     * 使用 Apache Commons Math3 的 Precision 工具进行精确比较
     *
     * @param value   数值
     * @param epsilon 精度
     * @return 是否为零
     */
    public static boolean isZero(double value, double epsilon) {
        return Precision.equals(value, 0.0, epsilon);
    }

    /**
     * 限制数值在指定范围内
     *
     * @param value 数值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的数值
     */
    public static double clamp(double value, double min, double max) {
        return FastMath.max(min, FastMath.min(max, value));
    }

    /**
     * 计算平方根
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param value 数值
     * @return 平方根
     */
    public static double sqrt(double value) {
        return FastMath.sqrt(value);
    }

    /**
     * 计算斜边长度(勾股定理)
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算,等同于 Math.hypot()
     *
     * @param x x分量
     * @param y y分量
     * @return 斜边长度 sqrt(x^2 + y^2)
     */
    public static double hypot(double x, double y) {
        return FastMath.hypot(x, y);
    }

    /**
     * 计算两点之间的距离
     * 使用 Apache Commons Math3 的 Vector2D 进行计算
     *
     * @param x1 第一个点的x坐标
     * @param y1 第一个点的y坐标
     * @param x2 第二个点的x坐标
     * @param y2 第二个点的y坐标
     * @return 距离
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Vector2D.distance(new Vector2D(x1, y1), new Vector2D(x2, y2));
    }

    /**
     * 取绝对值
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param value 数值
     * @return 绝对值
     */
    public static double abs(double value) {
        return FastMath.abs(value);
    }

    /**
     * 取两个数的最小值
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 较小的数
     */
    public static double min(double a, double b) {
        return FastMath.min(a, b);
    }

    /**
     * 取两个数的最大值
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param a 第一个数
     * @param b 第二个数
     * @return 较大的数
     */
    public static double max(double a, double b) {
        return FastMath.max(a, b);
    }

    /**
     * 计算幂运算
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param base     底数
     * @param exponent 指数
     * @return base的exponent次幂
     */
    public static double pow(double base, double exponent) {
        return FastMath.pow(base, exponent);
    }

    /**
     * 向下取整
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param value 数值
     * @return 小于或等于value的最大整数
     */
    public static double floor(double value) {
        return FastMath.floor(value);
    }

    /**
     * 向上取整
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param value 数值
     * @return 大于或等于value的最小整数
     */
    public static double ceil(double value) {
        return FastMath.ceil(value);
    }

    /**
     * 四舍五入
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param value 数值
     * @return 四舍五入后的整数
     */
    public static long round(double value) {
        return FastMath.round(value);
    }

    //  向量计算 

    /**
     * 计算向量的模长
     * 使用 Apache Commons Math3 的 Vector2D 进行计算
     *
     * @param x 向量x分量
     * @param y 向量y分量
     * @return 模长
     */
    public static double magnitude(double x, double y) {
        return new Vector2D(x, y).getNorm();
    }


    /**
     * 计算两向量的叉积(2D,返回标量)
     * 在2D平面中,叉积的结果是一个垂直于平面的向量的z分量
     *
     * @param x1 第一个向量的x分量
     * @param y1 第一个向量的y分量
     * @param x2 第二个向量的x分量
     * @param y2 第二个向量的y分量
     * @return 叉积的z分量(标量)
     */
    public static double crossProduct(double x1, double y1, double x2, double y2) {
        return x1 * y2 - y1 * x2;
    }

    /**
     * 归一化向量
     * 使用 Apache Commons Math3 的 Vector2D 进行计算
     *
     * @param x 向量x分量
     * @param y 向量y分量
     * @return 归一化后的向量 [nx, ny],如果向量长度为0则返回 [0, 0]
     */
    public static double[] normalize(double x, double y) {
        Vector2D vector = new Vector2D(x, y);
        double norm = vector.getNorm();
        if (isZero(norm)) {
            return new double[]{0, 0};
        }
        Vector2D normalized = vector.normalize();
        return new double[]{normalized.getX(), normalized.getY()};
    }

    //  几何计算 

    /**
     * 计算中点
     *
     * @param x1 第一个点的x坐标
     * @param y1 第一个点的y坐标
     * @param x2 第二个点的x坐标
     * @param y2 第二个点的y坐标
     * @return 中点
     */
    public static Point2D midpoint(double x1, double y1, double x2, double y2) {
        return new Point2D((x1 + x2) / 2, (y1 + y2) / 2);
    }

    /**
     * 计算点到线段的距离
     *
     * @param px 点的x坐标
     * @param py 点的y坐标
     * @param x1 线段起点x坐标
     * @param y1 线段起点y坐标
     * @param x2 线段终点x坐标
     * @param y2 线段终点y坐标
     * @return 点到线段的最短距离
     */
    public static double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (isZero(lengthSquared)) {
            // 线段退化为一个点
            return distance(px, py, x1, y1);
        }

        // 计算投影参数t
        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
        t = clamp(t, 0.0, 1.0);

        // 计算投影点
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return distance(px, py, projX, projY);
    }

    /**
     * 计算点到直线的距离(无限长直线)
     *
     * @param px 点的x坐标
     * @param py 点的y坐标
     * @param x1 直线上第一个点的x坐标
     * @param y1 直线上第一个点的y坐标
     * @param x2 直线上第二个点的x坐标
     * @param y2 直线上第二个点的y坐标
     * @return 点到直线的距离
     */
    public static double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = magnitude(dx, dy);

        if (isZero(length)) {
            return distance(px, py, x1, y1);
        }

        // 使用叉积计算距离
        return FastMath.abs(crossProduct(dx, dy, px - x1, py - y1)) / length;
    }


    //  角度计算

    /**
     * 归一化角度到 [0, 2π) 范围
     * 使用 Apache Commons Math3 的常量
     *
     * @param angle 角度(弧度)
     * @return 归一化后的角度
     */
    public static double normalizeAngle(double angle) {
        double twoPi = 2 * FastMath.PI;
        while (angle < 0) {
            angle += twoPi;
        }
        while (angle >= twoPi) {
            angle -= twoPi;
        }
        return angle;
    }

    /**
     * 计算三角函数 cos
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param angle 角度(弧度)
     * @return cos值
     */
    public static double cos(double angle) {
        return FastMath.cos(angle);
    }

    /**
     * 计算三角函数 sin
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param angle 角度(弧度)
     * @return sin值
     */
    public static double sin(double angle) {
        return FastMath.sin(angle);
    }

    /**
     * 计算反正切函数 atan2
     * 使用 Apache Commons Math3 的 FastMath 提供高性能计算
     *
     * @param y y坐标
     * @param x x坐标
     * @return 角度(弧度),范围 [-π, π]
     */
    public static double atan2(double y, double x) {
        return FastMath.atan2(y, x);
    }

    /**
     * 计算点到无限直线的垂直距离
     */
    public static double pointToInfiniteLineDistance(double px, double py,
                                                     double x1, double y1,
                                                     double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1e-10) {
            return Math.hypot(px - x1, py - y1);
        }
        return Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1) / len;
    }

}
