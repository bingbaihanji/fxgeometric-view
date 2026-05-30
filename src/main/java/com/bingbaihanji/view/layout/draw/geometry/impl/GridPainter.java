package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.GridMode;
import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldPainter;
import javafx.scene.canvas.GraphicsContext;

/**
 * 世界网格绘制器（路径批处理优化版）
 * <p>
 * 核心优化：将所有网格线汇入单个 beginPath()/stroke() 调用，
 * 而非逐条 strokeLine()，将 GPU 绘制调用从 O(n) 降低到 O(1)。
 * 参考 GeoGebra 的 DrawGrid + GeneralPath 批处理模式。
 *
 * @author bingbaihanji
 * @deprecated 已拆分为 CartesianGridGenerator, PolarGridGenerator, IsometricGridGenerator, DotGridGenerator。
 * 请使用 CoordSystemRenderer 替代。
 */
@Deprecated
public class GridPainter implements WorldPainter {

    private static final int SUB_GRID_DIVISIONS = 5;

    private EuclidianViewSettings settings;

    public GridPainter(EuclidianViewSettings settings) {
        this.settings = settings;
    }

    /**
     * 旧版兼容
     */
    public GridPainter(GridMode gridMode) {
        this.settings = new EuclidianViewSettings();
        this.settings.setGridType(GridType.fromGridMode(gridMode));
    }

    public void setSettings(EuclidianViewSettings settings) {
        this.settings = settings;
    }

    /**
     * 旧版兼容：获取当前网格类型对应的 GridMode
     */
    public GridMode getGridMode() {
        return settings.getGridType().toGridMode();
    }

    /**
     * 旧版兼容：设置网格模式（自动转换为新 GridType）
     */
    public void setGridMode(GridMode gridMode) {
        if (settings != null) {
            settings.setGridType(GridType.fromGridMode(gridMode));
        }
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform,
                      double width, double height) {
        if (settings == null || !settings.isShowGrid()) {
            return;
        }

        switch (settings.getGridType()) {
            case DOT -> paintDotGrid(gc, transform, width, height);
            case CARTESIAN -> paintCartesianGrid(gc, transform, width, height, false);
            case CARTESIAN_WITH_SUBGRID -> paintCartesianGrid(gc, transform, width, height, true);
            case POLAR -> paintPolarGrid(gc, transform, width, height);
            case ISOMETRIC -> paintIsometricGrid(gc, transform, width, height);
        }
    }

    // ==================== 点状网格 ====================

    private void paintDotGrid(GraphicsContext gc, WorldTransform transform,
                              double width, double height) {
        double step = getGridStep(transform.getScale());
        double tickStepX = transform.getScaleX() * step;
        double tickStepY = transform.getScaleY() * step;
        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);
        double startX = xZero % tickStepX;
        double startY = yZero % tickStepY;

