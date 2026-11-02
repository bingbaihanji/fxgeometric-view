package com.bingbaihanji.view.layout.core;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 点击落点涟漪动效
 * <p>
 * 在 {@link GridChartView} 上方叠加一层透明 Canvas，独立于主画布重绘。
 * 只在存在活动涟漪时运行 {@link AnimationTimer}，所有涟漪结束后自动停止。
 *
 * @author bingbaihanji
 * @date 2026-11-01
 */
public final class ClickRippleEffect {

    /**
     * 同时存在的最大涟漪数量，超出后移除最旧的涟漪
     */
    private static final int MAX_ACTIVE_RIPPLES = 24;

    /**
     * 晕环宽度倍率（相对核心线宽）
     */
    private static final double HALO_WIDTH_RATIO = 2.0;

    /**
     * 晕环不透明度倍率（相对核心不透明度）
     */
    private static final double HALO_OPACITY_RATIO = 0.25;

    /**
     * 透明覆盖层
     */
    private final Canvas overlay;

    /**
     * 活动涟漪列表
     */
    private final List<Ripple> ripples = new ArrayList<>();

    /**
     * 动画循环
     */
    private final AnimationTimer timer;

    /**
     * 动画循环是否正在运行
     */
    private boolean running;

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
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

        Iterator<Ripple> iterator = ripples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            double elapsedMs = (now - ripple.startNanos) / 1_000_000.0;

            if (elapsedMs >= ClickRippleAnimation.DURATION_MS) {
                iterator.remove();
                continue;
            }

            drawFrame(gc, ripple, ClickRippleAnimation.computeFrame(elapsedMs));
        }

        if (ripples.isEmpty()) {
            running = false;
            timer.stop();
        }
    }

    /**
     * 绘制单帧：外圈晕环 + 核心实心点/圆环（柔和光晕）
     *
     * @param gc     覆盖层画笔
     * @param ripple 涟漪位置与起始时间
     * @param frame  该时刻的外观
     */
    private void drawFrame(GraphicsContext gc, Ripple ripple, ClickRippleAnimation.Frame frame) {
        double radius = frame.radius();
        double x = ripple.x - radius;
        double y = ripple.y - radius;
        double diameter = radius * 2;

        // 晕环：更宽更淡的同色描边，形成晕散光晕
        gc.setGlobalAlpha(frame.opacity() * HALO_OPACITY_RATIO);
        gc.setStroke(frame.color());
        gc.setLineWidth(frame.lineWidth() * HALO_WIDTH_RATIO);
        gc.strokeOval(x, y, diameter, diameter);

        // 核心层：实心点或圆环
        gc.setGlobalAlpha(frame.opacity());
        if (frame.filled()) {
            gc.setFill(frame.color());
            gc.fillOval(x, y, diameter, diameter);
        } else {
            gc.setStroke(frame.color());
            gc.setLineWidth(frame.lineWidth());
            gc.strokeOval(x, y, diameter, diameter);
        }
    }

    /**
     * 停止动画循环并清空所有涟漪（视图销毁时调用）
     */
    public void stop() {
        timer.stop();
        running = false;
        ripples.clear();
    }

    /**
     * 单个涟漪的起始位置和起始时间
     */
    private record Ripple(double x, double y, long startNanos) {
    }
}
