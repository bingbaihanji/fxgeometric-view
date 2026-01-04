package com.bingbaihanji.util;

import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * <p>
 * 封装SLF4J日志框架，提供统一的日志接口
 *
 * @author bingbaihanji
 * @date 2025-01-01
 */
public class Logger {

    private final org.slf4j.Logger logger;

    private Logger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * 获取指定类的日志记录器
     *
     * @param clazz 类对象
     * @return 日志记录器实例
     */
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    /**
     * 记录调试信息
     */
    public void debug(String message) {
        logger.debug(message);
    }

    /**
     * 记录调试信息（带参数）
     */
    public void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    /**
     * 记录普通信息
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * 记录普通信息（带参数）
     */
    public void info(String format, Object... args) {
        logger.info(format, args);
    }

    /**
     * 记录警告信息
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * 记录警告信息（带参数）
     */
    public void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    /**
     * 记录警告信息（带异常）
     */
    public void warn(String message, Throwable t) {
        logger.warn(message, t);
    }

    /**
     * 记录错误信息
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * 记录错误信息（带参数）
     */
    public void error(String format, Object... args) {
        logger.error(format, args);
    }

    /**
     * 记录错误信息（带异常）
     */
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    /**
     * 是否启用调试级别日志
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * 是否启用信息级别日志
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }
}
