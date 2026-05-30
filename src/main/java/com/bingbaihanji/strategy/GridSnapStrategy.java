package com.bingbaihanji.strategy;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.CartesianGridGenerator;

/**
 * 网格吸附策略（支持笛卡尔、极坐标、等距网格）
 * <p>
 * 低优先级的吸附策略,吸附到网格交点。
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class GridSnapStrategy implements SnapStrategy {

    @Override
    public double[] snap(double x, double y, IDrawingContext context) {
        EuclidianViewSettings settings = context.getGridChartPane().getSettings();
        GridType gridType = settings.getGridType();
        WorldTransform transform = context.getTransform();

        if (gridType == GridType.POLAR) {
            return snapToPolar(x, y, settings, transform);
        } else if (gridType == GridType.ISOMETRIC) {
            return snapToIsometric(x, y, settings, transform);
        } else {
            return snapToCartesian(x, y, transform);
        }
    }

    /**
     * 笛卡尔网格吸附
     */
    private double[] snapToCartesian(double x, double y, WorldTransform transform) {
        double scale = transform.getScale();
        double threshold = GeometryConfig.Snapping.GRID_SNAP_THRESHOLD_PIXELS / scale;
        double step = AxisTickCalculator.calculateAxisTickDistance(scale, false);

        double gridX = Math.round(x / step) * step;
        double gridY = Math.round(y / step) * step;

        double distance = Math.hypot(gridX - x, gridY - y);
        if (distance < threshold) {
            if (Math.abs(gridX - Math.round(gridX)) < 1e-9) gridX = Math.round(gridX);
            if (Math.abs(gridY - Math.round(gridY)) < 1e-9) gridY = Math.round(gridY);
            return new double[]{gridX, gridY};
        }
        return null;
    }

    /**
     * 极坐标网格吸附：搜索鼠标附近 3×3 网格交点，找最近者
     */
    private double[] snapToPolar(double worldX, double worldY,
                                 EuclidianViewSettings settings, WorldTransform transform) {
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double angleStepDeg = settings.getPolarAngleStep();
        double angleStepRad = Math.toRadians(angleStepDeg);

        double r = Math.sqrt(worldX * worldX + worldY * worldY);
        double theta = Math.atan2(worldY, worldX);

        int kR = (int) Math.round(r / step);
        if (kR < 0) kR = 0;
        int kTheta = (int) Math.round(theta / angleStepRad);

        double mouseSX = transform.worldToScreenX(worldX);
        double mouseSY = transform.worldToScreenY(worldY);
        double bestDist = Double.MAX_VALUE;
        double bestX = worldX, bestY = worldY;

        for (int dr = -1; dr <= 1; dr++) {
            double cr = (kR + dr) * step;
            if (cr < 0) continue;
            for (int dt = -1; dt <= 1; dt++) {
                double ct = (kTheta + dt) * angleStepRad;
                double cx = cr * Math.cos(ct);
                double cy = cr * Math.sin(ct);
                double sx = transform.worldToScreenX(cx);
                double sy = transform.worldToScreenY(cy);
                double dist = Math.hypot(sx - mouseSX, sy - mouseSY);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = cx;
                    bestY = cy;
                }
            }
        }

        if (bestDist < GeometryConfig.Snapping.GRID_SNAP_THRESHOLD_PIXELS) {
            if (Math.abs(bestX - Math.round(bestX)) < 1e-9) bestX = Math.round(bestX);
            if (Math.abs(bestY - Math.round(bestY)) < 1e-9) bestY = Math.round(bestY);
            return new double[]{bestX, bestY};
        }
        return null;
    }

    /**
     * 等距网格吸附
     */
    private double[] snapToIsometric(double worldX, double worldY,
                                     EuclidianViewSettings settings, WorldTransform transform) {
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        double sqrt3 = Math.sqrt(3.0);

        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        double tickStepX = scaleX * step * sqrt3;
        double tickStepY = scaleY * step;
        double startX2 = x0 % (tickStepX / 2.0);
        double startY = y0 % tickStepY;

        int j = (int) Math.round((screenX - startX2) / (tickStepX / 2.0));
        double yBase = startY + ((j % 2) * tickStepY / 2.0);
        int k = (int) Math.round((screenY - yBase) / tickStepY);

        double snappedSX = startX2 + j * tickStepX / 2.0;
        double snappedSY = yBase + k * tickStepY;

        double pixelDist = Math.hypot(snappedSX - screenX, snappedSY - screenY);
        if (pixelDist < GeometryConfig.Snapping.GRID_SNAP_THRESHOLD_PIXELS) {
            double swx = transform.screenToWorldX(snappedSX);
            double swy = transform.screenToWorldY(snappedSY);
            if (Math.abs(swx - Math.round(swx)) < 1e-9) swx = Math.round(swx);
            if (Math.abs(swy - Math.round(swy)) < 1e-9) swy = Math.round(swy);
            return new double[]{swx, swy};
        }
        return null;
    }

    @Override
    public int getPriority() {
        return 10; // 低优先级
    }

    @Override
    public String getName() {
        return "GridSnap";
    }
}
