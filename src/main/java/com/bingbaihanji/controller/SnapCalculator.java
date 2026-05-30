package com.bingbaihanji.controller;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.constant.SnapMode;
import com.bingbaihanji.util.AxisTickCalculator;
import com.bingbaihanji.util.SpecialPointManager;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.CartesianGridGenerator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Set;

/**
 * 吸附计算器
 * <p>
 * 将 GridChartView 中的吸附计算逻辑提取为独立类
 */
public class SnapCalculator {

    private final EuclidianViewSettings settings;
    private final WorldTransform transform;
    private final Region host;

    public SnapCalculator(EuclidianViewSettings settings, WorldTransform transform) {
        this(settings, transform, null);
    }

    public SnapCalculator(EuclidianViewSettings settings, WorldTransform transform, Region host) {
        this.settings = settings;
        this.transform = transform;
        this.host = host;
    }

    /**
     * 获取视图设置（供 HoverTooltipManager 等读取网格类型）
     */
    public EuclidianViewSettings getSettings() {
        return settings;
    }

    /**
     * 主入口：计算吸附后坐标，并在 SnapResult 中携带视觉反馈信息
     */
    public SnapResult calculate(double rawX, double rawY, List<WorldObject> objects) {
        double x = rawX;
        double y = rawY;
        SpecialPoint snappedPoint = null;
        AxisSnapInfo axisSnapInfo = null;

        Set<SnapMode> snapModes = settings.getSnapModes();

        // 1. 特殊点吸附(最高优先级)
        if (snapModes.contains(SnapMode.POINT)) {
            SpecialPoint specialPoint = findNearestSpecialPoint(rawX, rawY, objects);
            if (specialPoint != null) {
                snappedPoint = specialPoint;
                return new SnapResult(specialPoint.getX(), specialPoint.getY(), snappedPoint, null);
            }
        }

        // 2. 轴向吸附(垂直/水平)
        AxisSnapResult axisSnap = findNearestAxisSnap(rawX, rawY, objects);
        if (axisSnap != null) {
            axisSnapInfo = axisSnap.snapInfo;
            if (axisSnap.snapInfo.isVertical) {
                x = axisSnap.snapInfo.snapValue;
            } else {
                y = axisSnap.snapInfo.snapValue;
            }
            return new SnapResult(x, y, null, axisSnapInfo);
        }

        // 3. 网格吸附(最低优先级)，根据网格类型分发
        if (settings.isGridSnapEnabled() && snapModes.contains(SnapMode.GRID)) {
            GridType gridType = settings.getGridType();
            if (gridType == GridType.POLAR) {
                double[] snapped = snapToPolarGrid(rawX, rawY);
                if (snapped != null) {
                    x = snapped[0];
                    y = snapped[1];
                }
            } else if (gridType == GridType.ISOMETRIC) {
                double[] snapped = snapToIsometricGrid(rawX, rawY);
                if (snapped != null) {
                    x = snapped[0];
                    y = snapped[1];
                }
            } else {
                // 笛卡尔 / 点状 / 含次网格：统一使用最近网格点对齐
                double step = AxisTickCalculator.calculateAxisTickDistance(
                        transform.getScale(), false);
                x = snapToInteger(rawX);
                x = snapToGrid(x, step);
                x = stabilize(x);
                y = snapToInteger(rawY);
                y = snapToGrid(y, step);
                y = stabilize(y);
            }
        }

        return new SnapResult(x, y, snappedPoint, axisSnapInfo);
    }

