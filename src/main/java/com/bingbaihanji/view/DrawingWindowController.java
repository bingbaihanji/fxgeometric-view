package com.bingbaihanji.view;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.geometry.WorldTransform;
import javafx.scene.canvas.GraphicsContext;

/**
 * 独立窗口所需的绘制控制能力
 * <p>
 * 由控制器实现。视图通过该接口驱动绘制, 不必依赖具体的控制器类型,
 * 从而让 view 包不反向依赖 controller 包。
 */
public interface DrawingWindowController {

    /**
     * 切换绘制模式
     *
     * @param mode 绘制模式
     */
    void setDrawMode(DrawMode mode);

    /**
     * 撤销
     */
    void undo();

    /**
     * 重做
     */
    void redo();

    /**
     * 清空画布
     */
    void clearAll();

    /**
     * 打开函数输入对话框
     */
    void showFunctionDialog();

    /**
     * 打开正多边形边数对话框
     */
    void showRegularPolygonDialog();

    /**
     * 绘制交互预览
     *
     * @param gc        图形上下文
     * @param transform 坐标变换
     */
    void paintPreview(GraphicsContext gc, WorldTransform transform);
}
