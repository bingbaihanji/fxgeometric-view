package com.binbaihanji.util;

import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.Map;

/**
 * 点命名管理器
 * <p>
 * 按照出现顺序自动为点命名：A, B, C, ..., Z, A1, B1, ..., Z1, A2, B2, ...
 *
 * @author bingbaihanji
 * @date 2025-12-24
 */
public class PointNameManager {

    /**
     * 坐标精度阈值（用于判断两个点是否相同）
     */
    private static final double EPSILON = 1e-6;
    /**
     * 单例实例
     */
    private static PointNameManager instance;
    /**
     * 点坐标到名称的映射
     */
    private final Map<String, String> pointNameMap = new HashMap<>();
    /**
     * 名称到索引的映射（用于跟踪已使用的索引）
     */
    private final Map<String, Integer> nameToIndexMap = new HashMap<>();
    /**
     * 下一个可用的命名索引
     */
    private int nextAvailableIndex = 0;

    private PointNameManager() {
    }

    /**
     * 获取单例实例
     */
    public static PointNameManager getInstance() {
        if (instance == null) {
            instance = new PointNameManager();
        }
        return instance;
    }

    /**
     * 为点分配名称（如果已存在则返回现有名称）
     *
     * @param x 点的X坐标
     * @param y 点的Y坐标
     * @return 点的名称
     */
    public String assignName(double x, double y) {
        String key = getPointKey(x, y);

        // 如果点已存在，返回已有名称
        if (pointNameMap.containsKey(key)) {
            return pointNameMap.get(key);
        }

        // 找到下一个未使用的索引
        while (isIndexUsed(nextAvailableIndex)) {
            nextAvailableIndex++;
        }

        // 生成新名称
        String name = generateName(nextAvailableIndex);
        pointNameMap.put(key, name);
        nameToIndexMap.put(name, nextAvailableIndex);
        nextAvailableIndex++;

        return name;
    }

    /**
     * 为点分配名称（Point2D版本）
     */
    public String assignName(Point2D point) {
        return assignName(point.getX(), point.getY());
    }

    /**
     * 获取点的名称（如果不存在则返回null）
     *
     * @param x 点的X坐标
     * @param y 点的Y坐标
     * @return 点的名称，如果不存在则返回null
     */
    public String getName(double x, double y) {
        String key = getPointKey(x, y);
        return pointNameMap.get(key);
    }

    /**
     * 获取点的名称（Point2D版本）
     */
    public String getName(Point2D point) {
        return getName(point.getX(), point.getY());
    }

    /**
     * 检查点是否已命名
     */
    public boolean hasName(double x, double y) {
        String key = getPointKey(x, y);
        return pointNameMap.containsKey(key);
    }

    /**
     * 移除点的名称
     */
    public void removeName(double x, double y) {
        String key = getPointKey(x, y);
        String name = pointNameMap.remove(key);
        if (name != null) {
            nameToIndexMap.remove(name);
        }
    }

    /**
     * 清除所有命名
     */
    public void clear() {
        pointNameMap.clear();
        nameToIndexMap.clear();
        nextAvailableIndex = 0;
    }

    /**
     * 根据索引生成名称
     * 规则：A-Z, A1-Z1, A2-Z2, ...
     *
     * @param index 索引（从0开始）
     * @return 点名称
     */
    private String generateName(int index) {
        int letterIndex = index % 26;
        int numberSuffix = index / 26;

        char letter = (char) ('A' + letterIndex);

        if (numberSuffix == 0) {
            return String.valueOf(letter);
        } else {
            return letter + String.valueOf(numberSuffix);
        }
    }

    /**
     * 生成点的唯一键（基于坐标）
     * 使用四舍五入来处理浮点数精度问题
     */
    private String getPointKey(double x, double y) {
        // 将坐标四舍五入到指定精度
        long xRounded = Math.round(x / EPSILON);
        long yRounded = Math.round(y / EPSILON);
        return xRounded + "," + yRounded;
    }

    /**
     * 获取当前已命名点的数量
     */
    public int getNamedPointCount() {
        return pointNameMap.size();
    }

    /**
     * 检查索引是否已被使用
     */
    private boolean isIndexUsed(int index) {
        String name = generateName(index);
        return nameToIndexMap.containsKey(name);
    }
}
