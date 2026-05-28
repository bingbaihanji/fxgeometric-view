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
 * 放射线生成方式：
 * <ul>
 *   <li>同心圆使用屏幕像素半径（tickStepR = xScale * gridStep）</li>
 *   <li>圆圈为屏幕正圆（锁定 1:1 时绘制极坐标网格）</li>
 *   <li>放射线遍历 [0, 2π) 所有方向，用 cos/sin 精确生成射线</li>
 *   <li>屏幕 Y 轴朝下：dx = cos(θ)*max，dy = -sin(θ)*max</li>
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

        // 世界原点在屏幕上的像素位置
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        // 极坐标锁定 1:1，scaleX == scaleY，同心圆在屏幕上为正圆
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double tickStepR = transform.getScaleX() * step;

        if (tickStepR < 1.0) {
            return elements;
        }

        // 视口最远角到原点的距离，确保所有可见圆和射线都被生成
        double farthestX = Math.max(Math.abs(x0), Math.abs(viewWidth - x0));
        double farthestY = Math.max(Math.abs(y0), Math.abs(viewHeight - y0));
        double maxDist = Math.sqrt(farthestX * farthestX + farthestY * farthestY) + tickStepR;

        // 同心圆：从 tickStepR 开始递增，直到覆盖视口
        Point2D center = new Point2D(x0, y0);
        for (double r = tickStepR; r <= maxDist; r += tickStepR) {
            elements.add(new GridElement.GridCircle(center, r, r, 0, 360, false));
        }

        // 放射线：以 polarAngleStep（度）为步长，整数计数避免浮点累积误差
        double angleStepDeg = settings.getPolarAngleStep();
        int rayCount = (int) Math.round(360.0 / angleStepDeg);
        for (int i = 0; i < rayCount; i++) {
            double theta = Math.toRadians(i * angleStepDeg);
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);
            // 屏幕 Y 轴朝下，故屏幕方向 dy = -sin(θ)
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(x0, y0),
                    new Point2D(x0 + cos * maxDist, y0 - sin * maxDist),
                    false
            ));
        }

        return elements;
    }
}
