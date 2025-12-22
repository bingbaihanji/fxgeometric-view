package com.binbaihanji.view.layout.draw.geometry;

import com.binbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;

public interface WorldPainter {

    /**
     * 在当前世界坐标系下绘制内容
     *
     * @param gc        画布上下文
     * @param transform 世界坐标变换
     * @param width     视图宽度（像素）
     * @param height    视图高度（像素）
     */
    void paint(
            GraphicsContext gc,
            WorldTransform transform,
            double width,
            double height
    );
}
