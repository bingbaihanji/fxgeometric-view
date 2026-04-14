package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.controller.SelectionManager;
import com.bingbaihanji.util.Hits;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * 选择处理器
 * <p>
 * 处理对象的选择操作,支持单选、多选(Ctrl+点击)和框选
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class SelectionHandler extends AbstractDrawingHandler {

    /**
     * 框选阈值(像素)- 防止误触
     * <p>
     * 只有当鼠标移动超过此阈值时,才开始显示框选矩形
     */
    private static final double SELECTION_RECT_THRESHOLD = 5.0; // 5像素

    /**
     * 框选模式
     */
    private boolean boxSelecting = false;

    /**
     * 框选起始点(屏幕坐标)
     */
    private double boxStartX = 0;
    private double boxStartY = 0;

    /**
     * 框选当前点(屏幕坐标)
     */
    private double boxCurrentX = 0;
    private double boxCurrentY = 0;

    @Override
    public boolean canHandle(DrawMode mode) {
        // 选择功能在非绘制模式下生效
        return mode == DrawMode.NONE;
    }

    @Override
    public boolean handleMousePressed(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || e.getButton() != MouseButton.PRIMARY) {
            return false;
        }

        // 检查是否按下Ctrl键(多选模式)
        boolean ctrlPressed = e.isControlDown();

        double worldX = context.getGridChartPane().screenToWorldX(e.getX());
        double worldY = context.getGridChartPane().screenToWorldY(e.getY());

        // 计算容差
        double scale = context.getTransform().getScale();
        double tolerance = 5.0 / scale; // 5像素的点击范围
        double vertexTolerance = 10.0 / scale; // 控制点使用更大的容差(10像素)

        // 优先级1：检查是否点击了控制点  
        // 控制点具有最高优先级,如果点击了控制点,不选中对象,让 DragHandler 处理
        Hits.HitPoint hitPoint = Hits.performPointHitTest(context.getObjects(), worldX, worldY, vertexTolerance);
        if (hitPoint != null) {
            // 点击了控制点,不处理选择逻辑,让 DragHandler 处理拖动
            return false;
        }

        // 优先级2：使用 Hits 检查是否点击了对象(非控制点部分) 
        // 尝试点击选中对象
        Hits hits = Hits.performHitTest(context.getObjects(), worldX, worldY, tolerance);
        hits.sortByZOrder(); // 按优先级排序

        WorldObject clickedObject = hits.getTopHit(); // 获取最高优先级的对象

        if (clickedObject != null) {
            // 命中对象(非控制点部分)
            SelectionManager selectionManager = context.getSelectionManager();

            if (ctrlPressed) {
                // Ctrl+点击：切换选中状态
                selectionManager.toggleSelection(clickedObject);
                context.getGridChartPane().redraw();
                return true; // 已处理,阻止拖动
            } else {
                // 普通点击：单选
                if (!clickedObject.isSelected()) {
                    // 对象未选中,执行选中操作
                    selectionManager.selectOnly(clickedObject);
                    context.getGridChartPane().redraw();
                    return true; // 已处理,阻止拖动
                } else {
                    // 对象已选中,不处理,让 DragHandler 处理拖动
                    return false; // 未处理,允许拖动
                }
            }
        } else {
            // 未命中对象
            // 已禁用：检查是否点击了BoundingBox区域内或旋转句柄(不清除选择)
            /*
            if (context.getSelectionManager().hasBoundingBox()) {
                BoundingBox bbox = context.getSelectionManager().getBoundingBox();
                double[] bounds = bbox.getBounds();
                double minX = bounds[0];
                double maxX = bounds[1];
                double minY = bounds[2];
                double maxY = bounds[3];
                
                // 检查是否点击了旋转句柄区域(旋转句柄在边界之外)
                double handleTolerance = 15.0 / scale; // 句柄区域容差
                ResizeHandle handle = bbox.hitTestHandles(worldX, worldY, handleTolerance);
                if (handle != null) {
                    // 点击了句柄,交由DragHandler处理
                    return false;
                }
                
                // 如果点击在BoundingBox边界内,不清除选择,交由DragHandler处理
                if (worldX >= minX && worldX <= maxX && worldY >= minY && worldY <= maxY) {
                    return false; // 不处理,让DragHandler处理
                }
            }
            */

            // 确实点击了空白区域,清除选择并启动框选
            // 在启动框选之前,再次检查是否接近控制点(使用更大的容差)
            // 这样可以避免用户点击控制点时稍微偏离而触发框选
            double largerTolerance = 15.0 / scale; // 使用更大的容差(15像素)
            Hits.HitPoint nearPoint = Hits.performPointHitTest(context.getObjects(), worldX, worldY, largerTolerance);
            if (nearPoint != null) {
                // 接近控制点,不启动框选,让 DragHandler 处理
                return false;
            }

            // 确实没有接近任何控制点或对象,启动框选
            if (!ctrlPressed) {
                // 如果没有按Ctrl,清除之前的选择
                SelectionManager selectionManager = context.getSelectionManager();
                selectionManager.clearSelection();
                context.getGridChartPane().redraw();
            }

            // 开始框选
            boxSelecting = true;
            boxStartX = e.getX();
            boxStartY = e.getY();
            boxCurrentX = e.getX();
            boxCurrentY = e.getY();
            return true; // 已处理
        }
    }

    @Override
    public boolean handleMouseDragged(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || !boxSelecting) {
            return false;
        }

        // 计算鼠标移动距离
        double dx = e.getX() - boxStartX;
        double dy = e.getY() - boxStartY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 只有超过阈值才显示框选矩形
        if (distance < SELECTION_RECT_THRESHOLD) {
            // 消费事件但不绘制矩形
            return true;
        }

        // 更新框选矩形
        boxCurrentX = e.getX();
        boxCurrentY = e.getY();
        context.getGridChartPane().redraw();
        return true;
    }

    @Override
    public boolean handleMouseReleased(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode()) || !boxSelecting) {
            return false;
        }

        // 完成框选
        boxCurrentX = e.getX();
        boxCurrentY = e.getY();

        // 先转换起始点和结束点到世界坐标
        double worldStartX = context.getGridChartPane().screenToWorldX(boxStartX);
        double worldStartY = context.getGridChartPane().screenToWorldY(boxStartY);
        double worldEndX = context.getGridChartPane().screenToWorldX(boxCurrentX);
        double worldEndY = context.getGridChartPane().screenToWorldY(boxCurrentY);

        // 计算框选矩形(确保 x1 < x2, y1 < y2)
        double worldX1 = Math.min(worldStartX, worldEndX);
        double worldY1 = Math.min(worldStartY, worldEndY);
        double worldX2 = Math.max(worldStartX, worldEndX);
        double worldY2 = Math.max(worldStartY, worldEndY);

        // 选中框内的所有对象
        SelectionManager selectionManager = context.getSelectionManager();
        boolean ctrlPressed = e.isControlDown();

        for (WorldObject obj : context.getObjects()) {
            if (isObjectInBox(obj, worldX1, worldY1, worldX2, worldY2)) {
                if (ctrlPressed) {
                    // Ctrl模式：添加到选择集
                    if (!obj.isSelected()) {
                        selectionManager.addSelectedObject(obj);
                    }
                } else {
                    // 普通模式：直接选中
                    selectionManager.addSelectedObject(obj);
                }
            }
        }

        // 结束框选
        boxSelecting = false;
        context.getGridChartPane().redraw();
        return true;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (boxSelecting) {
            // 计算距离,只有超过阈值才绘制框选矩形
            double dx = boxCurrentX - boxStartX;
            double dy = boxCurrentY - boxStartY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < SELECTION_RECT_THRESHOLD) {
                // 未超过阈值,不绘制
                return;
            }

            // 绘制框选矩形
            double x = Math.min(boxStartX, boxCurrentX);
            double y = Math.min(boxStartY, boxCurrentY);
            double width = Math.abs(boxCurrentX - boxStartX);
            double height = Math.abs(boxCurrentY - boxStartY);

            // 填充半透明矩形
            gc.setFill(new Color(0.5, 0.7, 1.0, 0.2));
            gc.fillRect(x, y, width, height);

            // 绘制边框
            gc.setStroke(new Color(0.3, 0.5, 0.8, 0.6));
            gc.setLineWidth(1.5);
            gc.setLineDashes(5, 5);
            gc.strokeRect(x, y, width, height);
            gc.setLineDashes(); // 重置虚线
        }
    }

    @Override
    public void reset() {
        boxSelecting = false;
    }

    /**
     * 判断对象是否在框选矩形内
     *
     * @param obj 对象
     * @param x1  矩形左下角X(世界坐标)
     * @param y1  矩形左下角Y(世界坐标)
     * @param x2  矩形右上角X(世界坐标)
     * @param y2  矩形右上角Y(世界坐标)
     * @return 是否在矩形内
     */
    private boolean isObjectInBox(WorldObject obj, double x1, double y1, double x2, double y2) {
        // 获取对象的边界框
        double[] bbox = obj.getBoundingBox();
        if (bbox == null) {
            // 如果对象没有实现getBoundingBox,使用简单的点判断
            // 尝试获取对象的可拖动点
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                double px = point.getX();
                double py = point.getY();
                if (px >= x1 && px <= x2 && py >= y1 && py <= y2) {
                    return true;
                }
            }
            return false;
        }

        // 使用边界框判断
        double objMinX = bbox[0];
        double objMaxX = bbox[1];
        double objMinY = bbox[2];
        double objMaxY = bbox[3];

        // 检查边界框是否与选择框相交或包含
        return !(objMaxX < x1 || objMinX > x2 || objMaxY < y1 || objMinY > y2);
    }
}
