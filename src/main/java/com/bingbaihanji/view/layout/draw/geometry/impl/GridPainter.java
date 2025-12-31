package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.GridMode;
import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldPainter;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 增强的世界网格绘制器
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 支持笛卡尔、极坐标、等距、主次网格等多种网格类型
 */
public class GridPainter implements WorldPainter {

    private static final double MINOR_GRID_COUNT = 5; // 次网格分割数

    private GridMode gridMode; // 旧版兼容
    private EuclidianViewSettings settings; // 新版配置

    /**
     * 旧版构造函数（向后兼容）
     */
    public GridPainter(GridMode gridMode) {
        this.gridMode = gridMode;
        this.settings = new EuclidianViewSettings(); // 使用默认设置
        // 将GridMode转换为GridType
        this.settings.setGridType(GridType.fromGridMode(gridMode));
    }

    /**
     * 新构造函数（支持配置注入）
     */
    public GridPainter(EuclidianViewSettings settings) {
        this.settings = settings;
    }

    public GridMode getGridMode() {
        return gridMode;
    }

    public void setGridMode(GridMode gridMode) {
        this.gridMode = gridMode;
        if (settings != null) {
            settings.setGridType(GridType.fromGridMode(gridMode));
        }
    }

    public void setSettings(EuclidianViewSettings settings) {
        this.settings = settings;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double width,
                      double height) {

        if (settings == null || !settings.isShowGrid()) {
            return;
        }

        GridType gridType = settings.getGridType();

        switch (gridType) {
            case DOT:
                paintDotGrid(gc, transform, width, height);
                break;
            case CARTESIAN:
                paintCartesianGrid(gc, transform, width, height, false);
                break;
            case CARTESIAN_WITH_SUBGRID:
                paintCartesianGrid(gc, transform, width, height, true);
                break;
            case POLAR:
                paintPolarGrid(gc, transform, width, height);
                break;
            case ISOMETRIC:
                paintIsometricGrid(gc, transform, width, height);
                break;
        }
    }

    /**
     * 绘制点状网格
     * 参考 GeoGebra 的模运算对齐原理
     */
    private void paintDotGrid(GraphicsContext gc, WorldTransform transform,
                               double width, double height) {
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        double step = getGridStep(transform.getScale());

        gc.setFill(settings.getGridColor());

        // 使用模运算对齐原点（GeoGebra方式）
        double tickStepX = transform.getScaleX() * step;
        double tickStepY = transform.getScaleY() * step;

        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);

        double startScreenX = xZero % tickStepX;
        double startScreenY = yZero % tickStepY;

