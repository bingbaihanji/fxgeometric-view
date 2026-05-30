package com.bingbaihanji.view.layout.draw.coordinate.render;

import com.bingbaihanji.constant.UnitLabelType;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.draw.coordinate.TickInfo;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

import java.util.List;

/**
 * 刻度标签渲染器
 * <p>
 * 负责格式化刻度数字（支持数值、π 单位、科学计数法）并绘制到画布上。
 * 迁移自 AxesPainter.formatNumber() / formatPiUnit() / formatNumericUnit()。
 *
 * @author bingbaihanji
 */
public class TickLabelRenderer {

    /**
     * 标签距离视口边缘的安全距离（像素）
     */
    private static final double LABEL_EDGE_MARGIN = 15;

    /**
     * 绘制 X 轴刻度标签
     *
     * @param gc       画布上下文
     * @param ticks    刻度信息列表（仅主刻度有标签文字）
     * @param axisY    Y 轴屏幕位置
     * @param unitType 单位标签类型
     * @param step     刻度步长（用于决定数值格式）
     * @param settings 视图配置
     */
    public void drawXLabels(GraphicsContext gc, List<TickInfo> ticks, double axisY,
                            UnitLabelType unitType, double step, EuclidianViewSettings settings) {
        gc.setStroke(settings.getAxesColor());
        gc.setFill(settings.getAxesColor());
        gc.setFont(Font.font(15));

        double canvasW = gc.getCanvas().getWidth();
        for (TickInfo tick : ticks) {
            if (tick.isMinor()) continue;
            if (tick.screenPos() < LABEL_EDGE_MARGIN || tick.screenPos() > canvasW - LABEL_EDGE_MARGIN) continue;

            String label = formatNumber(tick.worldPos(), unitType, step);
            gc.fillText(label, tick.screenPos() + 2, axisY - 6);
        }
    }

    /**
     * 绘制 Y 轴刻度标签
     *
     * @param gc       画布上下文
     * @param ticks    刻度信息列表
     * @param axisX    X 轴屏幕位置
     * @param unitType 单位标签类型（PI 模式下 Y 轴强制用 NUMERIC）
     * @param step     刻度步长
     * @param settings 视图配置
     */
    public void drawYLabels(GraphicsContext gc, List<TickInfo> ticks, double axisX,
                            UnitLabelType unitType, double step, EuclidianViewSettings settings) {
        gc.setStroke(settings.getAxesColor());
        gc.setFill(settings.getAxesColor());
        gc.setFont(Font.font(15));

        double canvasH = gc.getCanvas().getHeight();
        // Y 轴在 π 单位模式下强制用数值显示
        UnitLabelType effectiveType = (unitType == UnitLabelType.PI) ? UnitLabelType.NUMERIC : unitType;

        for (TickInfo tick : ticks) {
            if (tick.isMinor()) continue;
            if (tick.screenPos() < LABEL_EDGE_MARGIN || tick.screenPos() > canvasH - LABEL_EDGE_MARGIN) continue;

            String label = formatNumber(tick.worldPos(), effectiveType, step);
            gc.fillText(label, axisX + 6, tick.screenPos() + 4);
        }
    }

    /**
     * 格式化刻度数字
     *
     * @param v        世界坐标值
     * @param unitType 单位类型
     * @param step     刻度步长
     * @return 格式化后的字符串
     */
    private String formatNumber(double v, UnitLabelType unitType, double step) {
        if (unitType == UnitLabelType.PI) {
            return formatPiUnit(v);
        }
        return formatNumericUnit(v, step);
    }

    /**
     * 格式化 π 单位
     */
    private String formatPiUnit(double v) {
        double piMultiple = v / Math.PI;
        if (MathCalculationUtils.isZero(piMultiple, 1e-6)) return "0";
        if (Math.abs(piMultiple - Math.round(piMultiple)) < 1e-4) {
            int m = (int) Math.round(piMultiple);
            if (m == 1) return "π";
            if (m == -1) return "-π";
            return m + "π";
        }
        if (MathCalculationUtils.equals(piMultiple, 0.5, 1e-4)) return "π/2";
        if (MathCalculationUtils.equals(piMultiple, -0.5, 1e-4)) return "-π/2";
        if (MathCalculationUtils.equals(piMultiple, 0.25, 1e-4)) return "π/4";
        if (MathCalculationUtils.equals(piMultiple, -0.25, 1e-4)) return "-π/4";
        if (MathCalculationUtils.equals(piMultiple, 1.5, 1e-4)) return "3π/2";
        if (MathCalculationUtils.equals(piMultiple, -1.5, 1e-4)) return "-3π/2";
        return String.format("%.2fπ", piMultiple);
    }

    /**
     * 格式化数值单位
     */
    private String formatNumericUnit(double v, double step) {
        if (step >= 10000) {
            return String.format("%.1E", v).replaceAll("E([+-])0+(\\d)", "E$1$2");
        }
        if (step < 0.01) {
            int decimals = Math.min((int) Math.ceil(-Math.log10(step)), 6);
            return String.format("%." + decimals + "f", v);
        }
        if (Math.abs(v - Math.round(v)) < 1e-6) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format("%.2f", v);
    }
}
