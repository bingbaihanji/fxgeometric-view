package com.bingbaihanji.controller;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.handler.ConstraintHandler;
import com.bingbaihanji.controller.handler.IntersectionHandler;
import com.bingbaihanji.controller.handler.SnappingHandler;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 绘制上下文
 * <p>
 * 作为 Handler 之间的通信桥梁，提供共享的状态和服务
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class DrawingContext {

    /**
     * 坐标系面板（画布）
     */
    private final GridChartView gridChartPane;

    /**
     * 命令历史管理器
     */
    private final CommandHistory commandHistory;

    /**
     * 当前绘制模式
     */
    private DrawMode drawMode = DrawMode.NONE;

    /**
     * 当前绘制状态
     */
    private DrawingState state = DrawingState.IDLE;

    /**
     * 当前鼠标位置（世界坐标）
     */
    private double currentMouseX;
    private double currentMouseY;

    /**
     * 交点计算处理器
     */
    private IntersectionHandler intersectionHandler;

    /**
     * 约束管理处理器
     */
    private ConstraintHandler constraintHandler;

    /**
     * 磁性吸附处理器
     */
    private SnappingHandler snappingHandler;

    /**
     * 选择管理器
     */
    private SelectionManager selectionManager;

    /**
     * 构造函数
     *
     * @param gridChartPane  坐标系面板
     * @param commandHistory 命令历史管理器
     */
    public DrawingContext(GridChartView gridChartPane, CommandHistory commandHistory) {
        this.gridChartPane = gridChartPane;
        this.commandHistory = commandHistory;
        this.selectionManager = new SelectionManager();
    }

    // 状态管理

    /**
     * 获取当前绘制模式
     */
    public DrawMode getDrawMode() {
        return drawMode;
    }

    /**
     * 设置当前绘制模式
     */
    public void setDrawMode(DrawMode mode) {
        this.drawMode = mode;
    }

    /**
     * 获取当前绘制状态
     */
    public DrawingState getState() {
        return state;
    }

    /**
     * 设置当前绘制状态
     */
    public void setState(DrawingState state) {
        this.state = state;
    }

    /**
     * 获取当前鼠标 X 坐标（世界坐标）
     */
    public double getCurrentMouseX() {
        return currentMouseX;
    }

    /**
     * 设置当前鼠标 X 坐标（世界坐标）
     */
    public void setCurrentMouseX(double currentMouseX) {
        this.currentMouseX = currentMouseX;
    }

    /**
     * 获取当前鼠标 Y 坐标（世界坐标）
     */
    public double getCurrentMouseY() {
        return currentMouseY;
    }

    /**
     * 设置当前鼠标 Y 坐标（世界坐标）
     */
    public void setCurrentMouseY(double currentMouseY) {
        this.currentMouseY = currentMouseY;
    }

    // 画布操作

    /**
     * 获取坐标系面板
     */
    public GridChartView getGridChartPane() {
        return gridChartPane;
    }

    /**
     * 获取世界坐标变换
     */
    public WorldTransform getTransform() {
        return gridChartPane.getTransform();
    }

    /**
     * 添加几何对象到画布
     *
     * @param obj 几何对象
     */
    public void addObject(WorldObject obj) {
        gridChartPane.addObject(obj);
    }

    /**
     * 从画布移除几何对象
     *
     * @param obj 几何对象
     */
    public void removeObject(WorldObject obj) {
        gridChartPane.removeObject(obj);
    }

    /**
     * 获取画布上的所有几何对象
     */
    public List<WorldObject> getObjects() {
        return gridChartPane.getObjects();
    }

    /**
     * 重绘画布
     */
    public void redraw() {
        gridChartPane.redraw();
    }

    // 命令历史管理

    /**
     * 获取命令历史管理器
     */
    public CommandHistory getCommandHistory() {
        return commandHistory;
    }

    /**
     * 执行命令并记录到历史
     *
     * @param command 要执行的命令
     */
    public void executeCommand(CommandHistory.Command command) {
        commandHistory.execute(command);
    }

    /**
     * 添加命令到历史（不执行，用于已完成的操作）
     *
     * @param command 要记录的命令
     */
    public void addCommand(CommandHistory.Command command) {
        commandHistory.addCommand(command);
    }

    // 服务型 Handler 管理

    /**
     * 获取交点计算处理器
     */
    public IntersectionHandler getIntersectionHandler() {
        return intersectionHandler;
    }

    /**
     * 设置交点计算处理器
     */
    public void setIntersectionHandler(IntersectionHandler intersectionHandler) {
        this.intersectionHandler = intersectionHandler;
    }

    /**
     * 获取约束管理处理器
     */
    public ConstraintHandler getConstraintHandler() {
        return constraintHandler;
    }

    /**
     * 设置约束管理处理器
     */
    public void setConstraintHandler(ConstraintHandler constraintHandler) {
        this.constraintHandler = constraintHandler;
    }

    /**
     * 获取磁性吸附处理器
     */
    public SnappingHandler getSnappingHandler() {
        return snappingHandler;
    }

    /**
     * 设置磁性吸附处理器
     */
    public void setSnappingHandler(SnappingHandler snappingHandler) {
        this.snappingHandler = snappingHandler;
    }

    /**
     * 获取选择管理器
     */
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    /**
     * 设置选择管理器
     */
    public void setSelectionManager(SelectionManager selectionManager) {
        this.selectionManager = selectionManager;
    }
}
