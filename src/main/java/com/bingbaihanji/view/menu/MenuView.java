package com.bingbaihanji.view.menu;

import java.util.HashMap;
import java.util.Map;

import com.bingbaihanji.util.I18nRefreshable;
import com.bingbaihanji.util.I18nUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;

/**
 * 主菜单栏（实现 {@link I18nRefreshable}，语言切换时自动刷新文本）
 *
 * @author bingbaihanji
 * @date 2025-12-20
 * @updated 2026-06-23 实现 I18nRefreshable，语言切换时无需重建界面
 */
public class MenuView extends MenuBar implements I18nRefreshable {

    private final ObservableList<Menu> menus = FXCollections.observableArrayList();

    /** Menu / MenuItem → i18n key 映射，供 {@link #refreshI18n()} 使用 */
    private final Map<Object, String> i18nKeyMap = new HashMap<>();

    // 菜单项声明，方便外部访问和添加事件监听
    private MenuItem saveProjectItem;
    private MenuItem openProjectItem;
    private MenuItem screenshotItem;
    private RadioMenuItem dotModeItem;
    private RadioMenuItem gridModeItem;
    private RadioMenuItem polarModeItem;
    private RadioMenuItem isometricModeItem;

    private RadioMenuItem showAxis;
    private RadioMenuItem hideAxis;

    private MenuItem systemSettingsItem;
    private MenuItem drawingSettingsItem;
    private MenuItem lineStyleSettingsItem;

    public MenuView() {
        initializeMenus();
    }

    // ======================== I18nRefreshable ========================

    @Override
    public void refreshI18n() {
        for (var entry : i18nKeyMap.entrySet()) {
            String text = I18nUtil.getString(entry.getValue());
            if (entry.getKey() instanceof Menu menu) {
                menu.setText(text);
            } else if (entry.getKey() instanceof MenuItem item) {
                item.setText(text);
            }
        }
    }

    // ======================== 构建 ========================

    private void initializeMenus() {
        // 0. 创建"文件"菜单
        Menu fileMenu = new Menu();
        registerMenu(fileMenu, "menu.file");
        saveProjectItem = new MenuItem();
        registerMenuItem(saveProjectItem, "menu.file.saveProject");
        saveProjectItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        openProjectItem = new MenuItem();
        registerMenuItem(openProjectItem, "menu.file.openProject");
        openProjectItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        fileMenu.getItems().addAll(openProjectItem, saveProjectItem);

        // 1. 创建"工具"菜单
        Menu toolMenu = new Menu();
        registerMenu(toolMenu, "menu.view.tools");

        screenshotItem = new MenuItem();
        registerMenuItem(screenshotItem, "menu.view.tools.screenshots");
        screenshotItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+P"));
        toolMenu.getItems().add(screenshotItem);

        // 2. 创建"视图"菜单
        Menu viewMenu = new Menu();
        registerMenu(viewMenu, "menu.view.view");

        // 创建"格点模式"子菜单
        Menu gridModeMenu = new Menu();
        registerMenu(gridModeMenu, "menu.view.view.gridsDotsMode");

        // 创建单选按钮组
        ToggleGroup gridModeGroup = new ToggleGroup();

        dotModeItem = new RadioMenuItem();
        registerMenuItem(dotModeItem, "menu.view.view.dotsMode");
        dotModeItem.setToggleGroup(gridModeGroup);

        gridModeItem = new RadioMenuItem();
        registerMenuItem(gridModeItem, "menu.view.view.gridsMode");
        gridModeItem.setToggleGroup(gridModeGroup);

        polarModeItem = new RadioMenuItem();
        registerMenuItem(polarModeItem, "menu.view.view.polarMode");
        polarModeItem.setToggleGroup(gridModeGroup);

        isometricModeItem = new RadioMenuItem();
        registerMenuItem(isometricModeItem, "menu.view.view.isometricMode");
        isometricModeItem.setToggleGroup(gridModeGroup);

        dotModeItem.setSelected(true);

        gridModeMenu.getItems().addAll(dotModeItem, gridModeItem,
                polarModeItem, isometricModeItem);

        // 是否显示坐标轴
        Menu axisMenu = new Menu();
        registerMenu(axisMenu, "menu.view.axis");
        ToggleGroup iShowAxis = new ToggleGroup();
        showAxis = new RadioMenuItem();
        registerMenuItem(showAxis, "menu.view.axis.showAxis");
        showAxis.setToggleGroup(iShowAxis);

        hideAxis = new RadioMenuItem();
        registerMenuItem(hideAxis, "menu.view.axis.hideAxis");
        hideAxis.setToggleGroup(iShowAxis);

        showAxis.setSelected(true);

        axisMenu.getItems().addAll(showAxis, hideAxis);

        viewMenu.getItems().addAll(gridModeMenu, axisMenu);

        // 3. 创建"设置"菜单
        Menu settingsMenu = new Menu();
        registerMenu(settingsMenu, "menu.settings");

        systemSettingsItem = new MenuItem();
        registerMenuItem(systemSettingsItem, "menu.settings.systemSettings");

        drawingSettingsItem = new MenuItem();
        registerMenuItem(drawingSettingsItem, "menu.settings.drawingSettings");

        lineStyleSettingsItem = new MenuItem();
        registerMenuItem(lineStyleSettingsItem, "menu.settings.lineStyleSettings");

        settingsMenu.getItems().addAll(systemSettingsItem, drawingSettingsItem, lineStyleSettingsItem);

        // 4. 将所有菜单添加到菜单栏
        menus.addAll(fileMenu, toolMenu, viewMenu, settingsMenu);
        this.getMenus().addAll(fileMenu, toolMenu, viewMenu, settingsMenu);
    }

