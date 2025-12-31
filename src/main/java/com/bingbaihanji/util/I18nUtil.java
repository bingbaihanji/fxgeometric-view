package com.bingbaihanji.util;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * 国际化工具类
 * <p>
 * 支持动态加载语言文件，优先级：
 * 1. JAR 包同目录下的 language 文件夹（外部文件）
 * 2. JAR 包内部的 resources/language 文件夹（内部资源）
 * <p>
 * 注意：为了兼容 Java 模块化系统，不使用 ResourceBundle.Control
 *
 * @author bingbaihanji
 * @date 2025-12-20
 * @updated 2025-12-31 添加外部语言文件支持，兼容模块化系统
 */
public class I18nUtil {
    private static final String BASE_NAME = "language/language";
    // 语言变化监听器列表
    private static final List<Runnable> localeChangeListeners = new ArrayList<>();
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale;

    static {
        // 默认使用系统语言,如果不支持则使用简体中文
        currentLocale = Locale.getDefault();
        try {
            resourceBundle = loadResourceBundle(currentLocale);
        } catch (Exception e) {
            // 如果系统语言不支持,默认使用简体中文
            currentLocale = Locale.SIMPLIFIED_CHINESE;
            try {
                resourceBundle = loadResourceBundle(currentLocale);
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
            resourceBundle = loadResourceBundle(locale);
            // 通知所有监听器
            notifyLocaleChange();
        } catch (Exception e) {
            System.err.println("切换语言失败: " + locale);
            e.printStackTrace();
        }
    }

    /**
     * 加载资源文件
     * 优先级：1. 外部文件 2. classpath 资源
     *
     * @param locale 语言环境
     * @return ResourceBundle
     * @throws IOException 加载失败时抛出异常
     */
    private static ResourceBundle loadResourceBundle(Locale locale) throws IOException {
        // 构建资源文件名
        String bundleName = toBundleName(BASE_NAME, locale);
        String resourceName = toResourceName(bundleName);

        // 1. 尝试从外部文件加载
        ResourceBundle bundle = loadFromExternalFile(resourceName);

        // 2. 如果外部文件不存在，从 classpath 加载
        if (bundle == null) {
            bundle = loadFromClasspath(resourceName);
        }

        if (bundle == null) {
            throw new IOException("Cannot load resource bundle: " + resourceName);
        }

        return bundle;
    }

    /**
     * 从外部文件加载资源
     */
    private static ResourceBundle loadFromExternalFile(String resourceName) {
        try {
            File jarFile = getJarLocation();
            if (jarFile != null) {
                File jarDir = jarFile.getParentFile();
                File externalFile = new File(jarDir, resourceName);

                if (externalFile.exists() && externalFile.isFile()) {
                    try (InputStream stream = new FileInputStream(externalFile);
                         InputStreamReader reader = new InputStreamReader(stream, "UTF-8")) {
                        return new PropertyResourceBundle(reader);
                    }
                }
            }
        } catch (Exception e) {
            // 外部文件加载失败，继续使用内部资源
        }
        return null;
    }

    /**
     * 从 classpath 加载资源
     */
    private static ResourceBundle loadFromClasspath(String resourceName) {
        try {
            // 在模块化环境中，使用 Class.getResourceAsStream() 更可靠
            // 路径需要以 / 开头表示从 classpath 根目录开始
            String path = "/" + resourceName;
            InputStream stream = I18nUtil.class.getResourceAsStream(path);

            if (stream != null) {
                try (InputStream is = stream;
                     InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
                    return new PropertyResourceBundle(reader);
                }
            }
        } catch (Exception e) {
            // classpath 资源加载失败
            System.err.println("Failed to load resource from classpath: " + resourceName);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 JAR 包位置
     */
    private static File getJarLocation() {
        try {
            String path = I18nUtil.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            // 处理 Windows 路径问题（/C:/path/to/file）
            if (path.startsWith("/") && path.contains(":")) {
                path = path.substring(1);
            }

            File file = new File(path);

            // 如果是目录（IDE 开发环境），返回 null 表示不使用外部文件
            if (file.isDirectory()) {
                return null;
            }

            return file;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 将 baseName 和 locale 转换为 bundle 名称
     */
    private static String toBundleName(String baseName, Locale locale) {
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
     * 将 bundle 名称转换为资源文件名
     */
    private static String toResourceName(String bundleName) {
        return bundleName.replace('.', '/') + ".properties";
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
