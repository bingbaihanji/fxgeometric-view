package com.bingbaihanji.strategy;

import com.bingbaihanji.controller.DrawingContext;

/**
 * 吸附策略接口
 * <p>
 * 定义统一的吸附行为规范,支持灵活组合不同的吸附策略
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public interface SnapStrategy {

    /**
     * 应用吸附逻辑
     *
     * @param x       原始世界坐标X
     * @param y       原始世界坐标Y
     * @param context 绘制上下文
     * @return 吸附后的坐标 [x, y],如果不吸附则返回null
     */
    double[] snap(double x, double y, DrawingContext context);

    /**
     * 获取策略的优先级
     * <p>
     * 优先级越高的策略越先执行
     * 推荐范围：1-100,其中：
     * - 100: 最高优先级(如精确点吸附)
     * - 50: 中等优先级(如边吸附)
     * - 1: 最低优先级(如网格吸附)
     *
     * @return 优先级值
     */
    int getPriority();

    /**
     * 策略名称(用于调试和日志)
     */
    String getName();
}
