package com.bingbaihanji.config;

import com.bingbaihanji.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 配置管理器
 * <p>
 * 管理应用程序配置的保存和加载
 * <p>
 * 线程安全的单例模式实现(双重检查锁定)
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class ConfigManager {

    private static final Logger logger = Logger.getLogger(ConfigManager.class);
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.fxgeometric";
    private static final String CONFIG_FILE = "config.properties";
    private static ConfigManager instance;
    private final Properties properties;
    private final Path configPath;

    private ConfigManager() {
        this.properties = new Properties();
        this.configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
        loadConfig();
    }

    /**
     * 获取单例实例
     * <p>
     * 使用双重检查锁定模式保证线程安全
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        try {
            // 确保配置目录存在
            Files.createDirectories(configPath.getParent());

            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    properties.load(in);
                    logger.info("配置文件加载成功: {}", configPath);
                }
            } else {
                logger.info("配置文件不存在,使用默认配置");
                setDefaultConfig();
                // 初始化时保存默认配置
                saveConfig();
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
            setDefaultConfig();
        }
    }

    /**
     * 保存配置
     * <p>
     * 线程安全的实现
     */
    public synchronized void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) {
                properties.store(out, "FXGeometric Configuration");
                logger.info("配置文件保存成功: {}", configPath);
            }
        } catch (IOException e) {
            logger.error("保存配置文件失败", e);
        }
    }

    /**
     * 设置默认配置
     */
    private void setDefaultConfig() {
        properties.setProperty("window.width", "1200");
        properties.setProperty("window.height", "800");
        properties.setProperty("language", "zh_CN");
        properties.setProperty("grid.visible", "true");
        properties.setProperty("axis.visible", "true");
        properties.setProperty("snap.enabled", "true");
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取整数配置
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("配置项 {} 的值 {} 不是有效的整数,使用默认值 {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取双精度配置
     */
    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                logger.warn("配置项 {} 的值 {} 不是有效的浮点数,使用默认值 {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * 设置配置项
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * 设置整数配置
     */
    public void setInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    /**
     * 设置双精度配置
     */
    public void setDouble(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
    }

    /**
     * 设置布尔配置
     */
    public void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }
}
