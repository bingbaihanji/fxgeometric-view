package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.constant.*;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

/**
 * Euclidian视图配置类
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 集中管理GridChartView的所有可配置项, 包括坐标轴、网格、吸附等
 */
public class EuclidianViewSettings {

    //   坐标轴配置  

    /**
     * X轴缩放比例(像素/单位)
     */
    private double xScale = 50.0;

    /**
     * Y轴缩放比例(像素/单位)
     */
    private double yScale = 50.0;

    /**
     * X轴最小值
     */
    private double xMin = -10.0;

    /**
     * X轴最大值
     */
    private double xMax = 10.0;

    /**
     * Y轴最小值
     */
    private double yMin = -10.0;

    /**
     * Y轴最大值
     */
    private double yMax = 10.0;

    /**
     * 是否自动计算X轴刻度间距
     */
    private boolean autoXTickDistance = true;

    /**
     * X轴刻度间距(手动模式)
     */
    private double xTickDistance = 1.0;

    /**
     * 是否自动计算Y轴刻度间距
     */
    private boolean autoYTickDistance = true;

    /**
     * Y轴刻度间距(手动模式)
     */
    private double yTickDistance = 1.0;

    /**
     * X轴刻度样式
     */
    private AxisTickStyle xTickStyle = AxisTickStyle.MAJOR_ONLY;

    /**
     * Y轴刻度样式
     */
    private AxisTickStyle yTickStyle = AxisTickStyle.MAJOR_ONLY;

    /**
     * X轴箭头类型
     */
    private AxisArrowType xArrowType = AxisArrowType.ARROW;

    /**
     * Y轴箭头类型
     */
    private AxisArrowType yArrowType = AxisArrowType.ARROW;

    /**
     * 单位标签类型
     */
    private UnitLabelType unitLabelType = UnitLabelType.NUMERIC;

    /**
     * 自定义单位标签(如 "cm", "m" 等)
     */
    private String customUnitLabel = "";

    /**
     * 坐标轴颜色
     */
    private Color axesColor = Color.web("#f7a707"); // 橙色

    /**
     * 坐标轴线型
     */
    private LineType axesLineType = LineType.FULL;

    /**
     * 是否显示X轴
     */
    private boolean showXAxis = true;

    /**
     * 是否显示Y轴
     */
    private boolean showYAxis = true;

    /**
     * 是否显示坐标轴刻度数字
     */
    private boolean showAxesNumbers = true;

    //   网格配置  

    /**
     * 网格类型
     */
    private GridType gridType = GridType.DOT;

    /**
     * 是否显示网格
     */
    private boolean showGrid = true;

    /**
     * 网格颜色
     */
    private Color gridColor = Color.rgb(126, 126, 126);

    /**
     * 次网格颜色(用于主网格+次网格模式)
     */
    private Color subGridColor = Color.rgb(200, 200, 200);

    /**
     * 网格线型
     */
    private LineType gridLineType = LineType.FULL;

    /**
     * 是否自动计算网格间距
     */
    private boolean autoGridDistance = true;

    /**
     * 网格间距(手动模式)
     */
    private double gridDistance = 1.0;

    /**
     * 极坐标网格的角度步长(弧度)
     */
    private double polarAngleStep = Math.PI / 6; // 30度

    /**
     * 是否启用网格吸附
     * true: 鼠标在网格交点附近时自动吸附
     * false: 禁用网格吸附
     */
    private boolean gridSnapEnabled = true;

    /**
     * 网格距离因子(用于计算网格间距相对于坐标轴刻度的倍数)
     * 默认为1.0,表示网格间距 = 坐标轴刻度间距 * 1.0
     * 参考 GeoGebra 的 DEFAULT_GRID_DIST_FACTOR
     */
    private double gridDistanceFactor = 1.0;

    /**
     * 是否启用网格与坐标轴刻度同步
     * true: 网格间距自动跟随坐标轴刻度变化(推荐)
     * false: 网格间距独立计算
     */
    private boolean syncGridWithAxes = true;

    /**
     * X轴是否使用π单位(影响刻度计算)
     */
    private boolean xAxisPiUnit = false;

    /**
     * Y轴是否使用π单位(影响刻度计算)
     */
    private boolean yAxisPiUnit = false;

    //   吸附配置  

    /**
     * 吸附模式集合(可同时启用多种)
     */
    private Set<SnapMode> snapModes = new HashSet<>();

    /**
     * 吸附阈值(像素)
     */
    private double snapThreshold = 10.0;

    //   构造函数  

    public EuclidianViewSettings() {
        // 默认启用网格吸附、点吸附和轴向吸附
        snapModes.add(SnapMode.GRID);
        snapModes.add(SnapMode.POINT);
        snapModes.add(SnapMode.AXIS_VERTICAL);
        snapModes.add(SnapMode.AXIS_HORIZONTAL);
    }

    //   Getter and Setter  

    public double getXScale() {
        return xScale;
    }

