package com.bingbaihanji.view.layout.draw.coordinate;

import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.GridElement;
import com.bingbaihanji.view.layout.draw.coordinate.grid.PolarGridGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * 极坐标系
 * <p>
 * 以世界原点(0,0)为中心，生成同心圆和放射线网格。
 * 锁定轴比例为 1:1，保证同心圆在屏幕上为正圆。
 *
 * @author bingbaihanji
 */
public class PolarCoordinateSystem implements CoordinateSystem {

    private final PolarGridGenerator polarGridGenerator = new PolarGridGenerator();

    @Override
    public List<GridElement> generateGrid(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        return polarGridGenerator.generate(transform, settings, viewWidth, viewHeight);
    }

    @Override
    public List<TickInfo> calculateXTicks(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        double step = computeTickStep(transform, settings);
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(viewWidth);
        List<TickInfo> ticks = new ArrayList<>();

        for (double x = Math.floor(worldLeft / step) * step; x <= worldRight; x += step) {
            if (Math.abs(x) < 1e-8) continue;
            double sx = transform.worldToScreenX(x);
            ticks.add(new TickInfo(x, sx, "", false));
        }
        return ticks;
    }

    @Override
    public List<TickInfo> calculateYTicks(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        double step = computeTickStep(transform, settings);
        double worldBottom = transform.screenToWorldY(viewHeight);
        double worldTop = transform.screenToWorldY(0);
        List<TickInfo> ticks = new ArrayList<>();

        for (double y = Math.floor(worldBottom / step) * step; y <= worldTop; y += step) {
            if (Math.abs(y) < 1e-8) continue;
            double sy = transform.worldToScreenY(y);
            ticks.add(new TickInfo(y, sy, "", false));
        }
        return ticks;
    }

    @Override
    public boolean isAxesRatioLocked() {
        return true;
    }

    private double computeTickStep(WorldTransform transform, EuclidianViewSettings settings) {
        if (settings.isAutoXTickDistance()) {
            double step = AxisTickCalculator.calculateAxisTickDistance(
                    transform.getScale(), false);
            settings.setXTickDistance(step);
            settings.setYTickDistance(step);
            return step;
        }
        return settings.getXTickDistance();
    }
}
