package com.bingbaihanji.view.layout.core;

import javafx.scene.paint.Color;

/**
 * 点击涟漪动效的时间轴计算
 * <p>
 * 纯计算类，不依赖 JavaFX 窗口或 Canvas，便于单元测试。
 * 时间轴总时长 {@value #DURATION_MS} 毫秒：
 * <ol>
 *     <li>0-80ms：蓝色实心小点弹出（r=2→5）</li>
 *     <li>80-170ms：蓝点绽开为青色圆环（蓝→青）</li>
 *     <li>170-470ms：圆环向外扩散晕散（青→淡青），逐渐变淡</li>
 *     <li>470-540ms：到达最大半径变为红色圆环（淡青→红）</li>
 *     <li>540-660ms：红色圆环淡出消失</li>
 * </ol>
 *
 * @author bingbaihanji
 * @date 2026-11-01
 */
public final class ClickRippleAnimation {

    /**
     * 动效总时长（毫秒）
     */
    public static final double DURATION_MS = 660.0;

    /**
     * 最大扩散半径（屏幕像素）
     */
    public static final double MAX_RADIUS = 44.0;

    /**
     * 蓝色小点颜色
     */
    public static final Color BLUE = Color.rgb(65, 147, 249);

    /**
     * 青色圆环颜色
     */
    public static final Color CYAN = Color.rgb(0, 188, 212);

    /**
     * 淡青色（扩散晕散末端）
     */
    public static final Color LIGHT_CYAN = Color.rgb(144, 224, 239);

    /**
     * 红色圆环颜色
     */
    public static final Color RED = Color.rgb(255, 0, 0);

    /**
     * 蓝色小点阶段结束时间
     */
    private static final double DOT_END_MS = 80.0;

    /**
     * 蓝点绽开成环阶段结束时间
     */
    private static final double MORPH_END_MS = 170.0;

    /**
     * 扩散晕散阶段结束时间
     */
    private static final double EXPAND_END_MS = 470.0;

    /**
     * 变红阶段结束时间
     */
    private static final double FLASH_END_MS = 540.0;

    private static final double DOT_RADIUS = 2.0;
    private static final double DOT_END_RADIUS = 5.0;
    private static final double DOT_LINE_WIDTH = 5.0;
    private static final double MORPH_RADIUS = 11.0;
    private static final double RING_LINE_WIDTH = 3.0;

    private ClickRippleAnimation() {
    }

    /**
     * 计算指定时刻的涟漪外观
     *
     * @param elapsedMs 距点击时刻的毫秒数（超出范围会被截断）
     * @return 该时刻的半径、线宽、不透明度和颜色
     */
    public static Frame computeFrame(double elapsedMs) {
        double t = clamp(elapsedMs, 0.0, DURATION_MS);

        // 1. 蓝色实心小点弹出
        if (t <= DOT_END_MS) {
            double p = progress(t, 0.0, DOT_END_MS);
            return new Frame(
                    lerp(DOT_RADIUS, DOT_END_RADIUS, smoothStep(p)),
                    DOT_LINE_WIDTH,
                    1.0,
                    BLUE,
                    true
            );
        }

        // 2. 蓝点绽开为青色圆环（中点由实心转为描边）
        if (t <= MORPH_END_MS) {
            double p = progress(t, DOT_END_MS, MORPH_END_MS);
            double eased = smoothStep(p);
            return new Frame(
                    lerp(DOT_END_RADIUS, MORPH_RADIUS, eased),
                    lerp(DOT_LINE_WIDTH, RING_LINE_WIDTH, eased),
                    lerp(1.0, 0.95, p),
                    BLUE.interpolate(CYAN, p),
                    p < 0.5
            );
        }

        // 3. 向外扩散晕散并变淡（青→淡青）
        if (t <= EXPAND_END_MS) {
            double p = progress(t, MORPH_END_MS, EXPAND_END_MS);
            return new Frame(
                    lerp(MORPH_RADIUS, MAX_RADIUS, easeOutQuad(p)),
                    RING_LINE_WIDTH,
                    lerp(0.95, 0.45, p),
                    CYAN.interpolate(LIGHT_CYAN, p),
                    false
            );
        }

        // 4. 到达最大半径，淡青变为红色圆环（回闪变亮）
        if (t <= FLASH_END_MS) {
            double p = progress(t, EXPAND_END_MS, FLASH_END_MS);
            return new Frame(
                    MAX_RADIUS,
                    RING_LINE_WIDTH,
                    lerp(0.45, 1.0, p),
                    LIGHT_CYAN.interpolate(RED, p),
                    false
            );
        }

        // 5. 红色圆环淡出消失
        double p = progress(t, FLASH_END_MS, DURATION_MS);
        return new Frame(
                MAX_RADIUS,
                RING_LINE_WIDTH,
                lerp(1.0, 0.0, p),
                RED,
                false
        );
    }

    private static double progress(double value, double start, double end) {
        return (value - start) / (end - start);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    /**
     * 缓出曲线：先快后慢（二次，比三次缓出更平缓，扩散过程更易看清）
     */
    private static double easeOutQuad(double t) {
        double inverse = 1.0 - t;
        return 1.0 - inverse * inverse;
    }

    /**
     * 平滑过渡曲线：两端慢、中间快
     */
    private static double smoothStep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * 某一时刻的涟漪外观
     *
     * @param radius    圆环半径（屏幕像素）
     * @param lineWidth 线宽（屏幕像素）
     * @param opacity   不透明度 0.0-1.0
     * @param color     颜色
     * @param filled    true 渲染为实心圆点，false 渲染为圆环描边
     */
    public record Frame(double radius, double lineWidth, double opacity, Color color, boolean filled) {
    }
}