    public void setXScale(double xScale) {
        this.xScale = xScale;
    }

    public double getYScale() {
        return yScale;
    }

    public void setYScale(double yScale) {
        this.yScale = yScale;
    }

    public double getXMin() {
        return xMin;
    }

    public void setXMin(double xMin) {
        this.xMin = xMin;
    }

    public double getXMax() {
        return xMax;
    }

    public void setXMax(double xMax) {
        this.xMax = xMax;
    }

    public double getYMin() {
        return yMin;
    }

    public void setYMin(double yMin) {
        this.yMin = yMin;
    }

    public double getYMax() {
        return yMax;
    }

    public void setYMax(double yMax) {
        this.yMax = yMax;
    }

    public boolean isAutoXTickDistance() {
        return autoXTickDistance;
    }

    public void setAutoXTickDistance(boolean autoXTickDistance) {
        this.autoXTickDistance = autoXTickDistance;
    }

    public double getXTickDistance() {
        return xTickDistance;
    }

    public void setXTickDistance(double xTickDistance) {
        this.xTickDistance = xTickDistance;
    }

    public boolean isAutoYTickDistance() {
        return autoYTickDistance;
    }

    public void setAutoYTickDistance(boolean autoYTickDistance) {
        this.autoYTickDistance = autoYTickDistance;
    }

    public double getYTickDistance() {
        return yTickDistance;
    }

    public void setYTickDistance(double yTickDistance) {
        this.yTickDistance = yTickDistance;
    }

    public AxisTickStyle getXTickStyle() {
        return xTickStyle;
    }

    public void setXTickStyle(AxisTickStyle xTickStyle) {
        this.xTickStyle = xTickStyle;
        notifySettingsChanged();
    }

    public AxisTickStyle getYTickStyle() {
        return yTickStyle;
    }

    public void setYTickStyle(AxisTickStyle yTickStyle) {
        this.yTickStyle = yTickStyle;
        notifySettingsChanged();
    }

    public AxisArrowType getXArrowType() {
        return xArrowType;
    }

    public void setXArrowType(AxisArrowType xArrowType) {
        this.xArrowType = xArrowType;
        notifySettingsChanged();
    }

    public AxisArrowType getYArrowType() {
        return yArrowType;
    }

    public void setYArrowType(AxisArrowType yArrowType) {
        this.yArrowType = yArrowType;
        notifySettingsChanged();
    }

    public UnitLabelType getUnitLabelType() {
        return unitLabelType;
    }

    public void setUnitLabelType(UnitLabelType unitLabelType) {
        this.unitLabelType = unitLabelType;
        notifySettingsChanged();
    }

    public String getCustomUnitLabel() {
        return customUnitLabel;
    }

    public void setCustomUnitLabel(String customUnitLabel) {
        this.customUnitLabel = customUnitLabel;
    }

    public Color getAxesColor() {
        return axesColor;
    }

    public void setAxesColor(Color axesColor) {
        this.axesColor = axesColor;
        notifySettingsChanged();
    }

    public LineType getAxesLineType() {
        return axesLineType;
    }

    public void setAxesLineType(LineType axesLineType) {
        this.axesLineType = axesLineType;
        notifySettingsChanged();
    }

    public boolean isShowXAxis() {
        return showXAxis;
    }

    public void setShowXAxis(boolean showXAxis) {
        this.showXAxis = showXAxis;
        notifySettingsChanged();
    }

    public boolean isShowYAxis() {
        return showYAxis;
    }

    public void setShowYAxis(boolean showYAxis) {
        this.showYAxis = showYAxis;
        notifySettingsChanged();
    }

    public boolean isShowAxesNumbers() {
        return showAxesNumbers;
    }

    public void setShowAxesNumbers(boolean showAxesNumbers) {
        this.showAxesNumbers = showAxesNumbers;
        notifySettingsChanged();
    }

    public GridType getGridType() {
        return gridType;
    }

