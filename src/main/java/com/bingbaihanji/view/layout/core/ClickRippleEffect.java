package com.bingbaihanji.view.layout.core;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 点击落点涟漪动效
 * <p>
 * 在 {@link GridChartView} 上方叠加一层透明 Canvas，独立于主画布重绘。
 * 只在存在活动涟漪时运行 {@link AnimationTimer}，所有涟漪结束后自动停止。
 * <p>
 * 为避免与主视图重绘争抢同一帧，每帧只清除"上一帧区域 ∪ 本帧区域"的包围盒，
 * 而不是整块画布，把每帧的填充量从整屏降到涟漪附近的一小块。
 *
 * @author bingbaihanji
 * @date 2026-11-01
 */
public final class ClickRippleEffect {

    /**
     * 同时存在的最大涟漪数量，超出后移除最旧的涟漪
     */
    private static final int MAX_ACTIVE_RIPPLES = 8;

    /**
     * 柔光半径相对圆环半径的倍率
     */
    private static final double GLOW_RADIUS_RATIO = 1.55;

    /**
     * 柔光半径的额外留白（像素）
     */
    private static final double GLOW_PADDING = 2.0;

    /**
     * 低于该不透明度的图层直接跳过绘制
     */
    private static final double ALPHA_EPSILON = 0.001;

    /**
     * 透明覆盖层
     */
    private final Canvas overlay;

    /**
     * 活动涟漪列表
     */
    private final List<Ripple> ripples = new ArrayList<>();

    /**
     * 本帧待绘制的涟漪（复用列表，避免每帧分配）
     */
    private final List<Painted> painted = new ArrayList<>();

    /**
     * 动画循环
     */
    private final AnimationTimer timer;

    /**
     * 动画循环是否正在运行
     */
    private boolean running;

    /**
     * 上一帧实际覆盖的区域，供脏矩形清屏使用
     */
    private double dirtyX;
    private double dirtyY;
    private double dirtyWidth;
    private double dirtyHeight;

    /**
     * 上一帧的画布尺寸，尺寸变化时需要整层清屏
     */
    private double lastCanvasWidth;
    private double lastCanvasHeight;

