package com.bingbaihanji.view.layout.draw.coordinate.grid;

import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 等距网格生成器
 * <p>
 * 三组平行线交汇形成等边三角形格子，所有线经过世界原点。
 * 迁移自 GridPainter.paintIsometricGrid()。
 *
 * @author bingbaihanji
 */
public class IsometricGridGenerator {

    /**
     * 生成等距网格元素列表
     *
     * @param transform 世界坐标变换
     * @param settings  视图配置
     * @param viewWidth 视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 网格线段列表（屏幕坐标）
     */
    public List<GridElement> generate(WorldTransform transform, EuclidianViewSettings settings,
                                      double viewWidth, double viewHeight) {
        List<GridElement> elements = new ArrayList<>();
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double tickStepX = transform.getScaleX() * step * Math.sqrt(3.0);
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(viewWidth);
        double sqrt3 = Math.sqrt(3.0);

        int xCount = (int) Math.ceil(Math.max(Math.abs(worldLeft), Math.abs(worldRight))
                / (step * sqrt3));
        int offsetRange = (int) Math.ceil((viewWidth + viewHeight) / tickStepX) + xCount;

        double diagSlope = sqrt3 * transform.getScaleY() / transform.getScaleX();

        // 垂直线
        for (int i = -xCount; i <= xCount; i++) {
            double sx = x0 + i * tickStepX;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(sx, 0), new Point2D(sx, viewHeight), false));
        }

        // 60° 斜线（屏幕斜率为 -diagSlope）
        for (int i = -offsetRange; i <= offsetRange; i++) {
            double sx1 = x0 + i * tickStepX;
            double xTop = sx1 + y0 / diagSlope;
            double xBottom = sx1 - (viewHeight - y0) / diagSlope;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(xTop, 0), new Point2D(xBottom, viewHeight), false));
        }

        // 120° 斜线（屏幕斜率为 +diagSlope）
        for (int i = -offsetRange; i <= offsetRange; i++) {
            double sx1 = x0 + i * tickStepX;
            double xTop = sx1 - y0 / diagSlope;
            double xBottom = sx1 + (viewHeight - y0) / diagSlope;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(xTop, 0), new Point2D(xBottom, viewHeight), false));
        }

        return elements;
    }
}