    public void setGridType(GridType gridType) {
        this.gridType = gridType;
        notifySettingsChanged();
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        notifySettingsChanged();
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setGridColor(Color gridColor) {
        this.gridColor = gridColor;
        notifySettingsChanged();
    }

    public Color getSubGridColor() {
        return subGridColor;
    }

    public void setSubGridColor(Color subGridColor) {
        this.subGridColor = subGridColor;
        notifySettingsChanged();
    }

    public LineType getGridLineType() {
        return gridLineType;
    }

    public void setGridLineType(LineType gridLineType) {
        this.gridLineType = gridLineType;
        notifySettingsChanged();
    }

    public boolean isAutoGridDistance() {
        return autoGridDistance;
    }

    public void setAutoGridDistance(boolean autoGridDistance) {
        this.autoGridDistance = autoGridDistance;
        notifySettingsChanged();
    }

    public double getGridDistance() {
        return gridDistance;
    }

    public void setGridDistance(double gridDistance) {
        this.gridDistance = gridDistance;
    }

    public double getPolarAngleStep() {
        return polarAngleStep;
    }

    public void setPolarAngleStep(double polarAngleStep) {
        this.polarAngleStep = polarAngleStep;
    }

    public Set<SnapMode> getSnapModes() {
        return snapModes;
    }

    public void setSnapModes(Set<SnapMode> snapModes) {
        this.snapModes = snapModes;
    }

    public double getSnapThreshold() {
        return snapThreshold;
    }

    public void setSnapThreshold(double snapThreshold) {
        this.snapThreshold = snapThreshold;
    }

    public double getGridDistanceFactor() {
        return gridDistanceFactor;
    }

    public void setGridDistanceFactor(double gridDistanceFactor) {
        this.gridDistanceFactor = gridDistanceFactor;
        notifySettingsChanged();
    }

    public boolean isSyncGridWithAxes() {
        return syncGridWithAxes;
    }

    public void setSyncGridWithAxes(boolean syncGridWithAxes) {
        this.syncGridWithAxes = syncGridWithAxes;
        notifySettingsChanged();
    }

    public boolean isXAxisPiUnit() {
        return xAxisPiUnit;
    }

    public void setXAxisPiUnit(boolean xAxisPiUnit) {
        this.xAxisPiUnit = xAxisPiUnit;
    }

    public boolean isYAxisPiUnit() {
        return yAxisPiUnit;
    }

    public void setYAxisPiUnit(boolean yAxisPiUnit) {
        this.yAxisPiUnit = yAxisPiUnit;
    }

    public boolean isGridSnapEnabled() {
        return gridSnapEnabled;
    }

    public void setGridSnapEnabled(boolean gridSnapEnabled) {
        this.gridSnapEnabled = gridSnapEnabled;
    }

    // ==================== 设置变更传播 ====================

    /** 设置变更监听器列表 */
    private final java.util.List<Runnable> settingsChangeListeners = new java.util.ArrayList<>();

    /**
     * 批量修改多个设置，只触发一次回调通知
     *
     * @param updater 批量更新函数，接收当前 settings 实例
     */
    public void batchUpdate(java.util.function.Consumer<EuclidianViewSettings> updater) {
        updater.accept(this);
        notifySettingsChanged();
    }

    /**
     * 添加设置变更监听器
     *
     * @param listener 回调（设置变更后触发）
     */
    public void addSettingsChangeListener(Runnable listener) {
        if (listener != null && !settingsChangeListeners.contains(listener)) {
            settingsChangeListeners.add(listener);
        }
    }

    /**
     * 移除设置变更监听器
     */
    public void removeSettingsChangeListener(Runnable listener) {
        settingsChangeListeners.remove(listener);
    }

    /** 通知所有监听器设置已变更 */
    private void notifySettingsChanged() {
        for (Runnable listener : settingsChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                // 静默处理异常，不中断其他监听器
            }
        }
    }

    /**
     * 克隆配置
     *
     * @return 配置的副本
     */
    public EuclidianViewSettings clone() {
        EuclidianViewSettings copy = new EuclidianViewSettings();
        copy.xScale = this.xScale;
        copy.yScale = this.yScale;
        copy.xMin = this.xMin;
        copy.xMax = this.xMax;
        copy.yMin = this.yMin;
        copy.yMax = this.yMax;
        copy.autoXTickDistance = this.autoXTickDistance;
        copy.xTickDistance = this.xTickDistance;
        copy.autoYTickDistance = this.autoYTickDistance;
        copy.yTickDistance = this.yTickDistance;
        copy.xTickStyle = this.xTickStyle;
        copy.yTickStyle = this.yTickStyle;
        copy.xArrowType = this.xArrowType;
        copy.yArrowType = this.yArrowType;
        copy.unitLabelType = this.unitLabelType;
        copy.customUnitLabel = this.customUnitLabel;
        copy.axesColor = this.axesColor;
        copy.axesLineType = this.axesLineType;
        copy.showXAxis = this.showXAxis;
        copy.showYAxis = this.showYAxis;
        copy.showAxesNumbers = this.showAxesNumbers;
        copy.gridType = this.gridType;
        copy.showGrid = this.showGrid;
        copy.gridColor = this.gridColor;
        copy.subGridColor = this.subGridColor;
        copy.gridLineType = this.gridLineType;
        copy.autoGridDistance = this.autoGridDistance;
        copy.gridDistance = this.gridDistance;
        copy.polarAngleStep = this.polarAngleStep;
        copy.snapModes = new HashSet<>(this.snapModes);
        copy.snapThreshold = this.snapThreshold;
        copy.gridDistanceFactor = this.gridDistanceFactor;
        copy.syncGridWithAxes = this.syncGridWithAxes;
        copy.xAxisPiUnit = this.xAxisPiUnit;
        copy.yAxisPiUnit = this.yAxisPiUnit;
        copy.gridSnapEnabled = this.gridSnapEnabled;
        return copy;
    }
}
