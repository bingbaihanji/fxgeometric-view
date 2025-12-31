package com.bingbaihanji.util;

import com.bingbaihanji.constant.LineType;
import javafx.scene.canvas.GraphicsContext;

/**
 * 线型工具类
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 提供线型应用和转换的工具方法
 */
public class LineStyleUtil {

    /**
     * 应用线型到GraphicsContext
     *
     * @param gc       绘图上下文
     * @param lineType 线型
     */
    public static void applyLineStyle(GraphicsContext gc, LineType lineType) {
        if (lineType == null || lineType == LineType.FULL) {
            // 实线：清除虚线模式
            gc.setLineDashes();
        } else {
            // 应用虚线模式
            double[] pattern = lineType.getDashPattern();
            if (pattern != null) {
                gc.setLineDashes(pattern);
            } else {
                gc.setLineDashes();
            }
        }
    }

    /**
     * 获取虚线模式数组
     *
     * @param lineType 线型
     * @return 虚线模式数组，实线返回null
     */
    public static double[] getDashPattern(LineType lineType) {
        if (lineType == null) {
            return null;
        }
        return lineType.getDashPattern();
    }

    /**
     * 重置线型为实线
     *
     * @param gc 绘图上下文
     */
    public static void resetLineStyle(GraphicsContext gc) {
        gc.setLineDashes();
    }
}
