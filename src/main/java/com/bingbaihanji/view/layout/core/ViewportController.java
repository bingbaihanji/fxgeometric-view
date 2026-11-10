package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.util.AxisRangeCalculator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 视口控制器
 * <p>
 * 负责视图变换控制：缩放、自适应、重置、坐标轴比例。
 * 除鼠标滚轮缩放外，其余操作都以 400ms 缓动动画过渡，避免视图瞬变。
 */
public class ViewportController {

    private final GridChartView view;
    private final WorldTransform transform;
    private final EuclidianViewSettings settings;
    private final ZoomAnimation zoomAnimation = new ZoomAnimation();

    public ViewportController(GridChartView view, WorldTransform transform, EuclidianViewSettings settings) {
        this.view = view;
        this.transform = transform;
        this.settings = settings;
    }

    /**
     * 停止正在进行的缩放动画
     * <p>
     * 用户开始滚轮缩放或拖拽平移时调用，避免动画与手动操作互相拉扯
     */
    public void stopZoomAnimation() {
        zoomAnimation.stop();
    }

    /**
     * 以缓动动画把视图过渡到目标变换
     *
     * @param target 目标变换
     */
    private void animateTo(WorldTransform target) {
        zoomAnimation.animate(
                ZoomAnimation.TransformSnapshot.from(transform),
                ZoomAnimation.TransformSnapshot.from(target),
                // 每帧把插值结果写回真实变换并重绘
                interp -> {
                    transform.setOffset(interp.getOffsetX(), interp.getOffsetY());
                    transform.setScaleX(interp.getScaleX());
                    transform.setScaleY(interp.getScaleY());
                    view.invalidateBackground();
                    view.redraw();
                },
                // 动画结束后把最终比例写回设置
                () -> settings.silentUpdate(s -> {
                    s.setXScale(transform.getScaleX());
                    s.setYScale(transform.getScaleY());
                })
        );
    }

    /**
     * 缩放到指定百分比
     */
    public void zoomToPercent(double percent) {
        double newScale = AxisRangeCalculator.getScaleFromPercent(transform.getScale(), percent);

        double centerWorldX = transform.screenToWorldX(view.getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(view.getHeight() / 2);

        WorldTransform target = new WorldTransform();
        target.setScaleX(newScale);
        target.setScaleY(newScale);
        target.centerWorldAt(centerWorldX, centerWorldY, view.getWidth(), view.getHeight());

        animateTo(target);
    }

    /**
     * 设置坐标轴比例
     */
    public void setAxisRatio(double xRatio, double yRatio) {
        double centerWorldX = transform.screenToWorldX(view.getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(view.getHeight() / 2);

        WorldTransform target = new WorldTransform();
        target.setScaleX(transform.getScaleX());
        target.setScaleY(transform.getScaleY());
        target.setAxisRatio(xRatio, yRatio);
        target.centerWorldAtWithScales(centerWorldX, centerWorldY, view.getWidth(), view.getHeight());

        animateTo(target);
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

        WorldTransform target = new WorldTransform();
        target.setScaleX(newScale);
        target.setScaleY(newScale);
        target.centerWorldAt((range[0] + range[1]) / 2, (range[2] + range[3]) / 2,
                view.getWidth(), view.getHeight());

        animateTo(target);
    }

    /**
     * 重置到标准视图
     */
    public void resetToStandardView() {
        double standardScale = 50.0;

        WorldTransform target = new WorldTransform();
        target.setScaleX(standardScale);
        target.setScaleY(standardScale);
        target.centerWorldAt(0, 0, view.getWidth(), view.getHeight());

        animateTo(target);
    }
}
