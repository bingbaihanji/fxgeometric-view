package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.controller.SnapCalculator;

/**
 * 视图交互处理器(协调器)
 * <p>
 * 协调四个视口级子处理器: 缩放, 平移, 悬停, 点击涟漪.
 * 点击语义(选中/onClick)属于控制器层, 由 ObjectClickHandler 负责.
 */
public class ViewInteractionHandler {

    private final ZoomHandler zoomHandler;
    private final PanHandler panHandler;
    private final HoverHandler hoverHandler;
    private final RippleHandler rippleHandler;

    public ViewInteractionHandler(GridChartView view, WorldTransform transform,
                                  SnapCalculator snapCalculator, HoverTooltipManager tooltipManager) {
        this.zoomHandler = new ZoomHandler(view, transform, tooltipManager);
        this.panHandler = new PanHandler(view, transform, tooltipManager);
        this.hoverHandler = new HoverHandler(view, transform, snapCalculator);
        this.rippleHandler = new RippleHandler(view);
    }

    public void install() {
        zoomHandler.install();
        panHandler.install();
        hoverHandler.install();
        rippleHandler.install();
    }
}
