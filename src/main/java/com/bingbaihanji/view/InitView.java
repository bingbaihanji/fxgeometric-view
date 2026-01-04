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
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * 首页布局配置
 * <p>
 * 集成坐标系、绘图层、工具栏和绘制控制器
 * <p>
 * 职责：负责主窗口的UI布局组装和事件绑定
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
        BorderPane root = createMainLayout();
        Scene scene = createScene(root);

        stage.setTitle(I18nUtil.getString("application.name"));
        stage.setScene(scene);

        // 添加窗口关闭事件处理
        stage.setOnCloseRequest(event -> handleWindowClose());

        return stage;
    }

    /**
     * 创建主布局
     */
    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // 1. 创建坐标系面板
        GridChartView gridChartPane = new GridChartView();

        // 2. 创建工具栏
        ShapeToolPane toolPane = new ShapeToolPane();

        // 3. 创建绘制控制器
        DrawingController drawingController = new DrawingController(gridChartPane);
        this.drawingController = drawingController;

        // 4. 绑定事件
        bindToolPaneEvents(toolPane, drawingController);

        // 5. 设置中央分割面板
        SplitPane central = createCentralSplitPane(toolPane, gridChartPane);
        root.setCenter(central);

        // 6. 设置预览绘制回调
        gridChartPane.setPreviewPainter(drawingController::paintPreview);

        // 7. 创建菜单栏
        MenuView menuView = new MenuView();
        MenuEvent menuEvent = new MenuEvent(menuView);
        root.setTop(menuEvent.getMenuView(stage, gridChartPane));

        return root;
    }

    /**
     * 绑定工具栏事件
     */
    private void bindToolPaneEvents(ShapeToolPane toolPane, DrawingController controller) {
        // 工具栏模式切换
        toolPane.drawModeProperty().addListener((obs, oldMode, newMode) -> {
            controller.setDrawMode(newMode);
        });

        // 绑定撤销/恢复/清空按钮
        toolPane.setOnUndo(controller::undo);
        toolPane.setOnRedo(controller::redo);
        toolPane.setOnClear(controller::clearAll);

        // 绑定函数绘制按钮
        toolPane.setOnFunctionClick(controller::showFunctionDialog);
    }

    /**
     * 创建中央分割面板
     */
    private SplitPane createCentralSplitPane(ShapeToolPane toolPane, GridChartView gridChartPane) {
        SplitPane central = new SplitPane(toolPane, gridChartPane);
        central.setOrientation(Orientation.HORIZONTAL);
        central.setDividerPositions(0.23);

        // 限制分隔条的移动范围
        central.getDividers().forEach(div -> {
            div.positionProperty().addListener((obs, oldPos, newPos) -> {
                if (central.getDividers().indexOf(div) == 0) {
                    double position = newPos.doubleValue();
                    if (position < 0.23) {
                        Platform.runLater(() -> div.setPosition(0.23));
                    } else if (position > 0.33) {
                        Platform.runLater(() -> div.setPosition(0.33));
                    }
                }
            });
        });

        return central;
    }

    /**
     * 创建场景并添加快捷键支持
     */
    private Scene createScene(BorderPane root) {
        Scene scene = new Scene(root, 1000, 700);

        // 添加快捷键支持
        scene.setOnKeyPressed(event -> handleKeyPressed(event));

        // 添加语言变化监听器
        setupLocaleChangeListener();

        return scene;
    }

    /**
     * 设置语言变化监听器
     */
    private void setupLocaleChangeListener() {
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
    }

    /**
     * 处理窗口关闭事件
     */
    private void handleWindowClose() {
        if (drawingController != null) {
            drawingController.closeAllChildWindows();
        }
    }

    /**
     * 处理快捷键事件
     */
    private void handleKeyPressed(KeyEvent event) {
        if (drawingController == null) {
            return;
        }

        if (event.isControlDown()) {
            switch (event.getCode()) {
                case Z:
                    // Ctrl+Z: 撤销
                    drawingController.undo();
                    event.consume();
                    break;
                case Y:
                    // Ctrl+Y: 恢复
                    drawingController.redo();
                    event.consume();
                    break;
                case P:
                    // Ctrl+Shift+P: 截图
                    if (event.isShiftDown()) {
                        FxTools.screenshots(stage, stage.getScene().getRoot());
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
