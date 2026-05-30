package com.bingbaihanji.view.layout.core;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * 缩放动画
 * <p>
 * 在 15 帧内（约 400ms）平滑插值 xZero/yZero/xscale/yscale，
 * 使用 easeInOutCubic 缓动函数。
 * 参考 GeoGebra 的 CoordSystemAnimation。
 *
 * @author bingbaihanji
 */
public class ZoomAnimation {

    /**
     * 动画帧数
     */
    private static final int FRAMES = 15;

    /**
     * 总时长
     */
    private static final Duration DURATION = Duration.millis(400);

    private Timeline timeline;

    /**
     * 启动从 from 到 to 的平滑缩放动画
     *
     * @param from    起始变换状态
     * @param to      目标变换状态（Snapshot 记录 xZero/yZero/xscale/yscale）
     * @param onFrame 每帧回调（接收插值后的 WorldTransform）
     * @param onDone  动画完成回调（可为 null）
     */
    public void animate(TransformSnapshot from, TransformSnapshot to,
                        Consumer<WorldTransform> onFrame, Runnable onDone) {
        if (timeline != null && timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.stop();
        }

        timeline = new Timeline();
        WorldTransform interp = new WorldTransform();

        for (int i = 0; i <= FRAMES; i++) {
            final int frame = i;
            double t = easeInOutCubic((double) frame / FRAMES);

            KeyFrame kf = new KeyFrame(DURATION.multiply(t), e -> {
                double xz = lerp(from.xZero(), to.xZero(), t);
                double yz = lerp(from.yZero(), to.yZero(), t);
                double xs = lerp(from.xScale(), to.xScale(), t);
                double ys = lerp(from.yScale(), to.yScale(), t);

                interp.setOffset(xz, yz);
                interp.setScaleX(xs);
                interp.setScaleY(ys);
                onFrame.accept(interp);
            });
            timeline.getKeyFrames().add(kf);
        }

        timeline.setOnFinished(e -> {
            if (onDone != null) {
                onDone.run();
            }
        });
        timeline.play();
    }

    /**
     * 停止当前动画
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    /**
     * 线性插值
     */
    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * easeInOutCubic 缓动函数：起止慢，中间快
     */
    private double easeInOutCubic(double t) {
        return t < 0.5
                ? 4 * t * t * t
                : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * 变换快照 —— 记录某一时刻的 xZero/yZero/xscale/yscale
     *
     * @param xZero  X 方向偏移（像素）
     * @param yZero  Y 方向偏移（像素）
     * @param xScale X 轴缩放比例
     * @param yScale Y 轴缩放比例
     */
    public record TransformSnapshot(double xZero, double yZero, double xScale, double yScale) {

        /**
         * 从 WorldTransform 创建快照
         */
        public static TransformSnapshot from(WorldTransform t) {
            return new TransformSnapshot(t.getOffsetX(), t.getOffsetY(),
                    t.getScaleX(), t.getScaleY());
        }

        /**
         * 将快照应用到 WorldTransform
         */
        public void applyTo(WorldTransform t) {
            t.setOffset(xZero, yZero);
            t.setScaleX(xScale);
            t.setScaleY(yScale);
        }
    }
}
