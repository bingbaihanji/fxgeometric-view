package com.bingbaihanji.view;

import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.view.layout.core.GridChartView;

/**
 * 独立窗口的控制器工厂
 * <p>
 * 由组合根提供实现, 避免视图包直接 new 控制器,
 * 使 view 与 controller 之间保持单向依赖。
 */
@FunctionalInterface
public interface DrawingWindowControllerFactory {

    /**
     * 为独立窗口创建控制器
     *
     * @param view    独立窗口的画布
     * @param window  独立窗口
     * @param history 共享的命令历史, 可为 null 表示使用独立历史
     * @return 控制器
     */
    DrawingWindowController create(GridChartView view, DetachedCanvasWindow window, CommandHistory history);
}
