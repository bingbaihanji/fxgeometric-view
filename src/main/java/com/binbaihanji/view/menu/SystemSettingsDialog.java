package com.binbaihanji.view.menu;

import com.binbaihanji.util.I18nUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

        try {
            // 获取资源文件目录
            String baseName = "language";
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // 扫描所有language_*.properties文件
            File resourceDir = new File(classLoader.getResource("language").toURI());
            if (resourceDir.exists() && resourceDir.isDirectory()) {
                File[] files = resourceDir.listFiles((dir, name) ->
                        name.startsWith("language_") && name.endsWith(".properties"));

                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        // 提取语言代码 language_zh.properties -> zh
                        String langCode = fileName.substring(9, fileName.length() - 11);

                        Locale locale = createLocaleFromCode(langCode);
                        String displayName = getLanguageDisplayName(locale);
                        languages.add(new LanguageItem(displayName, locale));
                    }
                }
            }
        } catch (Exception e) {
            // 如果扫描失败，添加默认语言
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
        return switch (code) {
            case "zh" -> Locale.SIMPLIFIED_CHINESE;
            case "en" -> Locale.ENGLISH;
            case "zh_TW" -> Locale.TRADITIONAL_CHINESE;
            default -> new Locale(code);
        };
    }

    /**
     * 获取语言的显示名称
     */
    private String getLanguageDisplayName(Locale locale) {
        return switch (locale.getLanguage()) {
            case "zh" -> "中文";
            case "en" -> "English";
            default -> locale.getDisplayLanguage(locale);
        };
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
