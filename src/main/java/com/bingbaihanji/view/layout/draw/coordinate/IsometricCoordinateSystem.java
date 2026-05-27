package com.bingbaihanji.view.layout.draw.coordinate;

import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.GridElement;
import com.bingbaihanji.view.layout.draw.coordinate.grid.IsometricGridGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * 等距网格坐标系
 * <p>
 * 三组 60° 交错平行线形成等边三角形格子。
 * 轴刻度计算与笛卡尔坐标系相同，视觉上标注 X/Y 轴位置。
 *
 * @author bingbaihanji
 */
public class IsometricCoordinateSystem implements CoordinateSystem {

    private final IsometricGridGenerator isometricGridGenerator = new IsometricGridGenerator();

    @Override
    public List<GridElement> generateGrid(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        return isometricGridGenerator.generate(transform, settings, viewWidth, viewHeight);
    }

    @Override
    public List<TickInfo> calculateXTicks(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        double step = computeStep(transform, settings);
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
        double step = computeStep(transform, settings);
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
        return false;
    }

    private double computeStep(WorldTransform transform, EuclidianViewSettings settings) {
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
