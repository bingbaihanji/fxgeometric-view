package com.bingbaihanji.view;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.pane.ShapeToolPane;
import com.bingbaihanji.view.menu.MenuEvent;
import com.bingbaihanji.view.menu.MenuView;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    /**
     * 绘制控制器
     */
    private DrawingController drawingController;

    public InitView(Stage stage) {
        this.stage = stage;
    }

    public Stage init() {

        BorderPane root = new BorderPane();

        // 1. 创建坐标系面板
        GridChartView gridChartPane = new GridChartView();

        // 2. 创建工具栏
        ShapeToolPane toolPane = new ShapeToolPane();

        // 3. 创建绘制控制器
        DrawingController drawingController = new DrawingController(gridChartPane);

        // 4. 绑定事件
        // 工具栏模式切换
        toolPane.drawModeProperty().addListener((obs, oldMode, newMode) -> {
            drawingController.setDrawMode(newMode);
        });

        // 绑定撤销/恢复/清空按钮
        toolPane.setOnUndo(drawingController::undo);
        toolPane.setOnRedo(drawingController::redo);
        toolPane.setOnClear(drawingController::clearAll);

        // 5. 设置布局
        SplitPane central = new SplitPane(toolPane, gridChartPane);
        central.setOrientation(Orientation.HORIZONTAL);
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


        var menuView = new MenuView();

        MenuEvent menuEvent = new MenuEvent(menuView);
        root.setTop(menuEvent.getMenuView(stage, gridChartPane));

        Scene scene = new Scene(root, 1000, 700);

        // 7. 添加快捷键支持
        scene.setOnKeyPressed(event -> handleKeyPressed(event, stage, gridChartPane));
        this.drawingController = drawingController;

        // 8. 添加语言变化监听器，切换语言后重新加载界面
        I18nUtil.addLocaleChangeListener(() -> {
            Platform.runLater(() -> {
                // 重新初始化界面
                Stage newStage = init();
                // 保持窗口尺寸和位置
                newStage.setX(stage.getX());
                newStage.setY(stage.getY());
                newStage.setWidth(stage.getWidth());
                newStage.setHeight(stage.getHeight());
                stage.setScene(newStage.getScene());
                stage.setTitle(I18nUtil.getString("application.name"));
            });
        });

        stage.setTitle(I18nUtil.getString("application.name"));
        stage.setScene(scene);
        return stage;
    }

    /**
     * 处理快捷键事件
     */
    private void handleKeyPressed(KeyEvent event, Stage primaryStage, Node node) {
        if (event.isControlDown()) {
            if (event.getCode() == KeyCode.Z) {
                // Ctrl+Z: 撤销
                drawingController.undo();
                event.consume();
            } else if (event.getCode() == KeyCode.Y) {
                // Ctrl+Y: 恢复
                drawingController.redo();
                event.consume();
            }
        }
        // ctrl + shift + p 截图
        if (event.isControlDown() && event.isShiftDown()) {
            if (event.getCode() == KeyCode.P) {
                FxTools.screenshots(primaryStage, node);
                event.consume(); // 确保消费事件
            }
        }
    }
}
