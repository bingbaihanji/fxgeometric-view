package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

/**
 * 绘制处理器接口
 * <p>
 * 定义了处理绘制相关事件的标准接口，使用责任链模式处理鼠标事件
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public interface DrawingHandler {

    /**
     * 处理鼠标点击事件
     *
     * @param e       鼠标事件
     * @param context 绘制上下文
     * @return true 表示已处理该事件，false 表示未处理，继续传递给下一个 Handler
     */
    boolean handleMouseClicked(MouseEvent e, DrawingContext context);

    /**
     * 处理鼠标移动事件
     *
     * @param e       鼠标事件
     * @param context 绘制上下文
     * @return true 表示已处理该事件，false 表示未处理
     */
    boolean handleMouseMoved(MouseEvent e, DrawingContext context);

    /**
     * 处理鼠标按下事件
     *
     * @param e       鼠标事件
     * @param context 绘制上下文
     * @return true 表示已处理该事件，false 表示未处理
     */
    boolean handleMousePressed(MouseEvent e, DrawingContext context);

    /**
     * 处理鼠标拖拽事件
     *
     * @param e       鼠标事件
     * @param context 绘制上下文
     * @return true 表示已处理该事件，false 表示未处理
     */
    boolean handleMouseDragged(MouseEvent e, DrawingContext context);

    /**
     * 处理鼠标释放事件
     *
     * @param e       鼠标事件
     * @param context 绘制上下文
     * @return true 表示已处理该事件，false 表示未处理
     */
    boolean handleMouseReleased(MouseEvent e, DrawingContext context);

    /**
     * 绘制预览效果
     *
     * @param gc        图形上下文
     * @param transform 世界坐标变换
     * @param context   绘制上下文
     */
    void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context);

    /**
     * 判断此 Handler 是否可以处理当前绘制模式
     *
     * @param mode 绘制模式
     * @return true 表示可以处理，false 表示不能处理
     */
    boolean canHandle(DrawMode mode);

    /**
     * 重置 Handler 状态
     * <p>
     * 在切换绘制模式时调用，清除所有临时状态
     */
    void reset();
}
