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
import javafx.scene.input.KeyCode;
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

        // 绑定正多边形按钮
        toolPane.setOnRegularPolygonClick(controller::showRegularPolygonDialog);
    }

    /**
     * 创建中央分割面板
     * <p>
     * 优化版：通过设置子节点的最小/最大尺寸来约束分割线位置,避免频闪
     */
    private SplitPane createCentralSplitPane(ShapeToolPane toolPane, GridChartView gridChartPane) {
        SplitPane central = new SplitPane(toolPane, gridChartPane);
        central.setOrientation(Orientation.HORIZONTAL);

        // 方案1：设置工具面板的最小和最大宽度(更自然的约束方式)
        toolPane.setMinWidth(230);   // 最小宽度
        toolPane.setMaxWidth(330);   // 最大宽度

        // 设置初始分割位置
        central.setDividerPositions(0.23);

        // 可选：使用监听器微调(避免频闪的优化版本)
        // 仅在必要时使用,通常设置 min/max 宽度就足够了
        Platform.runLater(() -> {
            if (!central.getDividers().isEmpty()) {
                SplitPane.Divider divider = central.getDividers().get(0);

                // 使用标志位防止递归触发
                final boolean[] isAdjusting = {false};

                divider.positionProperty().addListener((obs, oldPos, newPos) -> {
                    if (isAdjusting[0]) {
                        return; // 防止递归
                    }

                    double position = newPos.doubleValue();
                    double adjustedPosition = position;

                    // 约束边界(作为备份保护)
                    final double MIN_POSITION = 0.23;
                    final double MAX_POSITION = 0.33;

                    if (position < MIN_POSITION) {
                        adjustedPosition = MIN_POSITION;
                    } else if (position > MAX_POSITION) {
                        adjustedPosition = MAX_POSITION;
                    }

                    // 只有在需要调整且差异足够大时才设置
                    if (Math.abs(adjustedPosition - position) > 0.001) {
                        isAdjusting[0] = true;
                        divider.setPosition(adjustedPosition);
                        isAdjusting[0] = false;
                    }
                });
            }
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

        // ESC键：优先取消当前绘制操作,如果没有绘制中则清除选择
        if (event.getCode() == KeyCode.ESCAPE) {
            if (drawingController.isDrawing()) {
                // 正在绘制中,取消当前操作
                drawingController.cancelCurrentOperation();
            } else {
                // 没有绘制操作,清除选择
                drawingController.clearSelection();
            }
            event.consume();
            return;
        }

        // Delete键：删除选中的对象
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            if (drawingController.hasSelection()) {
                drawingController.deleteSelection();
                event.consume();
                return;
            }
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
                case D:
                    // Ctrl+D: 删除选中对象(备用快捷键)
                    if (drawingController.hasSelection()) {
                        drawingController.deleteSelection();
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
