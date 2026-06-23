package com.bingbaihanji.util;

/**
 * 国际化可刷新接口
 * <p>
 * 实现此接口的组件在语言环境切换时会自动收到 {@link #refreshI18n()} 回调，
 * 用于更新 UI 文本，无需重建整个界面。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * public class MyPane implements I18nRefreshable {
 *     public void refreshI18n() {
 *         titleLabel.setText(I18nUtil.getString("my.title"));
 *     }
 * }
 *
 * // 注册到全局上下文
 * I18nUtil.addRefreshable(myPane);
 * }</pre>
 *
 * @author bingbaihanji
 * @date 2026-06-23
 */
@FunctionalInterface
public interface I18nRefreshable {

    /**
     * 语言切换后调用，组件应在此方法中刷新所有依赖 i18n 的文本
     */
    void refreshI18n();
}
