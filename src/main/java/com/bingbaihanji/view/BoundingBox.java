package com.bingbaihanji.view;

import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 边界框类
 * <p>
 * 用于显示选中对象的边界框和8个调整句柄
 * 参考 GeoGebra 的 EuclidianBoundingBoxHandler 设计
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public class BoundingBox {

    /**
     * 边界框颜色
     */
    private static final Color BBOX_COLOR = Color.rgb(0, 150, 255, 0.8);
    private static final Color BBOX_FILL = Color.rgb(0, 150, 255, 0.05);
    /**
     * 关联的对象列表
     */
    private final List<WorldObject> objects = new ArrayList<>();
    /**
     * 8个调整句柄 + 1个旋转句柄
     */
    private final List<ResizeHandle> handles = new ArrayList<>();
    /**
     * 边界框的世界坐标(左下角和右上角)
     */
    private double minX, minY, maxX, maxY;
    private ResizeHandle rotateHandle;

    /**
     * 构造函数
     */
    public BoundingBox() {
        initializeHandles();
    }

    /**
     * 初始化句柄：8个缩放句柄 + 1个旋转句柄
     */
    private void initializeHandles() {
        // 8个缩放句柄
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.TOP_LEFT));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.TOP));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.TOP_RIGHT));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.RIGHT));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.BOTTOM_RIGHT));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.BOTTOM));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.BOTTOM_LEFT));
        handles.add(new ResizeHandle(ResizeHandle.HandlePosition.LEFT));

        // 1个旋转句柄(位于顶部中央上方)
        rotateHandle = new ResizeHandle(ResizeHandle.HandlePosition.ROTATE);
    }

    /**
     * 计算边界框范围
     */
    private void calculateBounds() {
        if (objects.isEmpty()) {
            minX = minY = maxX = maxY = 0;
            return;
        }

        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;

        for (WorldObject obj : objects) {
            double[] bbox = obj.getBoundingBox();
            if (bbox != null && bbox.length >= 4) {
                // bbox格式：[minX, maxX, minY, maxY]
                minX = Math.min(minX, bbox[0]);
                maxX = Math.max(maxX, bbox[1]);
                minY = Math.min(minY, bbox[2]);
                maxY = Math.max(maxY, bbox[3]);
            }
        }

        // 添加一些边距
        double padding = 0.2;
        double width = maxX - minX;
        double height = maxY - minY;
        minX -= width * padding;
        maxX += width * padding;
        minY -= height * padding;
        maxY += height * padding;
    }

    /**
     * 更新句柄位置：8个缩放句柄 + 1个旋转句柄
     */
    private void updateHandlePositions() {
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        // 更新缩放句柄位置
        for (ResizeHandle handle : handles) {
            switch (handle.getPosition()) {
                case TOP_LEFT:
                    handle.setWorldPosition(minX, maxY);
                    break;
                case TOP:
                    handle.setWorldPosition(centerX, maxY);
                    break;
                case TOP_RIGHT:
                    handle.setWorldPosition(maxX, maxY);
                    break;
                case RIGHT:
                    handle.setWorldPosition(maxX, centerY);
                    break;
                case BOTTOM_RIGHT:
                    handle.setWorldPosition(maxX, minY);
                    break;
                case BOTTOM:
                    handle.setWorldPosition(centerX, minY);
                    break;
                case BOTTOM_LEFT:
                    handle.setWorldPosition(minX, minY);
                    break;
                case LEFT:
                    handle.setWorldPosition(minX, centerY);
                    break;
            }
        }

        // 更新旋转句柄位置(位于顶部中央上方,适中距离)
        double rotateOffset = 1.5; // 1.5个世界坐标单位,约等于30像素在默认缩放下
        rotateHandle.setWorldPosition(centerX, maxY + rotateOffset);
    }

    /**
     * 绘制边界框和句柄
     *
     * @param gc        绘制上下文
     * @param transform 坐标变换
     */
    public void paint(GraphicsContext gc, WorldTransform transform) {
        if (objects.isEmpty()) {
            return;
        }

        // 转换到屏幕坐标
        double screenMinX = transform.worldToScreenX(minX);
        double screenMinY = transform.worldToScreenY(minY);
        double screenMaxX = transform.worldToScreenX(maxX);
        double screenMaxY = transform.worldToScreenY(maxY);

        double screenWidth = screenMaxX - screenMinX;
        double screenHeight = screenMaxY - screenMinY;

        // 绘制填充(半透明)
        gc.setFill(BBOX_FILL);
        gc.fillRect(screenMinX, screenMaxY, screenWidth, -screenHeight);

        // 绘制边框(虚线)
        gc.setStroke(BBOX_COLOR);
        gc.setLineWidth(1.5);
        gc.setLineDashes(5, 5);
        gc.strokeRect(screenMinX, screenMaxY, screenWidth, -screenHeight);
        gc.setLineDashes(); // 重置虚线

        // 绘制句柄(8个缩放 + 1个旋转)
        for (ResizeHandle handle : handles) {
            handle.paint(gc, transform);
        }

        // 绘制旋转句柄
        if (rotateHandle != null) {
            rotateHandle.paint(gc, transform);

            // 绘制旋转句柄到边界框的连线(虚线)
            double centerX = (minX + maxX) / 2;
            double screenCenterX = transform.worldToScreenX(centerX);
            double screenMaxYRotate = transform.worldToScreenY(maxY);
            double screenRotateY = transform.worldToScreenY(rotateHandle.getWorldY());

            gc.setStroke(Color.rgb(255, 150, 0, 0.5));
            gc.setLineWidth(1);
            gc.setLineDashes(3, 3);
            gc.strokeLine(screenCenterX, screenMaxYRotate, screenCenterX, screenRotateY);
            gc.setLineDashes(); // 重置虚线
        }
    }

    /**
     * 检测句柄命中(包括缩放句柄和旋转句柄)
     *
     * @param worldX    世界坐标X
     * @param worldY    世界坐标Y
     * @param tolerance 容差
     * @return 命中的句柄,如果没有则返回null
     */
    public ResizeHandle hitTestHandles(double worldX, double worldY, double tolerance) {
        // 优先检测旋转句柄
        if (rotateHandle != null && rotateHandle.hitTest(worldX, worldY, tolerance)) {
            return rotateHandle;
        }

        // 检测缩放句柄
        for (ResizeHandle handle : handles) {
            if (handle.hitTest(worldX, worldY, tolerance)) {
                return handle;
            }
        }
        return null;
    }

    /**
     * 获取边界框范围
     *
     * @return [minX, maxX, minY, maxY]
     */
    public double[] getBounds() {
        return new double[]{minX, maxX, minY, maxY};
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return objects.isEmpty();
    }

    /**
     * 清空
     */
    public void clear() {
        objects.clear();
    }

    /**
     * 获取对象列表
     */
    public List<WorldObject> getObjects() {
        return new ArrayList<>(objects);
    }

    /**
     * 设置对象并计算边界框
     *
     * @param objects 对象列表
     */
    public void setObjects(List<WorldObject> objects) {
        this.objects.clear();
        this.objects.addAll(objects);
        calculateBounds();
        updateHandlePositions();
    }

    /**
     * 获取边界框中心点
     *
     * @return [中心X, 中心Y]
     */
    public double[] getCenter() {
        return new double[]{(minX + maxX) / 2, (minY + maxY) / 2};
    }
}
