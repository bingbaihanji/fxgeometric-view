package com.bingbaihanji.view.layout.draw.coordinate;

import javafx.geometry.Rectangle2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签位置追踪器
 * <p>
 * 收集刻度标签在画布上的包围盒，供网格线绘制时查询避让区间。
 * 参考 GeoGebra 的 axesLabelsPositionsX 机制。
 *
 * @author bingbaihanji
 */
public class LabelPositionTracker {

    /** 已占用区域列表（屏幕坐标） */
    private final List<Rectangle2D> occupiedRegions = new ArrayList<>();

    /** 标签周围的安全边距（像素） */
    private static final double PADDING = 4.0;

    /**
     * 记录一个标签的屏幕位置
     *
     * @param screenX 标签左上角 X（屏幕坐标）
     * @param screenY 标签左上角 Y（屏幕坐标）
     * @param width   标签文本宽度（像素）
     * @param height  标签文本高度（像素）
     */
    public void addLabel(double screenX, double screenY, double width, double height) {
        occupiedRegions.add(new Rectangle2D(
                screenX - PADDING, screenY - height - PADDING,
                width + PADDING * 2, height + PADDING * 2));
    }

    /**
     * 判断一条水平网格线段是否与某标签区域相交，
     * 若相交则返回需要跳过的 X 区间列表
     *
     * @param lineY      网格线的 Y 坐标
     * @param lineXStart 线段起点 X
     * @param lineXEnd   线段终点 X
     * @return 需要跳过的区间列表（若不相交则返回空列表）
     */
    public List<SkipInterval> getHorizAvoidIntervals(double lineY,
                                                     double lineXStart, double lineXEnd) {
        List<SkipInterval> intervals = new ArrayList<>();
        for (Rectangle2D region : occupiedRegions) {
            if (lineY >= region.getMinY() && lineY <= region.getMaxY()) {
                double skipStart = Math.max(lineXStart, region.getMinX());
                double skipEnd = Math.min(lineXEnd, region.getMaxX());
                if (skipStart < skipEnd) {
                    intervals.add(new SkipInterval(skipStart, skipEnd));
                }
            }
        }
        return intervals;
    }

    /**
     * 清空所有已记录的位置
     */
    public void clear() {
        occupiedRegions.clear();
    }

    /**
     * 跳过区间 —— 标记一条网格线上需要断开的部分
     *
     * @param start 区间起点 X（屏幕坐标）
     * @param end   区间终点 X（屏幕坐标）
     */
    public record SkipInterval(double start, double end) {}
}
