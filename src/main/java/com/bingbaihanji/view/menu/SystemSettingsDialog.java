package com.bingbaihanji.view.menu;

import com.bingbaihanji.util.I18nUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

import java.util.*;

/**
 * @author bingbaihanji
 * @description 系统设置对话框
 */
public class SystemSettingsDialog extends Dialog<ButtonType> {

    private ComboBox<LanguageItem> languageComboBox;
    private Locale selectedLocale;

    public SystemSettingsDialog() {
        initDialog();
    }

    private void initDialog() {
        // 设置对话框标题和模态
        setTitle(I18nUtil.getString("settings.dialog.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        // 创建主面板
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(15);
        gridPane.setPadding(new Insets(20, 20, 20, 20));

        // 语言设置部分
        Label languageLabel = new Label(I18nUtil.getString("settings.dialog.languageLabel"));
        languageComboBox = new ComboBox<>();
        languageComboBox.setPrefWidth(200);

        // 加载可用语言
        loadAvailableLanguages();

        gridPane.add(new Label(I18nUtil.getString("settings.dialog.language")), 0, 0, 2, 1);
        gridPane.add(languageLabel, 0, 1);
        gridPane.add(languageComboBox, 1, 1);

        // 设置对话框内容
        getDialogPane().setContent(gridPane);

        // 添加按钮
        ButtonType applyButtonType = new ButtonType(I18nUtil.getString("settings.dialog.apply"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(I18nUtil.getString("settings.dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(applyButtonType, cancelButtonType);

        // 应用按钮事件处理
        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                applySettings();
            }
            return dialogButton;
        });
    }

    /**
     * 加载可用的语言列表
     */
    private void loadAvailableLanguages() {
        List<LanguageItem> languages = new ArrayList<>();
        Set<String> langCodes = new HashSet<>();

        try {
            // 1. 从JAR包内部扫描语言文件
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            scanLanguageFilesFromClasspath(classLoader, langCodes);

            // 2. 从JAR包同目录扫描外部语言文件
            scanExternalLanguageFiles(langCodes);

            // 3. 根据扫描到的语言代码创建语言选项
            for (String langCode : langCodes) {
                Locale locale = createLocaleFromCode(langCode);
                String displayName = getLanguageDisplayName(locale);
                languages.add(new LanguageItem(displayName, locale));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 如果没有扫描到任何语言文件,添加默认语言
        if (languages.isEmpty()) {
            languages.add(new LanguageItem("中文", Locale.SIMPLIFIED_CHINESE));
            languages.add(new LanguageItem("English", Locale.ENGLISH));
        }

        // 排序语言列表
        languages.sort(Comparator.comparing(LanguageItem::getDisplayName));

        languageComboBox.getItems().addAll(languages);

        // 设置当前选中的语言
        Locale currentLocale = I18nUtil.getCurrentLocale();
        for (LanguageItem item : languages) {
            if (item.getLocale().getLanguage().equals(currentLocale.getLanguage())) {
                languageComboBox.setValue(item);
                break;
            }
        }
    }

    /**
     * 根据语言代码创建Locale
     */
    private Locale createLocaleFromCode(String code) {
        if (code == null || code.isEmpty()) {
            return Locale.getDefault();
        }

        // 处理带下划线的语言代码,如 zh_CN, zh_TW, en_US
        if (code.contains("_")) {
            String[] parts = code.split("_");
            if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            } else if (parts.length == 3) {
                return new Locale(parts[0], parts[1], parts[2]);
            }
        }

        return new Locale(code);
    }

    /**
     * 获取语言的显示名称
     * 从语言文件的 language.name 键读取,如果不存在则使用 Locale 的默认显示名称
     */
    private String getLanguageDisplayName(Locale locale) {
        try {
            // 尝试加载该语言的资源文件并读取 language.name
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle(
                    "language.language",
                    locale,
                    new com.bingbaihanji.util.ExternalFileResourceBundleControl()
            );
            return bundle.getString("language.name");
        } catch (Exception e) {
            // 如果读取失败,使用 Locale 自带的显示名称作为后备方案
            String displayName = locale.getDisplayLanguage(locale);
            // 首字母大写
            if (!displayName.isEmpty()) {
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            }
            return displayName;
        }
    }

    /**
     * 从classpath(JAR包内部)扫描语言文件
     */
    private void scanLanguageFilesFromClasspath(ClassLoader classLoader, Set<String> langCodes) {
        try {
            // 使用ClassLoader获取资源URL
            java.net.URL resourceUrl = classLoader.getResource("language");
            if (resourceUrl != null) {
                String protocol = resourceUrl.getProtocol();

                if ("file".equals(protocol)) {
                    // IDE环境下,直接读取文件系统
                    java.io.File dir = new java.io.File(resourceUrl.toURI());
                    if (dir.exists() && dir.isDirectory()) {
                        java.io.File[] files = dir.listFiles((d, name) ->
                                name.startsWith("language_") && name.endsWith(".properties"));
                        if (files != null) {
                            for (java.io.File file : files) {
                                String langCode = extractLangCode(file.getName());
                                if (langCode != null) {
                                    langCodes.add(langCode);
                                }
                            }
                        }
                    }
                } else if ("jar".equals(protocol)) {
                    // JAR包环境下,读取JAR内部资源
                    String jarPath = resourceUrl.getPath();
                    String[] parts = jarPath.split("!");
                    if (parts.length > 0) {
                        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(
                                new java.io.File(new java.net.URI(parts[0])))) {
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                java.util.jar.JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.startsWith("language/language_") && name.endsWith(".properties")) {
                                    String fileName = name.substring(name.lastIndexOf('/') + 1);
                                    String langCode = extractLangCode(fileName);
                                    if (langCode != null) {
                                        langCodes.add(langCode);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从JAR包同目录扫描外部语言文件
     */
    private void scanExternalLanguageFiles(Set<String> langCodes) {
        try {
            // 获取JAR包所在目录
            String jarPath = SystemSettingsDialog.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            java.io.File jarFile = new java.io.File(jarPath);
            java.io.File jarDir = jarFile.getParentFile();

            // 在JAR包同目录下查找language目录
            java.io.File languageDir = new java.io.File(jarDir, "language");
            if (languageDir.exists() && languageDir.isDirectory()) {
                java.io.File[] files = languageDir.listFiles((dir, name) ->
                        name.startsWith("language_") && name.endsWith(".properties"));
                if (files != null) {
                    for (java.io.File file : files) {
                        String langCode = extractLangCode(file.getName());
                        if (langCode != null) {
                            langCodes.add(langCode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 外部文件扫描失败不影响JAR内部语言加载
        }
    }

    /**
     * 从文件名提取语言代码
     * 例如: language_zh.properties -> zh
     * language_en.properties -> en
     */
    private String extractLangCode(String fileName) {
        if (fileName != null && fileName.startsWith("language_") && fileName.endsWith(".properties")) {
            return fileName.substring(9, fileName.length() - 11);
        }
        return null;
    }

    /**
     * 应用设置
     */
    private void applySettings() {
        LanguageItem selectedItem = languageComboBox.getValue();
        if (selectedItem != null) {
            selectedLocale = selectedItem.getLocale();
            I18nUtil.switchLocale(selectedLocale);
        }
    }

    /**
     * 获取选中的语言环境
     */
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    /**
     * 语言选项内部类
     */
    private static class LanguageItem {
        private final String displayName;
        private final Locale locale;

        public LanguageItem(String displayName, Locale locale) {
            this.displayName = displayName;
            this.locale = locale;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
