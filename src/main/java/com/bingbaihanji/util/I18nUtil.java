package com.bingbaihanji.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 国际化工具类（纯静态方法，线程安全）
 * <p>
 * 对 {@link I18nContext} 的便捷封装，涵盖常见使用场景。
 * 适合直接移植到其他 Java 项目，仅依赖 SLF4J（日志）和
 * {@link I18nContext}（资源加载）。
 * </p>
 *
 * <h3>常见场景</h3>
 * <pre>{@code
 * // 简单获取
 * String title = I18nUtil.getString("app.title");
 *
 * // 带参数
 * String msg = I18nUtil.getString("error.file.notFound", filename);
 *
 * // 带默认值
 * String label = I18nUtil.getStringOrDefault("key.maybe.missing", "默认值");
 *
 * // 枚举国际化名
 * String typeName = I18nUtil.getEnum(GridType.POLAR);
 *
 * // 类型安全获取
 * int max = I18nUtil.getInt("config.maxRetries", 3);
 *
 * // 逗号分隔列表
 * List<String> items = I18nUtil.getStringList("menu.recentFiles");
 *
 * // 检查 key 是否存在
 * if (I18nUtil.containsKey("feature.experimental")) { ... }
 *
 * // 独立于全局语言的提供者（测试/多租户）
 * I18nMessageProvider p = I18nUtil.createInstance(Locale.US);
 * String enText = p.get("hello");
 * }</pre>
 *
 * @author bingbaihanji
 * @date 2025-12-20
 * @updated 2026-06-23 重构为纯工具类，状态管理移交至 I18nContext
 */
public final class I18nUtil {

    private static final Logger log = LoggerFactory.getLogger(I18nUtil.class);

    private I18nUtil() {
        throw new AssertionError("Utility class");
    }

    // ======================== 接口 ========================

    /**
     * 国际化消息提供者（可注入、可 mock 的抽象）
     */
    public interface I18nMessageProvider {

        /** 获取国际化文本，key 不存在时返回 key 本身 */
        String get(String key);

        /** 获取带参数格式化的国际化文本 */
        String get(String key, Object... params);

        /** 检查 key 是否存在 */
        default boolean containsKey(String key) {
            try {
                return !key.equals(get(key));
            } catch (Exception e) {
                return false;
            }
        }

        /** 获取文本，key 不存在时返回指定默认值 */
        default String getOrDefault(String key, String defaultValue) {
            Objects.requireNonNull(key, "key");
            if (containsKey(key)) return get(key);
            return defaultValue;
        }

