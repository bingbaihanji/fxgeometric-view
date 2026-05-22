package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.visitor.IntersectionVisitor;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 交点计算处理器
 * <p>
 * 使用 IntersectionVisitor(访问者模式)替代手动 instanceof 树,
 * 新增几何类型时自动获得交点计算支持
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class IntersectionHandler {

    /**
     * 创建交点对象
     */
    private PointGeo createIntersectionPoint(double x, double y) {
        PointGeo point = new PointGeo(x, y, false);
        point.setColor(GeometryConfig.Colors.INTERSECTION_POINT);
        return point;
    }

    /**
     * 检查新添加的对象与已有对象的交点
     *
     * @param newObject 新添加的对象
     * @param context   绘制上下文
     * @return 交点列表
     */
    public List<PointGeo> checkIntersections(Object newObject, IDrawingContext context) {
        List<WorldObject> allObjects = new ArrayList<>(context.getObjects());
        List<PointGeo> intersectionPoints = new ArrayList<>();
        WorldObject newObj = (WorldObject) newObject;

        for (WorldObject obj : allObjects) {
            if (obj == newObject) continue;

            List<Point2D> points = newObj.accept(new IntersectionVisitor(obj));
            for (Point2D point : points) {
                intersectionPoints.add(createIntersectionPoint(point.getX(), point.getY()));
            }
        }

        return intersectionPoints;
    }

    /**
     * 重新计算所有图形之间的交点
     */
    public void recalculateAllIntersections(IDrawingContext context) {
        List<WorldObject> allObjects = new ArrayList<>(context.getObjects());
        for (WorldObject obj : allObjects) {
            if (obj instanceof PointGeo point && isIntersectionPoint(point)) {
                context.removeObject(obj);
            }
        }

        List<WorldObject> objects = new ArrayList<>(context.getObjects());
        List<PointGeo> newIntersectionPoints = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            WorldObject obj1 = objects.get(i);
            if (obj1 instanceof PointGeo) continue;

            for (int j = i + 1; j < objects.size(); j++) {
                WorldObject obj2 = objects.get(j);
                if (obj2 instanceof PointGeo) continue;

                List<Point2D> intersections = calculateIntersections(obj1, obj2);
                for (Point2D point : intersections) {
                    newIntersectionPoints.add(createIntersectionPoint(point.getX(), point.getY()));
                }
            }
        }

        for (PointGeo point : newIntersectionPoints) {
            context.addObject(point);
        }
    }

    /**
     * 计算两个几何对象之间的交点
     */
    public List<Point2D> calculateIntersections(WorldObject obj1, WorldObject obj2) {
        return obj1.accept(new IntersectionVisitor(obj2));
    }

    /**
     * 判断点是否为交点(通过颜色判断)
     */
    public boolean isIntersectionPoint(PointGeo point) {
        Color color = point.getColor();
        return GeometryConfig.Colors.INTERSECTION_POINT.equals(color);
    }
}