        // 遍历屏幕坐标绘制点
        for (double sx = startScreenX; sx <= width; sx += tickStepX) {
            for (double sy = startScreenY; sy <= height; sy += tickStepY) {
                gc.fillOval(sx - 1, sy - 1, 2, 2);
            }
        }
    }

    /**
     * 绘制笛卡尔网格（可选次网格）
     * 参考 GeoGebra 的 DrawGrid.drawCartesianGrid() 实现
     */
    private void paintCartesianGrid(GraphicsContext gc, WorldTransform transform,
                                      double width, double height, boolean withSubGrid) {
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        double step = getGridStep(transform.getScale());

        // 绘制次网格（如果启用）
        if (withSubGrid) {
            paintSubGrid(gc, transform, width, height, worldLeft, worldRight,
                    worldTop, worldBottom, step);
        }

        // 绘制主网格
        gc.setStroke(settings.getGridColor());
        gc.setLineWidth(1);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());

        // 计算像素步长（GeoGebra方式）
        double tickStepX = transform.getScaleX() * step;
        double tickStepY = transform.getScaleY() * step;

        // 原点在屏幕上的位置（关键！）
        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);

        // 垂直线 - 使用模运算相对原点对齐（GeoGebra DrawGrid.java:58-59）
        double startScreenX = xZero % tickStepX;
        for (double sx = startScreenX; sx <= width; sx += tickStepX) {
            gc.strokeLine(sx, 0, sx, height);
        }

        // 水平线 - 使用模运算相对原点对齐
        double startScreenY = yZero % tickStepY;
        for (double sy = startScreenY; sy <= height; sy += tickStepY) {
            gc.strokeLine(0, sy, width, sy);
        }

        LineStyleUtil.resetLineStyle(gc);
    }

    /**
     * 绘制次网格（浅色，细分主网格）
     * 参考 GeoGebra 的次网格实现（5等分）
     */
    private void paintSubGrid(GraphicsContext gc, WorldTransform transform,
                               double width, double height,
                               double worldLeft, double worldRight,
                               double worldTop, double worldBottom,
                               double mainStep) {
        double subStep = mainStep / MINOR_GRID_COUNT;

        gc.setStroke(settings.getSubGridColor());
        gc.setLineWidth(0.5);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());

        // 计算像素步长
        double mainTickStepX = transform.getScaleX() * mainStep;
        double subTickStepX = transform.getScaleX() * subStep;

        double mainTickStepY = transform.getScaleY() * mainStep;
        double subTickStepY = transform.getScaleY() * subStep;

        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);

        // 垂直次网格线 - 使用模运算对齐
        double startScreenX = xZero % subTickStepX;
        for (double sx = startScreenX; sx <= width; sx += subTickStepX) {
            // 跳过主网格线（像素级别判断）
            if (Math.abs((sx - xZero) % mainTickStepX) < 0.5) {
                continue;
            }
            gc.strokeLine(sx, 0, sx, height);
        }

        // 水平次网格线 - 使用模运算对齐
        double startScreenY = yZero % subTickStepY;
        for (double sy = startScreenY; sy <= height; sy += subTickStepY) {
            // 跳过主网格线（像素级别判断）
            if (Math.abs((sy - yZero) % mainTickStepY) < 0.5) {
                continue;
            }
            gc.strokeLine(0, sy, width, sy);
        }

        LineStyleUtil.resetLineStyle(gc);
    }

    /**
     * 绘制极坐标网格（同心圆 + 放射线）
     * 参考 Desktop 的 EuclidianView.java 实现
     */
    private void paintPolarGrid(GraphicsContext gc, WorldTransform transform,
                                 double width, double height) {
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        // 原点在屏幕上的位置
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        gc.setStroke(settings.getGridColor());
        gc.setLineWidth(1);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());

        // 径向步长（世界单位）
        double radialStep = getGridStep(transform.getScale());

        // 绘制同心圆
        double maxRadius = Math.sqrt(width * width + height * height) / transform.getScale();
        for (double r = radialStep; r <= maxRadius; r += radialStep) {
            double screenR = r * transform.getScale();
            gc.strokeOval(x0 - screenR, y0 - screenR, 2 * screenR, 2 * screenR);
        }

        // 绘制放射线（角度步长从配置读取，默认30度）
        double angleStep = settings.getPolarAngleStep(); // 弧度

        int numRays = (int) Math.ceil(2 * Math.PI / angleStep);
        for (int i = 0; i < numRays; i++) {
            double angle = i * angleStep;

            // 计算射线终点（足够远）
            double dx = Math.cos(angle) * maxRadius;
            double dy = Math.sin(angle) * maxRadius;

            double x1 = transform.worldToScreenX(dx);
            double y1 = transform.worldToScreenY(dy);

            gc.strokeLine(x0, y0, x1, y1);
        }

        LineStyleUtil.resetLineStyle(gc);
    }

    /**
     * 绘制等距网格（三角形格子）
     * 参考 Desktop 的 EuclidianView.java 实现（使用√3比例）
     */
    private void paintIsometricGrid(GraphicsContext gc, WorldTransform transform,
                                     double width, double height) {
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);
        double worldTop = transform.screenToWorldY(0);
        double worldBottom = transform.screenToWorldY(height);

        double step = getGridStep(transform.getScale());

        // 等距网格使用 √3 比例
        double tickStepX = transform.getScaleX() * step * Math.sqrt(3.0);
        double tickStepY = transform.getScaleY() * step;

        gc.setStroke(settings.getGridColor());
        gc.setLineWidth(1);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());

        // 原点在屏幕上的位置
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        // 绘制垂直线
        int xCount = (int) Math.ceil(Math.max(Math.abs(worldLeft), Math.abs(worldRight)) / (step * Math.sqrt(3.0)));
        for (int i = -xCount; i <= xCount; i++) {
            double sx = x0 + i * tickStepX;
            gc.strokeLine(sx, 0, sx, height);
        }

        // 绘制斜线（60度和120度）
        // 计算足够的偏移范围以覆盖整个屏幕
        int offsetRange = (int) Math.ceil((width + height) / tickStepX) + xCount;

        // 60度斜线（向右上）
        for (int i = -offsetRange; i <= offsetRange; i++) {
            double sx1 = x0 + i * tickStepX;
            double sy1 = 0;  // 从屏幕顶部开始
            double sx2 = sx1 + height * Math.sqrt(3.0);
            double sy2 = height;  // 到屏幕底部
            gc.strokeLine(sx1, sy1, sx2, sy2);
        }

        // 120度斜线（向左上）
        for (int i = -offsetRange; i <= offsetRange; i++) {
            double sx1 = x0 + i * tickStepX;
            double sy1 = 0;  // 从屏幕顶部开始
            double sx2 = sx1 - height * Math.sqrt(3.0);
            double sy2 = height;  // 到屏幕底部
            gc.strokeLine(sx1, sy1, sx2, sy2);
        }

        LineStyleUtil.resetLineStyle(gc);
    }

    /**
     * 获取网格步长（根据配置或自动计算）
     * 参考 GeoGebra 的 DrawGrid 实现和网格同步机制
     */
    private double getGridStep(double scale) {
        // 如果启用网格与坐标轴同步（推荐模式）
        if (settings.isSyncGridWithAxes() && settings.isAutoGridDistance()) {
            // 计算坐标轴刻度距离（使用统一的算法）
            double axisTickDistance = AxisTickCalculator.calculateAxisTickDistance(
                scale,
                false  // 网格通常不使用π单位
            );

            // 网格距离 = 坐标轴刻度 * 因子（参考GeoGebra的gridDistances计算）
            return AxisTickCalculator.calculateGridDistance(
                axisTickDistance,
                settings.getGridDistanceFactor()
            );
        }

        // 手动模式：使用配置的固定值
        if (!settings.isAutoGridDistance()) {
            return settings.getGridDistance();
        }

        // 独立自动计算模式（降级，保持向后兼容）
        return AxisTickCalculator.calculateAxisTickDistance(scale, false);
    }
}
