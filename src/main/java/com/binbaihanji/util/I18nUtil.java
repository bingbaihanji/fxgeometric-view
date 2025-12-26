package com.binbaihanji.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author bingbaihanji
 * @date 2025-12-20
 * @description 国际化工具类
 */
public class I18nUtil {
    private static final String BASE_NAME = "language.language";
    // 语言变化监听器列表
    private static final List<Runnable> localeChangeListeners = new ArrayList<>();
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale;

    static {
        // 默认使用系统语言,如果不支持则使用中文
        currentLocale = Locale.getDefault();
        try {
            resourceBundle = ResourceBundle.getBundle(BASE_NAME, currentLocale);
        } catch (Exception e) {
            // 如果系统语言不支持,默认使用中文
            currentLocale = Locale.SIMPLIFIED_CHINESE;
            resourceBundle = ResourceBundle.getBundle(BASE_NAME, currentLocale);
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
        resourceBundle = ResourceBundle.getBundle(BASE_NAME, locale);
        // 通知所有监听器
        notifyLocaleChange();
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
