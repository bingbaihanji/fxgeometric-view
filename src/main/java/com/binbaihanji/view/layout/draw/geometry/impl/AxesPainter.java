package com.binbaihanji.view.layout.draw.geometry.impl;

import com.binbaihanji.view.layout.core.WorldTransform;
import com.binbaihanji.view.layout.draw.geometry.WorldPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 世界坐标轴绘制器
 */
public class AxesPainter implements WorldPainter {

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double width,
                      double height) {

        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        gc.setStroke(Color.valueOf("#f7a707"));
        gc.setFill(Color.valueOf("#f7a707"));
        gc.setLineWidth(1.8);

        // X 轴
        if (y0 >= 0 && y0 <= height) {
            gc.strokeLine(0, y0, width, y0);
            drawArrow(gc, width - 10, y0, width, y0);
        }

        // Y 轴
        if (x0 >= 0 && x0 <= width) {
            gc.strokeLine(x0, 0, x0, height);
            drawArrow(gc, x0, 10, x0, 0);
        }

        drawAxisTicks(gc, transform, width, height);
    }

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

    private void drawAxisTicks(GraphicsContext gc,
                               WorldTransform transform,
                               double width,
                               double height) {

        double step = chooseAxisStep(transform.getScale());

        double worldLeft   = transform.screenToWorldX(0);
        double worldRight  = transform.screenToWorldX(width);
        double worldTop    = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        gc.setLineWidth(2);
        gc.setFont(Font.font(15));

        double y0 = transform.worldToScreenY(0);
        if (y0 >= 0 && y0 <= height) {
            for (double x = Math.floor(worldLeft / step) * step; x <= worldRight; x += step) {
                if (Math.abs(x) < 1e-8) continue;
                double sx = transform.worldToScreenX(x);
                gc.strokeLine(sx, y0 - 4, sx, y0 + 4);
                gc.fillText(formatNumber(x), sx + 2, y0 - 6);
            }
        }

        double x0 = transform.worldToScreenX(0);
        if (x0 >= 0 && x0 <= width) {
            for (double y = Math.floor(worldBottom / step) * step; y <= worldTop; y += step) {
                if (Math.abs(y) < 1e-8) continue;
                double sy = transform.worldToScreenY(y);
                gc.strokeLine(x0 - 4, sy, x0 + 4, sy);
                gc.fillText(formatNumber(y), x0 + 6, sy + 4);
            }
        }
    }

    private double chooseAxisStep(double scale) {
        if (scale > 200) return 0.5;
        if (scale > 100) return 1;
        if (scale > 50)  return 2;
        if (scale > 25)  return 5;
        return 10;
    }

    private String formatNumber(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-6) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format("%.2f", v);
    }
}
