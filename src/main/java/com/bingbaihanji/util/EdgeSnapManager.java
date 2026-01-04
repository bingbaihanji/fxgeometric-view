package com.bingbaihanji.util;

import com.bingbaihanji.util.visitor.EdgeSnapVisitor;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * 边吸附管理器（重构版 - 使用访问者模式）
 * <p>
 * 处理几何图形边的磁性吸附功能
 * 使用访问者模式消除instanceof判断，提高扩展性和类型安全
 *
 * @author bingbaihanji
 * @date 2025-01-01
 * @version 2.0 (2025-01-04 使用访问者模式重构)
 */
public class EdgeSnapManager {

    /**
     * 查找最近的边吸附点（使用访问者模式）
     *
     * @param x         当前鼠标x坐标（世界坐标）
     * @param y         当前鼠标y坐标（世界坐标）
     * @param objects   所有几何对象
     * @param threshold 吸附阈值（世界坐标距离）
     * @return 最近的边吸附结果，如果没有找到则返回null
     */
    public static EdgeSnapResult findNearestEdge(double x, double y,
                                                 List<WorldObject> objects,
                                                 double threshold) {
        EdgeSnapVisitor visitor = new EdgeSnapVisitor(x, y, threshold);
        EdgeSnapVisitor.SnapResult bestSnap = null;

        for (WorldObject obj : objects) {
            EdgeSnapVisitor.SnapResult snap = obj.accept(visitor);
            if (snap != null && (bestSnap == null || snap.distance < bestSnap.distance)) {
                bestSnap = snap;
            }
        }

        if (bestSnap != null) {
            return new EdgeSnapResult(bestSnap.x, bestSnap.y, null, bestSnap.edgeType);
        }
        return null;
    }

    /**
     * 检测圆相切吸附
     * <p>
     * 当绘制一个新圆时，检测其边缘是否靠近已有圆的边缘，
     * 如果靠近则调整半径使得两圆恰好相切
     *
     * @param centerX       新圆的圆心X
     * @param centerY       新圆的圆心Y
     * @param currentRadius 当前半径
     * @param objects       所有几何对象
     * @param threshold     吸附阈值
     * @return 相切吸附结果，如果没有则返回null
     */
    public static CircleTangentResult findCircleTangentSnap(double centerX, double centerY,
                                                            double currentRadius,
                                                            List<WorldObject> objects,
                                                            double threshold) {
        CircleTangentResult bestResult = null;
        double minDiff = threshold;

        for (WorldObject obj : objects) {
            // 这里仍需要instanceof判断，因为相切吸附是圆特有的逻辑
            // 但我们可以创建一个专门的访问者来处理
            if (obj instanceof CircleGeo targetCircle) {
                double targetCx = targetCircle.getCx();
                double targetCy = targetCircle.getCy();
                double targetR = targetCircle.getR();

                double centerDist = Math.hypot(centerX - targetCx, centerY - targetCy);

                if (centerDist < 0.0001) {
                    continue;
                }

                // 外切半径
                double externalRadius = centerDist - targetR;

                // 内切半径
                double internalRadius1 = Math.abs(centerDist - targetR);
                double internalRadius2 = targetR + centerDist;

                // 检查外切
                if (externalRadius > 0) {
                    double diff = Math.abs(currentRadius - externalRadius);
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestResult = new CircleTangentResult(externalRadius, targetCircle, true);
                    }
                }

                // 检查内切（新圆在目标圆内部）
                if (internalRadius1 > 0 && internalRadius1 < targetR) {
                    double diff = Math.abs(currentRadius - internalRadius1);
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestResult = new CircleTangentResult(internalRadius1, targetCircle, false);
                    }
                }

                // 检查内切（新圆包含目标圆）
                if (centerDist < targetR) {
                    double diff = Math.abs(currentRadius - internalRadius2);
                    if (diff < minDiff) {
                        minDiff = diff;
                        bestResult = new CircleTangentResult(internalRadius2, targetCircle, false);
                    }
                }
            }
        }

        return bestResult;
    }

    /**
     * 边吸附结果
     */
    public static class EdgeSnapResult {
        private final double x;
        private final double y;
        private final WorldObject sourceObject;
        private final String edgeType;

        public EdgeSnapResult(double x, double y, WorldObject sourceObject, String edgeType) {
            this.x = x;
            this.y = y;
            this.sourceObject = sourceObject;
            this.edgeType = edgeType;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public WorldObject getSourceObject() {
            return sourceObject;
        }

        public String getEdgeType() {
            return edgeType;
        }
    }

    /**
     * 圆相切吸附结果
     */
    public static class CircleTangentResult {
        private final double radius;
        private final CircleGeo targetCircle;
        private final boolean isExternal;

        public CircleTangentResult(double radius, CircleGeo targetCircle, boolean isExternal) {
            this.radius = radius;
            this.targetCircle = targetCircle;
            this.isExternal = isExternal;
        }

        public double getRadius() {
            return radius;
        }

        public CircleGeo getTargetCircle() {
            return targetCircle;
        }

        public boolean isExternal() {
            return isExternal;
        }
    }
}
