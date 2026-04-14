package com.bingbaihanji.util.constraint;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.geometry.Point2D;

/**
 * 参数化点约束接口
 * <p>
 * 约束点记录在图形上的参数位置,当图形变化时,约束点根据参数重新计算位置。
 * 这种方式确保约束点始终保持在图形上的相对位置。
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public interface PointConstraint {

    /**
     * 根据参数计算点在图形上的实际坐标
     * <p>
     * 例如：线段上t=0.2表示距起点20%的位置
     *
     * @return 计算后的坐标点
     */
    Point2D getPointFromParameter();

    /**
     * 根据给定坐标计算最近的参数值
     * <p>
     * 用于创建约束点时,根据鼠标点击位置确定参数
     *
     * @param x 目标x坐标
     * @param y 目标y坐标
     * @return 最近的参数值
     */
    double calculateParameter(double x, double y);

    /**
     * 获取当前参数值
     *
     * @return 当前参数
     */
    double getParameter();

    /**
     * 设置参数值
     *
     * @param parameter 新的参数值
     */
    void setParameter(double parameter);

    /**
     * 获取被约束的图形对象
     * <p>
     * 用于依赖关系管理
     *
     * @return 被约束的图形对象
     */
    WorldObject getConstrainedShape();

    /**
     * 获取约束类型标识
     *
     * @return 约束类型字符串(如 "LineConstraint", "CircleConstraint")
     */
    String getConstraintType();

    /**
     * 计算给定坐标到约束图形的距离
     * <p>
     * 用于吸附检测
     *
     * @param x x坐标
     * @param y y坐标
     * @return 距离
     */
    double distanceToShape(double x, double y);

    /**
     * 判断是否是顶点约束
     * <p>
     * 顶点约束意味着点本身就是几何图形的顶点/端点,
     * 拖动时参数应固定不变,带动整个顶点移动
     *
     * @return true表示是顶点约束
     */
    boolean isVertexConstraint();

    /**
     * 设置为顶点约束
     * <p>
     * 检查给定点是否是约束图形的顶点/端点,
     * 如果是,则锁定参数为对应的顶点参数
     *
     * @param point 要检查的点对象
     */
    void setAsVertexConstraintIfApplicable(com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point);
}
