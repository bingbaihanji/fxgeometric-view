package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.AxisArrowType;
import com.bingbaihanji.constant.AxisTickStyle;
import com.bingbaihanji.constant.UnitLabelType;
import com.bingbaihanji.util.*;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

/**
 * 增强的世界坐标轴绘制器
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 支持多种箭头类型、主次刻度、π单位、自定义颜色和线型
 */
public class AxesPainter implements WorldPainter {

    private static final double EDGE_THRESHOLD = 30; // 边缘绘制阈值(像素)
    private static final double MINOR_TICK_COUNT = 5; // 次刻度数量(主刻度间的分割数)

    private EuclidianViewSettings settings;
    private Boolean showCartesianCoordinateAxis = true;

    /**
     * 旧版构造函数(向后兼容)
     */
    public AxesPainter(Boolean showCartesianCoordinateAxis) {
        this.showCartesianCoordinateAxis = showCartesianCoordinateAxis;
        this.settings = new EuclidianViewSettings(); // 使用默认设置
    }

    /**
     * 新构造函数(支持配置注入)
     */
    public AxesPainter(Boolean showCartesianCoordinateAxis, EuclidianViewSettings settings) {
        this.showCartesianCoordinateAxis = showCartesianCoordinateAxis;
        this.settings = settings;
    }

    public Boolean getShowCartesianCoordinateAxis() {
        return showCartesianCoordinateAxis;
    }

    public void setShowCartesianCoordinateAxis(Boolean showCartesianCoordinateAxis) {
        this.showCartesianCoordinateAxis = showCartesianCoordinateAxis;
    }

    public void setSettings(EuclidianViewSettings settings) {
        this.settings = settings;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double width,
                      double height) {
        if (!showCartesianCoordinateAxis || settings == null) {
            return;
        }

        // 计算坐标原点在屏幕上的位置
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        // 检查坐标轴是否在可见范围内
        boolean xAxisVisible = y0 >= 0 && y0 <= height && settings.isShowXAxis();
        boolean yAxisVisible = x0 >= 0 && x0 <= width && settings.isShowYAxis();

        // 绘制主要坐标轴
        drawMainAxes(gc, transform, width, height, x0, y0, xAxisVisible, yAxisVisible);

        // 绘制边界坐标轴(如果主坐标轴不可见)
        drawBoundaryAxes(gc, transform, width, height, x0, y0, xAxisVisible, yAxisVisible);

        // 绘制坐标轴刻度标记
        if (settings.isShowAxesNumbers()) {
            drawAxisTicks(gc, transform, width, height, xAxisVisible, yAxisVisible);
        }
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

        // 设置坐标轴样式
        gc.setStroke(settings.getAxesColor());
        gc.setFill(settings.getAxesColor());
        gc.setLineWidth(1.8);

        // 应用线型
        LineStyleUtil.applyLineStyle(gc, settings.getAxesLineType());

        // 绘制X轴(仅在可见范围内绘制)
        if (xAxisVisible) {
            // 绘制水平轴线
            gc.strokeLine(0, y0, width, y0);
            // 绘制X轴箭头(根据配置)
            drawArrowByType(gc, settings.getXArrowType(), width - 10, y0, width, y0, true);
            // 绘制文字(X轴)
            drawAxisLabel(gc, getLabelName("axis.xAxis"), width - 25, y0, width, height, true);
        }

        // 绘制Y轴(仅在可见范围内绘制)
        if (yAxisVisible) {
            // 绘制垂直轴线
            gc.strokeLine(x0, 0, x0, height);
            // 绘制Y轴箭头(根据配置)
            drawArrowByType(gc, settings.getYArrowType(), x0, 10, x0, 0, false);
            // 绘制文字(Y轴)
            drawAxisLabel(gc, getLabelName("axis.yAxis"), x0, 20, width, height, false);
        }

        // 恢复实线样式
        LineStyleUtil.resetLineStyle(gc);
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

        // 设置边界坐标轴样式(蓝色)
        gc.setStroke(StyleManager.BOUNDARY_AXES_COLOR);
        gc.setFill(StyleManager.BOUNDARY_AXES_COLOR);
        gc.setLineWidth(1.5);

        // 绘制虚线样式
        gc.setLineDashes(5, 3);

        // 检查X轴是否接近边界(但不可见)
        if (!xAxisVisible && settings.isShowXAxis() && isAxisNearBoundary(y0, height)) {
            // 确定在哪个边界绘制
            double boundaryY = getBoundaryPosition(y0, height);

            // 绘制边界X轴
            gc.strokeLine(0, boundaryY, width, boundaryY);
            // 绘制箭头
            drawArrow(gc, width - 10, boundaryY, width, boundaryY);
            // 绘制文字(带边界指示)
            drawBoundaryLabel(gc, getLabelName("axis.xAxis"), width - 25, boundaryY,
                    y0 < 0 ? getLabelName("axis.upperBorder") : getLabelName("axis.lowerBorder"), width, height, true);
        }

        // 检查Y轴是否接近边界(但不可见)
        if (!yAxisVisible && settings.isShowYAxis() && isAxisNearBoundary(x0, width)) {
            // 确定在哪个边界绘制
            double boundaryX = getBoundaryPosition(x0, width);

            // 绘制边界Y轴
            gc.strokeLine(boundaryX, 0, boundaryX, height);
            // 绘制箭头
            drawArrow(gc, boundaryX, 10, boundaryX, 0);
            // 绘制文字(带边界指示)
            drawBoundaryLabel(gc, getLabelName("axis.yAxis"), boundaryX, 20,
                    x0 < 0 ? getLabelName("axis.leftBorder") : getLabelName("axis.rightBorder"), width, height, false);
        }

        // 恢复实线样式
        gc.setLineDashes(null);
    }

