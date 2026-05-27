package com.bingbaihanji.view.layout.draw.coordinate.grid;

import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 极坐标网格生成器
 * <p>
 * 以世界原点(0,0)为圆心生成同心圆和放射线。
 * X/Y 不等比例时同心圆变为椭圆（屏幕椭圆 = 世界正圆）。
 * 迁移自 GridPainter.paintPolarGrid()。
 *
 * @author bingbaihanji
 */
public class PolarGridGenerator {

    /**
     * 生成极坐标网格元素列表
     *
     * @param transform 世界坐标变换
     * @param settings  视图配置
     * @param viewWidth 视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 网格元素列表（屏幕坐标）
     */
    public List<GridElement> generate(WorldTransform transform, EuclidianViewSettings settings,
                                      double viewWidth, double viewHeight) {
        List<GridElement> elements = new ArrayList<>();
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double angleStep = settings.getPolarAngleStep();

        Point2D origin = new Point2D(x0, y0);

        double maxRadiusWorld = Math.max(
                Math.abs(transform.screenToWorldX(0) - transform.screenToWorldX(viewWidth)),
                Math.abs(transform.screenToWorldY(0) - transform.screenToWorldY(viewHeight))
        );

        // 同心圆
        for (double r = step; r <= maxRadiusWorld; r += step) {
            double srX = r * scaleX;
            double srY = r * scaleY;
            elements.add(new GridElement.GridCircle(origin, srX, srY, 0, 360, false));
        }

        // 放射线
        int numRays = (int) Math.ceil(2 * Math.PI / angleStep);
        double screenMaxRadiusX = maxRadiusWorld * scaleX;
        double screenMaxRadiusY = maxRadiusWorld * scaleY;
        for (int i = 0; i < numRays; i++) {
            double angle = i * angleStep;
            double dx = Math.cos(angle) * screenMaxRadiusX;
            double dy = -Math.sin(angle) * screenMaxRadiusY;
            elements.add(new GridElement.GridLineSegment(origin,
                    new Point2D(x0 + dx, y0 + dy), false));
        }

        return elements;
    }
}
