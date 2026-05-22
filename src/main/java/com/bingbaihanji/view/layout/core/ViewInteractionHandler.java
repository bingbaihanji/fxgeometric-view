package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.controller.SnapCalculator;

/**
 * 视图交互处理器(协调器)
 * <p>
 * 协调四个子处理器: 缩放、平移、点击、悬停
 */
public class ViewInteractionHandler {

    private final ZoomHandler zoomHandler;
    private final PanHandler panHandler;
    private final ClickHandler clickHandler;
    private final HoverHandler hoverHandler;

    public ViewInteractionHandler(GridChartView view, WorldTransform transform,
                                  SnapCalculator snapCalculator, HoverTooltipManager tooltipManager) {
        this.zoomHandler = new ZoomHandler(view, transform, tooltipManager);
        this.panHandler = new PanHandler(view, transform, tooltipManager);
        this.clickHandler = new ClickHandler(view, transform, snapCalculator);
        this.hoverHandler = new HoverHandler(view, transform, snapCalculator);
    }

    public void install() {
        zoomHandler.install();
        panHandler.install();
        clickHandler.install();
        hoverHandler.install();
    }
}
