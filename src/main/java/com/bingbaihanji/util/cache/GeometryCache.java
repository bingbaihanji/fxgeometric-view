package com.bingbaihanji.util.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 几何计算结果缓存
 * <p>
 * 使用 LRU 缓存策略，提高重复计算的性能
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class GeometryCache<K, V> {

    private final int maxSize;
    private final Map<K, V> cache;

    /**
     * 构造函数
     *
     * @param maxSize 最大缓存容量
     */
    public GeometryCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > GeometryCache.this.maxSize;
            }
        };
    }

    /**
     * 获取缓存值
     *
     * @param key 键
     * @return 缓存值，不存在则返回 null
     */
    public synchronized V get(K key) {
        return cache.get(key);
    }

    /**
     * 添加缓存
     *
     * @param key   键
     * @param value 值
     */
    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * 清空缓存
     */
    public synchronized void clear() {
        cache.clear();
    }

    /**
     * 获取缓存大小
     */
    public synchronized int size() {
        return cache.size();
    }

    /**
     * 检查是否包含指定键
     */
    public synchronized boolean containsKey(K key) {
        return cache.containsKey(key);
    }
}
