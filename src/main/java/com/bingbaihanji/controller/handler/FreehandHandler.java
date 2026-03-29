package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.impl.PathGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import com.bingbaihanji.view.layout.draw.tools.FreehandDrawingTool;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 手绘处理器
 * <p>
 * 处理手绘线的绘制，委托给 FreehandDrawingTool
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class FreehandHandler extends AbstractDrawingHandler {

    /**
     * 手绘工具（单例模式，全局共享配置）
     */
    private static final FreehandDrawingTool freehandTool = new FreehandDrawingTool();
    
    /**
     * 获取手绘工具实例
     */
    public static FreehandDrawingTool getFreehandTool() {
        return freehandTool;
    }

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.FREEHAND;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        freehandTool.onMousePressed(context.getGridChartPane(), e);
        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        freehandTool.onMouseDragged(context.getGridChartPane(), e);
        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        freehandTool.onMouseReleased(context.getGridChartPane(), e);

        // 获取手绘路径点并创建 PathGeo 对象
        List<Point2D> points = freehandTool.getPoints();
        // 立即清空路径点，防止预览显示
        freehandTool.clearPoints();

        if (points.size() >= 2) {
            PathGeo newPath = new PathGeo(new ArrayList<>(points));
            // 计算此路径产生的所有交点
            List<PointGeo> intersectionPoints = context.getIntersectionHandler()
                    .checkIntersections(newPath, context);

            context.executeCommand(new CommandHistory.Command() {
                @Override
                public void execute() {
                    context.addObject(newPath);
                    // 添加交点
                    for (PointGeo point : intersectionPoints) {
                        context.addObject(point);
                    }
                }

                @Override
                public void undo() {
                    context.removeObject(newPath);
                    // 移除交点
                    for (PointGeo point : intersectionPoints) {
                        context.removeObject(point);
                    }
                }
            });
        }

        context.redraw();
        e.consume();
        return true;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (canHandle(context.getDrawMode())) {
            freehandTool.paintPreview(gc, transform);
        }
    }

    @Override
    public void reset() {
        freehandTool.clearPoints();
    }
}
