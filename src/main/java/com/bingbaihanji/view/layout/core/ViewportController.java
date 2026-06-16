package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.util.AxisRangeCalculator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 视口控制器
 * <p>
 * 负责视图变换控制：缩放、自适应、重置、坐标轴比例
 */
public class ViewportController {

    private final GridChartView view;
    private final WorldTransform transform;
    private final EuclidianViewSettings settings;

    public ViewportController(GridChartView view, WorldTransform transform, EuclidianViewSettings settings) {
        this.view = view;
        this.transform = transform;
        this.settings = settings;
    }

    /**
     * 缩放到指定百分比
     */
    public void zoomToPercent(double percent) {
        double newScale = AxisRangeCalculator.getScaleFromPercent(transform.getScale(), percent);

        double centerWorldX = transform.screenToWorldX(view.getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(view.getHeight() / 2);

        transform.setScale(newScale);
        settings.silentUpdate(s -> {
            s.setXScale(newScale);
            s.setYScale(newScale);
        });

        transform.centerWorldAt(centerWorldX, centerWorldY, view.getWidth(), view.getHeight());
        view.invalidateBackground();
        view.redraw();
    }

    /**
     * 设置坐标轴比例
     */
    public void setAxisRatio(double xRatio, double yRatio) {
        double centerWorldX = transform.screenToWorldX(view.getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(view.getHeight() / 2);

        transform.setAxisRatio(xRatio, yRatio);
        settings.silentUpdate(s -> {
            s.setXScale(transform.getScaleX());
            s.setYScale(transform.getScaleY());
        });

        transform.centerWorldAtWithScales(centerWorldX, centerWorldY, view.getWidth(), view.getHeight());
        view.invalidateBackground();
        view.redraw();
    }

    /**
     * 显示所有对象(自动调整范围)
     */
    public void fitAllObjects() {
        List<WorldObject> objects = view.getObjects();
        if (objects.isEmpty()) {
            return;
        }

        double[] range = AxisRangeCalculator.fitAllObjects(
                objects, transform, view.getWidth(), view.getHeight()
        );

        double xRange = range[1] - range[0];
        double yRange = range[3] - range[2];

        double newScaleX = view.getWidth() / xRange;
        double newScaleY = view.getHeight() / yRange;

        double newScale = Math.min(newScaleX, newScaleY) * 0.9;

        transform.setScale(newScale);
        settings.silentUpdate(s -> {
            s.setXScale(newScale);
            s.setYScale(newScale);
        });

        double centerX = (range[0] + range[1]) / 2;
        double centerY = (range[2] + range[3]) / 2;

        transform.centerWorldAt(centerX, centerY, view.getWidth(), view.getHeight());
        view.invalidateBackground();
        view.redraw();
    }

    /**
     * 重置到标准视图
     */
    public void resetToStandardView() {
        double standardScale = 50.0;

        transform.setScale(standardScale);
        settings.silentUpdate(s -> {
            s.setXScale(standardScale);
            s.setYScale(standardScale);
        });

        transform.centerWorldAt(0, 0, view.getWidth(), view.getHeight());
        view.invalidateBackground();
        view.redraw();
    }
}
