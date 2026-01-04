package com.bingbaihanji.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性能监控工具
 * <p>
 * 用于监控和统计代码执行性能
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class PerformanceMonitor {

    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class);
    private static final Map<String, PerformanceStats> stats = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Long>> threadTimers = ThreadLocal.withInitial(HashMap::new);

    /**
     * 开始计时
     *
     * @param name 计时器名称
     */
    public static void start(String name) {
        threadTimers.get().put(name, System.nanoTime());
    }

    /**
     * 结束计时并记录
     *
     * @param name 计时器名称
     */
    public static void end(String name) {
        Long startTime = threadTimers.get().get(name);
        if (startTime == null) {
            logger.warn("计时器 {} 未启动", name);
            return;
        }

        long duration = System.nanoTime() - startTime;
        threadTimers.get().remove(name);

        // 更新统计信息
        stats.computeIfAbsent(name, k -> new PerformanceStats())
                .addSample(duration);
    }

    /**
     * 执行并计时
     *
     * @param name     计时器名称
     * @param runnable 要执行的代码
     */
    public static void measure(String name, Runnable runnable) {
        start(name);
        try {
            runnable.run();
        } finally {
            end(name);
        }
    }

    /**
     * 获取统计信息
     *
     * @param name 计时器名称
     * @return 统计信息
     */
    public static PerformanceStats getStats(String name) {
        return stats.get(name);
    }

    /**
     * 打印所有统计信息
     */
    public static void printStats() {
        logger.info("=== 性能统计 ===");
        stats.forEach((name, stat) -> {
            logger.info("{}: 平均={} ms, 最小={} ms, 最大={} ms, 调用次数={}",
                    name,
                    String.format("%.3f", stat.getAverage() / 1_000_000.0),
                    String.format("%.3f", stat.getMin() / 1_000_000.0),
                    String.format("%.3f", stat.getMax() / 1_000_000.0),
                    stat.getCount());
        });
    }

    /**
     * 清空统计信息
     */
    public static void reset() {
        stats.clear();
        threadTimers.get().clear();
    }

    /**
     * 性能统计数据
     */
    public static class PerformanceStats {
        private long count = 0;
        private long total = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;

        public synchronized void addSample(long duration) {
            count++;
            total += duration;
            min = Math.min(min, duration);
            max = Math.max(max, duration);
        }

        public long getCount() {
            return count;
        }

        public double getAverage() {
            return count == 0 ? 0 : (double) total / count;
        }

        public long getMin() {
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getMax() {
            return max == Long.MIN_VALUE ? 0 : max;
        }

        public long getTotal() {
            return total;
        }
    }
}
