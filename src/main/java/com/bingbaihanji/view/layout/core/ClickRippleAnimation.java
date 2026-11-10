package com.bingbaihanji.view.layout.core;

import javafx.scene.paint.Color;

/**
 * 点击涟漪动效的时间轴计算
 * <p>
 * 纯计算类，不依赖 JavaFX 窗口或 Canvas，便于单元测试。
 * <p>
 * 整个动效由单一归一化进度 p(0-1) 驱动，所有属性都是 p 的连续函数：
 * 相邻阶段之间既没有数值跳变，也没有速度跳变，因此观感一气呵成，不会出现
 * "停一下再猛冲"或"亮度回闪"这类分段拼接的痕迹。
 * <ol>
 *     <li>0-70ms：蓝色小点在点击处淡入并稳定显现，避免硬闪</li>
 *     <li>46-174ms：实心小点与圆环交叉淡入淡出，圆点"化开"为青色圆环（蓝→青）</li>
 *     <li>174-418ms：圆环向外扩散晕散并逐渐变细（青→淡青）</li>
 *     <li>418-487ms：到达最大半径后转为暖红</li>
 *     <li>487-580ms：红色圆环淡出消失</li>
 * </ol>
 *
 * @author bingbaihanji
 * @date 2026-11-01
 */
public final class ClickRippleAnimation {

    /**
     * 动效总时长（毫秒）
     */
    public static final double DURATION_MS = 580.0;

    /**
     * 最大扩散半径（屏幕像素）
     */
    public static final double MAX_RADIUS = 44.0;

    /**
     * 蓝色小点颜色（与应用交互主色一致：手柄/包围盒/吸附辅助线）
     */
    public static final Color BLUE = Color.rgb(0, 150, 255);

    /**
     * 青色圆环颜色
     */
    public static final Color CYAN = Color.rgb(0, 190, 214);

    /**
     * 淡青色（扩散晕散末端）
     */
    public static final Color LIGHT_CYAN = Color.rgb(150, 226, 240);

    /**
     * 暖红色圆环颜色（比纯红柔和，不打断界面配色）
     */
    public static final Color RED = Color.rgb(239, 83, 80);

    /**
     * 起始半径（像素）
     */
    private static final double MIN_RADIUS = 2.0;

    /**
     * 半径到达最大值时的进度（之后保持最大半径直到结束）
     */
    private static final double REACH_PROGRESS = 0.72;

    /**
     * 圆环线宽：随扩散由粗变细
     */
    private static final double START_LINE_WIDTH = 3.4;
    private static final double END_LINE_WIDTH = 2.2;

    /**
     * 不透明度包络：淡入 → 缓慢衰减 → 淡出，全程不回升
     */
    private static final double FADE_IN_END = 0.05;
    private static final double DECAY_END = 0.80;
    private static final double DECAY_FLOOR = 0.58;
    private static final double FADE_OUT_START = 0.80;

    /**
     * 形态交叉淡入淡出：实心点权重 1→0，圆环权重 0→1
     */
    private static final double FILL_FADE_START = 0.08;
    private static final double FILL_FADE_END = 0.30;
    private static final double RING_FADE_START = 0.10;
    private static final double RING_FADE_END = 0.30;

    /**
     * 颜色分段边界
     */
    private static final double BLUE_HOLD_UNTIL = 0.12;
    private static final double CYAN_AT = 0.30;
    private static final double LIGHT_CYAN_AT = 0.72;
    private static final double RED_AT = 0.84;

    /**
     * 光晕：由实心点权重与圆环权重线性混合，末段随整体淡出
     */
    private static final double GLOW_FILL_WEIGHT = 0.55;
    private static final double GLOW_RING_WEIGHT = 0.45;
    private static final double GLOW_MAX_ALPHA = 0.55;

    private ClickRippleAnimation() {
    }

    /**
     * 计算指定时刻的涟漪外观
     *
     * @param elapsedMs 距点击时刻的毫秒数（超出范围会被截断）
     * @return 该时刻的半径、线宽、各层不透明度与颜色
     */
    public static Frame computeFrame(double elapsedMs) {
        double t = clamp(elapsedMs, 0.0, DURATION_MS);
        double p = t / DURATION_MS;

        // 半径与线宽：全程同一条缓动曲线，末速度为 0，进入保持段时不会顿挫
        double radial = clamp(p / REACH_PROGRESS, 0.0, 1.0);
        double easedRadius = easeInOutCubic(radial);
        double radius = lerp(MIN_RADIUS, MAX_RADIUS, easedRadius);
        double lineWidth = lerp(START_LINE_WIDTH, END_LINE_WIDTH, easedRadius);

        // 不透明度：单峰包络，淡入后只减不增，杜绝"变红时回闪变亮"的 V 字
        double fadeIn = smoothStep(0.0, FADE_IN_END, p);
        double decay = lerp(1.0, DECAY_FLOOR, smoothStep(FADE_IN_END, DECAY_END, p));
        double fadeOut = 1.0 - smoothStep(FADE_OUT_START, 1.0, p);
        double alpha = fadeIn * decay * fadeOut;

        // 形态：实心点与圆环交叉淡入淡出，不存在布尔翻转造成的跳变
        double fillWeight = 1.0 - smoothStep(FILL_FADE_START, FILL_FADE_END, p);
        double ringWeight = smoothStep(RING_FADE_START, RING_FADE_END, p);
        double glowWeight = GLOW_FILL_WEIGHT * fillWeight + GLOW_RING_WEIGHT * ringWeight;

        return new Frame(
                radius,
                lineWidth,
                alpha * fillWeight,
                alpha * ringWeight,
                alpha * glowWeight * GLOW_MAX_ALPHA,
                colorAt(p)
        );
    }

    /**
     * 颜色沿"蓝 → 青 → 淡青 → 暖红"连续过渡，每段内部再用 smoothStep 缓动，
     * 段与段的交界处色值与色变速度都连续
     */
    private static Color colorAt(double p) {
        if (p <= CYAN_AT) {
            return BLUE.interpolate(CYAN, smoothStep(BLUE_HOLD_UNTIL, CYAN_AT, p));
        }
        if (p <= LIGHT_CYAN_AT) {
            return CYAN.interpolate(LIGHT_CYAN, smoothStep(CYAN_AT, LIGHT_CYAN_AT, p));
        }
        if (p <= RED_AT) {
            return LIGHT_CYAN.interpolate(RED, smoothStep(LIGHT_CYAN_AT, RED_AT, p));
        }
        return RED;
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    /**
     * 缓入缓出曲线：两端速度为 0，与应用缩放动画的缓动保持一致
     */
    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    /**
     * 平滑过渡曲线：在 [start, end] 内两端慢、中间快，区间外取边界值
     */
    private static double smoothStep(double start, double end, double value) {
        double t = clamp((value - start) / (end - start), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * 某一时刻的涟漪外观
     *
     * @param radius     圆环半径（屏幕像素）
     * @param lineWidth  圆环线宽（屏幕像素）
     * @param fillAlpha  实心小点不透明度 0.0-1.0
     * @param ringAlpha  圆环不透明度 0.0-1.0
     * @param glowAlpha  柔光不透明度 0.0-1.0
     * @param color      当前颜色
     */
    public record Frame(double radius, double lineWidth, double fillAlpha,
                        double ringAlpha, double glowAlpha, Color color) {
    }
}
