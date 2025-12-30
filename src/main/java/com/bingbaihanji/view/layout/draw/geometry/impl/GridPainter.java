package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.GridMode;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldPainter;
import javafx.scene.canvas.GraphicsContext;

/**
 * 世界网格绘制器
 */
public class GridPainter implements WorldPainter {

    private GridMode gridMode;

    public GridPainter(GridMode gridMode) {
        this.gridMode = gridMode;
    }

    public GridMode getGridMode() {
        return gridMode;
    }

    public void setGridMode(GridMode gridMode) {
        this.gridMode = gridMode;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double width,
                      double height) {

        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        double step = chooseStep(transform.getScale());

        if (gridMode == GridMode.DOT) {

            gc.setFill(StyleManager.GRID_DOT_COLOR);

            double startX = Math.floor(worldLeft / step) * step;
            double startY = Math.floor(worldBottom / step) * step;

            for (double x = startX; x <= worldRight; x += step) {
                for (double y = startY; y <= worldTop; y += step) {

                    double sx = transform.worldToScreenX(x);
                    double sy = transform.worldToScreenY(y);

                    gc.fillOval(sx - 1, sy - 1, 2, 2);
                }
            }
        }

        if (gridMode == GridMode.LINE) {

            gc.setStroke(StyleManager.GRID_LINE_COLOR);
            gc.setLineWidth(1);

            double startX = Math.floor(worldLeft / step) * step;
            for (double x = startX; x <= worldRight; x += step) {
                double sx = transform.worldToScreenX(x);
                gc.strokeLine(sx, 0, sx, height);
            }

            double startY = Math.floor(worldBottom / step) * step;
            for (double y = startY; y <= worldTop; y += step) {
                double sy = transform.worldToScreenY(y);
                gc.strokeLine(0, sy, width, sy);
            }
        }
    }

    private double chooseStep(double scale) {
        if (scale > 150) return 0.2;
        if (scale > 80) return 0.5;
        if (scale > 40) return 1;
        if (scale > 20) return 2;
        return 5;
    }
}
