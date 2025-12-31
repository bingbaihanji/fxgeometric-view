package com.bingbaihanji.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 几何对象 ID 生成器
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 为几何对象生成唯一ID，线程安全
 */
public class ObjectIdGenerator {

    /**
     * 全局 ID 计数器（线程安全）
     */
    private static final AtomicLong idCounter = new AtomicLong(1);

    /**
     * 生成下一个唯一 ID
     *
     * @return 唯一 ID
     */
    public static long nextId() {
        return idCounter.getAndIncrement();
    }

    /**
     * 重置 ID 计数器（通常在清空所有对象后调用）
     */
    public static void reset() {
        idCounter.set(1);
    }

    /**
     * 获取当前 ID 计数值（用于调试）
     *
     * @return 当前计数值
     */
    public static long getCurrentCount() {
        return idCounter.get();
    }
}
