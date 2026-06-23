package com.bingbaihanji.controller;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.handler.*;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.FunctionGeo;
import com.bingbaihanji.view.menu.FunctionInputDialog;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 图形绘制控制器(重构后的协调器版本)
 * <p>
 * 作为协调器,负责管理 Handler 并分发事件,不直接处理绘制逻辑
 *
 * @author bingbaihanji
 * @date 2025-12-21(原始版本)
 * @date 2025-12-31(重构版本)
 */
public class DrawingController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrawingController.class);

    private final IDrawingContext context;
    private final List<DrawingHandler> handlers = new ArrayList<>();
    private final List<DetachedCanvasWindow> childWindows = new ArrayList<>();
    private final MenuController menuController;
    private final LayerController layerController;

    public DrawingController(GridChartView gridChartPane) {
        this(gridChartPane, null);
    }

    public DrawingController(GridChartView gridChartPane, DetachedCanvasWindow parentWindow) {
        this.context = new DrawingContext(gridChartPane, new CommandHistory());
        registerHandlers();
        initMouseHandlers();

        gridChartPane.setPreviewPainter(this::paintPreview);

        this.menuController = new MenuController(this, gridChartPane, parentWindow);
        this.menuController.install();
        this.layerController = new LayerController(context);
    }

    private void registerHandlers() {
        IntersectionHandler intersectionHandler = new IntersectionHandler();
        ConstraintHandler constraintHandler = new ConstraintHandler();
        SnappingHandler snappingHandler = new SnappingHandler();

        context.setIntersectionHandler(intersectionHandler);
        context.setConstraintHandler(constraintHandler);
        context.setSnappingHandler(snappingHandler);

        handlers.add(new SelectionHandler());
        handlers.add(new DragHandler());
        handlers.add(new FreehandHandler());
        handlers.add(new BasicShapeHandler());
        handlers.add(new RegularPolygonHandler());
        handlers.add(new PolygonHandler());
        handlers.add(new ConstructionToolHandler());
        handlers.add(new RotationHandler());
        handlers.add(new FunctionHandler());
    }

    private void initMouseHandlers() {
        GridChartView pane = context.getGridChartPane();
        pane.setOnMouseClicked(this::handleMouseClicked);
        pane.setOnMouseMoved(this::handleMouseMoved);
        pane.setOnMousePressed(this::handleMousePressed);
        pane.setOnMouseDragged(this::handleMouseDragged);
        pane.setOnMouseReleased(this::handleMouseReleased);
    }

    public void handleMouseClicked(MouseEvent e) {
        try {
            for (DrawingHandler handler : handlers) {
                if (handler.canHandle(context.getDrawMode()) && handler.handleMouseClicked(e, context)) {
                    break;
                }
            }
        } catch (Exception ex) {
            logger.error("处理鼠标点击事件时发生错误", ex);
        }
    }

    public void handleMouseMoved(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode()) && handler.handleMouseMoved(e, context)) {
                break;
            }
        }
    }

    public void handleMousePressed(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode()) && handler.handleMousePressed(e, context)) {
                break;
            }
        }
    }

    public void handleMouseDragged(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode()) && handler.handleMouseDragged(e, context)) {
                break;
            }
        }
    }

    public void handleMouseReleased(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode()) && handler.handleMouseReleased(e, context)) {
                break;
            }
        }
    }

    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        context.getPreviewManager().paintAll(gc, transform);
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                handler.paintPreview(gc, transform, context);
            }
        }
    }

    public void setDrawMode(DrawMode mode) {
        if (mode == null) {
            logger.warn("尝试设置null绘制模式,已忽略");
            return;
        }
        logger.debug("切换绘制模式: {} -> {}", context.getDrawMode(), mode);

        for (DrawingHandler handler : handlers) {
            handler.reset();
        }
        context.getPreviewManager().resetAll();
        context.setMoveMode(com.bingbaihanji.constant.MoveMode.MOVE_NONE);
        context.setDrawMode(mode);
        context.setState(DrawingState.IDLE);

        if (mode == DrawMode.ROTATE) {
            context.setState(DrawingState.ROTATE_SELECT_SHAPE);
        }
        context.redraw();
    }

    public void clearAll() {
        try {
            List<WorldObject> objectsToClear = new ArrayList<>(context.getObjects());
            if (objectsToClear.isEmpty()) {
                logger.debug("画布已为空,无需清空");
                return;
            }
            logger.info("清空画布,共删除{}个对象", objectsToClear.size());

            context.executeCommand(new CommandHistory.Command() {
                @Override
                public void execute() {
                    context.getGridChartPane().clearAllObjects();
                    context.getIntersectionHandler().clearOwnershipMap();
                    PointNameManager.getInstance().clear();
                }

                @Override
                public void undo() {
                    for (WorldObject obj : objectsToClear) {
                        context.addObject(obj);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("清空画布时发生错误", e);
        }
    }

    public void undo() {
        context.getCommandHistory().undo();
        context.redraw();
    }

    public void redo() {
        context.getCommandHistory().redo();
        context.redraw();
    }

    public void showFunctionDialog() {
        Platform.runLater(() -> {
            double width = context.getGridChartPane().getWidth();
            double height = context.getGridChartPane().getHeight();
            FunctionInputDialog dialog = new FunctionInputDialog(context.getTransform(), width, height);
            Optional<FunctionInputResult> result = dialog.showAndWait();

            if (result.isPresent()) {
                FunctionInputResult input = result.get();
                try {
                    FunctionGeo function = com.bingbaihanji.factory.FunctionFactory.createFunction(input);
                    if (!input.autoRange()) {
                        function.setDomainRange(input.domainMin(), input.domainMax());
                    }
                    context.executeCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            context.addObject(function);
                        }

                        @Override
                        public void undo() {
                            context.removeObject(function);
                        }
                    });
                    context.redraw();
                } catch (com.bingbaihanji.factory.FunctionCreationException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("函数创建失败");
                    alert.setHeaderText("无法创建函数图像");
                    alert.setContentText(ex.getMessage());
                    alert.show();
                }
            }
        });
    }

    public void showRegularPolygonDialog() {
        for (DrawingHandler handler : handlers) {
            if (handler instanceof com.bingbaihanji.controller.handler.RegularPolygonHandler) {
                ((com.bingbaihanji.controller.handler.RegularPolygonHandler) handler).showSidesSelectionDialog();
                break;
            }
        }
    }

    public boolean canUndo() {
        return context.getCommandHistory().canUndo();
    }

    public boolean canRedo() {
        return context.getCommandHistory().canRedo();
    }

    public IDrawingContext getContext() {
        return context;
    }

    public MenuController getMenuController() {
        return menuController;
    }

    public LayerController getLayerController() {
        return layerController;
    }

    public void clearSelection() {
        context.getSelectionManager().clearSelection();
        context.redraw();
    }

    public boolean hasSelection() {
        return context.getSelectionManager().hasSelection();
    }

    public void deleteSelection() {
        SelectionManager selectionManager = context.getSelectionManager();
        if (!selectionManager.hasSelection()) {
            logger.debug("没有选中的对象,删除操作已忽略");
            return;
        }
        List<WorldObject> objectsToDelete = new ArrayList<>(selectionManager.getSelectedObjects());
        if (objectsToDelete.isEmpty()) return;

        logger.info("删除 {} 个选中对象", objectsToDelete.size());
        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                for (WorldObject obj : objectsToDelete) {
                    context.removeObject(obj);
                }
                selectionManager.clearSelection(false);
                context.invalidateSnapCache();
            }

            @Override
            public void undo() {
                for (WorldObject obj : objectsToDelete) {
                    context.addObject(obj);
                }
                for (int i = 0; i < objectsToDelete.size(); i++) {
                    selectionManager.addSelectedObject(objectsToDelete.get(i), i == objectsToDelete.size() - 1);
                }
                context.invalidateSnapCache();
            }
        });
        context.redraw();
        logger.debug("删除完成");
    }

    public boolean isDrawing() {
        return context.getState() != DrawingState.IDLE;
    }

    public void cancelCurrentOperation() {
        logger.debug("取消当前操作,当前状态: {}", context.getState());
        for (DrawingHandler handler : handlers) {
            handler.reset();
        }
        context.getPreviewManager().clearAll();
        context.setState(DrawingState.IDLE);
        context.setMoveMode(com.bingbaihanji.constant.MoveMode.MOVE_NONE);
        context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());
        context.redraw();
        logger.info("绘制操作已取消");
    }

    // 层级管理（委托给 LayerController）
    public void bringSelectionForward() {
        layerController.bringSelectionForward();
    }

    public void sendSelectionBackward() {
        layerController.sendSelectionBackward();
    }

    public void bringSelectionToFront() {
        layerController.bringSelectionToFront();
    }

    public void sendSelectionToBack() {
        layerController.sendSelectionToBack();
    }

    // 窗口管理
    public void addChildWindow(DetachedCanvasWindow childWindow) {
        childWindows.add(childWindow);
    }

    public void closeAllChildWindows() {
        for (DetachedCanvasWindow childWindow : childWindows) {
            childWindow.close();
        }
        childWindows.clear();
    }
}
