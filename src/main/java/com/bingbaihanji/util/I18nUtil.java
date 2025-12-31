package com.bingbaihanji.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 国际化工具类
 * <p>
 * 支持动态加载语言文件，优先级：
 * 1. JAR 包同目录下的 language 文件夹（外部文件）
 * 2. JAR 包内部的 resources/language 文件夹（内部资源）
 *
 * @author bingbaihanji
 * @date 2025-12-20
 * @updated 2025-12-31 添加外部语言文件支持
 */
public class I18nUtil {
    private static final String BASE_NAME = "language.language";
    // 自定义的 ResourceBundle.Control，支持外部文件加载
    private static final ExternalFileResourceBundleControl CONTROL = new ExternalFileResourceBundleControl();
    // 语言变化监听器列表
    private static final List<Runnable> localeChangeListeners = new ArrayList<>();
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale;

    static {
        // 默认使用系统语言,如果不支持则使用简体中文
        currentLocale = Locale.getDefault();
        try {
            resourceBundle = ResourceBundle.getBundle(BASE_NAME, currentLocale, CONTROL);
        } catch (Exception e) {
            // 如果系统语言不支持,默认使用简体中文
            currentLocale = Locale.SIMPLIFIED_CHINESE;
            try {
                resourceBundle = ResourceBundle.getBundle(BASE_NAME, currentLocale, CONTROL);
            } catch (Exception e2) {
                // 如果简体中文也加载失败，尝试使用默认 Locale
                System.err.println("无法加载语言资源文件，使用默认配置");
                e2.printStackTrace();
            }
        }
    }

    /**
     * 获取国际化文本
     *
     * @param key 配置文件中的key
     * @return 对应的值
     */
    public static String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * 切换语言
     *
     * @param locale 目标语言环境
     */
    public static void switchLocale(Locale locale) {
        currentLocale = locale;
        try {
            resourceBundle = ResourceBundle.getBundle(BASE_NAME, locale, CONTROL);
            // 通知所有监听器
            notifyLocaleChange();
        } catch (Exception e) {
            System.err.println("切换语言失败: " + locale);
            e.printStackTrace();
        }
    }

    /**
     * 获取当前语言环境
     *
     * @return 当前Locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * 添加语言变化监听器
     *
     * @param listener 监听器回调
     */
    public static void addLocaleChangeListener(Runnable listener) {
        if (listener != null && !localeChangeListeners.contains(listener)) {
            localeChangeListeners.add(listener);
        }
    }

    /**
     * 移除语言变化监听器
     *
     * @param listener 监听器回调
     */
    public static void removeLocaleChangeListener(Runnable listener) {
        localeChangeListeners.remove(listener);
    }

    /**
     * 通知所有监听器语言已变化
     */
    private static void notifyLocaleChange() {
        for (Runnable listener : localeChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
