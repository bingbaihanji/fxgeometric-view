package com.bingbaihanji.view.layout.draw.coordinate.render;

import com.bingbaihanji.constant.AxisArrowType;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;

/**
 * 坐标轴线与箭头渲染器
 * <p>
 * 负责绘制 X/Y 轴线（含虚线样式）、5 种箭头类型和边界辅助轴线。
 * 迁移自 AxesPainter.drawMainAxes() 和 drawBoundaryAxes()。
 *
 * @author bingbaihanji
 */
public class AxesLineRenderer {

    /**
     * 边界轴线判定阈值（像素）
     */
    private static final double EDGE_THRESHOLD = 30;

    /**
     * 绘制完整坐标轴（轴线 + 箭头）
     *
     * @param gc        画布上下文
     * @param transform 世界坐标变换
     * @param settings  视图配置
     * @param width     视口宽度
     * @param height    视口高度
     */
    public void paint(GraphicsContext gc, WorldTransform transform,
                      EuclidianViewSettings settings, double width, double height) {
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        boolean xAxisVisible = y0 >= 0 && y0 <= height && settings.isShowXAxis();
        boolean yAxisVisible = x0 >= 0 && x0 <= width && settings.isShowYAxis();

        drawMainAxes(gc, width, height, x0, y0, xAxisVisible, yAxisVisible, settings);
        drawBoundaryAxes(gc, width, height, x0, y0, xAxisVisible, yAxisVisible);
    }

    private void drawMainAxes(GraphicsContext gc, double width, double height,
                              double x0, double y0, boolean xAxisVisible, boolean yAxisVisible,
                              EuclidianViewSettings settings) {
        gc.setStroke(settings.getAxesColor());
        gc.setFill(settings.getAxesColor());
        gc.setLineWidth(1.8);
        LineStyleUtil.applyLineStyle(gc, settings.getAxesLineType());

        if (xAxisVisible) {
            gc.strokeLine(0, y0, width, y0);
            drawArrowByType(gc, settings.getXArrowType(), width - 10, y0, width, y0, true);
        }

        if (yAxisVisible) {
            gc.strokeLine(x0, 0, x0, height);
            drawArrowByType(gc, settings.getYArrowType(), x0, 10, x0, 0, false);
        }

        LineStyleUtil.resetLineStyle(gc);
    }

    private void drawBoundaryAxes(GraphicsContext gc, double width, double height,
                                  double x0, double y0, boolean xAxisVisible, boolean yAxisVisible) {
        gc.setStroke(StyleManager.BOUNDARY_AXES_COLOR);
        gc.setFill(StyleManager.BOUNDARY_AXES_COLOR);
        gc.setLineWidth(1.5);
        gc.setLineDashes(5, 3);

        if (!xAxisVisible && isNearBoundary(y0, height)) {
            double boundaryY = getBoundaryPos(y0, height);
            gc.strokeLine(0, boundaryY, width, boundaryY);
            drawArrow(gc, width - 10, boundaryY, width, boundaryY);
        }

        if (!yAxisVisible && isNearBoundary(x0, width)) {
            double boundaryX = getBoundaryPos(x0, width);
            gc.strokeLine(boundaryX, 0, boundaryX, height);
            drawArrow(gc, boundaryX, 10, boundaryX, 0);
        }

        gc.setLineDashes(null);
    }

    private void drawArrowByType(GraphicsContext gc, AxisArrowType type,
                                 double x1, double y1, double x2, double y2, boolean isXAxis) {
        if (type == null || type == AxisArrowType.NONE) return;

        boolean filled = (type == AxisArrowType.ARROW_FILLED || type == AxisArrowType.TWO_ARROWS_FILLED);
        if (filled) drawFilledArrow(gc, x1, y1, x2, y2);
        else drawArrow(gc, x1, y1, x2, y2);

        // 双箭头：另一侧也绘制
        if (type == AxisArrowType.TWO_ARROWS || type == AxisArrowType.TWO_ARROWS_FILLED) {
            if (isXAxis) {
                if (filled) drawFilledArrow(gc, 10, y2, 0, y2);
                else drawArrow(gc, 10, y2, 0, y2);
            } else {
                double canvasH = gc.getCanvas().getHeight();
                if (filled) drawFilledArrow(gc, x2, canvasH - 10, x2, canvasH);
                else drawArrow(gc, x2, canvasH - 10, x2, canvasH);
            }
        }
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len = 8;
        double a = Math.PI / 6;
        double xA = x2 - len * Math.cos(angle - a);
        double yA = y2 - len * Math.sin(angle - a);
        double xB = x2 - len * Math.cos(angle + a);
        double yB = y2 - len * Math.sin(angle + a);
        gc.strokeLine(x2, y2, xA, yA);
        gc.strokeLine(x2, y2, xB, yB);
    }

    private void drawFilledArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double len = 8;
        double a = Math.PI / 6;
        double xA = x2 - len * Math.cos(angle - a);
        double yA = y2 - len * Math.sin(angle - a);
        double xB = x2 - len * Math.cos(angle + a);
        double yB = y2 - len * Math.sin(angle + a);
        gc.fillPolygon(new double[]{x2, xA, xB}, new double[]{y2, yA, yB}, 3);
    }

    private boolean isNearBoundary(double pos, double dim) {
        return pos < -EDGE_THRESHOLD || pos > dim + EDGE_THRESHOLD;
    }

    private double getBoundaryPos(double pos, double dim) {
        return (pos < 0) ? EDGE_THRESHOLD : dim - EDGE_THRESHOLD;
    }
}
