package com.bingbaihanji.util;

import com.bingbaihanji.constant.FillType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 填充渲染工具类
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 提供各种填充模式的绘制功能
 */
public class FillRenderer {

    /**
     * 应用填充到圆形
     *
     * @param gc          GraphicsContext对象
     * @param fillType    填充类型
     * @param fillColor   填充颜色
     * @param fillOpacity 填充透明度
     * @param angle       填充角度（度）
     * @param distance    填充间距（像素）
     * @param centerX     圆心X（屏幕坐标）
     * @param centerY     圆心Y（屏幕坐标）
     * @param radius      半径（屏幕坐标）
     */
    public static void fillOval(GraphicsContext gc, FillType fillType, Color fillColor,
                                double fillOpacity, int angle, int distance,
                                double centerX, double centerY, double radius) {
        if (fillType == null || fillType == FillType.NONE) {
            return;
        }

        // 计算实际填充颜色（带透明度）
        Color effectiveFillColor = new Color(
                fillColor.getRed(),
                fillColor.getGreen(),
                fillColor.getBlue(),
                fillOpacity
        );

        if (fillType == FillType.STANDARD) {
            // 标准填充：纯色填充
            gc.setFill(effectiveFillColor);
            gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        } else {
            // 其他填充类型：使用图案填充
            // 先填充背景（可选）
            // gc.setFill(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), fillOpacity * 0.2));
            // gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // 然后绘制填充图案
            gc.save();
            // 设置裁剪区域为圆形
            gc.beginPath();
            gc.arc(centerX, centerY, radius, radius, 0, 360);
            gc.closePath();
            gc.clip();

            // 绘制填充图案
            drawFillPattern(gc, fillType, effectiveFillColor, angle, distance,
                    centerX - radius, centerY - radius, radius * 2, radius * 2);

            gc.restore();
        }
    }

    /**
     * 应用填充到多边形
     *
     * @param gc          GraphicsContext对象
     * @param fillType    填充类型
     * @param fillColor   填充颜色
     * @param fillOpacity 填充透明度
     * @param angle       填充角度（度）
     * @param distance    填充间距（像素）
     * @param xPoints     X坐标数组（屏幕坐标）
     * @param yPoints     Y坐标数组（屏幕坐标）
     * @param nPoints     点的数量
     */
    public static void fillPolygon(GraphicsContext gc, FillType fillType, Color fillColor,
                                   double fillOpacity, int angle, int distance,
                                   double[] xPoints, double[] yPoints, int nPoints) {
        if (fillType == null || fillType == FillType.NONE || nPoints < 3) {
            return;
        }

        // 计算实际填充颜色（带透明度）
        Color effectiveFillColor = new Color(
                fillColor.getRed(),
                fillColor.getGreen(),
                fillColor.getBlue(),
                fillOpacity
        );

        if (fillType == FillType.STANDARD) {
            // 标准填充：纯色填充
            gc.setFill(effectiveFillColor);
            gc.fillPolygon(xPoints, yPoints, nPoints);
        } else {
            // 其他填充类型：使用图案填充
            gc.save();

            // 设置裁剪区域为多边形
            gc.beginPath();
            gc.moveTo(xPoints[0], yPoints[0]);
            for (int i = 1; i < nPoints; i++) {
                gc.lineTo(xPoints[i], yPoints[i]);
            }
            gc.closePath();
            gc.clip();

            // 计算边界框
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
            for (int i = 0; i < nPoints; i++) {
                minX = Math.min(minX, xPoints[i]);
                maxX = Math.max(maxX, xPoints[i]);
                minY = Math.min(minY, yPoints[i]);
                maxY = Math.max(maxY, yPoints[i]);
            }

            // 绘制填充图案
            drawFillPattern(gc, fillType, effectiveFillColor, angle, distance,
                    minX, minY, maxX - minX, maxY - minY);

            gc.restore();
        }
    }

    /**
     * 绘制填充图案
     *
     * @param gc       GraphicsContext对象
     * @param fillType 填充类型
     * @param color    填充颜色
     * @param angle    填充角度（度）
     * @param distance 填充间距（像素）
     * @param x        区域X（屏幕坐标）
     * @param y        区域Y（屏幕坐标）
     * @param width    区域宽度
     * @param height   区域高度
     */
    private static void drawFillPattern(GraphicsContext gc, FillType fillType, Color color,
                                        int angle, int distance, double x, double y,
                                        double width, double height) {
        gc.setStroke(color);
        gc.setLineWidth(1);

        switch (fillType) {
            case HATCH_HORIZONTAL -> drawHorizontalHatch(gc, distance, x, y, width, height);
            case HATCH_VERTICAL -> drawVerticalHatch(gc, distance, x, y, width, height);
            case HATCH_DIAGONAL -> drawDiagonalHatch(gc, distance, x, y, width, height, 45);
            case HATCH_CROSS_DIAGONAL -> {
                drawDiagonalHatch(gc, distance, x, y, width, height, 45);
                drawDiagonalHatch(gc, distance, x, y, width, height, -45);
            }
            case HATCH_GRID -> {
                drawHorizontalHatch(gc, distance, x, y, width, height);
                drawVerticalHatch(gc, distance, x, y, width, height);
            }
            case DOTTED -> drawDottedPattern(gc, distance, x, y, width, height);
            case HONEYCOMB -> drawHoneycombPattern(gc, distance, x, y, width, height);
            case BRICK -> drawBrickPattern(gc, distance, x, y, width, height);
            case WEAVING -> drawWeavingPattern(gc, distance, x, y, width, height);
            case SYMBOLS -> drawSymbolsPattern(gc, distance, x, y, width, height);
            default -> {
                // 默认使用标准填充
                gc.setFill(color);
                gc.fillRect(x, y, width, height);
            }
        }
    }

