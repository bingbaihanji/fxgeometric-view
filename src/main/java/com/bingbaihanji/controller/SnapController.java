package com.bingbaihanji.controller;

import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 吸附控制器 - 状态机管理吸附行为
 * <p>
 * 状态转换流程：
 * FREE（自由状态）→ MAY_SNAP（可能吸附）→ SNAPPED_XXX（已吸附）→ FREE
 * <p>
 * 容差策略：
 * - 初始检测：5px（敏感触发）
 * - 持续吸附：40px（宽松维持）
 */
public class SnapController {

    // 容差常量
    private static final double INITIAL_TOLERANCE = 5.0;  // 初始检测容差（像素）
    private static final double SNAPPED_TOLERANCE = 40.0; // 已吸附维持容差（像素）
    // 当前状态
    private SnapState currentState = SnapState.FREE;
    // 已吸附的目标信息
    private double snappedX = 0.0;
    private double snappedY = 0.0;
    private WorldObject snappedObject = null;

    /**
     * 应用吸附逻辑（状态机核心方法）
     *
     * @param rawX          原始世界坐标X
     * @param rawY          原始世界坐标Y
     * @param gridChartView 图表视图（用于获取吸附配置和对象）
     * @return 吸附结果
     */
    public SnapResult applySnap(double rawX, double rawY, GridChartView gridChartView) {
        double scale = gridChartView.getTransform().getScale();

        // 根据当前状态选择容差
        double pixelTolerance = (currentState == SnapState.FREE || currentState == SnapState.MAY_SNAP)
                ? INITIAL_TOLERANCE
                : SNAPPED_TOLERANCE;

        double worldTolerance = pixelTolerance / scale;

        // 状态机逻辑
        switch (currentState) {
            case FREE:
                return handleFreeState(rawX, rawY, gridChartView, worldTolerance);

            case MAY_SNAP:
                // MAY_SNAP 状态立即转换为 SNAPPED（简化逻辑）
                return handleFreeState(rawX, rawY, gridChartView, worldTolerance);

            case SNAPPED_POINT:
            case SNAPPED_GRID:
            case SNAPPED_AXIS:
                return handleSnappedState(rawX, rawY, gridChartView, worldTolerance);

            default:
                return new SnapResult(rawX, rawY, SnapState.FREE);
        }
    }

    /**
     * 处理自由状态（尝试触发吸附）
     */
    private SnapResult handleFreeState(double rawX, double rawY, GridChartView gridChartView, double tolerance) {
        // 1. 尝试点吸附（最高优先级）
        SnapResult pointSnap = trySnapToPoint(rawX, rawY, gridChartView, tolerance);
        if (pointSnap != null) {
            currentState = SnapState.SNAPPED_POINT;
            snappedX = pointSnap.x;
            snappedY = pointSnap.y;
            snappedObject = pointSnap.snappedObject;
            return pointSnap;
        }

        // 2. 尝试轴向吸附
        SnapResult axisSnap = trySnapToAxis(rawX, rawY, gridChartView, tolerance);
        if (axisSnap != null) {
            currentState = SnapState.SNAPPED_AXIS;
            snappedX = axisSnap.x;
            snappedY = axisSnap.y;
            return axisSnap;
        }

        // 3. 尝试网格吸附（最低优先级）
        SnapResult gridSnap = trySnapToGrid(rawX, rawY, gridChartView, tolerance);
        if (gridSnap != null) {
            currentState = SnapState.SNAPPED_GRID;
            snappedX = gridSnap.x;
            snappedY = gridSnap.y;
            return gridSnap;
        }

        // 未找到吸附目标，保持自由状态
        currentState = SnapState.FREE;
        return new SnapResult(rawX, rawY, SnapState.FREE);
    }

    /**
     * 处理已吸附状态（维持或释放吸附）
     */
    private SnapResult handleSnappedState(double rawX, double rawY, GridChartView gridChartView, double tolerance) {
        // 计算与已吸附点的距离
        double dx = Math.abs(rawX - snappedX);
        double dy = Math.abs(rawY - snappedY);
        double distance = Math.hypot(dx, dy);

        // 如果距离在容差范围内，维持吸附
        if (distance <= tolerance) {
            return new SnapResult(snappedX, snappedY, currentState, snappedObject);
        }

        // 超出容差，释放吸附，返回自由状态
        currentState = SnapState.FREE;
        snappedObject = null;
        return new SnapResult(rawX, rawY, SnapState.FREE);
    }

