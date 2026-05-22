package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

/**
 * 绘制处理器抽象基类
 * <p>
 * 提供了 DrawingHandler 接口的默认实现,子类只需重写需要的方法
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public abstract class AbstractDrawingHandler implements DrawingHandler {

    @Override
    public boolean handleMouseClicked(MouseEvent e, IDrawingContext context) {
        return false;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, IDrawingContext context) {
        return false;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e, IDrawingContext context) {
        return false;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e, IDrawingContext context) {
        return false;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, IDrawingContext context) {
        return false;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, IDrawingContext context) {
        // 默认不绘制预览
    }

    @Override
    public void reset() {
        // 默认不做任何事
    }
}
