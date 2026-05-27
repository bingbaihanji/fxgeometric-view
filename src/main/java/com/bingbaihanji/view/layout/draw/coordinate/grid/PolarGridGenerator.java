package com.bingbaihanji.view.layout.draw.coordinate.grid;

import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 极坐标网格生成器（与 GeoGebra 绘制逻辑完全对齐）
 * <p>
 * 以世界原点(0,0)为圆心生成同心圆和放射线。
 * <p>
 * 与 GeoGebra EuclidianView.drawGrid() 对齐：
 * <ul>
 *   <li>同心圆使用屏幕像素半径（tickStepR = xScale * gridStep）</li>
 *   <li>圆圈为屏幕正圆（GeoGebra 只在锁定 1:1 时绘制极坐标网格）</li>
 *   <li>水平轴线（theta=0）单独绘制为全屏宽度线</li>
 *   <li>放射线仅遍历 0 到 PI（tan 天然生成双向线）</li>
 *   <li>一般放射线：m = tan(angle)，y1 = m*x0 + y0，y2 = m*(x0-width) + y0</li>
 * </ul>
 *
 * @author bingbaihanji
 */
public class PolarGridGenerator {

    /**
     * 生成极坐标网格元素列表
     *
     * @param transform  世界坐标变换
     * @param settings   视图配置
     * @param viewWidth  视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 网格元素列表（屏幕坐标）
     */
    public List<GridElement> generate(WorldTransform transform, EuclidianViewSettings settings,
                                      double viewWidth, double viewHeight) {
        List<GridElement> elements = new ArrayList<>();
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        double scaleX = transform.getScaleX();
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double angleStep = settings.getPolarAngleStep();

        // 屏幕像素半径步长（与 GeoGebra 一致：tickStepR = getXscale() * gridDistances[0]）
        double tickStepR = scaleX * step;

        // min 像素距离：原点到屏幕边缘的最近距离（GeoGebra 精确算法）
        double min;
        if (x0 > 0 && x0 < viewWidth && y0 > 0 && y0 < viewHeight) {
            min = 0;
        } else {
            double minW = Math.min(Math.abs(x0), Math.abs(x0 - viewWidth));
            double minH = Math.min(Math.abs(y0), Math.abs(y0 - viewHeight));
            min = Math.min(minW, minH);
        }

        // max 像素距离：原点到屏幕四角的最大距离（MyMath.length → sqrt）
        double max = computeMaxPixelRadius(x0, y0, viewWidth, viewHeight);

        // 同心圆：GeoGebra 绘制屏幕正圆（frame为正方形，2r × 2r）
        Point2D origin = new Point2D(x0, y0);
        double r = min - (min % tickStepR);
        while (r <= max) {
            // 屏幕正圆：radiusX = radiusY = r
            elements.add(new GridElement.GridCircle(origin, r, r, 0, 360, false));
            r += tickStepR;
        }

        // 水平轴线（theta=0）：全屏宽度
        elements.add(new GridElement.GridLineSegment(
                new Point2D(0, y0), new Point2D(viewWidth, y0), false));

        // 放射线：1 * angleStep 到 PI（不含），tan 天然双向
        // 注意：angleStep 是世界数学角度（Y轴朝上），屏幕坐标 Y 轴朝下
        // 因此屏幕斜率 = -tan(angle)，才能使放射线与世界坐标角度对齐
        for (int idx = 1; idx * angleStep < Math.PI; idx++) {
            double angle = idx * angleStep;

            if (Math.abs(angle - Math.PI / 2) < 0.0001) {
                // 垂直线（theta ≈ PI/2）：全屏高度
                elements.add(new GridElement.GridLineSegment(
                        new Point2D(x0, 0), new Point2D(x0, viewHeight), false));
            } else {
                // 屏幕斜率取负：世界数学角度 → 屏幕 Y 轴方向相反
                double m = -Math.tan(angle);
                double y1 = (m * x0) + y0;
                double y2 = (m * (x0 - viewWidth)) + y0;
                elements.add(new GridElement.GridLineSegment(
                        new Point2D(0, y1), new Point2D(viewWidth, y2), false));
            }
        }

        return elements;
    }

    /** 计算原点到屏幕四角的最大像素距离（与 GeoGebra MyMath.length 一致） */
    private double computeMaxPixelRadius(double x0, double y0, double viewWidth, double viewHeight) {
        double d1 = Math.sqrt(x0 * x0 + y0 * y0);
        double d2 = Math.sqrt(x0 * x0 + (y0 - viewHeight) * (y0 - viewHeight));
        double d3 = Math.sqrt((x0 - viewWidth) * (x0 - viewWidth) + y0 * y0);
        double d4 = Math.sqrt((x0 - viewWidth) * (x0 - viewWidth)
                + (y0 - viewHeight) * (y0 - viewHeight));
        return Math.max(Math.max(d1, d2), Math.max(d3, d4));
    }
}
