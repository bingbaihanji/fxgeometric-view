package com.binbaihanji.view;

import com.binbaihanji.controller.DrawingController;
import com.binbaihanji.util.I18nUtil;
import com.binbaihanji.view.layout.core.GridChartPane;
import com.binbaihanji.view.layout.pane.ShapeToolPane;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * 首页布局配置
 * <p>
 * 集成坐标系、绘图层、工具栏和绘制控制器
 *
 * @author bingbaihanji
 * @date 2025-12-20 15:25:06
 */
public class InitView {
    private final Stage stage;

    public InitView(Stage stage) {
        this.stage = stage;
    }

    public Stage init() {

        BorderPane root = new BorderPane();

        // 1. 创建坐标系面板
        GridChartPane gridChartPane = new GridChartPane();

        // 2. 创建工具栏
        ShapeToolPane toolPane = new ShapeToolPane();

        // 3. 创建绘制控制器
        DrawingController drawingController = new DrawingController(gridChartPane);

        // 4. 绑定事件
        // 工具栏模式切换
        toolPane.drawModeProperty().addListener((obs, oldMode, newMode) -> {
            drawingController.setDrawMode(newMode);
        });

        // 多边形边数切换
        toolPane.polygonSidesProperty().addListener((obs, oldSides, newSides) -> {
            drawingController.setPolygonSides(newSides.intValue());
        });

        // 5. 设置布局
        SplitPane central = new SplitPane(toolPane, gridChartPane);
        central.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        central.setDividerPositions(0.23);

        // 监听整个SplitPane的分隔条位置变化
        central.getDividers().forEach(div -> {
            div.positionProperty().addListener((obs, oldPos, newPos) -> {
                // 如果是第一个分隔条（索引0）
                if (central.getDividers().indexOf(div) == 0) {
                    if (newPos.doubleValue() < 0.23) {
                        Platform.runLater(() -> div.setPosition(0.23));
                    } else if (newPos.doubleValue() > 0.33) {
                        Platform.runLater(() -> div.setPosition(0.33));
                    }
                }
            });
        });

        root.setCenter(central);

        // 6. 设置预览绘制回调
        gridChartPane.setPreviewPainter(drawingController::paintPreview);

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle(I18nUtil.getString("application.name"));
        stage.setScene(scene);
        return stage;
    }
}
