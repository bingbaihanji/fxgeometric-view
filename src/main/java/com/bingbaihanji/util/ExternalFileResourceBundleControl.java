package com.bingbaihanji.util;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * 自定义 ResourceBundle.Control，支持从外部文件加载语言资源
 * <p>
 * 优先级：
 * 1. JAR 包同目录下的 language 文件夹（外部文件）
 * 2. JAR 包内部的 resources/language 文件夹（内部资源）
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class ExternalFileResourceBundleControl extends ResourceBundle.Control {

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                    ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {

        if (baseName == null || locale == null || format == null || loader == null) {
            throw new NullPointerException();
        }

        ResourceBundle bundle = null;
        if (format.equals("java.properties")) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            // 1. 尝试从外部文件加载
            bundle = loadFromExternalFile(resourceName);

            // 2. 如果外部文件不存在，从内部资源加载
            if (bundle == null) {
                bundle = loadFromClasspath(resourceName, loader, reload);
            }
        }
        return bundle;
    }

    /**
     * 从 JAR 包同目录的 language 文件夹加载语言文件
     */
    private ResourceBundle loadFromExternalFile(String resourceName) {
        try {
            // 获取 JAR 包所在目录
            File jarFile = getJarLocation();
            if (jarFile != null) {
                File jarDir = jarFile.getParentFile();
                // 构建外部语言文件路径：jar目录/language/language_xx_XX.properties
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
     * 从 classpath（JAR 包内部）加载语言文件
     */
    private ResourceBundle loadFromClasspath(String resourceName, ClassLoader loader, boolean reload)
            throws IOException {
        InputStream stream = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }

        if (stream != null) {
            try (InputStreamReader reader = new InputStreamReader(stream, "UTF-8")) {
                return new PropertyResourceBundle(reader);
            } finally {
                stream.close();
            }
        }
        return null;
    }

    /**
     * 获取 JAR 包位置
     */
    private File getJarLocation() {
        try {
            String path = ExternalFileResourceBundleControl.class
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

    @Override
    public List<Locale> getCandidateLocales(String baseName, Locale locale) {
        // 使用默认的候选 Locale 列表
        return super.getCandidateLocales(baseName, locale);
    }

    @Override
    public Locale getFallbackLocale(String baseName, Locale locale) {
        // 如果找不到资源，默认回退到简体中文
        if (Locale.getDefault().equals(locale)) {
            return null;
        }
        return Locale.getDefault();
    }
}
