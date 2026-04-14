package com.bingbaihanji.controller;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.handler.*;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.Logger;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.FunctionGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.menu.FunctionInputDialog;
import com.bingbaihanji.view.menu.GeometryContextMenu;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
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

    private static final Logger logger = Logger.getLogger(DrawingController.class);

    /**
     * 绘制上下文
     */
    private final DrawingContext context;

    /**
     * 交互型 Handler 列表(按优先级排序)
     */
    private final List<DrawingHandler> handlers = new ArrayList<>();
    /**
     * 从此控制器(主窗口或独立窗口)打开的所有子窗口
     */
    private final List<DetachedCanvasWindow> childWindows = new ArrayList<>();
    /**
     * 父窗口引用(如果在独立窗口中则不为null)
     */
    private DetachedCanvasWindow parentWindow;

    /**
     * 当前显示的右键菜单(用于在显示新菜单前关闭旧菜单)
     */
    private ContextMenu currentContextMenu = null;

    /**
     * 构造函数
     */
    public DrawingController(GridChartView gridChartPane) {
        this(gridChartPane, null);
    }

    /**
     * 构造函数(带父窗口引用)
     */
    public DrawingController(GridChartView gridChartPane, DetachedCanvasWindow parentWindow) {
        this.parentWindow = parentWindow;

        // 初始化 Context
        this.context = new DrawingContext(gridChartPane, new CommandHistory());

        // 注册所有 Handler
        registerHandlers();

        // 初始化鼠标事件监听
        initMouseHandlers();

        // 设置预览绘制回调
        gridChartPane.setPreviewPainter(this::paintPreview);

        // 已禁用：设置 BoundingBox 绘制回调
        /*
        gridChartPane.setBoundingBoxPainter((gc, transform) -> {
            if (context.getSelectionManager().hasBoundingBox()) {
                context.getSelectionManager().getBoundingBox().paint(gc, transform);
            }
        });
        */
    }

    /**
     * 注册所有 Handler(按优先级排序)
     */
    private void registerHandlers() {
        // 服务型 Handler(不处理鼠标事件)
        IntersectionHandler intersectionHandler = new IntersectionHandler();
        ConstraintHandler constraintHandler = new ConstraintHandler();
        SnappingHandler snappingHandler = new SnappingHandler();

        context.setIntersectionHandler(intersectionHandler);
        context.setConstraintHandler(constraintHandler);
        context.setSnappingHandler(snappingHandler);

        // 交互型 Handler(处理鼠标事件,按优先级排序)
        handlers.add(new SelectionHandler());         // 选择功能(在非绘制模式下生效,优先于拖动)
        handlers.add(new DragHandler());              // 拖动在非绘制模式下优先
        handlers.add(new FreehandHandler());          // 手绘优先级高
        handlers.add(new BasicShapeHandler());        // 基础图形
        handlers.add(new RegularPolygonHandler());    // 正多边形
        handlers.add(new PolygonHandler());           // 多边形
        handlers.add(new ConstructionToolHandler());  // 作图工具
        handlers.add(new RotationHandler());          // 旋转
        handlers.add(new FunctionHandler());          // 函数绘制
    }

    /**
     * 初始化鼠标事件处理器
     */
    private void initMouseHandlers() {
        context.getGridChartPane().setOnMouseClicked(this::handleMouseClicked);
        context.getGridChartPane().setOnMouseMoved(this::handleMouseMoved);
        context.getGridChartPane().setOnMousePressed(this::handleMousePressed);
        context.getGridChartPane().setOnMouseDragged(this::handleMouseDragged);
        context.getGridChartPane().setOnMouseReleased(this::handleMouseReleased);
        context.getGridChartPane().setOnContextMenuRequested(this::handleContextMenu);
    }

    // 事件分发方法(责任链模式)

    /**
     * 鼠标点击事件
     */
    public void handleMouseClicked(MouseEvent e) {
        try {
            for (DrawingHandler handler : handlers) {
                if (handler.canHandle(context.getDrawMode())) {
                    if (handler.handleMouseClicked(e, context)) {
                        break; // 已处理,停止传递
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("处理鼠标点击事件时发生错误", ex);
        }
    }

    /**
     * 鼠标移动事件(实时预览)
     */
    public void handleMouseMoved(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                if (handler.handleMouseMoved(e, context)) {
                    break; // 已处理,停止传递
                }
            }
        }
    }

    /**
     * 鼠标按下事件
     */
    public void handleMousePressed(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                if (handler.handleMousePressed(e, context)) {
                    break; // 已处理,停止传递
                }
            }
        }
    }

    /**
     * 鼠标拖拽事件
     */
    public void handleMouseDragged(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                if (handler.handleMouseDragged(e, context)) {
                    break; // 已处理,停止传递
                }
            }
        }
    }

    /**
     * 鼠标释放事件
     */
    public void handleMouseReleased(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                if (handler.handleMouseReleased(e, context)) {
                    break; // 已处理,停止传递
                }
            }
        }
    }

    /**
     * 绘制预览图形(统一由 PreviewManager 管理)
     */
    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        // 统一预览绘制: PreviewManager 管理所有 Previewable 对象
        context.getPreviewManager().paintAll(gc, transform);

        // Handler 的补充预览(用于特殊效果,如高亮、吸附提示等)
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                handler.paintPreview(gc, transform, context);
            }
        }
    }

    // 对外接口(保持兼容性)

    /**
     * 设置绘制模式
     */
    public void setDrawMode(DrawMode mode) {
        if (mode == null) {
            logger.warn("尝试设置null绘制模式,已忽略");
            return;
        }

        logger.debug("切换绘制模式: {} -> {}", context.getDrawMode(), mode);

        // 重置所有 Handler 状态
        for (DrawingHandler handler : handlers) {
            handler.reset();
        }

        // 重置 PreviewManager(清除所有预览对象)
        context.getPreviewManager().resetAll();

        // 重置移动模式
        context.setMoveMode(com.bingbaihanji.constant.MoveMode.MOVE_NONE);

        context.setDrawMode(mode);
        context.setState(DrawingState.IDLE);

        // 如果是旋转模式,进入选择图形状态
        if (mode == DrawMode.ROTATE) {
            context.setState(DrawingState.ROTATE_SELECT_SHAPE);
        }

        // 清除预览
        context.redraw();
    }

    /**
     * 清空画布
     */
    public void clearAll() {
        try {
            // 保存当前所有对象,用于撤销
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
                    // 清除点命名管理器
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

    /**
     * 撤销操作
     */
    public void undo() {
        context.getCommandHistory().undo();
        context.redraw();
    }

    /**
     * 恢复操作
     */
    public void redo() {
        context.getCommandHistory().redo();
        context.redraw();
    }

    /**
     * 显示函数绘制对话框
     */
    public void showFunctionDialog() {
        Platform.runLater(() -> {
            // 显示函数输入对话框
            double width = context.getGridChartPane().getWidth();
            double height = context.getGridChartPane().getHeight();

            FunctionInputDialog dialog = new FunctionInputDialog(
                    context.getTransform(),
                    width,
                    height
            );

            Optional<FunctionInputResult> result = dialog.showAndWait();

            if (result.isPresent()) {
                FunctionInputResult input = result.get();
                FunctionGeo function = createFunction(input);

                if (function != null) {
                    // 设置定义域
                    if (!input.isAutoRange()) {
                        function.setDomainRange(input.getDomainMin(), input.getDomainMax());
                    }

                    // 通过命令历史添加到画布(支持撤销)
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
                }
            }
        });
    }

    /**
     * 显示正多边形边数选择对话框
     */
    public void showRegularPolygonDialog() {
        // 找到RegularPolygonHandler并调用其对话框方法
        for (DrawingHandler handler : handlers) {
            if (handler instanceof com.bingbaihanji.controller.handler.RegularPolygonHandler) {
                ((com.bingbaihanji.controller.handler.RegularPolygonHandler) handler).showSidesSelectionDialog();
                break;
            }
        }
    }

    /**
     * 根据输入结果创建函数对象
     * <p>
     * 委托给 FunctionFactory 工厂类创建,避免代码重复
     */
    private FunctionGeo createFunction(FunctionInputResult input) {
        try {
            return com.bingbaihanji.factory.FunctionFactory.createFunction(input);
        } catch (Exception e) {
            logger.error("创建函数对象时发生错误", e);
            return null;
        }
    }

    /**
     * 判断是否可以撤销
     */
    public boolean canUndo() {
        return context.getCommandHistory().canUndo();
    }

    /**
     * 判断是否可以恢复
     */
    public boolean canRedo() {
        return context.getCommandHistory().canRedo();
    }

    /**
     * 获取绘制上下文
     */
    public DrawingContext getContext() {
        return context;
    }

    /**
     * 清除选择(ESC键和点击空白区域时调用)
     */
    public void clearSelection() {
        context.getSelectionManager().clearSelection();
        context.redraw();
    }

    /**
     * 判断是否有选中对象
     *
     * @return 如果有选中对象返回 true
     */
    public boolean hasSelection() {
        return context.getSelectionManager().hasSelection();
    }

    /**
     * 删除选中的对象
     * <p>
     * Delete键按下时调用,支持撤销/恢复
     */
    public void deleteSelection() {
        SelectionManager selectionManager = context.getSelectionManager();

        if (!selectionManager.hasSelection()) {
            logger.debug("没有选中的对象,删除操作已忽略");
            return;
        }

        // 获取要删除的对象列表(需要复制一份,因为删除后选择会被清空)
        List<WorldObject> objectsToDelete = new ArrayList<>(selectionManager.getSelectedObjects());

        if (objectsToDelete.isEmpty()) {
            return;
        }

        logger.info("删除 {} 个选中对象", objectsToDelete.size());

        // 创建支持撤销的删除命令
        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                // 执行删除
                for (WorldObject obj : objectsToDelete) {
                    context.removeObject(obj);
                }
                // 清除选择
                selectionManager.clearSelection(false);
                // 使缓存失效
                context.invalidateSnapCache();
            }

            @Override
            public void undo() {
                // 恢复删除的对象
                for (WorldObject obj : objectsToDelete) {
                    context.addObject(obj);
                }
                // 恢复选择状态(最后一个对象通知监听器)
                for (int i = 0; i < objectsToDelete.size(); i++) {
                    boolean isLast = (i == objectsToDelete.size() - 1);
                    selectionManager.addSelectedObject(objectsToDelete.get(i), isLast);
                }
                // 使缓存失效
                context.invalidateSnapCache();
            }
        });

        context.redraw();
        logger.debug("删除完成");
    }

    /**
     * 判断是否正在进行绘制操作
     * <p>
     * 用于ESC键判断：如果正在绘制则取消操作,否则清除选择
     *
     * @return 如果当前状态不是IDLE,说明正在绘制中
     */
    public boolean isDrawing() {
        return context.getState() != DrawingState.IDLE;
    }

    /**
     * 取消当前绘制操作
     * <p>
     * ESC键按下时调用,重置所有Handler状态并清除预览
     */
    public void cancelCurrentOperation() {
        logger.debug("取消当前操作,当前状态: {}", context.getState());

        // 重置所有 Handler 状态
        for (DrawingHandler handler : handlers) {
            handler.reset();
        }

        // 清除所有预览对象
        context.getPreviewManager().clearAll();

        // 重置绘制状态
        context.setState(DrawingState.IDLE);
        context.setMoveMode(com.bingbaihanji.constant.MoveMode.MOVE_NONE);

        // 恢复默认光标
        context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());

        // 重绘画布
        context.redraw();

        logger.info("绘制操作已取消");
    }

    // ========== 层级管理方法 ==========

    /**
     * 将选中对象上移一层
     */
    public void bringSelectionForward() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        for (WorldObject obj : selectedObjects) {
            int index = allObjects.indexOf(obj);
            if (index < allObjects.size() - 1) {
                allObjects.remove(index);
                allObjects.add(index + 1, obj);
            }
        }
        context.redraw();
    }

    /**
     * 将选中对象下移一层
     */
    public void sendSelectionBackward() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        for (WorldObject obj : selectedObjects) {
            int index = allObjects.indexOf(obj);
            if (index > 0) {
                allObjects.remove(index);
                allObjects.add(index - 1, obj);
            }
        }
        context.redraw();
    }

    /**
     * 将选中对象置于顶层
     */
    public void bringSelectionToFront() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        allObjects.removeAll(selectedObjects);
        allObjects.addAll(selectedObjects);
        context.redraw();
    }

    /**
     * 将选中对象置于底层
     */
    public void sendSelectionToBack() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        allObjects.removeAll(selectedObjects);
        allObjects.addAll(0, selectedObjects);
        context.redraw();
    }

    // 窗口管理方法(保留)

    /**
     * 添加子窗口到列表中
     */
    public void addChildWindow(DetachedCanvasWindow childWindow) {
        childWindows.add(childWindow);
    }

    /**
     * 关闭所有子窗口
     */
    public void closeAllChildWindows() {
        for (DetachedCanvasWindow childWindow : childWindows) {
            childWindow.close();
        }
        childWindows.clear();
    }

    // 右键菜单处理(保留)

    /**
     * 处理右键菜单事件
     */
    private void handleContextMenu(ContextMenuEvent event) {
        // 如果当前有菜单显示,先关闭它并返回(不显示新菜单)
        if (currentContextMenu != null && currentContextMenu.isShowing()) {
            hideCurrentContextMenu();
            event.consume();
            return;
        }

        double worldX = context.getGridChartPane().screenToWorldX(event.getX());
        double worldY = context.getGridChartPane().screenToWorldY(event.getY());
        double scale = context.getTransform().getScale();
        double vertexTolerance = 10.0 / scale; // 顶点使用更大的容差
        double objectTolerance = 5.0 / scale;  // 对象使用较小的容差

        List<WorldObject> objects = context.getObjects();

        //   已禁用：优先级0：检查是否点击了BoundingBox区域  
        /*
        // 如果有选中对象,且右键点击在BoundingBox边界内,显示BoundingBox菜单
        if (context.getSelectionManager().hasBoundingBox()) {
            double[] bounds = context.getSelectionManager().getBoundingBox().getBounds();
            double minX = bounds[0];
            double maxX = bounds[1];
            double minY = bounds[2];
            double maxY = bounds[3];
            
            // 检查点击是否在BoundingBox边界内部(除非点击的是独立点)
            if (worldX >= minX && worldX <= maxX && worldY >= minY && worldY <= maxY) {
                // 检查是否点击的是独立点对象(独立点优先级更高)
                boolean clickedPoint = false;
                for (int i = objects.size() - 1; i >= 0; i--) {
                    WorldObject obj = objects.get(i);
                    if (obj instanceof PointGeo point) {
                        if (point.hitTest(worldX, worldY, vertexTolerance)) {
                            clickedPoint = true;
                            break;
                        }
                    }
                }
                
                if (!clickedPoint) {
                    // 没有点击独立点,显示BoundingBox菜单
                    ContextMenu menu = GeometryContextMenu.createBoundingBoxMenu(context.getGridChartPane(), this);
                    showContextMenu(menu, event);
                    return;
                }
            }
        }
        */

        //   优先级1：检查是否点击了独立的点对象(PointGeo)  
        // 独立点对象应该使用专用的点菜单
        // 注意：即使该位置同时是其他图形的顶点,也优先显示点菜单
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj instanceof PointGeo point) {
                if (point.hitTest(worldX, worldY, vertexTolerance)) {
                    // 点击了独立的点对象,显示点菜单
                    ContextMenu menu = GeometryContextMenu.createPointMenu(point, context.getGridChartPane(), this);
                    showContextMenu(menu, event);
                    return;
                }
            }
        }

        //   优先级2：检查是否点击了图形的顶点(排除PointGeo)  
        for (WorldObject obj : objects) {
            // 跳过独立的点对象,它们已经在上面处理了
            if (obj instanceof PointGeo) {
                continue;
            }
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, vertexTolerance)) {
                    // 点击了图形的顶点,显示顶点菜单
                    ContextMenu menu = GeometryContextMenu.createVertexMenu(
                            point, obj, context.getGridChartPane(), this
                    );
                    showContextMenu(menu, event);
                    return;
                }
            }
        }

        //   优先级3：检查是否点击了其他图形对象  
        WorldObject clickedObject = null;
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            // 跳过独立的点对象
            if (obj instanceof PointGeo) {
                continue;
            }
            if (obj.hitTest(worldX, worldY, objectTolerance)) {
                clickedObject = obj;
                break;
            }
        }

        ContextMenu menu;
        if (clickedObject != null) {
            // 其他图形的右键菜单
            menu = GeometryContextMenu.createShapeMenu(clickedObject, context.getGridChartPane(), this);
        } else {
            // 画布的右键菜单
            menu = GeometryContextMenu.createCanvasMenu(context.getGridChartPane(), this, parentWindow);
        }

        showContextMenu(menu, event);
    }

    /**
     * 显示右键菜单并保存引用
     */
    private void showContextMenu(ContextMenu menu, ContextMenuEvent event) {
        currentContextMenu = menu;
        menu.show(context.getGridChartPane(), event.getScreenX(), event.getScreenY());
        event.consume();
    }

    /**
     * 隐藏当前显示的右键菜单
     */
    private void hideCurrentContextMenu() {
        if (currentContextMenu != null && currentContextMenu.isShowing()) {
            currentContextMenu.hide();
        }
        currentContextMenu = null;
    }
}