        gc.setFill(settings.getGridColor());
        double dotSize = 2;
        for (double sx = startX; sx <= width; sx += tickStepX) {
            for (double sy = startY; sy <= height; sy += tickStepY) {
                gc.fillOval(sx - dotSize / 2, sy - dotSize / 2, dotSize, dotSize);
            }
        }
    }

    // ==================== 笛卡尔网格（路径批处理） ====================

    /**
     * 笛卡尔网格 — 所有主网格线汇入一个路径后一次性描边。
     * 参考 GeoGebra DrawGrid.drawCartesianGrid() 的 startGeneralPath/endAndDrawGeneralPath 模式。
     */
    private void paintCartesianGrid(GraphicsContext gc, WorldTransform transform,
                                    double width, double height, boolean withSubGrid) {
        double step = getGridStep(transform.getScale());
        double tickStepX = transform.getScaleX() * step;
        double tickStepY = transform.getScaleY() * step;
        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);

        // 次网格（先绘制，位于主网格之下）
        if (withSubGrid) {
            paintSubGridBatched(gc, width, height, tickStepX, tickStepY, xZero, yZero);
        }

        // 主网格 — 路径批处理
        gc.setStroke(settings.getGridColor());
        gc.setLineWidth(1);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());
        gc.beginPath();

        double startX = xZero % tickStepX;
        for (double sx = startX; sx <= width; sx += tickStepX) {
            gc.moveTo(sx, 0);
            gc.lineTo(sx, height);
        }

        double startY = yZero % tickStepY;
        for (double sy = startY; sy <= height; sy += tickStepY) {
            gc.moveTo(0, sy);
            gc.lineTo(width, sy);
        }

        gc.stroke();
        LineStyleUtil.resetLineStyle(gc);
    }

    /**
     * 次网格 — 同样使用路径批处理，跳过与主网格重叠的线。
     */
    private void paintSubGridBatched(GraphicsContext gc,
                                     double width, double height,
                                     double mainTickStepX, double mainTickStepY,
                                     double xZero, double yZero) {
        double subStepX = mainTickStepX / SUB_GRID_DIVISIONS;
        double subStepY = mainTickStepY / SUB_GRID_DIVISIONS;

        gc.setStroke(settings.getSubGridColor());
        gc.setLineWidth(0.5);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());
        gc.beginPath();

        double startX = xZero % subStepX;
        for (double sx = startX; sx <= width; sx += subStepX) {
            if (Math.abs((sx - xZero) % mainTickStepX) < 0.5) continue;
            gc.moveTo(sx, 0);
            gc.lineTo(sx, height);
        }

        double startY = yZero % subStepY;
        for (double sy = startY; sy <= height; sy += subStepY) {
            if (Math.abs((sy - yZero) % mainTickStepY) < 0.5) continue;
            gc.moveTo(0, sy);
            gc.lineTo(width, sy);
        }

        gc.stroke();
        LineStyleUtil.resetLineStyle(gc);
    }

    // ==================== 极坐标网格 ====================

    /**
     * 极坐标网格 — 以世界原点(0,0)为圆心的同心圆 + 放射线。
     * 参考 GeoGebra 的 DrawGrid：
     * - 圆心固定在坐标原点
     * - X/Y 不等比例时画椭圆（屏幕椭圆 = 世界正圆）
     * - 放射线按 scaleX/scaleY 分别拉伸
     */
    private void paintPolarGrid(GraphicsContext gc, WorldTransform transform,
                                double width, double height) {
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        double step = getGridStep(transform.getScale());
        double angleStep = settings.getPolarAngleStep();

        // 世界最大半径（覆盖整个屏幕），取 min(scale) 确保四个角都能覆盖
        double maxRadiusWorld = Math.max(
                Math.abs(transform.screenToWorldX(0) - transform.screenToWorldX(width)),
                Math.abs(transform.screenToWorldY(0) - transform.screenToWorldY(height))
        );

        gc.setStroke(settings.getGridColor());
        gc.setLineWidth(1);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());

        // 同心圆 — 路径批处理
        // JavaFX arc(centerX, centerY, radiusX, radiusY, startAngle, length)
        // 参数是圆心坐标 + 半径，不是矩形左上角 + 宽高
        gc.beginPath();
        for (double r = step; r <= maxRadiusWorld; r += step) {
            double srX = r * scaleX;
            double srY = r * scaleY;
            gc.moveTo(x0 + srX, y0);
            gc.arc(x0, y0, srX, srY, 0, 360);
        }
        gc.stroke();

        // 放射线 — 路径批处理
        // 世界角度 θ 在屏幕空间的映射：x = cosθ * scaleX, y = -sinθ * scaleY（屏幕 y 向下）
        gc.beginPath();
        int numRays = (int) Math.ceil(2 * Math.PI / angleStep);
        double screenMaxRadiusX = maxRadiusWorld * scaleX;
        double screenMaxRadiusY = maxRadiusWorld * scaleY;
        for (int i = 0; i < numRays; i++) {
            double angle = i * angleStep;
            double dx = Math.cos(angle) * screenMaxRadiusX;
            double dy = -Math.sin(angle) * screenMaxRadiusY;  // 屏幕 y 向下
            gc.moveTo(x0, y0);
            gc.lineTo(x0 + dx, y0 + dy);
        }
        gc.stroke();

        LineStyleUtil.resetLineStyle(gc);
    }

    // ==================== 等距网格 ====================

    /**
     * 等距网格 — 三组平行线交汇形成等边三角形格子。
     * 参考 GeoGebra 的 DrawGrid.isometricGrid()：
     * - 所有线经过世界原点(0,0)，保证网格顶点与坐标轴同步
     * - 屏幕斜率 = ±√3 * scaleY/scaleX，考虑 X/Y 轴不等比例
     */
    private void paintIsometricGrid(GraphicsContext gc, WorldTransform transform,
                                    double width, double height) {
        double step = getGridStep(transform.getScale());
        double tickStepX = transform.getScaleX() * step * Math.sqrt(3.0);
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);
        double worldLeft = transform.screenToWorldX(0);
        double worldRight = transform.screenToWorldX(width);

        gc.setStroke(settings.getGridColor());
        gc.setLineWidth(1);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());

        int xCount = (int) Math.ceil(Math.max(Math.abs(worldLeft), Math.abs(worldRight)) / (step * Math.sqrt(3.0)));
        int offsetRange = (int) Math.ceil((width + height) / tickStepX) + xCount;

        double sqrt3 = Math.sqrt(3.0);
        // 屏幕斜率：世界 60°/120° 线映射到屏幕时，需考虑 Y 轴反转和 X/Y 不等比例
        double diagSlope = sqrt3 * transform.getScaleY() / transform.getScaleX();

        // 全部三种方向的线汇入一个路径
        gc.beginPath();

        // 垂直线 — 通过世界原点在 x 轴上的投影点
        for (int i = -xCount; i <= xCount; i++) {
            double sx = x0 + i * tickStepX;
            gc.moveTo(sx, 0);
            gc.lineTo(sx, height);
        }

        // 60° 斜线（世界坐标向右上）— 每条线经过 (sx1, y0)，屏幕斜率为 -diagSlope
        // 线方程: y - y0 = -diagSlope * (x - sx1)
        for (int i = -offsetRange; i <= offsetRange; i++) {
            double sx1 = x0 + i * tickStepX;
            double xTop = sx1 + y0 / diagSlope;
            double xBottom = sx1 - (height - y0) / diagSlope;
            gc.moveTo(xTop, 0);
            gc.lineTo(xBottom, height);
        }

        // 120° 斜线（世界坐标向左上）— 每条线经过 (sx1, y0)，屏幕斜率为 +diagSlope
        // 线方程: y - y0 = diagSlope * (x - sx1)
        for (int i = -offsetRange; i <= offsetRange; i++) {
            double sx1 = x0 + i * tickStepX;
            double xTop = sx1 - y0 / diagSlope;
            double xBottom = sx1 + (height - y0) / diagSlope;
            gc.moveTo(xTop, 0);
            gc.lineTo(xBottom, height);
        }

        gc.stroke();
        LineStyleUtil.resetLineStyle(gc);
    }

    // ==================== 网格步长计算 ====================

    /**
     * 获取网格步长（世界单位）。
     * 优先与坐标轴刻度同步，这是 GeoGebra 的核心设计理念。
     */
    private double getGridStep(double scale) {
        if (settings.isSyncGridWithAxes() && settings.isAutoGridDistance()) {
            double axisTickDistance = AxisTickCalculator.calculateAxisTickDistance(scale, false);
            return AxisTickCalculator.calculateGridDistance(axisTickDistance, settings.getGridDistanceFactor());
        }
        if (!settings.isAutoGridDistance()) {
            return settings.getGridDistance();
        }
        return AxisTickCalculator.calculateAxisTickDistance(scale, false);
    }
}
