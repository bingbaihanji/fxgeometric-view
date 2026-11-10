package com.bingbaihanji.view.menu;

import com.bingbaihanji.i18n.I18nUtil;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * 枚举下拉框的本地化显示
 * <p>
 * 枚举只提供 i18n key, 显示文案由 UI 层解析, 避免常量层反向依赖 i18n.
 */
public final class EnumComboSupport {

    private EnumComboSupport() {
    }

    /**
     * 为下拉框安装基于 i18n key 的显示转换器
     *
     * @param combo        下拉框
     * @param keyExtractor 从枚举值取 i18n key 的函数
     * @param <T>          枚举类型
     */
    public static <T> void localize(ComboBox<T> combo, Function<T, String> keyExtractor) {
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : I18nUtil.getString(keyExtractor.apply(value));
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        });
    }
}
