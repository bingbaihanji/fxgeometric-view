package com.binbaihanji.view;

import com.binbaihanji.controller.DrawingController;
import com.binbaihanji.util.I18nUtil;
import com.binbaihanji.view.layout.core.GridChartPane;
import com.binbaihanji.view.layout.pane.ShapeToolPane;
import javafx.scene.Scene;
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

        // 清除按钮
        toolPane.setClearAllCallback(drawingController::clearAll);

        // 5. 设置布局
        root.setLeft(toolPane);
        root.setCenter(gridChartPane);

        // 6. 设置预览绘制回调
        gridChartPane.setPreviewPainter(drawingController::paintPreview);

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle(I18nUtil.getString("application.name"));
        stage.setScene(scene);
        return stage;
    }
}