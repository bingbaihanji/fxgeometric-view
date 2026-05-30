package com.bingbaihanji.view.layout.draw.coordinate.grid;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 笛卡尔网格生成器
 * <p>
 * 生成垂直和水平网格线，支持主网格和次网格（5等分）。
 * 所有线段均为屏幕坐标，由 WorldTransform 换算。
 * 迁移自 GridPainter.paintCartesianGrid()。
 *
 * @author bingbaihanji
 */
public class CartesianGridGenerator {

    /**
     * 次网格等分数量
     */
    private static final int SUB_GRID_DIVISIONS = 5;

    /**
     * 计算网格步长（世界单位）
     * <p>
     * 优先与坐标轴刻度同步，参考 GeoGebra 的 gridDistances 计算。
     */
    public static double getGridStep(WorldTransform transform, EuclidianViewSettings settings) {
        if (settings.isSyncGridWithAxes() && settings.isAutoGridDistance()) {
            double axisTickDistance = AxisTickCalculator.calculateAxisTickDistance(
                    transform.getScale(), false);
            return AxisTickCalculator.calculateGridDistance(axisTickDistance,
                    settings.getGridDistanceFactor());
        }
        if (!settings.isAutoGridDistance()) {
            return settings.getGridDistance();
        }
        return AxisTickCalculator.calculateAxisTickDistance(transform.getScale(), false);
    }

    /**
     * 生成笛卡尔网格线列表
     *
     * @param transform  世界坐标变换
     * @param settings   视图配置
     * @param viewWidth  视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 网格线段列表（屏幕坐标）
     */
    public List<GridElement> generate(WorldTransform transform, EuclidianViewSettings settings,
                                      double viewWidth, double viewHeight) {
        List<GridElement> elements = new ArrayList<>();
        double step = getGridStep(transform, settings);
        double tickStepX = transform.getScaleX() * step;
        double tickStepY = transform.getScaleY() * step;
        double xZero = transform.worldToScreenX(0);
        double yZero = transform.worldToScreenY(0);

        boolean withSubGrid = (settings.getGridType() == GridType.CARTESIAN_WITH_SUBGRID);

        if (withSubGrid) {
            generateSubGridLines(elements, viewWidth, viewHeight,
                    tickStepX, tickStepY, xZero, yZero);
        }

        double startX = xZero % tickStepX;
        for (double sx = startX; sx <= viewWidth; sx += tickStepX) {
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(sx, 0), new Point2D(sx, viewHeight), false));
        }

        double startY = yZero % tickStepY;
        for (double sy = startY; sy <= viewHeight; sy += tickStepY) {
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(0, sy), new Point2D(viewWidth, sy), false));
        }

        return elements;
    }

    /**
     * 生成次网格线段，跳过与主网格重叠的线
     */
    private void generateSubGridLines(List<GridElement> elements,
                                      double viewWidth, double viewHeight,
                                      double mainStepX, double mainStepY,
                                      double xZero, double yZero) {
        double subStepX = mainStepX / SUB_GRID_DIVISIONS;
        double subStepY = mainStepY / SUB_GRID_DIVISIONS;

        double startX = xZero % subStepX;
        for (double sx = startX; sx <= viewWidth; sx += subStepX) {
            if (Math.abs((sx - xZero) % mainStepX) < 0.5) continue;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(sx, 0), new Point2D(sx, viewHeight), true));
        }

        double startY = yZero % subStepY;
        for (double sy = startY; sy <= viewHeight; sy += subStepY) {
            if (Math.abs((sy - yZero) % mainStepY) < 0.5) continue;
            elements.add(new GridElement.GridLineSegment(
                    new Point2D(0, sy), new Point2D(viewHeight, sy), true));
        }
    }
}