    /**
     * 绘制水平斜线填充
     */
    private static void drawHorizontalHatch(GraphicsContext gc, int distance,
                                            double x, double y, double width, double height) {
        for (double iy = y; iy <= y + height; iy += distance) {
            gc.strokeLine(x, iy, x + width, iy);
        }
    }

    /**
     * 绘制垂直斜线填充
     */
    private static void drawVerticalHatch(GraphicsContext gc, int distance,
                                          double x, double y, double width, double height) {
        for (double ix = x; ix <= x + width; ix += distance) {
            gc.strokeLine(ix, y, ix, y + height);
        }
    }

    /**
     * 绘制对角线填充
     *
     * @param angleDeg 角度（度，45或-45）
     */
    private static void drawDiagonalHatch(GraphicsContext gc, int distance,
                                          double x, double y, double width, double height, double angleDeg) {
        double maxDim = Math.max(width, height);
        double spacing = distance;

        if (angleDeg == 45) {
            // 从左下到右上
            for (double offset = -maxDim; offset <= maxDim + width; offset += spacing) {
                double x1 = x + offset;
                double y1 = y + height;
                double x2 = x + offset + height;
                double y2 = y;

                gc.strokeLine(x1, y1, x2, y2);
            }
        } else {
            // 从左上到右下
            for (double offset = -maxDim; offset <= maxDim + width; offset += spacing) {
                double x1 = x + offset;
                double y1 = y;
                double x2 = x + offset + height;
                double y2 = y + height;

                gc.strokeLine(x1, y1, x2, y2);
            }
        }
    }

    /**
     * 绘制点状填充
     */
    private static void drawDottedPattern(GraphicsContext gc, int distance,
                                          double x, double y, double width, double height) {
        gc.setFill(gc.getStroke());
        for (double iy = y; iy <= y + height; iy += distance) {
            for (double ix = x; ix <= x + width; ix += distance) {
                gc.fillOval(ix - 1, iy - 1, 2, 2);
            }
        }
    }

    /**
     * 绘制蜂窝状填充
     */
    private static void drawHoneycombPattern(GraphicsContext gc, int distance,
                                             double x, double y, double width, double height) {
        double hexSize = distance * 0.8;
        double hexHeight = hexSize * Math.sqrt(3) / 2;

        for (double row = 0; row * hexHeight <= height + hexHeight; row++) {
            double offsetX = (row % 2 == 0) ? 0 : hexSize * 1.5;
            for (double col = 0; col * hexSize * 3 <= width + hexSize * 3; col++) {
                double cx = x + offsetX + col * hexSize * 3;
                double cy = y + row * hexHeight;
                drawHexagon(gc, cx, cy, hexSize * 0.5);
            }
        }
    }

    /**
     * 绘制六边形
     */
    private static void drawHexagon(GraphicsContext gc, double cx, double cy, double radius) {
        double[] xPoints = new double[6];
        double[] yPoints = new double[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i;
            xPoints[i] = cx + radius * Math.cos(angle);
            yPoints[i] = cy + radius * Math.sin(angle);
        }
        gc.strokePolygon(xPoints, yPoints, 6);
    }

    /**
     * 绘制砖块状填充
     */
    private static void drawBrickPattern(GraphicsContext gc, int distance,
                                         double x, double y, double width, double height) {
        double brickWidth = distance * 2;
        double brickHeight = distance;

        for (double row = 0; row * brickHeight <= height; row++) {
            double offsetX = (row % 2 == 0) ? 0 : brickWidth / 2;
            for (double col = 0; col * brickWidth <= width + brickWidth; col++) {
                double bx = x + offsetX + col * brickWidth;
                double by = y + row * brickHeight;
                gc.strokeRect(bx, by, brickWidth, brickHeight);
            }
        }
    }

    /**
     * 绘制编织状填充
     */
    private static void drawWeavingPattern(GraphicsContext gc, int distance,
                                           double x, double y, double width, double height) {
        // 交替绘制水平和垂直的短线段
        for (double iy = y; iy <= y + height; iy += distance) {
            for (double ix = x; ix <= x + width; ix += distance * 2) {
                if (((int) ((iy - y) / distance)) % 2 == 0) {
                    gc.strokeLine(ix, iy, ix + distance, iy);
                } else {
                    gc.strokeLine(ix + distance / 2, iy - distance / 2,
                            ix + distance / 2, iy + distance / 2);
                }
            }
        }
    }

    /**
     * 绘制符号填充（小十字）
     */
    private static void drawSymbolsPattern(GraphicsContext gc, int distance,
                                           double x, double y, double width, double height) {
        double crossSize = distance * 0.3;
        for (double iy = y; iy <= y + height; iy += distance) {
            for (double ix = x; ix <= x + width; ix += distance) {
                // 绘制小十字
                gc.strokeLine(ix - crossSize, iy, ix + crossSize, iy);
                gc.strokeLine(ix, iy - crossSize, ix, iy + crossSize);
            }
        }
    }
}