    /**
     * 绘制坐标轴刻度线及数值标签(支持主次刻度)
     * 使用统一的AxisTickCalculator计算刻度,确保与网格同步
     */
    private void drawAxisTicks(GraphicsContext gc,
                               WorldTransform transform,
                               double width,
                               double height,
                               boolean xAxisVisible, boolean yAxisVisible) {
        // 使用统一的计算器计算刻度间隔
        double xStep = settings.isAutoXTickDistance() ?
                AxisTickCalculator.calculateAxisTickDistance(
                        transform.getScaleX(),
                        settings.isXAxisPiUnit()
                ) :
                settings.getXTickDistance();

        double yStep = settings.isAutoYTickDistance() ?
                AxisTickCalculator.calculateAxisTickDistance(
                        transform.getScaleY(),
                        settings.isYAxisPiUnit()
                ) :
                settings.getYTickDistance();

        // 更新settings中的实际刻度距离(供GridPainter使用)
        // 这确保了网格和坐标轴使用相同的刻度值
        if (settings.isAutoXTickDistance()) {
            settings.setXTickDistance(xStep);
        }
        if (settings.isAutoYTickDistance()) {
            settings.setYTickDistance(yStep);
        }

        // 计算当前可视区域在世界坐标系中的边界
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        // 设置刻度线和文字的样式(使用主坐标轴颜色)
        gc.setStroke(settings.getAxesColor());
        gc.setFill(settings.getAxesColor());
        gc.setLineWidth(2);
        gc.setFont(Font.font(15));

        // 绘制X轴刻度
        double y0 = transform.worldToScreenY(0);
        if ((xAxisVisible || isAxisNearBoundary(y0, height)) && settings.getXTickStyle() != AxisTickStyle.NONE) {
            double tickY = xAxisVisible ? y0 : getBoundaryPosition(y0, height);
            drawXAxisTicks(gc, transform, worldLeft, worldRight, xStep, tickY);
        }

        // 绘制Y轴刻度
        double x0 = transform.worldToScreenX(0);
        if ((yAxisVisible || isAxisNearBoundary(x0, width)) && settings.getYTickStyle() != AxisTickStyle.NONE) {
            double tickX = yAxisVisible ? x0 : getBoundaryPosition(x0, width);
            drawYAxisTicks(gc, transform, worldBottom, worldTop, yStep, tickX);
        }
    }