    /**
     * 尝试吸附到点
     */
    private SnapResult trySnapToPoint(double rawX, double rawY, GridChartView gridChartView, double tolerance) {
        // 使用 GridChartView 的内部吸附系统查找特殊点
        // 这里需要访问 GridChartView 的 findNearestSpecialPoint 方法
        // 由于该方法是私有的，我们需要通过公共接口实现

        // 临时方案：遍历所有对象的可拖动点
        List<WorldObject> objects = gridChartView.getObjects();
        for (WorldObject obj : objects) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                double dx = Math.abs(point.getX() - rawX);
                double dy = Math.abs(point.getY() - rawY);
                if (dx <= tolerance && dy <= tolerance) {
                    return new SnapResult(point.getX(), point.getY(), SnapState.SNAPPED_POINT, obj);
                }
            }
        }

        return null;
    }

    /**
     * 尝试吸附到轴向
     */
    private SnapResult trySnapToAxis(double rawX, double rawY, GridChartView gridChartView, double tolerance) {
        // 检查是否接近水平轴（Y=0）
        if (Math.abs(rawY) <= tolerance) {
            return new SnapResult(rawX, 0.0, SnapState.SNAPPED_AXIS);
        }

        // 检查是否接近垂直轴（X=0）
        if (Math.abs(rawX) <= tolerance) {
            return new SnapResult(0.0, rawY, SnapState.SNAPPED_AXIS);
        }

        return null;
    }

    /**
     * 尝试吸附到网格
     */
    private SnapResult trySnapToGrid(double rawX, double rawY, GridChartView gridChartView, double tolerance) {
        // 检查网格吸附是否启用
        if (!gridChartView.getSettings().isGridSnapEnabled()) {
            return null;
        }

        // 使用整数网格吸附（最简单的实现）
        double snappedX = Math.round(rawX);
        double snappedY = Math.round(rawY);

        double dx = Math.abs(rawX - snappedX);
        double dy = Math.abs(rawY - snappedY);

        // 只有当距离网格点足够近时才吸附
        if (dx <= tolerance && dy <= tolerance) {
            return new SnapResult(snappedX, snappedY, SnapState.SNAPPED_GRID);
        }

        return null;
    }

    /**
     * 重置状态机（在开始新的拖动时调用）
     */
    public void reset() {
        currentState = SnapState.FREE;
        snappedX = 0.0;
        snappedY = 0.0;
        snappedObject = null;
    }

    /**
     * 获取当前状态
     */
    public SnapState getCurrentState() {
        return currentState;
    }

    /**
     * 判断是否正在吸附
     */
    public boolean isSnapped() {
        return currentState != SnapState.FREE && currentState != SnapState.MAY_SNAP;
    }

    /**
     * 获取已吸附的对象（仅在点吸附时有效）
     */
    public WorldObject getSnappedObject() {
        return snappedObject;
    }

    /**
     * 吸附状态枚举
     */
    public enum SnapState {
        /**
         * 自由状态（无吸附）
         */
        FREE,
        /**
         * 可能吸附（检测到目标，但尚未触发）
         */
        MAY_SNAP,
        /**
         * 已吸附到点
         */
        SNAPPED_POINT,
        /**
         * 已吸附到网格
         */
        SNAPPED_GRID,
        /**
         * 已吸附到轴向（水平/垂直）
         */
        SNAPPED_AXIS
    }

    /**
     * 吸附结果
     */
    public static class SnapResult {
        /**
         * 吸附后的X坐标
         */
        public final double x;
        /**
         * 吸附后的Y坐标
         */
        public final double y;
        /**
         * 吸附状态
         */
        public final SnapState state;
        /**
         * 吸附到的对象（如果是点吸附）
         */
        public final WorldObject snappedObject;

        public SnapResult(double x, double y, SnapState state, WorldObject snappedObject) {
            this.x = x;
            this.y = y;
            this.state = state;
            this.snappedObject = snappedObject;
        }

        public SnapResult(double x, double y, SnapState state) {
            this(x, y, state, null);
        }
    }
}
