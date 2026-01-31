package com.bingbaihanji.strategy;

import com.bingbaihanji.controller.DrawingContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 复合吸附策略
 * <p>
 * 组合多个吸附策略，按优先级依次尝试吸附
 * 第一个成功的策略将被使用
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class CompositeSnapStrategy implements SnapStrategy {

    private final List<SnapStrategy> strategies = new ArrayList<>();
    private final String name;

    /**
     * 创建复合吸附策略
     *
     * @param name 策略名称
     */
    public CompositeSnapStrategy(String name) {
        this.name = name;
    }

    /**
     * 添加子策略
     *
     * @param strategy 要添加的策略
     */
    public void addStrategy(SnapStrategy strategy) {
        strategies.add(strategy);
        // 按优先级排序（高优先级在前）
        strategies.sort(Comparator.comparingInt(SnapStrategy::getPriority).reversed());
    }

    /**
     * 移除子策略
     */
    public void removeStrategy(SnapStrategy strategy) {
        strategies.remove(strategy);
    }

    /**
     * 清空所有子策略
     */
    public void clearStrategies() {
        strategies.clear();
    }

    @Override
    public double[] snap(double x, double y, DrawingContext context) {
        // 按优先级依次尝试每个策略
        for (SnapStrategy strategy : strategies) {
            double[] result = strategy.snap(x, y, context);
            if (result != null) {
                return result; // 找到第一个成功的策略，立即返回
            }
        }

        return null; // 所有策略都未成功
    }

    @Override
    public int getPriority() {
        // 复合策略的优先级取子策略的最高优先级
        return strategies.stream()
                .mapToInt(SnapStrategy::getPriority)
                .max()
                .orElse(0);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取所有子策略
     */
    public List<SnapStrategy> getStrategies() {
        return new ArrayList<>(strategies);
    }
}
