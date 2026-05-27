package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.util.EdgeSnapManager;
import com.bingbaihanji.util.SpecialPointManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.CartesianGridGenerator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 磁性吸附处理器
 * <p>
 * 处理特殊点、几何图形边和网格点的磁性吸附功能
 * - 点吸附：阈值为 15 像素(优先级高)
 * - 边吸附：阈值为 10 像素(优先级中)
 * - 网格吸附：阈值为 8 像素(优先级低)
 * <p>
 * 优化：使用缓存机制避免重复计算特殊点,提高拖动性能
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class SnappingHandler {

    /**
     * 点吸附阈值(像素)
     */
    private static final double POINT_SNAP_THRESHOLD_PIXELS = 15.0;

    /**
     * 边吸附阈值(像素)- 比点吸附弱一点
     */
    private static final double EDGE_SNAP_THRESHOLD_PIXELS = 10.0;

    /**
     * 网格吸附阈值(像素)
     */
    private static final double GRID_SNAP_THRESHOLD_PIXELS = 8.0;

    /**
     * 特殊点缓存
     */
    private List<SpecialPoint> cachedSpecialPoints = null;

    /**
     * 缓存是否有效
     */
    private boolean cacheValid = false;

    /**
     * 使缓存失效(当几何对象发生变化时调用)
     */
    public void invalidateCache() {
        cacheValid = false;
        cachedSpecialPoints = null;
    }

    /**
     * 查找最近的特殊点(带缓存优化)
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 最近的特殊点,如果在阈值范围内没有则返回 null
     */
    public SpecialPoint findNearestSpecialPoint(double x, double y, IDrawingContext context) {
        return findNearestSpecialPoint(x, y, context, null);
    }

    /**
     * 查找最近的特殊点(带排除对象)
     *
     * @param x              世界坐标 X
     * @param y              世界坐标 Y
     * @param context        绘制上下文
     * @param excludedObject 要排除的对象(通常是正在拖动的对象)
     * @return 最近的特殊点,如果在阈值范围内没有则返回 null
     */
    public SpecialPoint findNearestSpecialPoint(double x, double y, IDrawingContext context, WorldObject excludedObject) {
        // 如果缓存无效,重新提取特殊点
        if (!cacheValid || cachedSpecialPoints == null) {
            cachedSpecialPoints = SpecialPointManager.extractSpecialPoints(context.getObjects());
            cacheValid = true;
        }

        // 计算吸附阈值(像素距离转换为世界坐标距离)
        double scale = context.getTransform().getScale();
        double threshold = POINT_SNAP_THRESHOLD_PIXELS / scale;

        // 如果需要排除某个对象,过滤掉该对象的特殊点
        List<SpecialPoint> pointsToCheck = cachedSpecialPoints;
        if (excludedObject != null) {
            pointsToCheck = SpecialPointManager.extractSpecialPoints(context.getObjects());
            // 移除被排除对象的特殊点
            List<SpecialPoint> excludedPoints = SpecialPointManager.extractSpecialPoints(List.of(excludedObject));
            pointsToCheck.removeAll(excludedPoints);
        }

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, pointsToCheck, threshold);
    }

    /**
     * 查找最近的边吸附点
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 最近的边吸附结果,如果在阈值范围内没有则返回 null
     */
    public EdgeSnapManager.EdgeSnapResult findNearestEdge(double x, double y, IDrawingContext context) {
        return findNearestEdge(x, y, context, null);
    }

    /**
     * 查找最近的边吸附点(带排除对象)
     *
     * @param x              世界坐标 X
     * @param y              世界坐标 Y
     * @param context        绘制上下文
     * @param excludedObject 要排除的对象(通常是正在拖动的对象)
     * @return 最近的边吸附结果,如果在阈值范围内没有则返回 null
     */
    public EdgeSnapManager.EdgeSnapResult findNearestEdge(double x, double y, IDrawingContext context, WorldObject excludedObject) {
        // 计算吸附阈值(像素距离转换为世界坐标距离)
        double scale = context.getTransform().getScale();
        double threshold = EDGE_SNAP_THRESHOLD_PIXELS / scale;

        // 获取要检查的对象列表
        List<WorldObject> objectsToCheck = context.getObjects();
        if (excludedObject != null) {
            objectsToCheck = context.getObjects().stream()
                    .filter(obj -> obj != excludedObject)
                    .toList();
        }

        // 查找最近的边
        return EdgeSnapManager.findNearestEdge(x, y, objectsToCheck, threshold);
    }

    /**
     * 应用吸附逻辑(点优先,其次边,最后网格)
     * <p>
     * 1. 如果在阈值范围内有特殊点,返回该特殊点的坐标(优先级高)
     * 2. 如果没有点,尝试吸附到最近的边(优先级中)
     * 3. 如果启用了网格吸附,尝试吸附到网格点(优先级低)
     * 4. 都没有则返回原坐标
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 吸附后的坐标数组 [x, y]
     */
    public double[] applySnapping(double x, double y, IDrawingContext context) {
        // 1. 优先尝试点吸附
        SpecialPoint nearestPoint = findNearestSpecialPoint(x, y, context);
        if (nearestPoint != null) {
            return new double[]{nearestPoint.getX(), nearestPoint.getY()};
        }

        // 2. 如果没有点吸附,尝试边吸附
        EdgeSnapManager.EdgeSnapResult edgeSnap = findNearestEdge(x, y, context);
        if (edgeSnap != null) {
            return new double[]{edgeSnap.getX(), edgeSnap.getY()};
        }

        // 3. 如果启用了网格吸附,尝试吸附到网格点
        EuclidianViewSettings settings = context.getGridChartPane().getSettings();
        if (settings.isGridSnapEnabled()) {
            double[] gridSnapped = snapToGrid(x, y, context);
            if (gridSnapped != null) {
                return gridSnapped;
            }
        }

        // 4. 如果都没有,返回原始坐标
        return new double[]{x, y};
    }

    /**
     * 吸附到网格点（根据网格类型分发）
     * <p>
     * 支持笛卡尔、极坐标、等距三种网格的吸附算法
     *
     * @param x       世界坐标 X
     * @param y       世界坐标 Y
     * @param context 绘制上下文
     * @return 吸附后的坐标,如果距离太远则返回 null
     */
    private double[] snapToGrid(double x, double y, IDrawingContext context) {
        EuclidianViewSettings settings = context.getGridChartPane().getSettings();
        GridType gridType = settings.getGridType();
        WorldTransform transform = context.getTransform();

        if (gridType == GridType.POLAR) {
            return snapToPolarGrid(x, y, settings, transform);
        } else if (gridType == GridType.ISOMETRIC) {
            return snapToIsometricGrid(x, y, settings, transform);
        } else {
            // 笛卡尔网格、点状网格、含次网格：统一使用最近网格点吸附
            return snapToCartesianGrid(x, y, transform);
        }
    }

    /**
     * 笛卡尔网格吸附：对齐到最近的 (step, step) 倍点
     */
    private double[] snapToCartesianGrid(double x, double y, WorldTransform transform) {
        double scale = transform.getScale();
        double threshold = GRID_SNAP_THRESHOLD_PIXELS / scale;
        double step = AxisTickCalculator.calculateAxisTickDistance(scale, false);

        double gridX = Math.round(x / step) * step;
        double gridY = Math.round(y / step) * step;

        double distance = Math.hypot(gridX - x, gridY - y);
        if (distance < threshold) {
            gridX = stabilize(gridX);
            gridY = stabilize(gridY);
            return new double[]{gridX, gridY};
        }
        return null;
    }

    /**
     * 极坐标网格吸附：转为极坐标 → 对齐 r 和 θ → 转回直角坐标
     * <p>
     * 极坐标网格的交点位于同心圆与放射线的交点上。
     * 世界原点(0,0)为极点，正X轴为极轴。
     */
    private double[] snapToPolarGrid(double worldX, double worldY,
                                      EuclidianViewSettings settings, WorldTransform transform) {
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double angleStep = settings.getPolarAngleStep();

        // 世界坐标 → 极坐标（原点为极点）
        double r = Math.sqrt(worldX * worldX + worldY * worldY);
        double theta = Math.atan2(worldY, worldX);

        // 对齐 r 和 θ 到最近步长
        double snappedR = Math.round(r / step) * step;
        if (snappedR < 0) {
            snappedR = 0;
        }
        double snappedTheta = Math.round(theta / angleStep) * angleStep;

        // 极坐标 → 直角坐标
        double snappedX = snappedR * Math.cos(snappedTheta);
        double snappedY = snappedR * Math.sin(snappedTheta);

        // 用屏幕像素距离判断阈值（等比例缩放下世界距离×scale=像素距离）
        double scale = (transform.getScaleX() + transform.getScaleY()) / 2.0;
        double worldDist = Math.hypot(snappedX - worldX, snappedY - worldY);
        if (worldDist * scale < GRID_SNAP_THRESHOLD_PIXELS) {
            snappedX = stabilize(snappedX);
            snappedY = stabilize(snappedY);
            return new double[]{snappedX, snappedY};
        }
        return null;
    }

    /**
     * 等距网格吸附：转为屏幕坐标 → 查找最近的等边三角形格子顶点 → 转回世界坐标
     * <p>
     * 等距网格在屏幕空间中形成等边三角形格子，顶点位置为：
     * x = x0 + j * tickStepX/2, y = startY + (j%2)*tickStepY/2 + k*tickStepY
     */
    private double[] snapToIsometricGrid(double worldX, double worldY,
                                          EuclidianViewSettings settings, WorldTransform transform) {
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        double sqrt3 = Math.sqrt(3.0);

        // 世界 → 屏幕
        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);

        // 世界原点屏幕位置
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        // 屏幕空间网格参数（与 IsometricGridGenerator 一致）
        double tickStepX = scaleX * step * sqrt3;
        double tickStepY = scaleY * step;
        double startX2 = x0 % (tickStepX / 2.0);
        double startY = y0 % tickStepY;

        // 查找最近的垂直列索引 j
        double jDouble = (screenX - startX2) / (tickStepX / 2.0);
        int j = (int) Math.round(jDouble);

        // 该列对应的 y 基准（偶数/奇数行有半个步长的偏移）
        double yBase = startY + ((j % 2) * tickStepY / 2.0);

        // 查找最近的行索引 k
        double kDouble = (screenY - yBase) / tickStepY;
        int k = (int) Math.round(kDouble);

        // 计算吸附后的屏幕坐标
        double snappedScreenX = startX2 + j * tickStepX / 2.0;
        double snappedScreenY = yBase + k * tickStepY;

        // 屏幕像素距离判断
        double pixelDist = Math.hypot(snappedScreenX - screenX, snappedScreenY - screenY);
        if (pixelDist < GRID_SNAP_THRESHOLD_PIXELS) {
            double snappedWorldX = transform.screenToWorldX(snappedScreenX);
            double snappedWorldY = transform.screenToWorldY(snappedScreenY);
            snappedWorldX = stabilize(snappedWorldX);
            snappedWorldY = stabilize(snappedWorldY);
            return new double[]{snappedWorldX, snappedWorldY};
        }
        return null;
    }

    /**
     * 消除浮点抖动
     */
    private double stabilize(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) {
            return Math.round(v);
        }
        return v;
    }
}
