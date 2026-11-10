package com.bingbaihanji.view.layout.core;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 点击涟漪时间轴回归测试
 * <p>
 * 规格：蓝点淡入 → 圆点化开成环(蓝→青) → 扩散晕散(青→淡青) → 到达最大半径后转暖红 → 淡出消失。
 * 所有属性都由单一进度曲线驱动，重点保证整条时间轴上没有数值跳变与速度跳变。
 */
class ClickRippleAnimationTest {

    private static final double EPS = 1e-6;

    /** 蓝色小点 */
    private static final Color BLUE = Color.rgb(0, 150, 255);
    /** 青色圆环 */
    private static final Color CYAN = Color.rgb(0, 190, 214);
    /** 淡青色（晕散末端） */
    private static final Color LIGHT_CYAN = Color.rgb(150, 226, 240);
    /** 暖红色圆环 */
    private static final Color RED = Color.rgb(239, 83, 80);

    // ---- 阶段特征 ----

    @Test
    void seedStartsTransparentAndBlue() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(0.0);

        assertEquals(2.0, frame.radius(), EPS);
        assertEquals(0.0, frame.fillAlpha(), EPS, "起始帧从透明浮现，避免硬闪");
        assertEquals(0.0, frame.ringAlpha(), EPS);
        assertEquals(0.0, frame.glowAlpha(), EPS);
        assertColor(BLUE, frame.color());
    }

    @Test
    void dotPhaseIsAnOpaqueBlueDot() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(35.0);

        assertEquals(2.099, frame.radius(), 0.01, "淡入结束时仍是很小的圆点");
        assertEquals(3.397, frame.lineWidth(), 0.01);
        assertEquals(1.0, frame.fillAlpha(), 0.01, "圆点已完全显现");
        assertEquals(0.0, frame.ringAlpha(), EPS, "此时还没有圆环");
        assertTrue(frame.glowAlpha() > 0.0, "柔光已随圆点出现");
        assertColor(BLUE, frame.color());
    }

    @Test
    void dotCrossFadesIntoRingInsteadOfFlipping() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(116.0);

        assertTrue(frame.fillAlpha() > 0.05, "实心点尚未消失");
        assertTrue(frame.ringAlpha() > 0.05, "圆环已经出现");
    }

    @Test
    void ringIsFullyFormedOnceDotHasFaded() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(210.0);

        assertEquals(0.0, frame.fillAlpha(), EPS, "实心点已完全让位于圆环");
        assertEquals(0.842, frame.ringAlpha(), 0.01);
        assertEquals(23.37, frame.radius(), 0.05);
    }

    @Test
    void radiusReachesMaximumThenHolds() {
        assertEquals(44.0, ClickRippleAnimation.computeFrame(418.0).radius(), EPS);
        assertEquals(44.0, ClickRippleAnimation.computeFrame(490.0).radius(), EPS);
        assertEquals(44.0, ClickRippleAnimation.computeFrame(580.0).radius(), EPS);
    }

    @Test
    void lineWidthThinsAsRingSpreads() {
        assertTrue(ClickRippleAnimation.computeFrame(210.0).lineWidth()
                < ClickRippleAnimation.computeFrame(35.0).lineWidth(), "扩散过程中线宽变细");
        assertEquals(2.2, ClickRippleAnimation.computeFrame(418.0).lineWidth(), EPS);
    }

    // ---- 颜色 ----

    @Test
    void colorWalksBlueCyanLightCyanThenRed() {
        assertColor(BLUE, ClickRippleAnimation.computeFrame(0.0).color());
        assertColor(CYAN, ClickRippleAnimation.computeFrame(174.0).color(), "", 0.01);
        assertColor(LIGHT_CYAN, ClickRippleAnimation.computeFrame(418.0).color(), "", 0.01);
        assertColor(RED, ClickRippleAnimation.computeFrame(490.0).color());
        assertColor(RED, ClickRippleAnimation.computeFrame(580.0).color());
    }

    @Test
    void redAppearsOnlyAfterTheRingHasReachedFullSize() {
        Color justBefore = ClickRippleAnimation.computeFrame(417.0).color();
        Color justAfter = ClickRippleAnimation.computeFrame(490.0).color();

        assertEquals(150.0 / 255.0, justBefore.getRed(), 0.01, "最大半径之前仍是淡青");
        assertEquals(239.0 / 255.0, justAfter.getRed(), EPS, "最大半径之后才转暖红");
    }

    // ---- 连贯性 ----

    @Test
    void redTurnDoesNotBrightenTheRing() {
        double atMaxRadius = ClickRippleAnimation.computeFrame(418.0).ringAlpha();
        double atRed = ClickRippleAnimation.computeFrame(490.0).ringAlpha();
        double atFade = ClickRippleAnimation.computeFrame(540.0).ringAlpha();

        assertTrue(atRed <= atMaxRadius + EPS, "变红时不再回闪变亮");
        assertTrue(atFade <= atRed + EPS, "变红之后只减不增");
    }

    @Test
    void glowTracksCoreAndDisappearsWithIt() {
        assertTrue(ClickRippleAnimation.computeFrame(116.0).glowAlpha() > 0.05, "中途柔光可见");
        assertEquals(0.0, ClickRippleAnimation.computeFrame(580.0).glowAlpha(), EPS, "结束时柔光归零");
    }

    @Test
    void timelineHasNoVisibleJumps() {
        for (double t = 1.0; t <= 580.0; t += 1.0) {
            ClickRippleAnimation.Frame previous = ClickRippleAnimation.computeFrame(t - 1.0);
            ClickRippleAnimation.Frame current = ClickRippleAnimation.computeFrame(t);

            assertEquals(previous.radius(), current.radius(), 0.6, "t=" + t + " 半径跳变");
            assertEquals(previous.lineWidth(), current.lineWidth(), 0.05, "t=" + t + " 线宽跳变");
            assertEquals(previous.fillAlpha(), current.fillAlpha(), 0.08, "t=" + t + " 实心层跳变");
            assertEquals(previous.ringAlpha(), current.ringAlpha(), 0.08, "t=" + t + " 圆环层跳变");
            assertEquals(previous.glowAlpha(), current.glowAlpha(), 0.05, "t=" + t + " 柔光跳变");
            assertColor(previous.color(), current.color(), "t=" + t + " 颜色跳变", 0.05);
        }
    }

    // ---- 边界与不变量 ----

    @Test
    void clampsTimeOutsideTimeline() {
        ClickRippleAnimation.Frame atZero = ClickRippleAnimation.computeFrame(0.0);
        ClickRippleAnimation.Frame before = ClickRippleAnimation.computeFrame(-100.0);

        assertEquals(atZero.radius(), before.radius(), EPS, "负时间截断到 0");
        assertEquals(atZero.fillAlpha(), before.fillAlpha(), EPS);
        assertColor(atZero.color(), before.color());

        ClickRippleAnimation.Frame after = ClickRippleAnimation.computeFrame(9999.0);
        assertEquals(44.0, after.radius(), EPS);
        assertEquals(0.0, after.fillAlpha(), EPS, "超出时长截断为全透明");
        assertEquals(0.0, after.ringAlpha(), EPS);
        assertEquals(0.0, after.glowAlpha(), EPS);
    }

    @Test
    void frameStaysWithinBoundsAndRadiusNeverDecreases() {
        double previousRadius = 0.0;
        for (double t = 0.0; t <= 580.0; t += 1.0) {
            ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(t);

            assertTrue(frame.radius() >= previousRadius - EPS, "半径在 t=" + t + " 处回退");
            assertTrue(frame.radius() >= 2.0 - EPS && frame.radius() <= 44.0 + EPS, "半径越界 t=" + t);
            assertTrue(frame.lineWidth() >= 2.2 - EPS && frame.lineWidth() <= 3.4 + EPS, "线宽越界 t=" + t);
            assertOpacity(frame.fillAlpha(), t);
            assertOpacity(frame.ringAlpha(), t);
            assertOpacity(frame.glowAlpha(), t);

            previousRadius = frame.radius();
        }
    }

    // ---- 断言辅助 ----

    private static void assertOpacity(double value, double time) {
        assertTrue(value >= 0.0 && value <= 1.0, "不透明度越界 t=" + time + " -> " + value);
    }

    private static void assertColor(Color expected, Color actual) {
        assertColor(expected, actual, "", EPS);
    }

    private static void assertColor(Color expected, Color actual, String message) {
        assertColor(expected, actual, message, EPS);
    }

    private static void assertColor(Color expected, Color actual, String message, double eps) {
        assertEquals(expected.getRed(), actual.getRed(), eps, message + " (R)");
        assertEquals(expected.getGreen(), actual.getGreen(), eps, message + " (G)");
        assertEquals(expected.getBlue(), actual.getBlue(), eps, message + " (B)");
    }
}