    // ======================== 辅助 ========================

    /**
     * 为 Menu 设置 i18n 文本并记录 key
     */
    private void registerMenu(Menu menu, String i18nKey) {
        menu.setText(I18nUtil.getString(i18nKey));
        i18nKeyMap.put(menu, i18nKey);
    }

    /**
     * 为 MenuItem 设置 i18n 文本并记录 key
     */
    private void registerMenuItem(MenuItem item, String i18nKey) {
        item.setText(I18nUtil.getString(i18nKey));
        i18nKeyMap.put(item, i18nKey);
    }

    // ======================== Getter / Setter ========================

    public MenuItem getSaveProjectItem() {
        return saveProjectItem;
    }

    public MenuItem getOpenProjectItem() {
        return openProjectItem;
    }

    public MenuItem getScreenshotItem() {
        return screenshotItem;
    }

    public RadioMenuItem getDotModeItem() {
        return dotModeItem;
    }

    public RadioMenuItem getGridModeItem() {
        return gridModeItem;
    }

    public RadioMenuItem getPolarModeItem() {
        return polarModeItem;
    }

    public RadioMenuItem getIsometricModeItem() {
        return isometricModeItem;
    }

    public RadioMenuItem getShowAxis() {
        return showAxis;
    }

    public RadioMenuItem getHideAxis() {
        return hideAxis;
    }

    public MenuItem getSystemSettingsItem() {
        return systemSettingsItem;
    }

    public MenuItem getDrawingSettingsItem() {
        return drawingSettingsItem;
    }

    public MenuItem getLineStyleSettingsItem() {
        return lineStyleSettingsItem;
    }

    // ======================== 事件绑定 ========================

    public void setOnSaveProjectAction(Runnable action) {
        saveProjectItem.setOnAction(e -> action.run());
    }

    public void setOnOpenProjectAction(Runnable action) {
        openProjectItem.setOnAction(e -> action.run());
    }

    public void setOnScreenshotAction(Runnable action) {
        screenshotItem.setOnAction(e -> action.run());
    }

    public void setOnDotModeSelected(Runnable action) {
        dotModeItem.setOnAction(e -> action.run());
    }

    public void setOnGridModeSelected(Runnable action) {
        gridModeItem.setOnAction(e -> action.run());
    }

    public void setOnPolarModeSelected(Runnable action) {
        polarModeItem.setOnAction(e -> action.run());
    }

    public void setOnIsometricModeSelected(Runnable action) {
        isometricModeItem.setOnAction(e -> action.run());
    }

    public void setOnShowAxisSelected(Runnable action) {
        showAxis.setOnAction(e -> action.run());
    }

    public void setOnHideAxisSelected(Runnable action) {
        hideAxis.setOnAction(e -> action.run());
    }

    public void setOnSystemSettingsAction(Runnable action) {
        systemSettingsItem.setOnAction(e -> action.run());
    }

    public void setOnDrawingSettingsAction(Runnable action) {
        drawingSettingsItem.setOnAction(e -> action.run());
    }

    public void setOnLineStyleSettingsAction(Runnable action) {
        lineStyleSettingsItem.setOnAction(e -> action.run());
    }
}
