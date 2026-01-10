package com.bingbaihanji.view.layout.draw.geometry;

import com.bingbaihanji.view.layout.draw.geometry.impl.*;

/**
 * 几何对象访问者接口
 * <p>
 * 使用访问者模式消除类型判断，提高扩展性和类型安全
 * <p>
 * 使用场景：
 * - 边吸附计算
 * - 点复用检查
 * - 特殊点提取
 * - 导出不同格式
 * - 碰撞检测等
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public interface GeometryVisitor<T> {

    /**
     * 访问点对象
     */
    T visitPoint(PointGeo point);

    /**
     * 访问线段对象
     */
    T visitLine(LineGeo line);

    /**
     * 访问无限直线对象
     */
    T visitInfiniteLine(InfiniteLineGeo infiniteLine);

    /**
     * 访问圆对象
     */
    T visitCircle(CircleGeo circle);

    /**
     * 访问多边形对象
     */
    T visitPolygon(PolygonGeo polygon);

    /**
     * 访问正多边形对象
     */
    T visitRegularPolygon(RegularPolygonGeo regularPolygon);

    /**
     * 访问手绘路径对象
     */
    T visitPath(PathGeo path);

    /**
     * 访问函数对象
     */
    T visitFunction(FunctionGeo function);

    /**
     * 访问其他未知类型（默认处理）
     */
    default T visitOther(WorldObject object) {
        return null;
    }
}
