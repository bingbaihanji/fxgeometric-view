package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.view.layout.core.GridChartView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author bingbaihanji
 * @date 2025-08-25 16:03:06
 * @description
 */
public class MenuEvent {

    private final static Logger log = LoggerFactory.getLogger(MenuEvent.class);

    private MenuView menuView;

    public MenuEvent(MenuView menuView) {
        this.menuView = menuView;
    }

    public MenuEvent() {
    }

    public MenuView getMenuView(Stage primaryStage, Node node) {

        // 添加事件监听
        menuView.setOnScreenshotAction(() -> {
            log.info("截图功能被点击");
            FxTools.screenshots(primaryStage, node);
        });

        menuView.setOnDotModeSelected(() -> {
            log.info("切换到点模式");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getSettings().setGridType(GridType.DOT);
                    gridChartView.applySettings();
                });
            }

        });

        menuView.setOnGridModeSelected(() -> {
            log.info("切换到格子模式(含次网格)");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getSettings().setGridType(GridType.CARTESIAN_WITH_SUBGRID);
                    gridChartView.applySettings();
                });
            }
        });

        menuView.setOnPolarModeSelected(() -> {
            log.info("切换到极坐标模式");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getSettings().setGridType(GridType.POLAR);
                    gridChartView.applySettings();
                });
            }
        });

        menuView.setOnIsometricModeSelected(() -> {
            log.info("切换到等距网格模式");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getSettings().setGridType(GridType.ISOMETRIC);
                    gridChartView.applySettings();
                });
            }
        });

        menuView.getShowAxis().setOnAction(event -> {
            log.info("显示坐标轴");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getSettings().setShowXAxis(true);
                    gridChartView.getSettings().setShowYAxis(true);
                    gridChartView.applySettings();
                });
            }

        });
        menuView.getHideAxis().setOnAction(event -> {
            log.info("隐藏坐标轴");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getSettings().setShowXAxis(false);
                    gridChartView.getSettings().setShowYAxis(false);
                    gridChartView.applySettings();
                });
            }
        });

        // 系统设置菜单
        menuView.setOnSystemSettingsAction(() -> {
            log.info("打开系统设置");
            Platform.runLater(() -> {
                SystemSettingsDialog dialog = new SystemSettingsDialog();
                dialog.showAndWait();
            });
        });

        // 绘制设置菜单
        menuView.setOnDrawingSettingsAction(() -> {
            log.info("打开绘制设置");
            Platform.runLater(() -> {
                // 获取当前的 FreehandDrawingTool 配置
                var freehandTool = com.bingbaihanji.controller.handler.FreehandHandler.getFreehandTool();

                DrawingSettingsDialog dialog = new DrawingSettingsDialog(
                        freehandTool.getSimplifyEpsilon(),
                        freehandTool.getSmoothSegments(),
                        freehandTool.getTension(),
                        freehandTool.getMinPointDistance(),
                        freehandTool.isEnableSmoothing()
                );

                var result = dialog.showAndWait();
                result.ifPresent(settings -> {
                    // 应用新的设置
                    freehandTool.setSimplifyEpsilon(settings.getSimplifyEpsilon());
                    freehandTool.setSmoothSegments(settings.getSmoothSegments());
                    freehandTool.setTension(settings.getTension());
                    freehandTool.setMinPointDistance(settings.getMinPointDistance());
                    freehandTool.setEnableSmoothing(settings.isEnableSmoothing());
                    log.info("绘制设置已更新");
                });
            });
        });

        // 线条样式设置菜单
        menuView.setOnLineStyleSettingsAction(() -> {
            log.info("打开线条样式设置");
            Platform.runLater(() -> {
                LineStyleSettingsDialog dialog = new LineStyleSettingsDialog();
                var result = dialog.showAndWait();
                result.ifPresent(settings -> {
                    com.bingbaihanji.util.StyleManager.defaultLineColor = settings.getColor();
                    com.bingbaihanji.util.StyleManager.defaultLineWidth = settings.getLineWidth();
                    com.bingbaihanji.util.StyleManager.GLOW_ENABLED = settings.isGlowEnabled();
                    com.bingbaihanji.util.StyleManager.GLOW_ALPHA = settings.getGlowAlpha();
                    com.bingbaihanji.util.StyleManager.GLOW_WIDTH_BONUS = settings.getGlowWidth();
                    log.info("线条样式设置已更新");
                    // 已绘制图形需要重绘以反映变化
                    if (node instanceof GridChartView gcv) {
                        gcv.redraw();
                    }
                });
            });
        });

        return menuView;
    }

    public void setMenuView(MenuView menuView) {
        this.menuView = menuView;
    }


}
