package com.bingbaihanji.util;

/**
 * 坐标轴刻度距离计算器
 * 参考 GeoGebra 的 EuclidianView.setAxesIntervals() 实现
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 集中管理坐标轴刻度和网格步长的计算逻辑，确保网格步长始终与坐标轴刻度保持同步
 */
public class AxisTickCalculator {

    /**
     * 默认网格距离因子（与GeoGebra保持一致）
     * gridDistance = axisTickDistance * DEFAULT_GRID_DIST_FACTOR
     */
    public static final double DEFAULT_GRID_DIST_FACTOR = 1.0;

    /**
     * 最大像素间隔（每100像素最多一个刻度）
     * 参考 GeoGebra 的自适应算法
     */
    private static final double MAX_PIXELS_PER_TICK = 100.0;

    /**
     * 计算坐标轴刻度距离（自动模式）
     * 参考 GeoGebra 的 setAxesIntervals() 方法（EuclidianView.java:1913-1951）
     * <p>
     * 算法说明：
     * 1. 计算每100像素对应的世界单位数（units）
     * 2. 将units转换为科学计数法形式：units = n * 10^exp，其中 1 <= n < 10
     * 3. 根据n的值选择合适的步长：
     * - n > 5 时，选择 5 * 10^exp
     * - 2 < n <= 5 时，选择 2 * 10^exp
     * - n <= 2 时，选择 1 * 10^exp
     *
     * @param scale    缩放比例（像素/单位），例如 scale=50 表示 1个世界单位 = 50像素
     * @param isPiUnit 是否使用π单位（影响刻度计算）
     * @return 刻度距离（世界单位）
     */
    public static double calculateAxisTickDistance(double scale, boolean isPiUnit) {
        // 特殊处理：π单位模式
        if (isPiUnit) {
            return Math.PI;
        }

        // 计算100像素对应的世界单位数
        double units = MAX_PIXELS_PER_TICK / scale;

        // 计算科学计数法的指数部分
        int exp = (int) Math.floor(Math.log10(units));

        // 计算10的幂
        double pot = Math.pow(10, exp);

        // 计算系数 n（1 <= n < 10）
        double n = units / pot;

        // GeoGebra算法：根据n的大小选择合适的步长
        if (n > 5) {
            return 5 * pot;  // 例如：units=7.2 -> 5 * 10^0 = 5
        } else if (n > 2) {
            return 2 * pot;  // 例如：units=3.5 -> 2 * 10^0 = 2
        } else {
            return pot;      // 例如：units=1.8 -> 1 * 10^0 = 1
        }
    }

    /**
     * 计算网格距离（基于轴刻度距离）
     * 参考 GeoGebra 的 gridDistances 计算（EuclidianView.java:1980-1983）
     * <p>
     * 在 GeoGebra 中：
     * if (automaticGridDistance && axis < 2) {
     * gridDistances[axis] = axesNumberingDistances[axis] * DEFAULT_GRID_DIST_FACTOR;
     * }
     *
     * @param axisTickDistance 坐标轴刻度距离（世界单位）
     * @param gridDistFactor   网格距离因子（默认为1.0）
     * @return 网格距离（世界单位）
     */
    public static double calculateGridDistance(double axisTickDistance, double gridDistFactor) {
        return axisTickDistance * gridDistFactor;
    }

    /**
     * 计算次刻度距离
     * 参考 GeoGebra 的次刻度实现（主刻度的1/2）
     *
     * @param majorTickDistance 主刻度距离（世界单位）
     * @return 次刻度距离（世界单位）
     */
    public static double calculateMinorTickDistance(double majorTickDistance) {
        return majorTickDistance / 2.0;
    }

    /**
     * 计算网格的屏幕起始位置（使用模运算对齐原点）
     * 参考 GeoGebra 的 DrawGrid.java:58-59
     * <p>
     * 原理说明：
     * - originScreenPos：原点在屏幕上的坐标（像素）
     * - tickStepPixels：网格步长（像素）
     * - 返回值：第一条网格线相对屏幕边缘的偏移量
     * <p>
     * 模运算确保网格线始终经过原点的整数倍位置，无论如何缩放和平移。
     * <p>
     * 示例：
     * - 原点在屏幕x=250px，网格步长100px
     * - 第一条网格线位置：250 % 100 = 50px
     * - 后续网格线：50px, 150px, 250px（原点）, 350px, ...
     *
     * @param originScreenPos 原点在屏幕上的位置（像素）
     * @param tickStepPixels  网格步长（像素）
     * @return 第一条网格线的屏幕位置（像素）
     */
    public static double calculateGridStartPosition(double originScreenPos, double tickStepPixels) {
        return originScreenPos % tickStepPixels;
    }
}
