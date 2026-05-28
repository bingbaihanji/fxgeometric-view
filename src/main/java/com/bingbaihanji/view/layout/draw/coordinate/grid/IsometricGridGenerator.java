package com.bingbaihanji.view.layout.draw.coordinate.grid;

import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 等距网格生成器（与 GeoGebra 绘制逻辑完全对齐）
 * <p>
 * 三组平行线交汇形成等边三角形格子：
 * <ul>
 *   <li>屏幕垂直线 → 世界空间中平行于 Y 轴的线（90°）</li>
 *   <li>负斜率对角线 → 世界空间 30° 线（tan=1/√3）</li>
 *   <li>正斜率对角线 → 世界空间 150° 线（tan=-1/√3）</li>
 * </ul>
 * <p>
 * 三条线族两两夹角均为 60°，形成等边三角形交点。
 *
 * @author bingbaihanji
 */
public class IsometricGridGenerator {

    /**
     * 生成等距网格元素列表
     *
     * @param transform  世界坐标变换
     * @param settings   视图配置
     * @param viewWidth  视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 网格线段列表（屏幕坐标）
     */
    public List<GridElement> generate(WorldTransform transform, EuclidianViewSettings settings,
                                      double viewWidth, double viewHeight) {
        List<GridElement> elements = new ArrayList<>();
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        double sqrt3 = Math.sqrt(3.0);

        // tickStepX：水平方向的网格间距（世界 step * scale * √3）
        double tickStepX = scaleX * step * sqrt3;
        // tickStepY：垂直方向的网格间距
        double tickStepY = scaleY * step;

        // 相位偏移（保证网格线经过世界原点）
        double startX = x0 % tickStepX;
        double startX2 = x0 % (tickStepX / 2);
        double startY = y0 % tickStepY;

        // 垂直线（世界 Y 轴平行线）：间距 tickStepX/2
        double pix = startX2;
        for (int j = 0; pix <= viewWidth; j++) {
            // 跳过与 Y 轴重合的线
            if (Math.abs(pix - x0) >= 0.5) {
                elements.add(new GridElement.GridLineSegment(
                        new Point2D(pix, 0), new Point2D(pix, viewHeight), false));
            }
            pix = startX2 + (j * tickStepX / 2.0);
        }

        // extra：斜线额外偏移量，确保原点在屏幕外时全覆盖
        // 参考 GeoGebra：(height * xScale / yScale * √3 / tickStepX) + 3
        int extra = (int) (viewHeight * scaleX / scaleY * sqrt3 / tickStepX) + 3;

        // 对角线跨屏所需的 X 跨度 = (height + tickStepY) * √3 * xScale / yScale
        double diagSpanX = (viewHeight + tickStepY) * sqrt3 * scaleX / scaleY;

        // 负斜率对角线（世界 30° 线）：屏幕斜率 = -yScale/(√3*xScale)
        pix = startX + (-(extra + 1) * tickStepX);
        for (int j = -extra; pix <= viewWidth; j++) {
            double endx = pix + diagSpanX;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(pix, startY - tickStepY),
                    new Point2D(endx, startY + viewHeight),
                    false));
            pix = startX + (j * tickStepX);
        }

        // 正斜率对角线（世界 150° 线）：屏幕斜率 = +yScale/(√3*xScale)
        pix = startX;
        for (int j = 0; pix <= viewWidth + diagSpanX; j++) {
            double endx = pix - diagSpanX;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(pix, startY - tickStepY),
                    new Point2D(endx, startY + viewHeight),
                    false));
            pix = startX + (j * tickStepX);
        }

        return elements;
    }
}
