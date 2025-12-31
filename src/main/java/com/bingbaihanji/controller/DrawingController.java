package com.bingbaihanji.controller;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.handler.*;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.menu.GeometryContextMenu;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 图形绘制控制器（重构后的协调器版本）
 * <p>
 * 作为协调器，负责管理 Handler 并分发事件，不直接处理绘制逻辑
 *
 * @author bingbaihanji
 * @date 2025-12-21（原始版本）
 * @date 2025-12-31（重构版本）
 */
public class DrawingController {

    /**
     * 绘制上下文
     */
    private final DrawingContext context;

    /**
     * 交互型 Handler 列表（按优先级排序）
     */
    private final List<DrawingHandler> handlers = new ArrayList<>();
    /**
     * 从此控制器（主窗口或独立窗口）打开的所有子窗口
     */
    private final List<DetachedCanvasWindow> childWindows = new ArrayList<>();
    /**
     * 父窗口引用（如果在独立窗口中则不为null）
     */
    private DetachedCanvasWindow parentWindow;

    /**
     * 构造函数
     */
    public DrawingController(GridChartView gridChartPane) {
        this(gridChartPane, null);
    }

    /**
     * 构造函数（带父窗口引用）
     */
    public DrawingController(GridChartView gridChartPane, DetachedCanvasWindow parentWindow) {
        this.parentWindow = parentWindow;

        // 初始化 Context
        this.context = new DrawingContext(gridChartPane, new CommandHistory());

        // 注册所有 Handler
        registerHandlers();

        // 初始化鼠标事件监听
        initMouseHandlers();
    }

    /**
     * 注册所有 Handler（按优先级排序）
     */
    private void registerHandlers() {
        // 服务型 Handler（不处理鼠标事件）
        IntersectionHandler intersectionHandler = new IntersectionHandler();
        ConstraintHandler constraintHandler = new ConstraintHandler();
        SnappingHandler snappingHandler = new SnappingHandler();

        context.setIntersectionHandler(intersectionHandler);
        context.setConstraintHandler(constraintHandler);
        context.setSnappingHandler(snappingHandler);

        // 交互型 Handler（处理鼠标事件，按优先级排序）
        handlers.add(new SelectionHandler());         // 选择功能（在非绘制模式下生效，优先于拖动）
        handlers.add(new DragHandler());              // 拖动在非绘制模式下优先
        handlers.add(new FreehandHandler());          // 手绘优先级高
        handlers.add(new BasicShapeHandler());        // 基础图形
        handlers.add(new PolygonHandler());           // 多边形
        handlers.add(new ConstructionToolHandler());  // 作图工具
        handlers.add(new RotationHandler());          // 旋转
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

    // 事件分发方法（责任链模式）

    /**
     * 鼠标点击事件
     */
    public void handleMouseClicked(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                if (handler.handleMouseClicked(e, context)) {
                    break; // 已处理，停止传递
                }
            }
        }
    }

    /**
     * 鼠标移动事件（实时预览）
     */
    public void handleMouseMoved(MouseEvent e) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                if (handler.handleMouseMoved(e, context)) {
                    break; // 已处理，停止传递
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
                    break; // 已处理，停止传递
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
                    break; // 已处理，停止传递
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
                    break; // 已处理，停止传递
                }
            }
        }
    }

    /**
     * 绘制预览图形
     */
    public void paintPreview(GraphicsContext gc, WorldTransform transform) {
        for (DrawingHandler handler : handlers) {
            if (handler.canHandle(context.getDrawMode())) {
                handler.paintPreview(gc, transform, context);
            }
        }
    }

    // 对外接口（保持兼容性）

    /**
     * 设置绘制模式
     */
    public void setDrawMode(DrawMode mode) {
        // 重置所有 Handler 状态
        for (DrawingHandler handler : handlers) {
            handler.reset();
        }

        context.setDrawMode(mode);
        context.setState(DrawingState.IDLE);

        // 如果是旋转模式，进入选择图形状态
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
        // 保存当前所有对象，用于撤销
        List<WorldObject> objectsToClear = new ArrayList<>(context.getObjects());
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

    // 窗口管理方法（保留）

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

    // 右键菜单处理（保留）

    /**
     * 处理右键菜单事件
     */
    private void handleContextMenu(ContextMenuEvent event) {
        double worldX = context.getGridChartPane().screenToWorldX(event.getX());
        double worldY = context.getGridChartPane().screenToWorldY(event.getY());
        double scale = context.getTransform().getScale();
        double vertexTolerance = 10.0 / scale; // 顶点使用更大的容差
        double objectTolerance = 5.0 / scale;  // 对象使用较小的容差

        //   优先级1：检查是否点击了图形的顶点  
        List<WorldObject> objects = context.getObjects();
        for (WorldObject obj : objects) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                if (point.hitTest(worldX, worldY, vertexTolerance)) {
                    // 点击了顶点，显示顶点菜单
                    ContextMenu menu = GeometryContextMenu.createVertexMenu(
                            point, obj, context.getGridChartPane(), this
                    );
                    menu.show(context.getGridChartPane(), event.getScreenX(), event.getScreenY());
                    event.consume();
                    return;
                }
            }
        }

        //   优先级2：检查是否点击了独立的点对象  
        WorldObject clickedObject = null;
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj.hitTest(worldX, worldY, objectTolerance)) {
                clickedObject = obj;
                break;
            }
        }

        ContextMenu menu;
        if (clickedObject instanceof com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo point) {
            // 点的右键菜单
            menu = GeometryContextMenu.createPointMenu(point, context.getGridChartPane(), this);
        } else if (clickedObject != null) {
            // 其他图形的右键菜单
            menu = GeometryContextMenu.createShapeMenu(clickedObject, context.getGridChartPane(), this);
        } else {
            // 画布的右键菜单
            menu = GeometryContextMenu.createCanvasMenu(context.getGridChartPane(), this, parentWindow);
        }

        menu.show(context.getGridChartPane(), event.getScreenX(), event.getScreenY());
        event.consume();
    }
}
