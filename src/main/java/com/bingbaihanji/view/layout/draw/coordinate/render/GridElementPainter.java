package com.bingbaihanji.view.layout.draw.coordinate.render;

import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import com.bingbaihanji.view.layout.draw.coordinate.grid.GridElement;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * GridElement 统一绘制器
 * <p>
 * 遍历 GridElement 列表，根据类型分发到具体绘制方法。
 * 将同类型线段汇入单个 beginPath()/stroke() 路径批次，
 * 减少 GPU 绘制调用次数（参考 GeoGebra 的 GeneralPath 批处理模式）。
 *
 * @author bingbaihanji
 */
public class GridElementPainter {

    /** 网格点直径（像素） */
    private static final double DOT_SIZE = 2.0;

    /**
     * 绘制所有网格元素
     * <p>
     * 分两次遍历：先次网格（低层，细线），再主网格（上层，粗线），
     * 最后绘制点状网格节点。
     *
     * @param gc       画布上下文
     * @param elements 网格元素列表
     * @param settings 视图配置
     */
    public void paint(GraphicsContext gc, List<GridElement> elements,
                      EuclidianViewSettings settings) {
        if (elements.isEmpty()) {
            return;
        }

        // 次网格先绘制，位于主网格下方，颜色较淡
        paintByType(gc, elements, true, settings.getSubGridColor(), 0.5, settings);
        // 主网格后绘制，位于次网格上方
        paintByType(gc, elements, false, settings.getGridColor(), 1.0, settings);
        // 点状网格节点单独绘制
        paintDots(gc, elements, settings.getGridColor());
    }

    /** 绘制指定类型的网格线段 */
    private void paintByType(GraphicsContext gc, List<GridElement> elements, boolean subGrid,
                             Color color, double lineWidth, EuclidianViewSettings settings) {
        boolean hasContent = false;
        for (GridElement element : elements) {
            if (matchesType(element, subGrid)) {
                hasContent = true;
                break;
            }
        }
        if (!hasContent) return;

        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        LineStyleUtil.applyLineStyle(gc, settings.getGridLineType());
        gc.beginPath();

        for (GridElement element : elements) {
            if (element instanceof GridElement.GridLineSegment seg) {
                if (seg.isSubGrid() == subGrid) {
                    gc.moveTo(seg.start().getX(), seg.start().getY());
                    gc.lineTo(seg.end().getX(), seg.end().getY());
                }
            } else if (element instanceof GridElement.GridCircle circle) {
                if (circle.isSubGrid() == subGrid) {
                    gc.moveTo(
                            circle.center().getX() + circle.radiusX(),
                            circle.center().getY());
                    gc.arc(circle.center().getX(), circle.center().getY(),
                            circle.radiusX(), circle.radiusY(),
                            circle.startAngle(), circle.length());
                }
            }
        }

        gc.stroke();
        LineStyleUtil.resetLineStyle(gc);
    }

    /** 检查元素是否属于目标类型 */
    private boolean matchesType(GridElement element, boolean subGrid) {
        if (element instanceof GridElement.GridLineSegment seg) {
            return seg.isSubGrid() == subGrid;
        } else if (element instanceof GridElement.GridCircle circle) {
            return circle.isSubGrid() == subGrid;
        }
        return false;
    }

    /** 绘制点状网格节点 */
    private void paintDots(GraphicsContext gc, List<GridElement> elements, Color color) {
        gc.setFill(color);
        for (GridElement element : elements) {
            if (element instanceof GridElement.GridDot dot) {
                gc.fillOval(dot.position().getX() - DOT_SIZE / 2,
                        dot.position().getY() - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
            }
        }
    }
}
