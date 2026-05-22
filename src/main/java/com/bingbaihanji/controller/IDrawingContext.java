package com.bingbaihanji.controller;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.constant.MoveMode;
import com.bingbaihanji.controller.handler.ConstraintHandler;
import com.bingbaihanji.controller.handler.IntersectionHandler;
import com.bingbaihanji.controller.handler.SnappingHandler;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.CursorManager;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 绘制上下文接口
 * <p>
 * 定义 Handler 与画布之间的通信契约,解耦 Handler 与具体实现
 *
 * @author bingbaihanji
 * @date 2026-05-22
 */
public interface IDrawingContext {

    // === 状态管理 ===

    DrawMode getDrawMode();

    void setDrawMode(DrawMode mode);

    DrawingState getState();

    void setState(DrawingState state);

    MoveMode getMoveMode();

    void setMoveMode(MoveMode moveMode);

    double getCurrentMouseX();

    void setCurrentMouseX(double currentMouseX);

    double getCurrentMouseY();

    void setCurrentMouseY(double currentMouseY);

    // === 画布操作 ===

    GridChartView getGridChartPane();

    WorldTransform getTransform();

    void addObject(WorldObject obj);

    void removeObject(WorldObject obj);

    List<WorldObject> getObjects();

    void redraw();

    void invalidateSnapCache();

    // === 命令历史管理 ===

    CommandHistory getCommandHistory();

    void executeCommand(CommandHistory.Command command);

    void addCommand(CommandHistory.Command command);

    // === 服务型 Handler 管理 ===

    IntersectionHandler getIntersectionHandler();

    void setIntersectionHandler(IntersectionHandler intersectionHandler);

    ConstraintHandler getConstraintHandler();

    void setConstraintHandler(ConstraintHandler constraintHandler);

    SnappingHandler getSnappingHandler();

    void setSnappingHandler(SnappingHandler snappingHandler);

    // === 管理器 ===

    SelectionManager getSelectionManager();

    void setSelectionManager(SelectionManager selectionManager);

    PreviewManager getPreviewManager();

    CursorManager getCursorManager();
}
