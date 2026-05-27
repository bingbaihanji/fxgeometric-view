package com.bingbaihanji.view.layout.core;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * 背景层离屏缓存
 * <p>
 * 将网格和坐标轴的绘制结果缓存到离屏 WritableImage 中，
 * 仅在坐标变换或设置变更时重新绘制，避免每帧重复渲染。
 * 参考 GeoGebra 的 bgImage 离屏缓冲机制。
 *
 * @author bingbaihanji
 */
public class BackgroundBuffer {

    /** 离屏画布，尺寸跟随视口 */
    private Canvas offscreenCanvas;

    /** 脏标记：true 表示缓存失效，需要重绘 */
    private boolean dirty = true;

    /** 缓存的快照图像，避免每次 copyTo 时重新分配 */
    private WritableImage cachedImage;

    /** 当前缓存对应的视口宽度 */
    private double cachedWidth;

    /** 当前缓存对应的视口高度 */
    private double cachedHeight;

    /**
     * 标脏，下次 beginDraw 时重建画布
     */
    public void invalidate() {
        dirty = true;
        cachedImage = null;  // 释放旧快照
    }

    /**
     * 判断缓存是否有效（非脏且尺寸匹配）
     *
     * @param viewWidth  当前视口宽度
     * @param viewHeight 当前视口高度
     * @return true 如果缓存可直接复用
     */
    public boolean isValid(double viewWidth, double viewHeight) {
        return !dirty && offscreenCanvas != null
                && Math.abs(cachedWidth - viewWidth) < 0.5
                && Math.abs(cachedHeight - viewHeight) < 0.5;
    }

    /**
     * 开始离屏绘制，返回画布上下文
     * <p>
     * 若缓存失效或尺寸变化则重建离屏画布。
     *
     * @param width  视口宽度
     * @param height 视口高度
     * @return 离屏画布的 GraphicsContext
     */
    public GraphicsContext beginDraw(double width, double height) {
        if (offscreenCanvas == null
                || Math.abs(cachedWidth - width) > 0.5
                || Math.abs(cachedHeight - height) > 0.5) {
            offscreenCanvas = new Canvas(width, height);
        }
        cachedWidth = width;
        cachedHeight = height;
        GraphicsContext gc = offscreenCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        return gc;
    }

    /**
     * 完成离屏绘制，清除脏标记
     */
    public void endDraw() {
        if (offscreenCanvas != null) {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            cachedImage = offscreenCanvas.snapshot(params, null);
        }
        dirty = false;
    }

    /**
     * 将缓存内容绘制到目标画布上
     *
     * @param targetGc 目标画布的 GraphicsContext
     */
    public void copyTo(GraphicsContext targetGc) {
        if (cachedImage != null) {
            targetGc.drawImage(cachedImage, 0, 0);
        }
    }
}