    /**
     * 绘制X轴刻度(主刻度 + 次刻度)
     */
    private void drawXAxisTicks(GraphicsContext gc, WorldTransform transform,
                                double worldLeft, double worldRight,
                                double step, double tickY) {
        boolean drawMinor = settings.getXTickStyle() == AxisTickStyle.MAJOR_MINOR;

        for (double x = Math.floor(worldLeft / step) * step; x <= worldRight; x += step) {
            if (MathCalculationUtils.isZero(x, 1e-8)) continue; // 跳过原点

            double sx = transform.worldToScreenX(x);

            // 绘制主刻度线(长度8)
            gc.setLineWidth(2);
            gc.strokeLine(sx, tickY - 4, sx, tickY + 4);

            // 绘制刻度数值标签(π单位模式下,X轴用π,Y轴用数值)
            UnitLabelType xAxisUnitType = settings.getUnitLabelType();
            gc.fillText(formatNumber(x, xAxisUnitType, step), sx + 2, tickY - 6);

            // 绘制次刻度
            if (drawMinor) {
                drawMinorTicks(gc, transform, x, step, tickY, true);
            }
        }
    }

    /**
     * 绘制Y轴刻度(主刻度 + 次刻度)
     */
    private void drawYAxisTicks(GraphicsContext gc, WorldTransform transform,
                                double worldBottom, double worldTop,
                                double step, double tickX) {
        boolean drawMinor = settings.getYTickStyle() == AxisTickStyle.MAJOR_MINOR;

        for (double y = Math.floor(worldBottom / step) * step; y <= worldTop; y += step) {
            if (MathCalculationUtils.isZero(y, 1e-8)) continue; // 跳过原点

            double sy = transform.worldToScreenY(y);

            // 绘制主刻度线(长度8)
            gc.setLineWidth(2);
            gc.strokeLine(tickX - 4, sy, tickX + 4, sy);

            // 绘制刻度数值标签(π单位模式下,Y轴始终用数值)
            UnitLabelType yAxisUnitType = settings.getUnitLabelType() == UnitLabelType.PI
                    ? UnitLabelType.NUMERIC
                    : settings.getUnitLabelType();
            gc.fillText(formatNumber(y, yAxisUnitType, step), tickX + 6, sy + 4);

            // 绘制次刻度
            if (drawMinor) {
                drawMinorTicks(gc, transform, y, step, tickX, false);
            }
        }
    }

    /**
     * 绘制次刻度
     */
    private void drawMinorTicks(GraphicsContext gc, WorldTransform transform,
                                double majorValue, double step, double axisPos, boolean isXAxis) {
        gc.setLineWidth(1);
        double minorStep = step / MINOR_TICK_COUNT;

        for (int i = 1; i < MINOR_TICK_COUNT; i++) {
            double value = majorValue + i * minorStep;

            if (isXAxis) {
                double sx = transform.worldToScreenX(value);
                // 次刻度线(长度4,较短)
                gc.strokeLine(sx, axisPos - 2, sx, axisPos + 2);
            } else {
                double sy = transform.worldToScreenY(value);
                // 次刻度线(长度4,较短)
                gc.strokeLine(axisPos - 2, sy, axisPos + 2, sy);
            }
        }
    }

    /**
     * 根据箭头类型绘制箭头
     */
    private void drawArrowByType(GraphicsContext gc, AxisArrowType type,
                                 double x1, double y1, double x2, double y2, boolean isXAxis) {
        if (type == null || type == AxisArrowType.NONE) {
            return;
        }

        boolean filled = (type == AxisArrowType.ARROW_FILLED || type == AxisArrowType.TWO_ARROWS_FILLED);

        // 绘制主箭头(正向)
        if (filled) {
            drawFilledArrow(gc, x1, y1, x2, y2);
        } else {
            drawArrow(gc, x1, y1, x2, y2);
        }

        // 绘制双箭头的另一侧
        if (type == AxisArrowType.TWO_ARROWS || type == AxisArrowType.TWO_ARROWS_FILLED) {
            if (isXAxis) {
                // X轴：左侧也画箭头
                if (filled) {
                    drawFilledArrow(gc, 10, y2, 0, y2);
                } else {
                    drawArrow(gc, 10, y2, 0, y2);
                }
            } else {
                // Y轴：底部也画箭头
                double height = gc.getCanvas().getHeight();
                if (filled) {
                    drawFilledArrow(gc, x2, height - 10, x2, height);
                } else {
                    drawArrow(gc, x2, height - 10, x2, height);
                }
            }
        }
    }

