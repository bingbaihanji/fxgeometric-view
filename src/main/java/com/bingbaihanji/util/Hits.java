package com.bingbaihanji.util;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 命中测试结果管理器
 * <p>
 * 参考 GeoGebra 的 Hits 类设计,用于管理和过滤命中的对象
 * 支持按类型、层次、优先级排序和筛选
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public class Hits {

    /**
     * 命中对象列表(按Z-order降序,即最上层的对象在前面)
     */
    private final List<WorldObject> hits = new ArrayList<>();

    /**
     * 创建命中测试结果
     *
     * @param objects   所有可能的对象
     * @param worldX    点击的世界X坐标
     * @param worldY    点击的世界Y坐标
     * @param tolerance 容差
     * @return 命中测试结果
     */
    public static Hits performHitTest(List<WorldObject> objects, double worldX, double worldY, double tolerance) {
        Hits hits = new Hits();

        // 从后往前遍历(后添加的对象在上层)
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj.hitTest(worldX, worldY, tolerance)) {
                hits.add(obj);
            }
        }

        // 按优先级排序
        hits.sortByZOrder();
        return hits;
    }

    /**
     * 检测控制点命中
     *
     * @param objects   所有可能的对象
     * @param worldX    点击的世界X坐标
     * @param worldY    点击的世界Y坐标
     * @param tolerance 容差
     * @return 命中的控制点,如果没有则返回null
     */
    public static HitPoint performPointHitTest(List<WorldObject> objects, double worldX, double worldY, double tolerance) {
        // 优先检查所有控制点
        for (WorldObject obj : objects) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, tolerance)) {
                    return new HitPoint(point, obj);
                }
            }
        }
        return null;
    }

    /**
     * 添加命中对象
     */
    public void add(WorldObject obj) {
        if (!hits.contains(obj)) {
            hits.add(obj);
        }
    }

    /**
     * 添加多个命中对象
     */
    public void addAll(List<WorldObject> objects) {
        for (WorldObject obj : objects) {
            add(obj);
        }
    }

    /**
     * 清空所有命中对象
     */
    public void clear() {
        hits.clear();
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return hits.isEmpty();
    }

    /**
     * 获取命中对象数量
     */
    public int size() {
        return hits.size();
    }

    /**
     * 获取指定位置的对象
     */
    public WorldObject get(int index) {
        return hits.get(index);
    }

    /**
     * 获取所有命中对象(副本)
     */
    public List<WorldObject> getAll() {
        return new ArrayList<>(hits);
    }

    /**
     * 获取最上层(第一个)命中对象
     */
    public WorldObject getTopHit() {
        return isEmpty() ? null : hits.get(0);
    }

    // ========== 类型过滤方法 ==========

    /**
     * 按Z-order降序排序(最上层在前)
     * <p>
     * 排序规则:
     * 1. 点对象优先级最高(最容易被选中)
     * 2. 线段次之
     * 3. 圆和多边形再次之
     * 4. 函数优先级最低
     */
    public void sortByZOrder() {
        hits.sort((o1, o2) -> {
            int priority1 = getTypePriority(o1);
            int priority2 = getTypePriority(o2);
            return Integer.compare(priority2, priority1); // 降序
        });
    }

    /**
     * 获取对象类型优先级
     * <p>
     * 优先级越高,越容易被选中
     */
    private int getTypePriority(WorldObject obj) {
        if (obj instanceof PointGeo) return 100;
        if (obj instanceof LineGeo) return 80;
        if (obj instanceof InfiniteLineGeo) return 75;
        if (obj instanceof CircleGeo) return 60;
        if (obj instanceof PolygonGeo) return 50;
        if (obj instanceof PathGeo) return 40;
        if (obj instanceof FunctionGeo) return 20;
        return 0; // 默认
    }

    /**
     * 只保留点对象
     */
    public void keepOnlyPoints() {
        hits.removeIf(obj -> !(obj instanceof PointGeo));
    }

    /**
     * 移除所有点对象
     */
    public void removeAllPoints() {
        hits.removeIf(obj -> obj instanceof PointGeo);
    }

    /**
     * 只保留线段
     */
    public void keepOnlyLines() {
        hits.removeIf(obj -> !(obj instanceof LineGeo));
    }

    /**
     * 移除所有线段
     */
    public void removeAllLines() {
        hits.removeIf(obj -> obj instanceof LineGeo);
    }

    /**
     * 只保留圆形
     */
    public void keepOnlyCircles() {
        hits.removeIf(obj -> !(obj instanceof CircleGeo));
    }

    /**
     * 移除所有圆形
     */
    public void removeAllCircles() {
        hits.removeIf(obj -> obj instanceof CircleGeo);
    }

    /**
     * 只保留多边形
     */
    public void keepOnlyPolygons() {
        hits.removeIf(obj -> !(obj instanceof PolygonGeo));
    }

    /**
     * 移除所有多边形
     */
    public void removeAllPolygons() {
        hits.removeIf(obj -> obj instanceof PolygonGeo);
    }

    /**
     * 只保留函数
     */
    public void keepOnlyFunctions() {
        hits.removeIf(obj -> !(obj instanceof FunctionGeo));
    }

    // ========== 高级过滤方法 ==========

    /**
     * 移除所有函数
     */
    public void removeAllFunctions() {
        hits.removeIf(obj -> obj instanceof FunctionGeo);
    }

    /**
     * 移除所有路径
     */
    public void removeAllPaths() {
        hits.removeIf(obj -> obj instanceof PathGeo);
    }

    /**
     * 按条件过滤
     */
    public void filter(Predicate<WorldObject> condition) {
        hits.removeIf(obj -> !condition.test(obj));
    }

    /**
     * 只保留指定类型的对象
     */
    public void keepOnly(Class<? extends WorldObject> clazz) {
        hits.removeIf(obj -> !clazz.isInstance(obj));
    }

    /**
     * 移除指定类型的对象
     */
    public void removeType(Class<? extends WorldObject> clazz) {
        hits.removeIf(clazz::isInstance);
    }

    // ========== 查询方法 ==========

    /**
     * 移除指定的对象
     */
    public void remove(WorldObject obj) {
        hits.remove(obj);
    }

    /**
     * 移除指定对象列表
     */
    public void removeAll(List<WorldObject> objects) {
        hits.removeAll(objects);
    }

    /**
     * 是否包含指定类型
     */
    public boolean hasType(Class<? extends WorldObject> clazz) {
        return hits.stream().anyMatch(clazz::isInstance);
    }

    /**
     * 是否包含点对象
     */
    public boolean hasPoint() {
        return hasType(PointGeo.class);
    }

    /**
     * 是否包含线段
     */
    public boolean hasLine() {
        return hasType(LineGeo.class);
    }

    /**
     * 是否包含圆形
     */
    public boolean hasCircle() {
        return hasType(CircleGeo.class);
    }

    /**
     * 是否包含多边形
     */
    public boolean hasPolygon() {
        return hasType(PolygonGeo.class);
    }

    /**
     * 是否包含函数
     */
    public boolean hasFunction() {
        return hasType(FunctionGeo.class);
    }

    /**
     * 获取指定类型的所有对象
     */
    @SuppressWarnings("unchecked")
    public <T extends WorldObject> List<T> getByType(Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (WorldObject obj : hits) {
            if (clazz.isInstance(obj)) {
                result.add((T) obj);
            }
        }
        return result;
    }

    /**
     * 获取所有点对象
     */
    public List<PointGeo> getPoints() {
        return getByType(PointGeo.class);
    }

    /**
     * 获取所有线段
     */
    public List<LineGeo> getLines() {
        return getByType(LineGeo.class);
    }

    /**
     * 获取所有圆形
     */
    public List<CircleGeo> getCircles() {
        return getByType(CircleGeo.class);
    }

    /**
     * 获取所有多边形
     */
    public List<PolygonGeo> getPolygons() {
        return getByType(PolygonGeo.class);
    }

    // ========== 统计方法 ==========

    /**
     * 获取所有函数
     */
    public List<FunctionGeo> getFunctions() {
        return getByType(FunctionGeo.class);
    }

    /**
     * 获取第一个指定类型的对象
     */
    public <T extends WorldObject> T getFirstOfType(Class<T> clazz) {
        List<T> list = getByType(clazz);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 统计点对象数量
     */
    public int countPoints() {
        return (int) hits.stream().filter(obj -> obj instanceof PointGeo).count();
    }

    /**
     * 统计线段数量
     */
    public int countLines() {
        return (int) hits.stream().filter(obj -> obj instanceof LineGeo).count();
    }

    // ========== 工具方法 ==========

    /**
     * 统计圆形数量
     */
    public int countCircles() {
        return (int) hits.stream().filter(obj -> obj instanceof CircleGeo).count();
    }

    /**
     * 统计多边形数量
     */
    public int countPolygons() {
        return (int) hits.stream().filter(obj -> obj instanceof PolygonGeo).count();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Hits[");
        for (int i = 0; i < hits.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(hits.get(i).getClass().getSimpleName());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 命中的控制点信息
     */
    public static class HitPoint {
        private final WorldObject.DraggablePoint point;
        private final WorldObject owner;

        public HitPoint(WorldObject.DraggablePoint point, WorldObject owner) {
            this.point = point;
            this.owner = owner;
        }

        public WorldObject.DraggablePoint getPoint() {
            return point;
        }

        public WorldObject getOwner() {
            return owner;
        }
    }
}
