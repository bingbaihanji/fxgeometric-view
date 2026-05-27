package com.bingbaihanji.view.layout.draw.coordinate;

import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.coordinate.grid.GridElement;

import java.util.List;

/**
 * 坐标系统接口
 * <p>
 * 每种坐标系负责生成自己的网格元素列表和刻度信息。
 * 几何对象始终存储于笛卡尔世界空间，CoordinateSystem 仅控制视觉呈现。
 *
 * @author bingbaihanji
 */
public interface CoordinateSystem {

    /**
     * 生成当前坐标系下的网格元素列表
     *
     * @param transform  世界坐标变换
     * @param settings   视图配置
     * @param viewWidth  视口宽度（像素）
     * @param viewHeight 视口高度（像素）
     * @return 网格元素列表（屏幕坐标）
     */
    List<GridElement> generateGrid(WorldTransform transform, EuclidianViewSettings settings,
                                   double viewWidth, double viewHeight);

    /**
     * 计算 X 轴刻度
     *
     * @param transform  世界坐标变换
     * @param settings   视图配置
     * @param viewWidth  视口宽度
     * @param viewHeight 视口高度
     * @return X 轴刻度列表
     */
    List<TickInfo> calculateXTicks(WorldTransform transform, EuclidianViewSettings settings,
                                   double viewWidth, double viewHeight);

    /**
     * 计算 Y 轴刻度
     *
     * @param transform  世界坐标变换
     * @param settings   视图配置
     * @param viewWidth  视口宽度
     * @param viewHeight 视口高度
     * @return Y 轴刻度列表
     */
    List<TickInfo> calculateYTicks(WorldTransform transform, EuclidianViewSettings settings,
                                   double viewWidth, double viewHeight);

    /**
     * 是否锁定轴比例（极坐标系锁定 1:1）
     *
     * @return true 如果轴比例不可自由调整
     */
    boolean isAxesRatioLocked();
}
