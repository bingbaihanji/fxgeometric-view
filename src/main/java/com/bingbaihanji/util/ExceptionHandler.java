package com.bingbaihanji.util;

/**
 * 异常处理工具类
 * <p>
 * 提供统一的异常处理和用户友好的错误提示
 * 委托到 FxTools 工具类实现对话框显示功能，实现代码复用
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ExceptionHandler {

    private static final Logger logger = Logger.getLogger(ExceptionHandler.class);

    /**
     * 显示错误对话框
     *
     * @param title   标题
     * @param message 消息
     */
    public static void showErrorDialog(String title, String message) {
        FxTools.showErrorAlert(title, message);
    }

    /**
     * 显示错误对话框（带异常堆栈）
     *
     * @param title   标题
     * @param message 消息
     * @param e       异常
     */
    public static void showErrorDialog(String title, String message, Throwable e) {
        logger.error(message, e);
        FxTools.showErrorAlert(title, message, e);
    }

    /**
     * 显示警告对话框
     *
     * @param title   标题
     * @param message 消息
     */
    public static void showWarningDialog(String title, String message) {
        FxTools.showWarningAlert(title, message);
    }

    /**
     * 显示信息对话框
     *
     * @param title   标题
     * @param message 消息
     */
    public static void showInfoDialog(String title, String message) {
        FxTools.showInfoAlert(title, message);
    }

    /**
     * 处理异常（记录日志但不显示对话框）
     *
     * @param message 消息
     * @param e       异常
     */
    public static void handleException(String message, Throwable e) {
        logger.error(message, e);
    }

    /**
     * 处理异常（记录日志并显示对话框）
     *
     * @param title   对话框标题
     * @param message 消息
     * @param e       异常
     */
    public static void handleExceptionWithDialog(String title, String message, Throwable e) {
        logger.error(message, e);
        showErrorDialog(title, message, e);
    }
}
