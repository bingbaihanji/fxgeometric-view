package com.bingbaihanji.view.layout.core;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 点击涟漪时间轴回归测试
 * <p>
 * 规格：蓝点弹出 → 绽开成环(蓝→青) → 扩散晕散(青→淡青) → 到达最大半径变红 → 淡出消失
 */
class ClickRippleAnimationTest {

    private static final double EPS = 1e-6;

    /** 蓝色小点 */
    private static final Color BLUE = Color.rgb(65, 147, 249);
    /** 青色圆环 */
    private static final Color CYAN = Color.rgb(0, 188, 212);
    /** 淡青色（晕散末端） */
    private static final Color LIGHT_CYAN = Color.rgb(144, 224, 239);
    /** 红色圆环 */
    private static final Color RED = Color.rgb(255, 0, 0);

    // ---- 阶段特征 ----

    @Test
    void dotGrowsFromBlueSeedDuringPopIn() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(40.0);

        assertEquals(3.5, frame.radius(), EPS, "0-80ms 蓝点从 r=2 弹出到 r=5");
        assertEquals(1.0, frame.opacity(), EPS);
        assertColor(BLUE, frame.color());
    }

    @Test
    void morphThinsLineWidthTowardRing() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(125.0);

        assertEquals(8.0, frame.radius(), EPS, "80-170ms 半径 5→11，中点 8");
        assertEquals(4.0, frame.lineWidth(), EPS, "线宽 5→3，中点 4");
        assertColor(BLUE.interpolate(CYAN, 0.5), frame.color(), "蓝→青中点");
    }

    @Test
    void expansionSpreadsFromCyanTowardLightCyan() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(320.0);

        assertEquals(35.75, frame.radius(), EPS, "170-470ms 半径 11→44 二次缓出，中点 35.75");
        assertEquals(0.70, frame.opacity(), EPS, "不透明度 0.95→0.45，中点 0.70");
        assertColor(CYAN.interpolate(LIGHT_CYAN, 0.5), frame.color(), "青→淡青中点");
    }

    @Test
    void reachesMaxRadiusAsLightCyan() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(470.0);

        assertEquals(44.0, frame.radius(), EPS, "扩散终点即最大半径");
        assertEquals(0.45, frame.opacity(), EPS);
        assertColor(LIGHT_CYAN, frame.color(), "扩散末端为淡青色");
    }

    @Test
    void turnsRedAtMaxRadius() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(505.0);

        assertEquals(44.0, frame.radius(), EPS, "变红阶段半径保持最大");
        assertEquals(0.725, frame.opacity(), EPS, "变红同时回闪变亮 0.45→1.0");
        assertColor(LIGHT_CYAN.interpolate(RED, 0.5), frame.color(), "淡青→红中点");
    }

    @Test
    void redRingFadesOutAtMaxRadius() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(600.0);

        assertEquals(44.0, frame.radius(), EPS);
        assertEquals(0.5, frame.opacity(), EPS, "540-660ms 红环 1.0→0 淡出");
        assertColor(RED, frame.color());
    }

    // ---- 实心点/圆环形态 ----

    @Test
    void seedAndPopInRenderAsFilledDot() {
        assertTrue(ClickRippleAnimation.computeFrame(0.0).filled(), "起始帧为实心点");
        assertTrue(ClickRippleAnimation.computeFrame(40.0).filled(), "弹出阶段为实心点");
        assertTrue(ClickRippleAnimation.computeFrame(80.0).filled(), "绽开开始时仍是实心点");
    }

    @Test
    void morphFlipsToStrokeRingMidMorph() {
        assertFalse(ClickRippleAnimation.computeFrame(125.0).filled(), "绽开中点已成圆环");
    }

    @Test
    void laterPhasesRenderAsStrokeRing() {
        assertFalse(ClickRippleAnimation.computeFrame(250.0).filled(), "扩散阶段为圆环");
        assertFalse(ClickRippleAnimation.computeFrame(505.0).filled(), "变红阶段为圆环");
        assertFalse(ClickRippleAnimation.computeFrame(600.0).filled(), "淡出阶段为圆环");
    }

    // ---- 边界与不变量 ----

    @Test
    void startFrameIsOpaqueBlueDot() {
        ClickRippleAnimation.Frame frame = ClickRippleAnimation.computeFrame(0.0);

        assertEquals(2.0, frame.radius(), EPS);
        assertEquals(5.0, frame.lineWidth(), EPS);
        assertEquals(1.0, frame.opacity(), EPS);
        assertColor(BLUE, frame.color());
    }

    @Test
    void clampsTimeOutsideTimeline() {
        ClickRippleAnimation.Frame before = ClickRippleAnimation.computeFrame(-100.0);
        ClickRippleAnimation.Frame atZero = ClickRippleAnimation.computeFrame(0.0);

        assertEquals(atZero.radius(), before.radius(), EPS, "负时间截断到 0");
        assertEquals(atZero.opacity(), before.opacity(), EPS);
        assertColor(atZero.color(), before.color());

        ClickRippleAnimation.Frame after = ClickRippleAnimation.computeFrame(9999.0);
        assertEquals(0.0, after.opacity(), EPS, "超出时长截断为全透明");
        assertEquals(44.0, after.radius(), EPS);
    }

    @Test
    void phaseBoundariesHaveNoJumps() {
        double[] boundaries = {80.0, 170.0, 470.0, 540.0};
        for (double boundary : boundaries) {
            ClickRippleAnimation.Frame before = ClickRippleAnimation.computeFrame(boundary - 1e-4);
            ClickRippleAnimation.Frame after = ClickRippleAnimation.computeFrame(boundary + 1e-4);

            assertEquals(before.radius(), after.radius(), 1e-3, "t=" + boundary + " 半径跳变");
            assertEquals(before.lineWidth(), after.lineWidth(), 1e-3, "t=" + boundary + " 线宽跳变");
            assertEquals(before.opacity(), after.opacity(), 1e-3, "t=" + boundary + " 不透明度跳变");
            assertColor(before.color(), after.color(), "t=" + boundary + " 颜色跳变", 1e-3);
        }
    }

    @Test
    void radiusNeverDecreasesAndStaysInBounds() {
        double previous = Double.NEGATIVE_INFINITY;
        for (double t = 0.0; t <= 660.0; t += 1.0) {
            double radius = ClickRippleAnimation.computeFrame(t).radius();
            assertTrue(radius >= previous - EPS, "半径在 t=" + t + " 处回退");
            assertTrue(radius > 0 && radius <= 44.0 + EPS, "半径超出范围 t=" + t);
            previous = radius;
        }
    }

    @Test
    void opacityStaysInUnitRange() {
        for (double t = -50.0; t <= 700.0; t += 1.0) {
            double opacity = ClickRippleAnimation.computeFrame(t).opacity();
            assertTrue(opacity >= 0.0 && opacity <= 1.0, "不透明度越界 t=" + t);
        }
    }

    // ---- 断言辅助 ----

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