    /**
     * 绘制空心箭头
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
     * 绘制实心箭头
     */
    private void drawFilledArrow(GraphicsContext gc,
                                 double x1, double y1,
                                 double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLength = 8;
        double arrowAngle = Math.PI / 6;

        double xA = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double yA = y2 - arrowLength * Math.sin(angle - arrowAngle);

        double xB = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double yB = y2 - arrowLength * Math.sin(angle + arrowAngle);

        // 绘制填充三角形
        gc.fillPolygon(
                new double[]{x2, xA, xB},
                new double[]{y2, yA, yB},
                3
        );
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
     * 绘制边界坐标轴标签(带边界指示)
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
     * 检查坐标轴是否在边界附近
     */
    private boolean isAxisNearBoundary(double position, double dimension) {
        return position < -EDGE_THRESHOLD || position > dimension + EDGE_THRESHOLD;
    }

    /**
     * 获取边界位置
     */
    private double getBoundaryPosition(double position, double dimension) {
        // 如果坐标轴在左侧/上方边界外,固定在顶部/左侧边缘
        if (position < 0) {
            return EDGE_THRESHOLD;
        }
        // 如果坐标轴在右侧/下方边界外,固定在底部/右侧边缘
        else {
            return dimension - EDGE_THRESHOLD;
        }
    }

    /**
     * 格式化数字显示(支持π单位)
     *
     * @param step 当前刻度步长，用于判断整条轴是否需要科学计数法
     */
    private String formatNumber(double v, UnitLabelType unitType, double step) {
        if (unitType == UnitLabelType.PI) {
            return formatPiUnit(v);
        } else {
            return formatNumericUnit(v, step);
        }
    }

    /**
     * 格式化数值单位
     * <ul>
     *   <li>步长 >= 10000：科学计数法（缩小，坐标值很大）</li>
     *   <li>步长 < 0.01 且 |v| < 1：科学计数法，精度由步长决定</li>
     *   <li>步长 < 0.01 且 |v| >= 1：固定小数位，避免大数科学计数法精度丢失</li>
     *   <li>其他：整数或保留两位小数</li>
     * </ul>
     */
    private String formatNumericUnit(double v, double step) {
        if (step >= 10000) {
            return String.format("%.1E", v).replaceAll("E([+-])0+(\\d)", "E$1$2");
        }
        if (step < 0.01) {
            // 统一用固定小数位，位数由步长决定，避免格式在边界跳变
            int decimals = Math.min((int) Math.ceil(-Math.log10(step)), 6);
            return String.format("%." + decimals + "f", v);
        }
        if (Math.abs(v - Math.round(v)) < 1e-6) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format("%.2f", v);
    }

    /**
     * 格式化π单位(基本倍数：π/2, π, 2π等)
     */
    private String formatPiUnit(double v) {
        // 将值除以π得到倍数
        double piMultiple = v / Math.PI;

        // 检查是否接近整数或简单分数
        if (MathCalculationUtils.isZero(piMultiple, 1e-6)) {
            return "0";
        }

        // 检查是否接近整数倍
        if (Math.abs(piMultiple - Math.round(piMultiple)) < 1e-4) {
            int multiple = (int) Math.round(piMultiple);
            if (multiple == 1) {
                return "π";
            } else if (multiple == -1) {
                return "-π";
            } else {
                return multiple + "π";
            }
        }

        // 检查是否接近 1/2
        if (MathCalculationUtils.equals(piMultiple, 0.5, 1e-4)) {
            return "π/2";
        } else if (MathCalculationUtils.equals(piMultiple, -0.5, 1e-4)) {
            return "-π/2";
        }

        // 检查是否接近 1/4
        if (MathCalculationUtils.equals(piMultiple, 0.25, 1e-4)) {
            return "π/4";
        } else if (MathCalculationUtils.equals(piMultiple, -0.25, 1e-4)) {
            return "-π/4";
        }

        // 检查是否接近 3/2
        if (MathCalculationUtils.equals(piMultiple, 1.5, 1e-4)) {
            return "3π/2";
        } else if (MathCalculationUtils.equals(piMultiple, -1.5, 1e-4)) {
            return "-3π/2";
        }

        // 其他情况,显示小数形式
        return String.format("%.2fπ", piMultiple);
    }

    private String getLabelName(String i18nKey) {
        return I18nUtil.getString(i18nKey);
    }
}
