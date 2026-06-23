package com.bingbaihanji.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 国际化语言环境上下文（单例、线程安全）
 * <p>
 * 负责管理当前语言环境、资源包加载以及语言变化通知。
 * 加载优先级：外部文件（JAR 同级目录）→ classpath。
 * 语言回退链：zh_CN_Variant → zh_CN → zh → root。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 全局单例
 * I18nContext ctx = I18nContext.getInstance();
 * String text = ctx.getBundle().getString("menu.file");
 *
 * // 独立上下文（测试、多租户）
 * I18nContext independent = I18nContext.of(Locale.US);
 *
 * // 自定义基名的独立上下文
 * I18nContext custom = I18nContext.of("i18n/messages", Locale.JAPAN);
 * }</pre>
 *
 * @author bingbaihanji
 * @date 2026-06-23
 */
public final class I18nContext {

    private static final Logger log = LoggerFactory.getLogger(I18nContext.class);

    /** 默认资源文件基名 */
    public static final String DEFAULT_BASE_NAME = "language/language";

    /** 系统属性 key：覆盖默认资源文件基名 */
    public static final String SYS_PROP_BASE_NAME = "i18n.base.name";

    private static volatile I18nContext instance;

    /** 资源文件基名（如 "language/language"） */
    private final String baseName;

    /** 当前语言环境 */
    private volatile Locale currentLocale;

    /** 当前资源包 */
    private volatile ResourceBundle resourceBundle;

    /** 语言变化监听器 */
    private final Set<Runnable> localeChangeListeners = new CopyOnWriteArraySet<>();

    /** 可刷新组件（语言切换时自动更新文本） */
    private final Set<I18nRefreshable> refreshable = new CopyOnWriteArraySet<>();

    // ======================== 构造 ========================

    /**
     * 私有构造器
     *
     * @param baseName 资源文件基名
     * @param locale   初始语言环境
     * @throws MissingResourceException 如果任何候选语言的资源包都加载不到
     */
    private I18nContext(String baseName, Locale locale) {
        this.baseName = Objects.requireNonNull(baseName, "baseName");
        this.currentLocale = Objects.requireNonNull(locale, "locale");
        this.resourceBundle = loadResourceBundle(baseName, locale);
    }

    // ======================== 获取实例 ========================

    /**
     * 获取全局单例（双重检查锁定，首次调用时延迟初始化）
     * <p>
     * 通过系统属性 {@value #SYS_PROP_BASE_NAME} 可自定义资源文件基名，
     * 否则使用默认值 {@value #DEFAULT_BASE_NAME}。
     * 初始化失败时依次回退到简体中文 → 英语 → 抛异常。
     * </p>
     *
     * @return 全局 I18nContext 实例
     */
    public static I18nContext getInstance() {
        if (instance == null) {
            synchronized (I18nContext.class) {
                if (instance == null) {
                    String bn = System.getProperty(SYS_PROP_BASE_NAME, DEFAULT_BASE_NAME);
                    instance = initSingleton(bn);
                }
            }
        }
        return instance;
    }

    /**
     * 创建独立的 I18nContext 实例（不随全局语言切换而变化）
     *
     * @param locale 目标语言环境
     * @return 独立的 I18nContext
     * @throws MissingResourceException 如果资源包不可用
     */
    public static I18nContext of(Locale locale) {
        return new I18nContext(DEFAULT_BASE_NAME, locale);
    }

    /**
     * 创建使用自定义基名的独立 I18nContext 实例
     *
     * @param baseName 资源文件基名，如 "i18n/messages"
     * @param locale   目标语言环境
     * @return 独立的 I18nContext
     * @throws MissingResourceException 如果资源包不可用
     */
    public static I18nContext of(String baseName, Locale locale) {
        return new I18nContext(baseName, locale);
    }