    /**
     * 查找最近的特殊点(用于磁性吸附)
     *
     * @param x       当前鼠标x坐标(世界坐标)
     * @param y       当前鼠标y坐标(世界坐标)
     * @param objects 对象列表
     * @return 最近的特殊点,如果没有找到则返回null
     */
    private SpecialPoint findNearestSpecialPoint(double x, double y, List<WorldObject> objects) {
        // 获取所有特殊点
        List<SpecialPoint> specialPoints = SpecialPointManager.extractSpecialPoints(objects);

        // 计算吸附阈值(像素距离转换为世界坐标距离)
        double scale = transform.getScale();
        double threshold = com.bingbaihanji.config.GeometryConfig.Tolerance.SPECIAL_POINT_SNAP_RADIUS_PIXELS / scale; // 特殊点磁力吸附半径

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, specialPoints, threshold);
    }

    /**
     * 查找最近的轴向吸附(垂直线或水平线)
     *
     * @param worldX  当前世界坐标X
     * @param worldY  当前世界坐标Y
     * @param objects 对象列表
     * @return 轴向吸附结果,如果没有找到返回null
     */
    private AxisSnapResult findNearestAxisSnap(double worldX, double worldY, List<WorldObject> objects) {
        Set<SnapMode> snapModes = settings.getSnapModes();
        boolean enableVertical = snapModes.contains(SnapMode.AXIS_VERTICAL);
        boolean enableHorizontal = snapModes.contains(SnapMode.AXIS_HORIZONTAL);

        if (!enableVertical && !enableHorizontal) {
            return null;
        }

        // 计算吸附阈值(像素转世界坐标)
        double threshold = settings.getSnapThreshold() / transform.getScale();

        AxisSnapResult bestSnap = null;
        double minDistance = threshold;

        // 遍历所有点对象,检测是否接近其垂直线或水平线
        List<SpecialPoint> specialPoints = SpecialPointManager.extractSpecialPoints(objects);

        double viewWidth = host != null ? host.getWidth() : 1000;
        double viewHeight = host != null ? host.getHeight() : 1000;

        for (SpecialPoint point : specialPoints) {
            // 检查垂直线吸附
            if (enableVertical) {
                double distX = Math.abs(worldX - point.getX());
                if (distX < minDistance) {
                    // 计算辅助线的屏幕坐标
                    double screenX = transform.worldToScreenX(point.getX());

                    AxisSnapInfo snapInfo = new AxisSnapInfo(
                            true,
                            point.getX(),
                            screenX, 0,
                            screenX, viewHeight
                    );

                    bestSnap = new AxisSnapResult(snapInfo, distX);
                    minDistance = distX;
                }
            }

            // 检查水平线吸附
            if (enableHorizontal) {
                double distY = Math.abs(worldY - point.getY());
                if (distY < minDistance) {
                    // 计算辅助线的屏幕坐标
                    double screenY = transform.worldToScreenY(point.getY());

                    AxisSnapInfo snapInfo = new AxisSnapInfo(
                            false,
                            point.getY(),
                            0, screenY,
                            viewWidth, screenY
                    );

                    bestSnap = new AxisSnapResult(snapInfo, distY);
                    minDistance = distY;
                }
            }
        }

        return bestSnap;
    }

    /**
     * 吸附到最近整数点(优先级最高)
     * <p>
     * 当世界坐标值接近整数时,自动吸附到该整数点
     *
     * @param worldValue 世界坐标值
     * @return 吸附后的坐标值
     */
    private double snapToInteger(double worldValue) {
        double scale = transform.getScale();
        double nearest = Math.round(worldValue);
        double pixelDistance = Math.abs(worldValue - nearest) * scale;

        // 整数磁力更强： 5像素
        if (pixelDistance < 5) {
            return nearest;
        }
        return worldValue;
    }

    /**
     * 吸附到最近网格点(基于当前轴刻度)
     * <p>
     * 当世界坐标值接近网格点时,自动吸附到该网格点
     *
     * @param worldValue 世界坐标值
     * @param step       网格步长
     * @return 吸附后的坐标值
     */
    private double snapToGrid(double worldValue, double step) {
        double scale = transform.getScale();
        double snapped = Math.round(worldValue / step) * step;

        double pixelDistance = Math.abs(worldValue - snapped) * scale;

        if (pixelDistance < 5) {
            return snapped;
        }
        return worldValue;
    }

    /**
     * 极坐标网格吸附：搜索鼠标附近 3×3 网格交点，找最近者
     */
    private double[] snapToPolarGrid(double worldX, double worldY) {
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double angleStepDeg = settings.getPolarAngleStep();
        double angleStepRad = Math.toRadians(angleStepDeg);

        double r = Math.sqrt(worldX * worldX + worldY * worldY);
        double theta = Math.atan2(worldY, worldX);

        int kR = (int) Math.round(r / step);
        if (kR < 0) kR = 0;
        int kTheta = (int) Math.round(theta / angleStepRad);

        double mouseSX = transform.worldToScreenX(worldX);
        double mouseSY = transform.worldToScreenY(worldY);
        double bestDist = Double.MAX_VALUE;
        double bestX = worldX, bestY = worldY;

        for (int dr = -1; dr <= 1; dr++) {
            double cr = (kR + dr) * step;
            if (cr < 0) continue;
            for (int dt = -1; dt <= 1; dt++) {
                double ct = (kTheta + dt) * angleStepRad;
                double cx = cr * Math.cos(ct);
                double cy = cr * Math.sin(ct);
                double sx = transform.worldToScreenX(cx);
                double sy = transform.worldToScreenY(cy);
                double dist = Math.hypot(sx - mouseSX, sy - mouseSY);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = cx;
                    bestY = cy;
                }
            }
        }

        if (bestDist < 12) {
            return new double[]{stabilize(bestX), stabilize(bestY)};
        }
        return null;
    }

    /**
     * 等距网格吸附：屏幕空间查找最近格子顶点 → 转回世界坐标
     */
    private double[] snapToIsometricGrid(double worldX, double worldY) {
        double step = CartesianGridGenerator.getGridStep(transform, settings);
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        double sqrt3 = Math.sqrt(3.0);

        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);
        double x0 = transform.worldToScreenX(0);
        double y0 = transform.worldToScreenY(0);

        double tickStepX = scaleX * step * sqrt3;
        double tickStepY = scaleY * step;
        double startX2 = x0 % (tickStepX / 2.0);
        double startY = y0 % tickStepY;

        int j = (int) Math.round((screenX - startX2) / (tickStepX / 2.0));
        double yBase = startY + ((j % 2) * tickStepY / 2.0);
        int k = (int) Math.round((screenY - yBase) / tickStepY);

        double snappedSX = startX2 + j * tickStepX / 2.0;
        double snappedSY = yBase + k * tickStepY;

        double pixelDist = Math.hypot(snappedSX - screenX, snappedSY - screenY);
        if (pixelDist < 8) { // GRID_SNAP_THRESHOLD_PIXELS = 8
            double swx = transform.screenToWorldX(snappedSX);
            double swy = transform.screenToWorldY(snappedSY);
            return new double[]{stabilize(swx), stabilize(swy)};
        }
        return null;
    }

    /**
     * 消除浮点抖动
     * <p>
     * 将接近整数的浮点数修正为整数,避免浮点运算误差
     *
     * @param v 待稳定化的数值
     * @return 稳定化后的数值
     */
    private double stabilize(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) {
            return Math.round(v);
        }
        return v;
    }

    /**
     * 吸附结果，包含坐标和视觉反馈状态
     */
    public static class SnapResult {
        public final double x;
        public final double y;
        public final SpecialPoint snappedPoint;   // 吸附到特殊点时非 null
        public final AxisSnapInfo axisSnapInfo;   // 触发轴向吸附时非 null

        public SnapResult(double x, double y, SpecialPoint snappedPoint, AxisSnapInfo axisSnapInfo) {
            this.x = x;
            this.y = y;
            this.snappedPoint = snappedPoint;
            this.axisSnapInfo = axisSnapInfo;
        }
    }

    /**
     * 轴向吸附信息类
     */
    public static class AxisSnapInfo {
        public final boolean isVertical;   // true=垂直线吸附, false=水平线吸附
        public final double snapValue;     // 吸附的x或y坐标值
        public final double lineStartX;    // 辅助线起点X(屏幕坐标)
        public final double lineStartY;    // 辅助线起点Y(屏幕坐标)
        public final double lineEndX;      // 辅助线终点X(屏幕坐标)
        public final double lineEndY;      // 辅助线终点Y(屏幕坐标)

        public AxisSnapInfo(boolean isVertical, double snapValue,
                            double lineStartX, double lineStartY,
                            double lineEndX, double lineEndY) {
            this.isVertical = isVertical;
            this.snapValue = snapValue;
            this.lineStartX = lineStartX;
            this.lineStartY = lineStartY;
            this.lineEndX = lineEndX;
            this.lineEndY = lineEndY;
        }
    }

    /**
     * 轴向吸附结果类
     */
    private static class AxisSnapResult {
        AxisSnapInfo snapInfo;
        double distance;  // 到吸附线的距离

        AxisSnapResult(AxisSnapInfo snapInfo, double distance) {
            this.snapInfo = snapInfo;
            this.distance = distance;
        }
    }
}
