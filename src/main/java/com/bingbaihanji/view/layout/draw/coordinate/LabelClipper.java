package com.bingbaihanji.view.layout.draw.coordinate;

/**
 * 标签边界裁剪工具
 * <p>
 * 判断刻度标签是否靠近视口边缘，避免文字被 Canvas 边界裁剪。
 * 迁移自 AxesPainter 中的边缘阈值判断逻辑。
 *
 * @author bingbaihanji
 */
public class LabelClipper {

    /**
     * 默认边缘安全距离（像素）
     */
    private static final int DEFAULT_MARGIN = 15;

    /**
     * 判断屏幕坐标是否靠近视口水平边缘
     *
     * @param screenX   屏幕 X 坐标
     * @param viewWidth 视口宽度
     * @return true 如果太靠近边缘
     */
    public static boolean isNearHorizontalEdge(double screenX, double viewWidth) {
        return screenX < DEFAULT_MARGIN || screenX > viewWidth - DEFAULT_MARGIN;
    }

    /**
     * 判断屏幕坐标是否靠近视口垂直边缘
     *
     * @param screenY    屏幕 Y 坐标
     * @param viewHeight 视口高度
     * @return true 如果太靠近边缘
     */
    public static boolean isNearVerticalEdge(double screenY, double viewHeight) {
        return screenY < DEFAULT_MARGIN || screenY > viewHeight - DEFAULT_MARGIN;
    }

    /**
     * 判断屏幕坐标是否靠近视口任一边缘
     *
     * @param screenX    屏幕 X 坐标
     * @param screenY    屏幕 Y 坐标
     * @param viewWidth  视口宽度
     * @param viewHeight 视口高度
     * @return true 如果太靠近任一边缘
     */
    public static boolean isNearEdge(double screenX, double screenY,
                                     double viewWidth, double viewHeight) {
        return isNearHorizontalEdge(screenX, viewWidth)
                || isNearVerticalEdge(screenY, viewHeight);
    }
}