    /**
     * 首次创建单例时的回退链路
     */
    private static I18nContext initSingleton(String bn) {
        try {
            return new I18nContext(bn, Locale.getDefault());
        } catch (MissingResourceException e) {
            log.warn("系统默认语言资源不可用 [{}]，尝试简体中文", bn);
        }
        try {
            return new I18nContext(bn, Locale.SIMPLIFIED_CHINESE);
        } catch (MissingResourceException e) {
            log.warn("简体中文资源不可用，尝试英语");
        }
        try {
            return new I18nContext(bn, Locale.US);
        } catch (MissingResourceException e) {
            throw new ExceptionInInitializerError("无法以任何语言初始化 I18nContext: " + e.getMessage());
        }
    }

    // ======================== 公开方法 ========================

    /**
     * 获取当前资源包
     */
    public ResourceBundle getBundle() {
        return resourceBundle;
    }

    /**
     * 获取资源文件基名
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * 获取当前语言环境
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * 切换语言环境（成功后触发所有监听器）
     *
     * @param locale 目标语言环境
     * @throws MissingResourceException 如果目标语言的资源包不可用
     */
    public void switchLocale(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        ResourceBundle newBundle = loadResourceBundle(baseName, locale);
        synchronized (this) {
            this.currentLocale = locale;
            this.resourceBundle = newBundle;
        }
        notifyListeners();
    }

