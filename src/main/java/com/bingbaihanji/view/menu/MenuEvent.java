package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.GridMode;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.impl.AxesPainter;
import com.bingbaihanji.view.layout.draw.geometry.impl.GridPainter;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Stage;

/**
 * @author bingbaihanji
 * @date 2025-08-25 16:03:06
 * @description
 */
public class MenuEvent {

    private MenuView menuView;

    public MenuEvent(MenuView menuView) {
        this.menuView = menuView;
    }

    public MenuEvent() {
    }

    public MenuView getMenuView(Stage primaryStage, Node node) {

        // 添加事件监听
        menuView.setOnScreenshotAction(() -> {
            System.out.println("截图功能被点击");
            FxTools.screenshots(primaryStage, node);
        });

        menuView.setOnDotModeSelected(() -> {
            System.out.println("切换到点模式");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getPainters().forEach(painter -> {
                        if (painter instanceof GridPainter gridPainter) {
                            gridPainter.setGridMode(GridMode.DOT);
                        }
                    });
                    gridChartView.redraw();
                });
            }

        });

        menuView.setOnGridModeSelected(() -> {
            System.out.println("切换到格子模式");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getPainters().forEach(painter -> {
                        if (painter instanceof GridPainter gridPainter) {
                            gridPainter.setGridMode(GridMode.LINE);
                        }
                    });
                    gridChartView.redraw();
                });
            }
        });


        menuView.getShowAxis().setOnAction(event -> {
            System.out.println("显示坐标轴");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getPainters().forEach(painter -> {
                        if (painter instanceof AxesPainter axesPainter) {
                            axesPainter.setShowCartesianCoordinateAxis(true);
                        }
                    });
                    gridChartView.redraw();
                });
            }

        });
        menuView.getHideAxis().setOnAction(event -> {
            System.out.println("隐藏坐标轴");
            if (node instanceof GridChartView gridChartView) {
                Platform.runLater(() -> {
                    gridChartView.getPainters().forEach(painter -> {
                        if (painter instanceof AxesPainter axesPainter) {
                            axesPainter.setShowCartesianCoordinateAxis(false);
                        }
                    });
                    gridChartView.redraw();
                });
            }
        });

        // 系统设置菜单
        menuView.setOnSystemSettingsAction(() -> {
            System.out.println("打开系统设置");
            Platform.runLater(() -> {
                SystemSettingsDialog dialog = new SystemSettingsDialog();
                dialog.showAndWait();
            });
        });

        return menuView;
    }

    public void setMenuView(MenuView menuView) {
        this.menuView = menuView;
    }


}
