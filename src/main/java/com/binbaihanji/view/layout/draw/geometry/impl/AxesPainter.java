package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.util.I18nUtil;
import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 世界坐标轴绘制器
 */
public class AxesPainter implements WorldPainter {

    // 颜色常量
    private static final Color AXES_COLOR = Color.valueOf("#f7a707"); // 主坐标轴颜色
    private static final Color BOUNDARY_AXES_COLOR = Color.valueOf("#4287f5"); // 边界坐标轴颜色
    private static final double EDGE_THRESHOLD = 30; // 边缘绘制阈值（像素）


    private Boolean showCartesianCoordinateAxis;

    public AxesPainter(Boolean showCartesianCoordinateAxis) {
        this.showCartesianCoordinateAxis = showCartesianCoordinateAxis;
    }

    public void setShowCartesianCoordinateAxis(Boolean showCartesianCoordinateAxis) {
        this.showCartesianCoordinateAxis = showCartesianCoordinateAxis;
    }

    public Boolean getShowCartesianCoordinateAxis() {
        return showCartesianCoordinateAxis;
    }


    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double width,
                      double height) {
        if (showCartesianCoordinateAxis == false) {
            return;
        }
        // 计算坐标原点在屏幕上的位置
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        // 检查坐标轴是否在可见范围内
        boolean xAxisVisible = y0 >= 0 && y0 <= height;
        boolean yAxisVisible = x0 >= 0 && x0 <= width;

        // 绘制主要坐标轴
        drawMainAxes(gc, transform, width, height, x0, y0, xAxisVisible, yAxisVisible);

        // 绘制边界坐标轴（如果主坐标轴不可见）
        drawBoundaryAxes(gc, transform, width, height, x0, y0, xAxisVisible, yAxisVisible);

        // 绘制坐标轴刻度标记
        drawAxisTicks(gc, transform, width, height, xAxisVisible, yAxisVisible);
    }

    /**
     * 绘制主要坐标轴
     */
    private void drawMainAxes(GraphicsContext gc,
                              WorldTransform transform,
                              double width,
                              double height,
                              double x0, double y0,
                              boolean xAxisVisible, boolean yAxisVisible) {

        // 设置坐标轴样式（橙色）
        gc.setStroke(AXES_COLOR);
        gc.setFill(AXES_COLOR);
        gc.setLineWidth(1.8);

        // 绘制X轴（仅在可见范围内绘制）
        if (xAxisVisible) {
            // 绘制水平轴线
            gc.strokeLine(0, y0, width, y0);
            // 绘制X轴箭头（右侧）
            drawArrow(gc, width - 10, y0, width, y0);
            // 绘制文字(X轴)
            drawAxisLabel(gc, getLabelName("axis.xAxis"), width - 25, y0, width, height, true);
        }

        // 绘制Y轴（仅在可见范围内绘制）
        if (yAxisVisible) {
            // 绘制垂直轴线
            gc.strokeLine(x0, 0, x0, height);
            // 绘制Y轴箭头（上方）
            drawArrow(gc, x0, 10, x0, 0);
            // 绘制文字(Y轴)
            drawAxisLabel(gc, getLabelName("axis.yAxis"), x0, 20, width, height, false);
        }
    }

    /**
     * 绘制边界坐标轴
     */
    private void drawBoundaryAxes(GraphicsContext gc,
                                  WorldTransform transform,
                                  double width,
                                  double height,
                                  double x0, double y0,
                                  boolean xAxisVisible, boolean yAxisVisible) {

        // 设置边界坐标轴样式（蓝色）
        gc.setStroke(BOUNDARY_AXES_COLOR);
        gc.setFill(BOUNDARY_AXES_COLOR);
        gc.setLineWidth(1.5);

        // 绘制虚线样式
        gc.setLineDashes(5, 3);

        // 检查X轴是否接近边界（但不可见）
        if (!xAxisVisible && isAxisNearBoundary(y0, height)) {
            // 确定在哪个边界绘制
            double boundaryY = getBoundaryPosition(y0, height);

            // 绘制边界X轴
            gc.strokeLine(0, boundaryY, width, boundaryY);
            // 绘制箭头
            drawArrow(gc, width - 10, boundaryY, width, boundaryY);
            // 绘制文字（带边界指示）
            drawBoundaryLabel(gc, getLabelName("axis.xAxis"), width - 25, boundaryY,
                    y0 < 0 ? getLabelName("axis.upperBorder") : getLabelName("axis.lowerBorder"), width, height, true);
        }

        // 检查Y轴是否接近边界（但不可见）
        if (!yAxisVisible && isAxisNearBoundary(x0, width)) {
            // 确定在哪个边界绘制
            double boundaryX = getBoundaryPosition(x0, width);

            // 绘制边界Y轴
            gc.strokeLine(boundaryX, 0, boundaryX, height);
            // 绘制箭头
            drawArrow(gc, boundaryX, 10, boundaryX, 0);
            // 绘制文字（带边界指示）
            drawBoundaryLabel(gc, getLabelName("axis.yAxis"), boundaryX, 20,
                    x0 < 0 ? getLabelName("axis.leftBorder") : getLabelName("axis.rightBorder"), width, height, false);
        }

        // 恢复实线样式
        gc.setLineDashes(null);
    }

    /**
     * 检查坐标轴是否在边界附近
     */
    private boolean isAxisNearBoundary(double position, double dimension) {
        return position < -EDGE_THRESHOLD || position > dimension + EDGE_THRESHOLD;
    }

    /**
     * 获取边界位置
     */
    private double getBoundaryPosition(double position, double dimension) {
        // 如果坐标轴在左侧/上方边界外，固定在顶部/左侧边缘
        if (position < 0) {
            return EDGE_THRESHOLD;
        }
        // 如果坐标轴在右侧/下方边界外，固定在底部/右侧边缘
        else {
            return dimension - EDGE_THRESHOLD;
        }
    }

    /**
     * 绘制坐标轴刻度线及数值标签
     */
    private void drawAxisTicks(GraphicsContext gc,
                               WorldTransform transform,
                               double width,
                               double height,
                               boolean xAxisVisible, boolean yAxisVisible) {
        // 根据缩放级别选择合适的刻度间隔
        double step = chooseAxisStep(transform.getScale());

        // 计算当前可视区域在世界坐标系中的边界
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        // 设置刻度线和文字的样式（使用主坐标轴颜色）
        gc.setStroke(AXES_COLOR);
        gc.setFill(AXES_COLOR);
        gc.setLineWidth(2);
        gc.setFont(Font.font(15));

        // 绘制X轴刻度（只在X轴可见或接近边界时绘制）
        double y0 = transform.worldToScreenY(0);
        if (xAxisVisible || isAxisNearBoundary(y0, height)) {
            double tickY = xAxisVisible ? y0 : getBoundaryPosition(y0, height);

            for (double x = Math.floor(worldLeft / step) * step; x <= worldRight; x += step) {
                if (Math.abs(x) < 1e-8) continue;
                double sx = transform.worldToScreenX(x);
                // 绘制短刻度线
                gc.strokeLine(sx, tickY - 4, sx, tickY + 4);
                // 绘制刻度数值标签
                gc.fillText(formatNumber(x), sx + 2, tickY - 6);
            }
        }

        // 绘制Y轴刻度（只在Y轴可见或接近边界时绘制）
        double x0 = transform.worldToScreenX(0);
        if (yAxisVisible || isAxisNearBoundary(x0, width)) {
            double tickX = yAxisVisible ? x0 : getBoundaryPosition(x0, width);

            for (double y = Math.floor(worldBottom / step) * step; y <= worldTop; y += step) {
                if (Math.abs(y) < 1e-8) continue;
                double sy = transform.worldToScreenY(y);
                // 绘制短刻度线
                gc.strokeLine(tickX - 4, sy, tickX + 4, sy);
                // 绘制刻度数值标签
                gc.fillText(formatNumber(y), tickX + 6, sy + 4);
            }
        }
    }

    /**
     * 绘制坐标轴标签
     */
    private void drawAxisLabel(GraphicsContext gc, String text,
                               double x, double y,
                               double width, double height,
                               boolean isXAxis) {
        if (isXAxis) {
            double textY;
            if (y < 15) {
                textY = y + 15;
            } else {
                textY = y - 8;
            }
            gc.fillText(text, x, textY);
        } else {
            double textX;
            if (x > width - 25) {
                textX = x - 25;
            } else {
                textX = x + 6;
            }
            gc.fillText(text, textX, y);
        }
    }

    /**
     * 绘制边界坐标轴标签（带边界指示）
     */
    private void drawBoundaryLabel(GraphicsContext gc, String axisText,
                                   double x, double y,
                                   String boundaryText,
                                   double width, double height,
                                   boolean isXAxis) {
        // 设置更小的字体用于边界指示
        gc.setFont(Font.font(12));

        if (isXAxis) {
            // 绘制主标签
            double textY = (y < 15) ? y + 15 : y - 8;
            gc.fillText(axisText, x, textY);

            // 指定区域绘制边界指示
            gc.fillText("(" + boundaryText + ")", x - 20, textY + 25);
        } else {
            // 绘制主标签
            double textX = (x > width - 45) ? x - 45 : x + 6;
            gc.fillText(axisText, textX, y);

            // 指定区域绘制边界指示
            gc.fillText("(" + boundaryText + ")", textX, y + 45);
        }

        // 恢复原来的字体大小
        gc.setFont(Font.font(15));
    }

    /**
     * 绘制箭头（方法不变）
     */
    private void drawArrow(GraphicsContext gc,
                           double x1, double y1,
                           double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLength = 8;
        double arrowAngle = Math.PI / 6;

        double xA = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double yA = y2 - arrowLength * Math.sin(angle - arrowAngle);

        double xB = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double yB = y2 - arrowLength * Math.sin(angle + arrowAngle);

        gc.strokeLine(x2, y2, xA, yA);
        gc.strokeLine(x2, y2, xB, yB);
    }

    /**
     * 根据缩放比例选择合适的坐标轴刻度步长
     */
    private double chooseAxisStep(double scale) {
        if (scale > 200) return 0.5;
        if (scale > 100) return 1;
        if (scale > 50) return 2;
        if (scale > 25) return 5;
        return 10;
    }

    /**
     * 格式化数字显示
     */
    private String formatNumber(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-6) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format("%.2f", v);
    }

    private String getLabelName(String i18nKey) {
        return I18nUtil.getString(i18nKey);
    }

}