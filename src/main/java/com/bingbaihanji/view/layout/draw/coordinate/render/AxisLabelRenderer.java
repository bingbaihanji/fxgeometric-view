package com.bingbaihanji.view.layout.draw.coordinate.render;

import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

/**
 * 轴名称标签渲染器
 * <p>
 * 负责在轴线末端绘制 X/Y 轴标识和原点 "0" 标签。
 * 迁移自 AxesPainter.drawAxisLabel() 和 原点标记逻辑。
 *
 * @author bingbaihanji
 */
public class AxisLabelRenderer {

    /**
     * 绘制 X/Y 轴名称标签和原点标记
     *
     * @param gc        画布上下文
     * @param transform 世界坐标变换
     * @param settings  视图配置
     * @param width     视口宽度
     * @param height    视口高度
     */
    public void paint(GraphicsContext gc, WorldTransform transform,
                      EuclidianViewSettings settings, double width, double height) {
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        boolean xAxisVisible = y0 >= 0 && y0 <= height && settings.isShowXAxis();
        boolean yAxisVisible = x0 >= 0 && x0 <= width && settings.isShowYAxis();

        gc.setStroke(settings.getAxesColor());
        gc.setFill(settings.getAxesColor());
        gc.setFont(Font.font(15));

        // X 轴名称标签，放在轴线右端，避让刻度数字
        if (xAxisVisible) {
            String label = I18nUtil.getString("axis.xAxis");
            double textY = (y0 < 20) ? y0 + 18 : y0 - 10;
            gc.fillText(label, width - 40, textY);
        }

        // Y 轴名称标签，放在轴线上端
        if (yAxisVisible) {
            String label = I18nUtil.getString("axis.yAxis");
            double textX = (x0 > width - 40) ? x0 - 35 : x0 + 8;
            gc.fillText(label, textX, 28);
        }

        // 原点 "0" 标签
        if (xAxisVisible && yAxisVisible) {
            gc.fillText("0", x0 + 6, y0 - 6);
        }
    }
}