    /**
     * 构造函数
     *
     * @param view 宿主视图，覆盖层尺寸与其保持一致
     */
    public ClickRippleEffect(GridChartView view) {
        this.overlay = new Canvas();
        this.overlay.widthProperty().bind(view.widthProperty());
        this.overlay.heightProperty().bind(view.heightProperty());
        this.overlay.setMouseTransparent(true);
        // 尺寸已由绑定决定，退出 Pane 的布局流程，省掉无谓的布局开销
        this.overlay.setManaged(false);

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                render(now);
            }
        };
    }

    /**
     * 获取覆盖层 Canvas，用于添加到宿主视图
     */
    public Canvas getCanvas() {
        return overlay;
    }

    /**
     * 在指定屏幕坐标处生成一个涟漪
     *
     * @param screenX 屏幕 X 坐标（相对画布）
     * @param screenY 屏幕 Y 坐标（相对画布）
     */
    public void spawn(double screenX, double screenY) {
        if (ripples.size() >= MAX_ACTIVE_RIPPLES) {
            ripples.remove(0);
        }

        ripples.add(new Ripple(screenX, screenY, System.nanoTime()));

        if (!running) {
            running = true;
            timer.start();
        }
    }

    /**
     * 逐帧绘制所有活动涟漪
     *
     * @param now 当前帧时间（纳秒）
     */
    private void render(long now) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        clearIfResized(gc);

        // 第一遍：计算本帧外观与总包围盒，同时移除已结束的涟漪
        painted.clear();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        Iterator<Ripple> iterator = ripples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            double elapsedMs = (now - ripple.startNanos) / 1_000_000.0;

            if (elapsedMs >= ClickRippleAnimation.DURATION_MS) {
                iterator.remove();
                continue;
            }

            ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(elapsedMs);
            painted.add(new Painted(ripple.x(), ripple.y(), frame));

            double extent = frame.radius() * GLOW_RADIUS_RATIO + GLOW_PADDING + frame.lineWidth();
            minX = Math.min(minX, ripple.x() - extent);
            minY = Math.min(minY, ripple.y() - extent);
            maxX = Math.max(maxX, ripple.x() + extent);
            maxY = Math.max(maxY, ripple.y() + extent);
        }

        // 第二遍：只清除上一帧与本帧的并集，再重新绘制
        clearDirtyArea(gc, !painted.isEmpty(), minX, minY, maxX, maxY);

        for (Painted item : painted) {
            drawFrame(gc, item);
        }

        if (painted.isEmpty()) {
            resetDirtyArea();
            running = false;
            timer.stop();
        } else {
            dirtyX = minX;
            dirtyY = minY;
            dirtyWidth = maxX - minX;
            dirtyHeight = maxY - minY;
        }
    }

    /**
     * 画布尺寸变化时整层清屏，避免旧位置残留
     */
    private void clearIfResized(GraphicsContext gc) {
        double width = overlay.getWidth();
        double height = overlay.getHeight();
        if (width == lastCanvasWidth && height == lastCanvasHeight) {
            return;
        }
        gc.clearRect(0, 0, Math.max(width, lastCanvasWidth), Math.max(height, lastCanvasHeight));
        lastCanvasWidth = width;
        lastCanvasHeight = height;
        resetDirtyArea();
    }

    /**
     * 清除上一帧区域与本帧区域的并集
     */
    private void clearDirtyArea(GraphicsContext gc, boolean hasCurrent,
                                double minX, double minY, double maxX, double maxY) {
        boolean hasPrevious = dirtyWidth > 0 && dirtyHeight > 0;
        if (!hasCurrent && !hasPrevious) {
            return;
        }

        double clearMinX = Double.POSITIVE_INFINITY;
        double clearMinY = Double.POSITIVE_INFINITY;
        double clearMaxX = Double.NEGATIVE_INFINITY;
        double clearMaxY = Double.NEGATIVE_INFINITY;

        if (hasCurrent) {
            clearMinX = Math.min(clearMinX, minX);
            clearMinY = Math.min(clearMinY, minY);
            clearMaxX = Math.max(clearMaxX, maxX);
            clearMaxY = Math.max(clearMaxY, maxY);
        }
        if (hasPrevious) {
            clearMinX = Math.min(clearMinX, dirtyX);
            clearMinY = Math.min(clearMinY, dirtyY);
            clearMaxX = Math.max(clearMaxX, dirtyX + dirtyWidth);
            clearMaxY = Math.max(clearMaxY, dirtyY + dirtyHeight);
        }

        gc.clearRect(clearMinX, clearMinY, clearMaxX - clearMinX, clearMaxY - clearMinY);
    }

    private void resetDirtyArea() {
        dirtyX = 0;
        dirtyY = 0;
        dirtyWidth = 0;
        dirtyHeight = 0;
    }

    /**
     * 绘制单帧：柔光 → 实心小点 → 圆环，三层均以不透明度参与交叉淡入淡出
     *
     * @param gc    覆盖层画笔
     * @param item  涟漪位置与该时刻的外观
     */
    private void drawFrame(GraphicsContext gc, Painted item) {
        ClickRippleAnimation.Frame frame = item.frame();
        double radius = frame.radius();
        double diameter = radius * 2;
        double left = item.x() - radius;
        double top = item.y() - radius;

        // 柔光：径向渐变一次填充，外缘自然衰减，没有硬边
        if (frame.glowAlpha() > ALPHA_EPSILON) {
            double glowRadius = radius * GLOW_RADIUS_RATIO + GLOW_PADDING;
            gc.setGlobalAlpha(frame.glowAlpha());
            gc.setFill(glowPaint(frame.color()));
            gc.fillOval(item.x() - glowRadius, item.y() - glowRadius, glowRadius * 2, glowRadius * 2);
        }

        // 实心小点：随圆环出现逐步淡出
        if (frame.fillAlpha() > ALPHA_EPSILON) {
            gc.setGlobalAlpha(frame.fillAlpha());
            gc.setFill(frame.color());
            gc.fillOval(left, top, diameter, diameter);
        }

        // 圆环
        if (frame.ringAlpha() > ALPHA_EPSILON) {
            gc.setGlobalAlpha(frame.ringAlpha());
            gc.setStroke(frame.color());
            gc.setLineWidth(frame.lineWidth());
            gc.strokeOval(left, top, diameter, diameter);
        }
    }

    /**
     * 柔光画笔：中心 55% → 45% 处 18% → 边缘全透明的径向渐变
     */
    private static Paint glowPaint(Color color) {
        return new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, color.deriveColor(0, 1, 1, 0.55)),
                new Stop(0.45, color.deriveColor(0, 1, 1, 0.18)),
                new Stop(1.0, Color.TRANSPARENT)
        );
    }

    /**
     * 停止动画循环并清空所有涟漪（视图销毁时调用）
     */
    public void stop() {
        timer.stop();
        running = false;
        ripples.clear();
        painted.clear();
        double width = overlay.getWidth();
        double height = overlay.getHeight();
        if (width > 0 && height > 0) {
            overlay.getGraphicsContext2D().clearRect(0, 0, width, height);
        }
        resetDirtyArea();
    }

    /**
     * 单个涟漪的起始位置和起始时间
     */
    private record Ripple(double x, double y, long startNanos) {
    }

    /**
     * 某一帧待绘制的涟漪位置与外观
     */
    private record Painted(double x, double y, ClickRippleAnimation.Frame frame) {
    }
}
