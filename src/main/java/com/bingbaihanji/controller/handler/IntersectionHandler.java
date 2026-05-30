package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.visitor.IntersectionVisitor;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Point2D;

import java.util.*;

/**
 * 交点计算处理器（增量原地更新版）
 * <p>
 * 参考 GeoGebra 的 AlgoIntersection 模式：
 * - 用 ownershipMap 追踪每个交点由哪两个图形产生
 * - 拖动时仅重算涉及被拖对象的交点，原地更新坐标（O(k)，非 O(n²)）
 * - 消除"删除-重建"的闪烁间隙
 * <p>
 * 多个交点支持：ownershipMap 的值为列表（如直线与圆可有 0~2 个交点），
 * 增量更新时优先原地复用已有 PointGeo 对象。
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class IntersectionHandler {

    /**
     * 交点所有权映射：无序对象对 → 交点列表
     * <p>
     * 一对图形可能产生多个交点（直线×圆 → 最多2个，圆×圆 → 最多2个）
     */
    private final Map<IntersectionPair, List<PointGeo>> ownershipMap = new HashMap<>();

    /**
     * 获取交点所有权映射（供外部查询）
     */
    public Map<IntersectionPair, List<PointGeo>> getOwnershipMap() {
        return Collections.unmodifiableMap(ownershipMap);
    }

    /**
     * 记录所有权 — 将交点添加到对应 pair 的列表中
     */
    private void addOwnership(WorldObject obj1, WorldObject obj2, PointGeo point) {
        IntersectionPair pair = new IntersectionPair(obj1, obj2);
        ownershipMap.computeIfAbsent(pair, k -> new ArrayList<>()).add(point);
    }

    /**
     * 移除某个图形的所有交点并清理所有权记录
     */
    private void unregisterOwner(WorldObject obj, IDrawingContext context) {
        List<IntersectionPair> toRemove = new ArrayList<>();
        for (Map.Entry<IntersectionPair, List<PointGeo>> entry : ownershipMap.entrySet()) {
            if (entry.getKey().contains(obj)) {
                for (PointGeo point : entry.getValue()) {
                    context.removeObject(point);
                }
                toRemove.add(entry.getKey());
            }
        }
        for (IntersectionPair key : toRemove) {
            ownershipMap.remove(key);
        }
    }

    // ==================== 增量更新（拖动时调用） ====================

    /**
     * 增量更新受拖动的图形影响的所有交点。
     * <p>
     * 参考 GeoGebra 的更新级联机制：
     * 1. 找到 ownershipMap 中所有与 movedObj 配对的图形
     * 2. 对每对重新计算交点，原地更新已有 PointGeo，不足的补建、多余的删除
     * 3. 检测 movedObj 与其他图形的新交点
     *
     * @param movedObj 被拖动的图形对象
     * @param context  绘图上下文
     */
    public void updateAffectedIntersections(WorldObject movedObj, IDrawingContext context) {
        List<WorldObject> allObjects = new ArrayList<>(context.getObjects());
        Set<WorldObject> handledPartners = new HashSet<>();

        // 遍历所有已注册的交点配对，更新涉及 movedObj 的
        for (Map.Entry<IntersectionPair, List<PointGeo>> entry :
                new ArrayList<>(ownershipMap.entrySet())) {
            IntersectionPair pair = entry.getKey();
            if (!pair.contains(movedObj)) continue;

            WorldObject other = pair.getOther(movedObj);
            handledPartners.add(other);
            List<PointGeo> existingPoints = entry.getValue();

            // 重新计算交点
            List<Point2D> newPositions = calculateIntersections(movedObj, other);

            if (newPositions.isEmpty()) {
                // 不再相交 — 删除所有交点
                for (PointGeo p : existingPoints) {
                    context.removeObject(p);
                }
                ownershipMap.remove(pair);
            } else {
                // 原地更新现有交点，不足则补建，多余则删除
                int reuseCount = Math.min(existingPoints.size(), newPositions.size());

                for (int i = 0; i < reuseCount; i++) {
                    PointGeo existing = existingPoints.get(i);
                    Point2D newPos = newPositions.get(i);
                    existing.setPositionDirectly(newPos.getX(), newPos.getY());
                }

                // 移除多余的交点
                for (int i = existingPoints.size() - 1; i >= reuseCount; i--) {
                    context.removeObject(existingPoints.get(i));
                    existingPoints.remove(i);
                }

                // 创建不足的交点
                for (int i = reuseCount; i < newPositions.size(); i++) {
                    Point2D newPos = newPositions.get(i);
                    PointGeo newPoint = createIntersectionPoint(newPos.getX(), newPos.getY());
                    existingPoints.add(newPoint);
                    context.addObject(newPoint);
                }
            }
        }

        // 检测新交点：movedObj 与尚未配对的图形
        for (WorldObject obj : allObjects) {
            if (obj == movedObj || obj instanceof PointGeo) continue;
            if (handledPartners.contains(obj)) continue;

            IntersectionPair pair = new IntersectionPair(movedObj, obj);
            if (ownershipMap.containsKey(pair)) continue;

            List<Point2D> intersections = calculateIntersections(movedObj, obj);
            if (!intersections.isEmpty()) {
                List<PointGeo> newPoints = new ArrayList<>();
                for (Point2D point : intersections) {
                    PointGeo newPoint = createIntersectionPoint(point.getX(), point.getY());
                    newPoints.add(newPoint);
                    context.addObject(newPoint);
                }
                ownershipMap.put(pair, newPoints);
            }
        }
    }

    // ==================== 全量计算（对象创建/删除时调用） ====================

    /**
     * 检查新添加的对象与已有对象的交点（创建时调用）。
     * <p>
     * 同时注册所有权到 ownershipMap。
     */
    public List<PointGeo> checkIntersections(WorldObject newObject, IDrawingContext context) {
        List<WorldObject> allObjects = new ArrayList<>(context.getObjects());
        List<PointGeo> intersectionPoints = new ArrayList<>();

        for (WorldObject obj : allObjects) {
            if (obj == newObject || obj instanceof PointGeo) continue;

            List<Point2D> points = newObject.accept(new IntersectionVisitor(obj));
            if (points.isEmpty()) continue;

            List<PointGeo> pairPoints = new ArrayList<>();
            for (Point2D point : points) {
                PointGeo created = createIntersectionPoint(point.getX(), point.getY());
                pairPoints.add(created);
                intersectionPoints.add(created);
            }
            ownershipMap.put(new IntersectionPair(newObject, obj), pairPoints);
        }

        return intersectionPoints;
    }

    /**
     * 全量重算所有交点（撤消/重做后调用）。
     * <p>
     * 清空 ownershipMap 并重新构建，保证状态一致。
     */
    public void recalculateAllIntersections(IDrawingContext context) {
        // 清除所有旧交点
        for (List<PointGeo> points : ownershipMap.values()) {
            for (PointGeo point : points) {
                context.removeObject(point);
            }
        }
        ownershipMap.clear();

        // 全量 O(n²) 重算
        List<WorldObject> objects = new ArrayList<>(context.getObjects());
        for (int i = 0; i < objects.size(); i++) {
            WorldObject obj1 = objects.get(i);
            if (obj1 instanceof PointGeo) continue;

            for (int j = i + 1; j < objects.size(); j++) {
                WorldObject obj2 = objects.get(j);
                if (obj2 instanceof PointGeo) continue;

                List<Point2D> intersections = calculateIntersections(obj1, obj2);
                if (!intersections.isEmpty()) {
                    List<PointGeo> pairPoints = new ArrayList<>();
                    for (Point2D point : intersections) {
                        PointGeo created = createIntersectionPoint(point.getX(), point.getY());
                        pairPoints.add(created);
                        context.addObject(created);
                    }
                    ownershipMap.put(new IntersectionPair(obj1, obj2), pairPoints);
                }
            }
        }
    }

    // ==================== 对象删除时清理所有权 ====================

    /**
     * 删除图形时清理其所有权记录（在 context.removeObject 或 clearAllObjects 之前调用）。
     * <p>
     * 同时移除该图形相关的所有交点。
     */
    public void cleanupOwnership(WorldObject obj, IDrawingContext context) {
        unregisterOwner(obj, context);
    }

    /**
     * 清空所有权映射（在 clearAllObjects 时调用）
     */
    public void clearOwnershipMap() {
        ownershipMap.clear();
    }

    // ==================== 工具方法 ====================

    /**
     * 计算两个几何对象之间的交点
     */
    public List<Point2D> calculateIntersections(WorldObject obj1, WorldObject obj2) {
        return obj1.accept(new IntersectionVisitor(obj2));
    }

    /**
     * 判断点是否为交点（通过颜色判断）
     */
    public boolean isIntersectionPoint(PointGeo point) {
        return GeometryConfig.Colors.INTERSECTION_POINT.equals(point.getColor());
    }

    /**
     * 创建交点对象
     */
    private PointGeo createIntersectionPoint(double x, double y) {
        PointGeo point = new PointGeo(x, y, false);
        point.setColor(GeometryConfig.Colors.INTERSECTION_POINT);
        return point;
    }

    // ==================== 内部类：无序对象对 ====================

    /**
     * 无序对象对，用作 ownershipMap 的 key。
     * <p>
     * 基于 identityHashCode 排序以保证 (A,B) 和 (B,A) 的等价性。
     * 使用身份相等（==）而非值相等，确保即使同类型同坐标的图形也能区分。
     */
    public static class IntersectionPair {
        final WorldObject a;
        final WorldObject b;

        IntersectionPair(WorldObject a, WorldObject b) {
            if (System.identityHashCode(a) <= System.identityHashCode(b)) {
                this.a = a;
                this.b = b;
            } else {
                this.a = b;
                this.b = a;
            }
        }

        public WorldObject getA() {
            return a;
        }

        public WorldObject getB() {
            return b;
        }

        boolean contains(WorldObject obj) {
            return a == obj || b == obj;
        }

        WorldObject getOther(WorldObject obj) {
            return a == obj ? b : a;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IntersectionPair that)) return false;
            return (a == that.a && b == that.b) || (a == that.b && b == that.a);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(a) ^ System.identityHashCode(b);
        }
    }
}
