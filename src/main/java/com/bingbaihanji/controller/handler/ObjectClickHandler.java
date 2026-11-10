package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.geometry.WorldTransform;
import com.bingbaihanji.geometry.WorldObject;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 对象点击处理器
 * <p>
 * 非绘制模式下的点击语义: 吸附计算 -> 命中测试 -> 分派 onClick 或记录坐标.
 * 绘制模式由 canHandle 天然隔离, 因此不再需要视图层的模式标志.
 *
 * @author bingbaihanji
 * @date 2026-11-10
 */
public class ObjectClickHandler extends AbstractDrawingHandler {

    private static final Logger log = LoggerFactory.getLogger(ObjectClickHandler.class);

    @Override
    public boolean canHandle(DrawMode mode) {
        // 只在非绘制模式下接管点击语义
        return mode == DrawMode.NONE;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, IDrawingContext context) {
        GridChartView view = context.getGridChartPane();
        WorldTransform transform = context.getTransform();
        SnapCalculator snapCalculator = context.getSnapCalculator();

        // 屏幕坐标转世界坐标
        double worldX = view.screenToWorldX(e.getX());
        double worldY = view.screenToWorldY(e.getY());

        // 吸附计算并把吸附提示状态写回视图
        SnapCalculator.SnapResult snapped = snapCalculator.calculate(worldX, worldY, view.getObjects());
        view.setNearbySpecialPoint(snapped.snappedPoint);
        view.setAxisSnapInfo(snapped.axisSnapInfo);
        worldX = snapped.x;
        worldY = snapped.y;

        // 由上到下做命中测试, 命中最上层对象即分派
        double tolerance = GeometryConfig.Tolerance.HIT_TEST_PIXELS / transform.getScale();
        List<WorldObject> objects = view.getObjects();
        for (int i = objects.size() - 1; i >= 0; i--) {
            WorldObject obj = objects.get(i);
            if (obj.hitTest(worldX, worldY, tolerance)) {
                obj.onClick(worldX, worldY);
                context.redraw();
                return true;
            }
        }

        // 未命中任何对象时记录坐标
        log.info("point(x = {}, y = {})", String.format("%.2f", worldX), String.format("%.2f", worldY));
        return false;
    }
}
