package com.bingbaihanji.view.layout.draw.coordinate.grid;

import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 点状网格生成器
 * <p>
 * 在网格交点处生成圆点。迁移自 GridPainter.paintDotGrid()。
 *
 * @author bingbaihanji
 */
public class DotGridGenerator {

    /**
     * 生成点状网格元素列表
     *
     * @param transform 世界坐标变换
     * @param settings  视图配置
     * @param viewWidth 视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 点状网格元素列表（屏幕坐标）
     */
    public List<GridElement> generate(WorldTransform transform, EuclidianViewSettings settings,
                                      double viewWidth, double viewHeight) {
        List<GridElement> elements = new ArrayList<>();
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double tickStepX = transform.getScaleX() * step;
        double tickStepY = transform.getScaleY() * step;
        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);

        double startX = xZero % tickStepX;
        double startY = yZero % tickStepY;

        for (double sx = startX; sx <= viewWidth; sx += tickStepX) {
            for (double sy = startY; sy <= viewHeight; sy += tickStepY) {
                elements.add(new GridElement.GridDot(new Point2D(sx, sy)));
            }
        }

        return elements;
    }
}
