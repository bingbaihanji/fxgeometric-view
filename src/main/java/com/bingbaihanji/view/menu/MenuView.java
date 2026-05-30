package com.bingbaihanji.view.menu;

import com.bingbaihanji.util.I18nUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;

public class MenuView extends MenuBar {

    private final ObservableList<Menu> menus = FXCollections.observableArrayList();

    // 菜单项声明,方便外部访问和添加事件监听
    private MenuItem saveProjectItem;
    private MenuItem openProjectItem;
    private MenuItem screenshotItem;
    private RadioMenuItem dotModeItem;
    private RadioMenuItem gridModeItem;
    private RadioMenuItem polarModeItem;
    private RadioMenuItem isometricModeItem;

    private RadioMenuItem showAxis; // 显示坐标轴
    private RadioMenuItem hideAxis; // 隐藏坐标轴

    private MenuItem systemSettingsItem; // 系统设置菜单项
    private MenuItem drawingSettingsItem; // 绘制设置菜单项
    private MenuItem lineStyleSettingsItem; // 线条样式设置菜单项

    public MenuView() {
        initializeMenus();
    }

    private void initializeMenus() {
        // 0. 创建"文件"菜单
        Menu fileMenu = new Menu(getMenuName("menu.file"));
        saveProjectItem = new MenuItem(getMenuName("menu.file.saveProject"));
        saveProjectItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        openProjectItem = new MenuItem(getMenuName("menu.file.openProject"));
        openProjectItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        fileMenu.getItems().addAll(openProjectItem, saveProjectItem);

        // 1. 创建"工具"菜单
        Menu toolMenu = new Menu(getMenuName("menu.view.tools"));

        // 创建"截图"菜单项
        screenshotItem = new MenuItem(getMenuName("menu.view.tools.screenshots"));
        // 可以为截图菜单项添加快捷键
        screenshotItem.setAccelerator(
                KeyCombination.keyCombination("Ctrl+Shift+P")
        );

        toolMenu.getItems().add(screenshotItem);

        // 2. 创建"视图"菜单
        Menu viewMenu = new Menu(getMenuName("menu.view.view"));

        // 创建"格点模式"子菜单
        Menu gridModeMenu = new Menu(getMenuName("menu.view.view.gridsDotsMode"));

        // 创建单选按钮组,确保点模式和格子模式互斥
        ToggleGroup gridModeGroup = new ToggleGroup();

        // 创建"点模式"单选菜单项
        dotModeItem = new RadioMenuItem(getMenuName("menu.view.view.dotsMode"));
        dotModeItem.setToggleGroup(gridModeGroup);

        // 创建"格子模式"单选菜单项
        gridModeItem = new RadioMenuItem(getMenuName("menu.view.view.gridsMode"));
        gridModeItem.setToggleGroup(gridModeGroup);

        // 创建"极坐标模式"单选菜单项
        polarModeItem = new RadioMenuItem(getMenuName("menu.view.view.polarMode"));
        polarModeItem.setToggleGroup(gridModeGroup);

        // 创建"等距网格模式"单选菜单项
        isometricModeItem = new RadioMenuItem(getMenuName("menu.view.view.isometricMode"));
        isometricModeItem.setToggleGroup(gridModeGroup);

        // 默认选择"点模式"
        dotModeItem.setSelected(true);

        // 将所有模式添加到"格点模式"子菜单
        gridModeMenu.getItems().addAll(dotModeItem, gridModeItem,
                polarModeItem, isometricModeItem);


        // 是否显示坐标轴
        Menu axisMenu = new Menu(getMenuName("menu.view.axis"));
        ToggleGroup iShowAxis = new ToggleGroup();
        showAxis = new RadioMenuItem(getMenuName("menu.view.axis.showAxis"));
        showAxis.setToggleGroup(iShowAxis);

        hideAxis = new RadioMenuItem(getMenuName("menu.view.axis.hideAxis"));
        hideAxis.setToggleGroup(iShowAxis);

        // 默认选择显示坐标轴
        showAxis.setSelected(true);

        axisMenu.getItems().addAll(showAxis, hideAxis);

        // 将"格点模式"子菜单添加到"视图"菜单
        viewMenu.getItems().addAll(gridModeMenu, axisMenu);


        // 3. 创建"设置"菜单
        Menu settingsMenu = new Menu(getMenuName("menu.settings"));

        // 创建"系统设置"菜单项
        systemSettingsItem = new MenuItem(getMenuName("menu.settings.systemSettings"));

        // 创建"绘制设置"菜单项
        drawingSettingsItem = new MenuItem(getMenuName("menu.settings.drawingSettings"));

        // 创建"线条样式设置"菜单项
        lineStyleSettingsItem = new MenuItem(getMenuName("menu.settings.lineStyleSettings"));

        settingsMenu.getItems().addAll(systemSettingsItem, drawingSettingsItem, lineStyleSettingsItem);

        // 4. 将所有菜单添加到菜单栏
        menus.addAll(fileMenu, toolMenu, viewMenu, settingsMenu);
        this.getMenus().addAll(fileMenu, toolMenu, viewMenu, settingsMenu);
    }


    private String getMenuName(String i18nKey) {
        return I18nUtil.getString(i18nKey);
    }


    // Getter 方法,方便外部添加事件监听

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

public ToggleGroup getGridModeGroup() {
        return dotModeItem.getToggleGroup();
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
    // 添加事件监听器的方法

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

    public MenuItem getLineStyleSettingsItem() {
        return lineStyleSettingsItem;
    }

    public void setOnLineStyleSettingsAction(Runnable action) {
        lineStyleSettingsItem.setOnAction(e -> action.run());
    }
}