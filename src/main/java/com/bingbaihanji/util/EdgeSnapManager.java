package com.bingbaihanji.util;

import com.bingbaihanji.util.visitor.EdgeSnapVisitor;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.InfiniteLineGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.LineGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PolygonGeo;
import javafx.geometry.Point2D;

import java.util.List;

/**
 * 边吸附管理器（重构版 - 使用访问者模式）
 * <p>
 * 处理几何图形边的磁性吸附功能
 * 使用访问者模式消除instanceof判断，提高扩展性和类型安全
 *
 * @author bingbaihanji
 * @version 2.0 (2025-01-04 使用访问者模式重构)
 * @date 2025-01-01
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
     * 检测圆与圆相切吸附（拖动圆心时）
     * <p>
     * 当拖动圆心时，检测圆的边缘是否靠近其他圆的边缘，
     * 如果靠近则调整圆心位置使得两圆恰好相切
     *
     * @param centerX        被拖动圆的圆心X
     * @param centerY        被拖动圆的圆心Y
     * @param radius         被拖动圆的半径
     * @param objects        所有几何对象
     * @param threshold      吸附阈值
     * @param excludedObject 要排除的对象（被拖动的圆）
     * @return 圆与圆相切吸附结果，包含调整后的圆心位置
     */
    public static CircleToCircleSnapResult findCircleToCircleTangentSnap(double centerX, double centerY,
                                                                         double radius,
                                                                         List<WorldObject> objects,
                                                                         double threshold,
                                                                         WorldObject excludedObject) {
        CircleToCircleSnapResult bestResult = null;
        double minEdgeDistance = Double.MAX_VALUE;

        for (WorldObject obj : objects) {
            if (obj == excludedObject) {
                continue;
            }

            if (obj instanceof CircleGeo targetCircle) {
                double targetCx = targetCircle.getCx();
                double targetCy = targetCircle.getCy();
                double targetR = targetCircle.getR();

                // 两圆心距离
                double centerDist = Math.hypot(centerX - targetCx, centerY - targetCy);
                if (centerDist < 1e-9) {
                    continue; // 圆心重合，跳过
                }

                // 计算外切时的边缘距离：|centerDist - (radius + targetR)|
                double externalEdgeDist = Math.abs(centerDist - (radius + targetR));
                // 计算内切时的边缘距离：|centerDist - |radius - targetR||
                double internalEdgeDist = Math.abs(centerDist - Math.abs(radius - targetR));

                // 方向单位向量（从目标圆心指向被拖动圆心）
                double ux = (centerX - targetCx) / centerDist;
                double uy = (centerY - targetCy) / centerDist;

                // 检查外切
                if (externalEdgeDist < threshold && externalEdgeDist < minEdgeDistance) {
                    // 外切：新圆心到目标圆心的距离应为 radius + targetR
                    double newCenterX = targetCx + ux * (radius + targetR);
                    double newCenterY = targetCy + uy * (radius + targetR);
                    minEdgeDistance = externalEdgeDist;
                    bestResult = new CircleToCircleSnapResult(newCenterX, newCenterY, targetCircle, true);
                }

                // 检查内切
                if (internalEdgeDist < threshold && internalEdgeDist < minEdgeDistance) {
                    // 内切：新圆心到目标圆心的距离应为 |radius - targetR|
                    double newDist = Math.abs(radius - targetR);
                    double newCenterX = targetCx + ux * newDist;
                    double newCenterY = targetCy + uy * newDist;
                    minEdgeDistance = internalEdgeDist;
                    bestResult = new CircleToCircleSnapResult(newCenterX, newCenterY, targetCircle, false);
                }
            }
        }

        return bestResult;
    }

    /**
     * 检测圆边缘与所有边（线段、多边形边）的相切吸附
     * 这是一个统一的方法，支持线段、无限直线和多边形的边
     */
    public static LineTangentResult findCircleToAllEdgesTangentSnap(double centerX, double centerY,
                                                                    double radius,
                                                                    List<WorldObject> objects,
                                                                    double threshold,
                                                                    WorldObject excludedObject) {
        LineTangentResult bestResult = null;
        double minEdgeDistance = Double.MAX_VALUE;

        for (WorldObject obj : objects) {
            if (obj == excludedObject) {
                continue;
            }

            // 处理线段
            if (obj instanceof LineGeo line) {
                LineTangentResult result = checkLineTangent(centerX, centerY, radius,
                        line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY(),
                        threshold, minEdgeDistance, obj, true);
                if (result != null) {
                    minEdgeDistance = Math.abs(Math.hypot(centerX - result.getCenterX(), centerY - result.getCenterY()) -
                            Math.hypot(result.getCenterX() - line.getStartX(), result.getCenterY() - line.getStartY()));
                    // 重新计算实际的edgeDistance
                    Point2D foot = projectPointOntoSegment(centerX, centerY,
                            line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
                    double distToLine = Math.hypot(foot.getX() - centerX, foot.getY() - centerY);
                    minEdgeDistance = Math.abs(distToLine - radius);
                    bestResult = result;
                }
            }
            // 处理无限直线
            else if (obj instanceof InfiniteLineGeo infiniteLine) {
                LineTangentResult result = checkLineTangent(centerX, centerY, radius,
                        infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(),
                        infiniteLine.getPoint2X(), infiniteLine.getPoint2Y(),
                        threshold, minEdgeDistance, obj, false);
                if (result != null) {
                    Point2D foot = projectPointOntoLine(centerX, centerY,
                            infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(),
                            infiniteLine.getPoint2X(), infiniteLine.getPoint2Y());
                    double distToLine = Math.hypot(foot.getX() - centerX, foot.getY() - centerY);
                    minEdgeDistance = Math.abs(distToLine - radius);
                    bestResult = result;
                }
            }
            // 处理多边形的所有边
            else if (obj instanceof PolygonGeo polygon) {
                int vertexCount = polygon.getVertexCount();
                for (int i = 0; i < vertexCount; i++) {
                    Point2D p1 = polygon.getVertex(i);
                    Point2D p2 = polygon.getVertex((i + 1) % vertexCount);
                    LineTangentResult result = checkLineTangent(centerX, centerY, radius,
                            p1.getX(), p1.getY(), p2.getX(), p2.getY(),
                            threshold, minEdgeDistance, obj, true);
                    if (result != null) {
                        Point2D foot = projectPointOntoSegment(centerX, centerY,
                                p1.getX(), p1.getY(), p2.getX(), p2.getY());
                        double distToLine = Math.hypot(foot.getX() - centerX, foot.getY() - centerY);
                        double newEdgeDist = Math.abs(distToLine - radius);
                        if (newEdgeDist < minEdgeDistance) {
                            minEdgeDistance = newEdgeDist;
                            bestResult = result;
                        }
                    }
                }
            }
        }

        return bestResult;
    }

    /**
     * 检查圆与单条线段/直线的相切
     */
    private static LineTangentResult checkLineTangent(double centerX, double centerY, double radius,
                                                      double x1, double y1, double x2, double y2,
                                                      double threshold, double currentMinEdgeDist,
                                                      WorldObject sourceObj, boolean isSegment) {
        Point2D nearestPoint = isSegment ?
                projectPointOntoSegment(centerX, centerY, x1, y1, x2, y2) :
                projectPointOntoLine(centerX, centerY, x1, y1, x2, y2);

        double distanceToLine = Math.hypot(nearestPoint.getX() - centerX, nearestPoint.getY() - centerY);
        double edgeDistance = Math.abs(distanceToLine - radius);

        // 要求圆心不能太靠近直线（避免与圆心吸附冲突）
        if (edgeDistance < threshold && edgeDistance < currentMinEdgeDist && distanceToLine > radius * 0.1) {
            double dx = centerX - nearestPoint.getX();
            double dy = centerY - nearestPoint.getY();
            double dist = Math.hypot(dx, dy);

            if (dist > 1e-9) {
                double ux = dx / dist;
                double uy = dy / dist;
                double newCenterX = nearestPoint.getX() + ux * radius;
                double newCenterY = nearestPoint.getY() + uy * radius;
                return new LineTangentResult(newCenterX, newCenterY, sourceObj);
            }
        }
        return null;
    }

    /**
     * 检测圆心到所有边（线段、多边形边）的吸附
     * 支持圆心吸附到线段、无限直线和多边形的边上
     */
    public static CircleCenterToLineResult findCircleCenterToAllEdgesSnap(double centerX, double centerY,
                                                                          List<WorldObject> objects,
                                                                          double threshold,
                                                                          WorldObject excludedObject) {
        CircleCenterToLineResult bestResult = null;
        double minDistance = Double.MAX_VALUE;

        for (WorldObject obj : objects) {
            if (obj == excludedObject) {
                continue;
            }

            // 处理线段
            if (obj instanceof LineGeo line) {
                Point2D nearestPoint = projectPointOntoSegment(centerX, centerY,
                        line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
                double dist = Math.hypot(nearestPoint.getX() - centerX, nearestPoint.getY() - centerY);
                if (dist < threshold && dist < minDistance) {
                    minDistance = dist;
                    bestResult = new CircleCenterToLineResult(nearestPoint.getX(), nearestPoint.getY(), obj, dist);
                }
            }
            // 处理无限直线
            else if (obj instanceof InfiniteLineGeo infiniteLine) {
                Point2D nearestPoint = projectPointOntoLine(centerX, centerY,
                        infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(),
                        infiniteLine.getPoint2X(), infiniteLine.getPoint2Y());
                double dist = Math.hypot(nearestPoint.getX() - centerX, nearestPoint.getY() - centerY);
                if (dist < threshold && dist < minDistance) {
                    minDistance = dist;
                    bestResult = new CircleCenterToLineResult(nearestPoint.getX(), nearestPoint.getY(), obj, dist);
                }
            }
            // 处理多边形的所有边
            else if (obj instanceof PolygonGeo polygon) {
                int vertexCount = polygon.getVertexCount();
                for (int i = 0; i < vertexCount; i++) {
                    Point2D p1 = polygon.getVertex(i);
                    Point2D p2 = polygon.getVertex((i + 1) % vertexCount);
                    Point2D nearestPoint = projectPointOntoSegment(centerX, centerY,
                            p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    double dist = Math.hypot(nearestPoint.getX() - centerX, nearestPoint.getY() - centerY);
                    if (dist < threshold && dist < minDistance) {
                        minDistance = dist;
                        bestResult = new CircleCenterToLineResult(nearestPoint.getX(), nearestPoint.getY(), obj, dist);
                    }
                }
            }
        }

        return bestResult;
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
     * 检测圆与直线相切吸附
     * <p>
     * 当拖动圆心时，检测圆的边缘是否靠近直线，
     * 如果圆的边缘在阈值范围内接近直线，则调整圆心位置使得圆与直线恰好相切
     * <p>
     * 关键：只有当圆的边缘（而不是圆心）靠近直线时才触发吸附，
     * 避免圆心靠近直线时产生"排斥"的感觉
     *
     * @param centerX   圆心X
     * @param centerY   圆心Y
     * @param radius    圆半径
     * @param objects   所有几何对象
     * @param threshold 吸附阈值（圆边缘到直线的距离小于此值时触发）
     * @return 切线吸附结果，如果没有则返回null
     */
    public static LineTangentResult findLineTangentSnap(double centerX, double centerY,
                                                        double radius,
                                                        List<WorldObject> objects,
                                                        double threshold) {
        return findLineTangentSnap(centerX, centerY, radius, objects, threshold, null);
    }

    /**
     * 检测圆与直线相切吸附（带排除对象）
     *
     * @param centerX        圆心X
     * @param centerY        圆心Y
     * @param radius         圆半径
     * @param objects        所有几何对象
     * @param threshold      吸附阈值
     * @param excludedObject 要排除的对象（通常是被拖动的圆）
     * @return 切线吸附结果，如果没有则返回null
     */
    public static LineTangentResult findLineTangentSnap(double centerX, double centerY,
                                                        double radius,
                                                        List<WorldObject> objects,
                                                        double threshold,
                                                        WorldObject excludedObject) {
        LineTangentResult bestResult = null;
        double minEdgeDistance = Double.MAX_VALUE;

        for (WorldObject obj : objects) {
            // 排除被拖动的对象自己
            if (obj == excludedObject) {
                continue;
            }

            Point2D nearestPoint = null;
            double distanceToLine = 0;

            if (obj instanceof LineGeo line) {
                // 线段：计算圆心到线段的距离
                nearestPoint = projectPointOntoSegment(centerX, centerY,
                        line.getStartX(), line.getStartY(),
                        line.getEndX(), line.getEndY());
                distanceToLine = Math.hypot(nearestPoint.getX() - centerX,
                        nearestPoint.getY() - centerY);
            } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                // 无限直线：计算圆心到直线的距离
                nearestPoint = projectPointOntoLine(centerX, centerY,
                        infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(),
                        infiniteLine.getPoint2X(), infiniteLine.getPoint2Y());
                distanceToLine = Math.hypot(nearestPoint.getX() - centerX,
                        nearestPoint.getY() - centerY);
            }

            if (nearestPoint != null) {
                // 计算圆边缘到直线的距离
                // edgeDistance = |distanceToLine - radius|
                double edgeDistance = Math.abs(distanceToLine - radius);

                // 只有当圆的边缘靠近直线时才触发吸附
                // 同时要求圆心不能太靠近直线（避免与圆心吸附冲突）
                if (edgeDistance < threshold && edgeDistance < minEdgeDistance && distanceToLine > radius * 0.1) {
                    // 计算调整后的圆心位置，使圆与直线相切
                    // 新圆心 = 垂足 + 方向单位向量 * 半径
                    double dx = centerX - nearestPoint.getX();
                    double dy = centerY - nearestPoint.getY();

                    // 计算方向单位向量
                    double dist = Math.hypot(dx, dy);
                    if (dist > 1e-9) {
                        double ux = dx / dist;
                        double uy = dy / dist;

                        // 新圆心距离垂足恰好等于半径
                        double newCenterX = nearestPoint.getX() + ux * radius;
                        double newCenterY = nearestPoint.getY() + uy * radius;

                        minEdgeDistance = edgeDistance;
                        bestResult = new LineTangentResult(newCenterX, newCenterY, obj);
                    }
                }
            }
        }

        return bestResult;
    }

    /**
     * 将点投影到线段上
     */
    private static Point2D projectPointOntoSegment(double px, double py,
                                                   double x1, double y1,
                                                   double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return new Point2D(x1, y1);
        }

        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));
        return new Point2D(x1 + t * dx, y1 + t * dy);
    }

    /**
     * 将点投影到无限直线上
     */
    private static Point2D projectPointOntoLine(double px, double py,
                                                double x1, double y1,
                                                double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            return new Point2D(x1, y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
        return new Point2D(x1 + t * dx, y1 + t * dy);
    }

    /**
     * 检测圆心到直线的吸附
     * <p>
     * 当拖动圆心时，如果圆心本身靠近直线（在阈值范围内），
     * 则将圆心吸附到直线上最近的点（垂足）
     *
     * @param centerX        圆心X
     * @param centerY        圆心Y
     * @param objects        所有几何对象
     * @param threshold      吸附阈值（圆心到直线的距离小于此值时触发）
     * @param excludedObject 要排除的对象（通常是被拖动的圆）
     * @return 圆心吸附结果，如果没有则返回null
     */
    public static CircleCenterToLineResult findCircleCenterToLineSnap(double centerX, double centerY,
                                                                      List<WorldObject> objects,
                                                                      double threshold,
                                                                      WorldObject excludedObject) {
        CircleCenterToLineResult bestResult = null;
        double minDistance = Double.MAX_VALUE;

        for (WorldObject obj : objects) {
            // 排除被拖动的对象自己
            if (obj == excludedObject) {
                continue;
            }

            Point2D nearestPoint = null;
            double distanceToLine = 0;

            if (obj instanceof LineGeo line) {
                // 线段：计算圆心到线段的距离和垂足
                nearestPoint = projectPointOntoSegment(centerX, centerY,
                        line.getStartX(), line.getStartY(),
                        line.getEndX(), line.getEndY());
                distanceToLine = Math.hypot(nearestPoint.getX() - centerX,
                        nearestPoint.getY() - centerY);
            } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                // 无限直线：计算圆心到直线的距离和垂足
                nearestPoint = projectPointOntoLine(centerX, centerY,
                        infiniteLine.getPoint1X(), infiniteLine.getPoint1Y(),
                        infiniteLine.getPoint2X(), infiniteLine.getPoint2Y());
                distanceToLine = Math.hypot(nearestPoint.getX() - centerX,
                        nearestPoint.getY() - centerY);
            }

            // 如果圆心靠近直线（在阈值范围内），则记录吸附点
            if (nearestPoint != null && distanceToLine < threshold && distanceToLine < minDistance) {
                minDistance = distanceToLine;
                bestResult = new CircleCenterToLineResult(nearestPoint.getX(), nearestPoint.getY(), obj, distanceToLine);
            }
        }

        return bestResult;
    }

    /**
     * 圆与圆相切吸附结果（拖动时）
     */
    public static class CircleToCircleSnapResult {
        private final double centerX;
        private final double centerY;
        private final CircleGeo targetCircle;
        private final boolean isExternal;

        public CircleToCircleSnapResult(double centerX, double centerY, CircleGeo targetCircle, boolean isExternal) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.targetCircle = targetCircle;
            this.isExternal = isExternal;
        }

        public double getCenterX() {
            return centerX;
        }

        public double getCenterY() {
            return centerY;
        }

        public CircleGeo getTargetCircle() {
            return targetCircle;
        }

        public boolean isExternal() {
            return isExternal;
        }
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
     * 圆心到直线吸附结果
     */
    public static class CircleCenterToLineResult {
        private final double centerX;
        private final double centerY;
        private final WorldObject targetLine;
        private final double distance;

        public CircleCenterToLineResult(double centerX, double centerY, WorldObject targetLine, double distance) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.targetLine = targetLine;
            this.distance = distance;
        }

        public double getCenterX() {
            return centerX;
        }

        public double getCenterY() {
            return centerY;
        }

        public WorldObject getTargetLine() {
            return targetLine;
        }

        public double getDistance() {
            return distance;
        }
    }

    /**
     * 圆与直线相切吸附结果
     */
    public static class LineTangentResult {
        private final double centerX;
        private final double centerY;
        private final WorldObject targetLine;

        public LineTangentResult(double centerX, double centerY, WorldObject targetLine) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.targetLine = targetLine;
        }

        public double getCenterX() {
            return centerX;
        }

        public double getCenterY() {
            return centerY;
        }

        public WorldObject getTargetLine() {
            return targetLine;
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