    /**
     * 添加语言变化监听器
     */
    public void addLocaleChangeListener(Runnable listener) {
        localeChangeListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * 移除语言变化监听器
     */
    public void removeLocaleChangeListener(Runnable listener) {
        localeChangeListeners.remove(listener);
    }

    /**
     * 注册可刷新组件（语言切换时自动调用 {@link I18nRefreshable#refreshI18n()}）
     */
    public void addRefreshable(I18nRefreshable refreshable) {
        this.refreshable.add(Objects.requireNonNull(refreshable, "refreshable"));
    }

    /**
     * 取消注册可刷新组件
     */
    public void removeRefreshable(I18nRefreshable refreshable) {
        this.refreshable.remove(refreshable);
    }

    /**
     * 检查指定语言环境是否有可用资源（仅检查文件/资源是否存在，不加载完整 bundle）
     *
     * @param locale 目标语言环境
     * @return true 表示存在至少一个候选语言的资源文件
     */
    public boolean isLocaleAvailable(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        for (Locale candidate : getCandidateLocales(locale)) {
            String resourceName = toResourceName(toBundleName(baseName, candidate));
            if (externalFileExists(resourceName) || classpathResourceExists(resourceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取已安装的语言环境列表（扫描常用语言，通过 classpath 检测）
     */
    public List<Locale> getAvailableLocales() {
        List<Locale> result = new ArrayList<>();
        for (Locale candidate : COMMON_LOCALES) {
            if (isLocaleAvailable(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    // ======================== 资源加载 ========================

    /**
     * 加载资源包（含语言回退链）
     * <p>
     * 回退顺序示例（locale=zh_CN_xxx）：zh_CN_xxx → zh_CN → zh → root
     * 每个候选语言：先查外部文件，再查 classpath。
     * </p>
     */
    private ResourceBundle loadResourceBundle(String baseName, Locale locale) {
        for (Locale candidate : getCandidateLocales(locale)) {
            String resourceName = toResourceName(toBundleName(baseName, candidate));
            ResourceBundle bundle = loadFromExternalFile(resourceName);
            if (bundle != null) {
                return bundle;
            }
            bundle = loadFromClasspath(resourceName);
            if (bundle != null) {
                return bundle;
            }
        }
        throw new MissingResourceException(
                "Can't find resource bundle: baseName=" + baseName + ", locale=" + locale,
                I18nContext.class.getName(), baseName);
    }

    /**
     * 生成候选 Locale 列表（精确 → 宽松 → 默认）
     */
    private List<Locale> getCandidateLocales(Locale locale) {
        List<Locale> candidates = new ArrayList<>(4);
        candidates.add(locale);
        if (!locale.getVariant().isEmpty()) {
            candidates.add(new Locale(locale.getLanguage(), locale.getCountry()));
        }
        if (!locale.getCountry().isEmpty()) {
            candidates.add(new Locale(locale.getLanguage()));
        }
        if (!locale.getLanguage().isEmpty()) {
            candidates.add(Locale.ROOT);
        }
        return candidates;
    }

    /**
     * 将基名和 Locale 拼接为完整 bundle 名（如 "language/language_zh_CN"）
     */
    private String toBundleName(String baseName, Locale locale) {
        if (locale == Locale.ROOT || locale.getLanguage().isEmpty()) {
            return baseName;
        }
        StringBuilder sb = new StringBuilder(baseName);
        sb.append('_').append(locale.getLanguage());
        if (!locale.getCountry().isEmpty()) {
            sb.append('_').append(locale.getCountry());
        }
        if (!locale.getVariant().isEmpty()) {
            sb.append('_').append(locale.getVariant());
        }
        return sb.toString();
    }

    /**
     * bundle 名 → .properties 文件路径（如 "language/language_zh_CN.properties"）
     */
    private String toResourceName(String bundleName) {
        return bundleName.replace('.', '/') + ".properties";
    }

    // ----- 外部文件加载 -----

    /**
     * 从 JAR 同级目录的外部 .properties 文件加载资源
     */
    private ResourceBundle loadFromExternalFile(String resourceName) {
        File file = getExternalResourceFile(resourceName);
        if (file != null && file.isFile()) {
            try (InputStream in = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            } catch (IOException e) {
                log.warn("读取外部资源文件失败: {}", file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    private File getExternalResourceFile(String resourceName) {
        File dir = getJarDirectory();
        return dir != null ? new File(dir, resourceName) : null;
    }

    private boolean externalFileExists(String resourceName) {
        File file = getExternalResourceFile(resourceName);
        return file != null && file.isFile();
    }

    /**
     * 获取 JAR/class 所在目录（运行时位置）
     */
    private File getJarDirectory() {
        try {
            String path = I18nContext.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            // Windows: /C:/... → C:/...
            if (File.separatorChar == '\\' && path.length() > 2
                    && path.startsWith("/") && path.charAt(2) == ':') {
                path = path.substring(1);
            }
            File file = new File(path);
            return file.isDirectory() ? file : file.getParentFile();
        } catch (URISyntaxException e) {
            log.debug("无法解析 JAR 位置", e);
            return null;
        }
    }

    // ----- Classpath 加载 -----

    private ResourceBundle loadFromClasspath(String resourceName) {
        String path = "/" + resourceName;
        try (InputStream in = I18nContext.class.getResourceAsStream(path)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        } catch (IOException e) {
            log.warn("从 classpath 加载资源失败: {}", resourceName, e);
        }
        return null;
    }

    private boolean classpathResourceExists(String resourceName) {
        return I18nContext.class.getResource("/" + resourceName) != null;
    }

    // ======================== 内部 | 常用语言 | 通知 ========================

    /** 常用语言列表（按需扩展） */
    private static final Locale[] COMMON_LOCALES = {
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE,
            Locale.US,
            Locale.UK,
            Locale.JAPAN,
            Locale.KOREA,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.ITALY,
            Locale.forLanguageTag("pt-BR"),
            Locale.forLanguageTag("es-ES"),
            new Locale("ar"),
            new Locale("ru"),
    };

    private void notifyListeners() {
        // 通知旧式监听器（向后兼容）
        for (Runnable listener : localeChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                log.error("语言变化监听器执行异常", e);
            }
        }
        // 通知可刷新组件
        for (I18nRefreshable refreshable : refreshable) {
            try {
                refreshable.refreshI18n();
            } catch (Exception e) {
                log.error("I18nRefreshable 刷新异常: {}", refreshable.getClass().getName(), e);
            }
        }
    }
}