        /** 获取带参数的文本，key 不存在时以默认值作为模板格式化 */
        default String getOrDefault(String key, String defaultValue, Object... params) {
            Objects.requireNonNull(key, "key");
            if (containsKey(key)) return get(key, params);
            if (params == null || params.length == 0) return defaultValue;
            try {
                return MessageFormat.format(defaultValue, params);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        /** 获取布尔值，key 不存在时返回默认值 */
        default boolean getBoolean(String key, boolean defaultValue) {
            if (!containsKey(key)) return defaultValue;
            String value = get(key);
            return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                    || "1".equals(value) || "on".equalsIgnoreCase(value);
        }

        /** 获取 int 值，key 不存在或解析失败时返回默认值 */
        default int getInt(String key, int defaultValue) {
            if (!containsKey(key)) return defaultValue;
            try { return Integer.parseInt(get(key)); } catch (NumberFormatException e) { return defaultValue; }
        }

        /** 获取 long 值 */
        default long getLong(String key, long defaultValue) {
            if (!containsKey(key)) return defaultValue;
            try { return Long.parseLong(get(key)); } catch (NumberFormatException e) { return defaultValue; }
        }

        /** 获取 double 值 */
        default double getDouble(String key, double defaultValue) {
            if (!containsKey(key)) return defaultValue;
            try { return Double.parseDouble(get(key)); } catch (NumberFormatException e) { return defaultValue; }
        }

        /** 获取逗号分隔的字符串列表 */
        default List<String> getStringList(String key) {
            if (!containsKey(key)) return Collections.emptyList();
            String value = get(key).trim();
            if (value.isEmpty()) return Collections.emptyList();
            return Arrays.stream(value.split("\\s*,\\s*"))
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    // ======================== 全局便捷方法（委托给单例 I18nContext） ========================

    /**
     * 获取当前语言环境下的国际化文本
     *
     * @param key 资源文件中的 key
     * @return 对应文本，若 key 不存在则返回 key 本身
     */
    public static String getString(String key) {
        Objects.requireNonNull(key, "key");
        return safeGetBundle().getString(key);
    }

    /**
     * 获取带参数格式化的国际化文本
     * <p>
     * 使用 {@link MessageFormat} 语法，如 {@code getString("welcome", userName)}
     * 匹配模板 {@code "欢迎, {0}！"}。
     * </p>
     *
     * @param key    资源文件中的 key
     * @param params 格式化参数
     * @return 格式化后的文本，若 key 不存在则返回 key 本身
     */
    public static String getString(String key, Object... params) {
        Objects.requireNonNull(key, "key");
        String pattern = safeGetBundle().getString(key);
        if (params == null || params.length == 0) return pattern;
        try {
            return MessageFormat.format(pattern, params);
        } catch (Exception e) {
            log.debug("格式化文本失败: key={}", key, e);
            return key;
        }
    }

    /**
     * 获取国际化文本，key 不存在时返回默认值
     *
     * @param key          资源文件中的 key
     * @param defaultValue key 不存在时的返回值
     * @return 国际化文本或默认值
     */
    public static String getStringOrDefault(String key, String defaultValue) {
        Objects.requireNonNull(key, "key");
        if (containsKey(key)) return getString(key);
        return defaultValue;
    }

    /**
     * 获取带参数的国际化文本，key 不存在时以默认值作为模板格式化
     *
     * @param key          资源文件中的 key
     * @param defaultValue 默认值模板（使用 {0}, {1} 占位符）
     * @param params       格式化参数
     * @return 国际化文本或格式化后的默认值
     */
    public static String getStringOrDefault(String key, String defaultValue, Object... params) {
        Objects.requireNonNull(key, "key");
        if (containsKey(key)) return getString(key, params);
        if (params == null || params.length == 0) return defaultValue;
        try {
            return MessageFormat.format(defaultValue, params);
        } catch (Exception e) {
            log.debug("格式化默认文本失败: key={}", key, e);
            return defaultValue;
        }
    }

    /**
     * 检查指定 key 在当前资源包中是否存在
     *
     * @param key 资源文件中的 key
     * @return true 表示存在
     */
    public static boolean containsKey(String key) {
        Objects.requireNonNull(key, "key");
        ResourceBundle bundle = I18nContext.getInstance().getBundle();
        return bundle.containsKey(key);
    }

    /**
     * 获取布尔值
     * <p>
     * 支持的 true 值：true / yes / 1 / on（不区分大小写）
     * </p>
     *
     * @param key          资源文件中的 key
     * @param defaultValue key 不存在时的默认值
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        Objects.requireNonNull(key, "key");
        if (!containsKey(key)) return defaultValue;
        String value = getString(key);
        return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                || "1".equals(value) || "on".equalsIgnoreCase(value);
    }

    /**
     * 获取 int 值
     *
     * @param key          资源文件中的 key
     * @param defaultValue key 不存在或解析失败时的默认值
     */
    public static int getInt(String key, int defaultValue) {
        Objects.requireNonNull(key, "key");
        if (!containsKey(key)) return defaultValue;
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            log.debug("解析 int 失败: key={}", key);
            return defaultValue;
        }
    }

    /**
     * 获取 long 值
     */
    public static long getLong(String key, long defaultValue) {
        Objects.requireNonNull(key, "key");
        if (!containsKey(key)) return defaultValue;
        try {
            return Long.parseLong(getString(key));
        } catch (NumberFormatException e) {
            log.debug("解析 long 失败: key={}", key);
            return defaultValue;
        }
    }

    /**
     * 获取 double 值
     */
    public static double getDouble(String key, double defaultValue) {
        Objects.requireNonNull(key, "key");
        if (!containsKey(key)) return defaultValue;
        try {
            return Double.parseDouble(getString(key));
        } catch (NumberFormatException e) {
            log.debug("解析 double 失败: key={}", key);
            return defaultValue;
        }
    }

    /**
     * 获取枚举常量的国际化显示名
     * <p>
     * 拼接规则：枚举类全名（不含包名）+ "." + 常量名 → key。
     * 例：{@code I18nUtil.getEnum(GridType.POLAR)} 生成 key
     * {@code "GridType.POLAR"}。若未定义则返回常量名本身。
     * </p>
     *
     * @param enumValue 枚举常量
     * @return 国际化显示名
     */
    public static String getEnum(Enum<?> enumValue) {
        Objects.requireNonNull(enumValue, "enumValue");
        String key = enumValue.getClass().getSimpleName() + "." + enumValue.name();
        if (containsKey(key)) {
            return getString(key);
        }
        key = enumValue.name();
        if (containsKey(key)) {
            return getString(key);
        }
        return enumValue.name();
    }

    /**
     * 获取逗号分隔的字符串列表
     * <p>
     * 如资源文件定义 {@code menu.recent=文件1, 文件2, 文件3}，
     * 调用 {@code getStringList("menu.recent")} 返回 {@code ["文件1", "文件2", "文件3"]}。
     * key 不存在或值为空时返回空列表。
     * </p>
     *
     * @param key 资源文件中的 key
     * @return 不可变字符串列表
     */
    public static List<String> getStringList(String key) {
        Objects.requireNonNull(key, "key");
        if (!containsKey(key)) return Collections.emptyList();
        String value = getString(key).trim();
        if (value.isEmpty()) return Collections.emptyList();
        return Arrays.stream(value.split("\\s*,\\s*"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 直接格式化模板字符串（不走资源文件查找）
     *
     * @param pattern {@link MessageFormat} 模板，如 {@code "第{0}页, 共{1}页"}
     * @param params  格式化参数
     * @return 格式化后的字符串，pattern 为 null 时返回空字符串
     */
    public static String format(String pattern, Object... params) {
        if (pattern == null) return "";
        if (params == null || params.length == 0) return pattern;
        try {
            return MessageFormat.format(pattern, params);
        } catch (Exception e) {
            log.debug("格式化模板失败: {}", pattern, e);
            return pattern;
        }
    }

    // ======================== 语言管理 ========================

    /**
     * 切换全局语言环境
     */
    public static void switchLocale(Locale locale) {
        I18nContext.getInstance().switchLocale(locale);
    }

    /**
     * 获取当前全局语言环境
     */
    public static Locale getCurrentLocale() {
        return I18nContext.getInstance().getCurrentLocale();
    }

    /**
     * 添加语言变化监听器
     */
    public static void addLocaleChangeListener(Runnable listener) {
        I18nContext.getInstance().addLocaleChangeListener(listener);
    }

    /**
     * 移除语言变化监听器
     */
    public static void removeLocaleChangeListener(Runnable listener) {
        I18nContext.getInstance().removeLocaleChangeListener(listener);
    }

    /**
     * 检查指定语言是否有可用资源
     */
    public static boolean isLocaleAvailable(Locale locale) {
        return I18nContext.getInstance().isLocaleAvailable(locale);
    }

    /**
     * 获取当前资源文件中所有已安装的语言列表
     */
    public static List<Locale> getAvailableLocales() {
        return I18nContext.getInstance().getAvailableLocales();
    }

    /**
     * 注册可刷新组件（语言切换时自动调用其 {@link I18nRefreshable#refreshI18n()}）
     */
    public static void addRefreshable(I18nRefreshable refreshable) {
        I18nContext.getInstance().addRefreshable(refreshable);
    }

    /**
     * 取消注册可刷新组件
     */
    public static void removeRefreshable(I18nRefreshable refreshable) {
        I18nContext.getInstance().removeRefreshable(refreshable);
    }

    // ======================== 独立实例（测试、多租户等场景） ========================

    /**
     * 创建独立于全局语言环境的国际化消息提供者
     * <p>
     * 返回的提供者持有指定语言的资源包，不受 {@link #switchLocale} 影响。
     * 适用于测试、多租户或临时使用不同语言的场景。
     * </p>
     * <pre>{@code
     * I18nMessageProvider en = I18nUtil.createInstance(Locale.US);
     * String text = en.get("hello");       // 始终使用英文
     * boolean ok = en.containsKey("bye");  // 检查 key
     * }</pre>
     *
     * @param locale 目标语言环境
     * @return 独立的 I18nMessageProvider
     * @throws java.util.MissingResourceException 如果资源包不可用
     */
    public static I18nMessageProvider createInstance(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        I18nContext context = I18nContext.of(locale);
        return new LocalizedMessageProvider(context);
    }

    /**
     * 创建使用自定义资源基名的独立提供者
     * <p>
     * 当一个项目需要多套独立的国际化资源时使用。
     * 例：主应用使用 "language/language"，插件使用 "plugin/i18n/strings"。
     * </p>
     *
     * @param baseName 资源文件基名
     * @param locale   目标语言环境
     * @return 独立的 I18nMessageProvider
     * @throws java.util.MissingResourceException 如果资源包不可用
     */
    public static I18nMessageProvider createInstance(String baseName, Locale locale) {
        Objects.requireNonNull(baseName, "baseName");
        Objects.requireNonNull(locale, "locale");
        I18nContext context = I18nContext.of(baseName, locale);
        return new LocalizedMessageProvider(context);
    }

    // ======================== 内部实现 ========================

    /**
     * 安全获取 ResourceBundle（处理单例未初始化等异常）
     */
    private static ResourceBundle safeGetBundle() {
        return I18nContext.getInstance().getBundle();
    }

    /**
         * 独立的国际化消息提供者实现
         * <p>
         * 持有一个独立的 I18nContext 引用，不随全局语言切换而变化。
         * </p>
         */
        private record LocalizedMessageProvider(I18nContext context) implements I18nMessageProvider {

        @Override
            public String get(String key) {
                Objects.requireNonNull(key, "key");
                try {
                    return context.getBundle().getString(key);
                } catch (Exception e) {
                    return key;
                }
            }

            @Override
            public String get(String key, Object... params) {
                Objects.requireNonNull(key, "key");
                try {
                    String pattern = context.getBundle().getString(key);
                    if (params == null || params.length == 0) return pattern;
                    return MessageFormat.format(pattern, params);
                } catch (Exception e) {
                    return key;
                }
            }

            @Override
            public boolean containsKey(String key) {
                Objects.requireNonNull(key, "key");
                return context.getBundle().containsKey(key);
            }
        }
}
