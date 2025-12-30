package com.bingbaihanji.view;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.pane.ShapeToolPane;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 独立画布窗口
 * <p>
 * 将绘图视图弹出到独立窗口，支持多显示器工作流
 * 与主窗口共享对象列表，实现双向同步
 * 包含完整的工具栏和绘制功能
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class DetachedCanvasWindow {

    private final Stage stage;
    private final GridChartView detachedView;
    private final DrawingController drawingController;
    // 存储从此窗口打开的所有子窗口
    private final List<DetachedCanvasWindow> childWindows = new ArrayList<>();
    // 父窗口引用（如果有）
    private DetachedCanvasWindow parentWindow;

    /**
     * 构造独立窗口
     *
     * @param sourceView 源视图（主窗口的GridChartView）
     */
    public DetachedCanvasWindow(GridChartView sourceView) {
        this(sourceView, null);
    }

    /**
     * 构造独立窗口（带父窗口引用）
     *
     * @param sourceView 源视图（主窗口的GridChartView）
     * @param parent 父窗口（如果从其他独立窗口打开）
     */
    public DetachedCanvasWindow(GridChartView sourceView, DetachedCanvasWindow parent) {
        this.stage = new Stage();
        this.parentWindow = parent;

        // 如果有父窗口，将自己添加到父窗口的子窗口列表中
        if (parent != null) {
            parent.addChildWindow(this);
        }

        // 使用共享的对象列表创建新视图（关键：两个窗口共享同一个对象列表）
        this.detachedView = new GridChartView(sourceView.getObjects());

        // 创建工具栏
        ShapeToolPane toolPane = new ShapeToolPane();

        // 创建绘制控制器（传递自身引用以支持子窗口层级管理）
        this.drawingController = new DrawingController(detachedView, this);

        // 绑定工具栏事件
        toolPane.drawModeProperty().addListener((obs, oldMode, newMode) -> {
            drawingController.setDrawMode(newMode);
        });

        toolPane.setOnUndo(drawingController::undo);
        toolPane.setOnRedo(drawingController::redo);
        toolPane.setOnClear(drawingController::clearAll);

        // 设置预览绘制回调
        detachedView.setPreviewPainter(drawingController::paintPreview);

        // 初始化窗口
        initWindow(toolPane);
    }

    /**
     * 初始化窗口
     */
    private void initWindow(ShapeToolPane toolPane) {
        BorderPane root = new BorderPane();

        // 创建分割面板（工具栏 + 画布）
        SplitPane central = new SplitPane(toolPane, detachedView);
        central.setOrientation(Orientation.HORIZONTAL);
        central.setDividerPositions(0.23);

        // 监听分隔条位置变化，限制范围
        central.getDividers().forEach(div -> {
            div.positionProperty().addListener((obs, oldPos, newPos) -> {
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

        // 创建菜单栏
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu(I18nUtil.getString("geo.menu.file"));

        // 导出PNG
        MenuItem exportItem = new MenuItem(I18nUtil.getString("geo.menu.exportPNG"));
        exportItem.setOnAction(e -> exportToPNG());

        // 关闭窗口
        MenuItem closeItem = new MenuItem(I18nUtil.getString("geo.menu.close"));
        closeItem.setOnAction(e -> close());

        fileMenu.getItems().addAll(exportItem, new SeparatorMenuItem(), closeItem);
        menuBar.getMenus().add(fileMenu);

        root.setTop(menuBar);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle(I18nUtil.getString("geo.window.detached.title"));

        // 添加窗口关闭事件处理
        stage.setOnCloseRequest(event -> {
            closeAllChildWindows();
        });

        // 添加快捷键支持
        scene.setOnKeyPressed(event -> handleKeyPressed(event));
    }

    /**
     * 处理快捷键事件
     */
    private void handleKeyPressed(KeyEvent event) {
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
        // Ctrl + Shift + P 截图
        if (event.isControlDown() && event.isShiftDown()) {
            if (event.getCode() == KeyCode.P) {
                FxTools.screenshots(stage, detachedView);
                event.consume();
            }
        }
    }

    /**
     * 导出为PNG
     */
    private void exportToPNG() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nUtil.getString("geo.dialog.export.title"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );
        fileChooser.setInitialFileName("geometric_view.png");

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                // 创建快照
                WritableImage image = detachedView.snapshot(new SnapshotParameters(), null);

                // 保存为PNG
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

                // 显示成功提示
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18nUtil.getString("geo.dialog.export.success.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nUtil.getString("geo.dialog.export.success.content"));
                alert.showAndWait();
            } catch (IOException e) {
                // 显示错误提示
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.getString("geo.dialog.export.error.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nUtil.getString("geo.dialog.export.error.content") + "\n" + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * 显示窗口
     */
    public void show() {
        stage.show();
    }

    /**
     * 关闭窗口
     */
    public void close() {
        closeAllChildWindows();
        stage.close();
    }

    /**
     * 关闭所有子窗口
     */
    private void closeAllChildWindows() {
        // 创建子窗口列表的副本，避免在迭代时修改集合
        List<DetachedCanvasWindow> childrenCopy = new ArrayList<>(childWindows);
        for (DetachedCanvasWindow child : childrenCopy) {
            child.close(); // 递归关闭子窗口及其子窗口
        }
        childWindows.clear();
    }

    /**
     * 添加子窗口
     */
    private void addChildWindow(DetachedCanvasWindow child) {
        childWindows.add(child);
    }

    /**
     * 获取GridChartView，供子窗口创建时使用
     */
    public GridChartView getGridChartView() {
        return detachedView;
    }

    /**
     * 获取Stage对象
     */
    public Stage getStage() {
        return stage;
    }
}
