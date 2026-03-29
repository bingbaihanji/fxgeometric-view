package com.bingbaihanji.util;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 曲线平滑工具类
 *
 * 提供多种曲线平滑算法：
 * 1. Douglas-Peucker 算法：简化路径点
 * 2. Catmull-Rom 样条曲线：生成平滑曲线
 * 3. 高斯平滑：去除噪声
 * 4. 角度过滤：去除尖刺点
 *
 * @author bingbaihanji
 * @date 2026-03-29
 */
public class CurveSmoothing {

    /**
     * Douglas-Peucker 算法简化点集（修复版：使用点到线段距离，正确处理回笔）
     *
     * @param points 原始点集
     * @param epsilon 容差值，值越大简化程度越高（建议 0.5-3.0，基于世界坐标）
     * @return 简化后的点集
     */
    public static List<Point2D> simplifyDouglasPeucker(List<Point2D> points, double epsilon) {
        if (points == null || points.size() <= 2) {
            return new ArrayList<>(points);
        }

        List<Point2D> result = new ArrayList<>();
        boolean[] keep = new boolean[points.size()];
        
        // 标记起点和终点为保留
        keep[0] = true;
        keep[points.size() - 1] = true;
        
        // 递归处理
        douglasPeuckerHelper(points, 0, points.size() - 1, epsilon, keep);
        
        // 收集保留的点
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) {
                result.add(points.get(i));
            }
        }
        
        return result;
    }

    /**
     * Douglas-Peucker 递归辅助函数
     */
    private static void douglasPeuckerHelper(List<Point2D> points, int start, int end,
                                             double epsilon, boolean[] keep) {
        if (start >= end - 1) {
            return;
        }

        Point2D startPoint = points.get(start);
        Point2D endPoint = points.get(end);

        // 找到距离起点-终点线段最远的点（使用点到线段距离，不是投影距离）
        double maxDistance = 0;
        int maxIndex = start;

        for (int i = start + 1; i < end; i++) {
            Point2D point = points.get(i);
            double distance = pointToSegmentDistance(point, startPoint, endPoint);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }

        // 如果最大距离大于容差，保留该点并递归分割
        if (maxDistance > epsilon) {
            keep[maxIndex] = true;
            douglasPeuckerHelper(points, start, maxIndex, epsilon, keep);
            douglasPeuckerHelper(points, maxIndex, end, epsilon, keep);
        }
    }

    /**
     * 计算点到线段的距离（修复版：正确处理线段外的点）
     */
    private static double pointToSegmentDistance(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double dx = lineEnd.getX() - lineStart.getX();
        double dy = lineEnd.getY() - lineStart.getY();
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return point.distance(lineStart);
        }

        // 计算投影参数 t，并限制在 [0, 1] 范围内
        double t = Math.max(0, Math.min(1, 
            ((point.getX() - lineStart.getX()) * dx + (point.getY() - lineStart.getY()) * dy) / lengthSquared));

        double projX = lineStart.getX() + t * dx;
        double projY = lineStart.getY() + t * dy;

        return Math.hypot(point.getX() - projX, point.getY() - projY);
    }

    /**
     * 去除尖刺点：基于角度检测和移除异常点
     *
     * @param points 原始点集
     * @param angleThreshold 角度阈值（弧度，建议 Math.PI * 0.7，即约126度）
     * @return 去除尖刺后的点集
     */
    public static List<Point2D> removeSpikes(List<Point2D> points, double angleThreshold) {
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points);
        }

        List<Point2D> result = new ArrayList<>();
        result.add(points.get(0));

        for (int i = 1; i < points.size() - 1; i++) {
            Point2D prev = points.get(i - 1);
            Point2D curr = points.get(i);
            Point2D next = points.get(i + 1);

            // 计算向量
            double dx1 = curr.getX() - prev.getX();
            double dy1 = curr.getY() - prev.getY();
            double dx2 = next.getX() - curr.getX();
            double dy2 = next.getY() - curr.getY();

            // 计算角度变化
            double angle1 = Math.atan2(dy1, dx1);
            double angle2 = Math.atan2(dy2, dx2);
            double angleDiff = Math.abs(normalizeAngle(angle2 - angle1));

            // 如果角度变化小于阈值，保留该点
            if (angleDiff < angleThreshold) {
                result.add(curr);
            }
            // 否则跳过该点（认为是尖刺）
        }

        result.add(points.get(points.size() - 1));
        return result;
    }

    /**
     * 将角度归一化到 [-PI, PI]
     */
    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /**
     * 使用 Catmull-Rom 样条生成平滑曲线点（带张力控制）
     *
     * @param points 控制点
     * @param segmentsPerCurve 每段曲线的细分数（值越大越平滑，建议 10-20）
     * @param tension 张力系数（0.0 = 直线，0.5 = 默认平滑，1.0 = 最紧，建议 0.4-0.6）
     * @return 平滑后的曲线点
     */
    public static List<Point2D> smoothCatmullRom(List<Point2D> points, int segmentsPerCurve, double tension) {
        if (points == null || points.size() < 2) {
            return new ArrayList<>(points);
        }

        if (points.size() == 2) {
            return new ArrayList<>(points);
        }

        List<Point2D> smoothed = new ArrayList<>();
        smoothed.add(points.get(0));

        // 对每个线段进行 Catmull-Rom 插值
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p0 = i > 0 ? points.get(i - 1) : reflectPoint(points.get(i + 1), points.get(i));
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i + 1);
            Point2D p3 = i < points.size() - 2 ? points.get(i + 2) : reflectPoint(points.get(i), points.get(i + 1));

            for (int j = 1; j <= segmentsPerCurve; j++) {
                double t = (double) j / segmentsPerCurve;
                Point2D interpolated = catmullRomInterpolate(p0, p1, p2, p3, t, tension);
                smoothed.add(interpolated);
            }
        }

        return smoothed;
    }

    /**
     * 使用默认张力的 Catmull-Rom 样条
     */
    public static List<Point2D> smoothCatmullRom(List<Point2D> points, int segmentsPerCurve) {
        return smoothCatmullRom(points, segmentsPerCurve, 0.5);
    }

    /**
     * 反射点：用于边界处理
     */
    private static Point2D reflectPoint(Point2D source, Point2D pivot) {
        return new Point2D(2 * pivot.getX() - source.getX(), 2 * pivot.getY() - source.getY());
    }

    /**
     * Catmull-Rom 样条插值（带张力控制）
     *
     * @param p0 前一个控制点
     * @param p1 当前段起点
     * @param p2 当前段终点
     * @param p3 后一个控制点
     * @param t 参数 [0, 1]
     * @param tension 张力系数 [0, 1]
     * @return 插值点
     */
    private static Point2D catmullRomInterpolate(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t, double tension) {
        double t2 = t * t;
        double t3 = t2 * t;

        // 计算切线向量（带张力控制）
        double tAdjusted = tension * 0.5;
        double m0x = (p2.getX() - p0.getX()) * tAdjusted;
        double m0y = (p2.getY() - p0.getY()) * tAdjusted;
        double m1x = (p3.getX() - p1.getX()) * tAdjusted;
        double m1y = (p3.getY() - p1.getY()) * tAdjusted;

        // 三次 Hermite 插值
        double h00 = 2 * t3 - 3 * t2 + 1;  // p1 的系数
        double h10 = t3 - 2 * t2 + t;       // m0 的系数
        double h01 = -2 * t3 + 3 * t2;      // p2 的系数
        double h11 = t3 - t2;               // m1 的系数

        double x = h00 * p1.getX() + h10 * m0x + h01 * p2.getX() + h11 * m1x;
        double y = h00 * p1.getY() + h10 * m0y + h01 * p2.getY() + h11 * m1y;

        return new Point2D(x, y);
    }

    /**
     * 移动平均平滑
     * 简单的平滑方法，对相邻点进行平均
     *
     * @param points 原始点集
     * @param windowSize 平均窗口大小（建议 3-5）
     * @return 平滑后的点集
     */
    public static List<Point2D> smoothMovingAverage(List<Point2D> points, int windowSize) {
        if (points == null || points.size() <= 2 || windowSize <= 1) {
            return new ArrayList<>(points);
        }

        List<Point2D> smoothed = new ArrayList<>();
        int halfWindow = windowSize / 2;

        for (int i = 0; i < points.size(); i++) {
            double sumX = 0;
            double sumY = 0;
            int count = 0;

            // 计算窗口内的平均值
            for (int j = Math.max(0, i - halfWindow); j <= Math.min(points.size() - 1, i + halfWindow); j++) {
                sumX += points.get(j).getX();
                sumY += points.get(j).getY();
                count++;
            }

            smoothed.add(new Point2D(sumX / count, sumY / count));
        }

        return smoothed;
    }

    /**
     * 综合平滑方法：先简化再平滑（基础版）
     *
     * @param points 原始点集
     * @param simplifyEpsilon 简化容差（0.5-3.0，基于世界坐标）
     * @param smoothSegments 平滑细分数（10-20）
     * @return 平滑后的点集
     */
    public static List<Point2D> smoothCurve(List<Point2D> points, double simplifyEpsilon, int smoothSegments) {
        return smoothCurve(points, simplifyEpsilon, smoothSegments, 0.5);
    }

    /**
     * 综合平滑方法：先简化再平滑（带张力控制）
     *
     * @param points 原始点集
     * @param simplifyEpsilon 简化容差（0.5-3.0，基于世界坐标）
     * @param smoothSegments 平滑细分数（10-20）
     * @param tension 张力系数（0.4-0.6）
     * @return 平滑后的点集
     */
    public static List<Point2D> smoothCurve(List<Point2D> points, double simplifyEpsilon, int smoothSegments, double tension) {
        if (points == null || points.size() < 2) {
            return new ArrayList<>(points);
        }

        // 1. 先简化点集（减少冗余点，使用点到线段距离算法）
        List<Point2D> simplified = simplifyDouglasPeucker(points, simplifyEpsilon);

        // 2. 再进行 Catmull-Rom 平滑（带张力控制）
        return smoothCatmullRom(simplified, smoothSegments, tension);
    }

    /**
     * 手绘专用平滑：针对手绘输入优化的完整处理流程
     *
     * @param points 原始手绘点集
     * @param simplifyEpsilon 简化容差（建议 0.3-2.0）
     * @param smoothSegments 平滑细分数（建议 12-20）
     * @param tension 张力系数（建议 0.4-0.6，越低越平滑）
     * @return 优化后的曲线点
     */
    public static List<Point2D> smoothHandDrawnCurve(List<Point2D> points, 
                                                      double simplifyEpsilon, 
                                                      int smoothSegments, 
                                                      double tension) {
        if (points == null || points.size() < 2) {
            return new ArrayList<>(points);
        }

        // 1. 高斯平滑去噪（轻量级）
        List<Point2D> denoised = smoothGaussian(points, 1.5);

        // 2. 去除尖刺点
        List<Point2D> despiked = removeSpikes(denoised, Math.PI * 0.75);

        // 3. 简化路径（使用修复后的点到线段距离算法）
        List<Point2D> simplified = simplifyDouglasPeucker(despiked, simplifyEpsilon);

        // 4. 确保至少有一些点
        if (simplified.size() < 2) {
            simplified = new ArrayList<>(points.subList(0, Math.min(3, points.size())));
        }

        // 5. Catmull-Rom 平滑（带张力控制）
        return smoothCatmullRom(simplified, smoothSegments, tension);
    }

    /**
     * 高斯平滑：去除高频噪声
     *
     * @param points 原始点集
     * @param sigma 高斯核标准差（建议 1.0-2.0）
     * @return 平滑后的点集
     */
    public static List<Point2D> smoothGaussian(List<Point2D> points, double sigma) {
        if (points == null || points.size() <= 3 || sigma <= 0) {
            return new ArrayList<>(points);
        }

        int kernelSize = Math.max(3, (int) (sigma * 3) * 2 + 1);
        kernelSize = Math.min(kernelSize, points.size());
        // 确保 kernelSize 是奇数，否则调整
        if (kernelSize % 2 == 0) {
            kernelSize = Math.max(3, kernelSize - 1);
        }
        int halfSize = kernelSize / 2;

        // 生成高斯核
        double[] kernel = new double[kernelSize];
        double sum = 0;
        for (int i = 0; i < kernelSize; i++) {
            double x = i - halfSize;
            kernel[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        // 归一化
        for (int i = 0; i < kernelSize; i++) {
            kernel[i] /= sum;
        }

        List<Point2D> smoothed = new ArrayList<>();
        
        for (int i = 0; i < points.size(); i++) {
            double sumX = 0;
            double sumY = 0;
            double weightSum = 0;

            for (int j = -halfSize; j <= halfSize; j++) {
                int idx = Math.max(0, Math.min(points.size() - 1, i + j));
                double weight = kernel[j + halfSize];
                sumX += points.get(idx).getX() * weight;
                sumY += points.get(idx).getY() * weight;
                weightSum += weight;
            }

            smoothed.add(new Point2D(sumX / weightSum, sumY / weightSum));
        }

        return smoothed;
    }

    /**
     * 重采样：按固定距离间隔重新采样点
     *
     * @param points 原始点集
     * @param interval 采样间隔距离
     * @return 重采样后的点集
     */
    public static List<Point2D> resample(List<Point2D> points, double interval) {
        if (points == null || points.size() < 2) {
            return new ArrayList<>(points);
        }

        List<Point2D> resampled = new ArrayList<>();
        resampled.add(points.get(0));

        double accumulatedLength = 0;
        double targetLength = interval;

        for (int i = 1; i < points.size(); i++) {
            Point2D prev = points.get(i - 1);
            Point2D curr = points.get(i);
            double segmentLength = prev.distance(curr);

            accumulatedLength += segmentLength;

            while (accumulatedLength >= targetLength) {
                // 在当前线段上插值
                double t = (targetLength - (accumulatedLength - segmentLength)) / segmentLength;
                double x = prev.getX() + t * (curr.getX() - prev.getX());
                double y = prev.getY() + t * (curr.getY() - prev.getY());
                resampled.add(new Point2D(x, y));

                targetLength += interval;
            }
        }

        // 确保最后一个点被包含
        Point2D last = points.get(points.size() - 1);
        if (resampled.isEmpty() || !resampled.get(resampled.size() - 1).equals(last)) {
            resampled.add(last);
        }

        return resampled;
    }
}
