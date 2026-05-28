package com.bingbaihanji.view.layout.draw.coordinate;

import com.bingbaihanji.constant.AxisTickStyle;
import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.CartesianGridGenerator;
import com.bingbaihanji.view.layout.draw.coordinate.grid.DotGridGenerator;
import com.bingbaihanji.view.layout.draw.coordinate.grid.GridElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 笛卡尔坐标系
 * <p>
 * 标准直角坐标系，支持点状网格、笛卡尔网格和带子网格的笛卡尔网格。
 *
 * @author bingbaihanji
 */
public class CartesianCoordinateSystem implements CoordinateSystem {

    private final CartesianGridGenerator cartesianGridGenerator = new CartesianGridGenerator();
    private final DotGridGenerator dotGridGenerator = new DotGridGenerator();

    @Override
    public List<GridElement> generateGrid(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        GridType gridType = settings.getGridType();
        if (gridType == GridType.DOT) {
            return dotGridGenerator.generate(transform, settings, viewWidth, viewHeight);
        } else if (gridType == GridType.CARTESIAN || gridType == GridType.CARTESIAN_WITH_SUBGRID) {
            return cartesianGridGenerator.generate(transform, settings, viewWidth, viewHeight);
        }
        return List.of();
    }

    @Override
    public List<TickInfo> calculateXTicks(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        double step = computeXStep(transform, settings);
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(viewWidth);

        List<TickInfo> ticks = new ArrayList<>();
        boolean drawMinor = settings.getXTickStyle() == AxisTickStyle.MAJOR_MINOR;

        for (double x = Math.floor(worldLeft / step) * step; x <= worldRight; x += step) {
            if (Math.abs(x) < 1e-8) continue;
            double sx = transform.worldToScreenX(x);
            ticks.add(new TickInfo(x, sx, "", false));

            if (drawMinor) {
                double minorStep = step / 5;
                for (int i = 1; i < 5; i++) {
                    double minorX = x + i * minorStep;
                    double minorSx = transform.worldToScreenX(minorX);
                    ticks.add(new TickInfo(minorX, minorSx, "", true));
                }
            }
        }
        return ticks;
    }

    @Override
    public List<TickInfo> calculateYTicks(WorldTransform transform, EuclidianViewSettings settings,
                                          double viewWidth, double viewHeight) {
        double step = computeYStep(transform, settings);
        double worldBottom = transform.screenToWorldY(viewHeight);
        double worldTop = transform.screenToWorldY(0);

        List<TickInfo> ticks = new ArrayList<>();
        boolean drawMinor = settings.getYTickStyle() == AxisTickStyle.MAJOR_MINOR;

        for (double y = Math.floor(worldBottom / step) * step; y <= worldTop; y += step) {
            if (Math.abs(y) < 1e-8) continue;
            double sy = transform.worldToScreenY(y);
            ticks.add(new TickInfo(y, sy, "", false));

            if (drawMinor) {
                double minorStep = step / 5;
                for (int i = 1; i < 5; i++) {
                    double minorY = y + i * minorStep;
                    double minorSy = transform.worldToScreenY(minorY);
                    ticks.add(new TickInfo(minorY, minorSy, "", true));
                }
            }
        }
        return ticks;
    }

    @Override
    public boolean isAxesRatioLocked() {
        return false;
    }

    private double computeXStep(WorldTransform transform, EuclidianViewSettings settings) {
        if (settings.isAutoXTickDistance()) {
            double step = AxisTickCalculator.calculateAxisTickDistance(
                    transform.getScaleX(), settings.isXAxisPiUnit());
            settings.setXTickDistance(step);
            return step;
        }
        return settings.getXTickDistance();
    }

    private double computeYStep(WorldTransform transform, EuclidianViewSettings settings) {
        if (settings.isAutoYTickDistance()) {
            double step = AxisTickCalculator.calculateAxisTickDistance(
                    transform.getScaleY(), settings.isYAxisPiUnit());
            settings.setYTickDistance(step);
            return step;
        }
        return settings.getYTickDistance();
    }
}
