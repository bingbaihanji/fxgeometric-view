package com.bingbaihanji.view;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/**
 * 调整句柄类
 * <p>
 * 表示BoundingBox的8个调整句柄
 * 参考 GeoGebra 的调整句柄设计
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public class ResizeHandle {

    /**
     * 句柄大小(屏幕像素)
     */
    private static final double HANDLE_SIZE = 8;
    /**
     * 句柄颜色
     */
    private static final Color HANDLE_COLOR = GeometryConfig.Colors.HANDLE_COLOR;
    private static final Color HANDLE_FILL = GeometryConfig.Colors.HANDLE_FILL;
    /**
     * 句柄位置
     */
    private final HandlePosition position;
    /**
     * 世界坐标
     */
    private double worldX;
    private double worldY;

    /**
     * 构造函数
     *
     * @param position 句柄位置
     */
    public ResizeHandle(HandlePosition position) {
        this.position = position;
    }

    /**
     * 设置世界坐标
     */
    public void setWorldPosition(double x, double y) {
        this.worldX = x;
        this.worldY = y;
    }

    /**
     * 获取位置
     */
    public HandlePosition getPosition() {
        return position;
    }

    /**
     * 获取世界坐标X
     */
    public double getWorldX() {
        return worldX;
    }

    /**
     * 获取世界坐标Y
     */
    public double getWorldY() {
        return worldY;
    }

    /**
     * 绘制句柄
     *
     * @param gc        绘制上下文
     * @param transform 坐标变换
     */
    public void paint(GraphicsContext gc, WorldTransform transform) {
        double screenX = transform.worldToScreenX(worldX);
        double screenY = transform.worldToScreenY(worldY);

        double halfSize = HANDLE_SIZE / 2;

        if (position == HandlePosition.ROTATE) {
            // 旋转句柄绘制为圆形
            gc.setFill(HANDLE_FILL);
            gc.fillOval(screenX - halfSize, screenY - halfSize, HANDLE_SIZE, HANDLE_SIZE);

            gc.setStroke(GeometryConfig.Colors.ROTATION_HANDLE_COLOR); // 橙色边框
            gc.setLineWidth(1.5);
            gc.strokeOval(screenX - halfSize, screenY - halfSize, HANDLE_SIZE, HANDLE_SIZE);

            // 绘制旋转图标(小箭头)
            gc.setStroke(GeometryConfig.Colors.ROTATION_HANDLE_COLOR);
            gc.setLineWidth(1);
            gc.strokeArc(screenX - 3, screenY - 3, 6, 6, 45, 270, ArcType.OPEN);
        } else {
            // 缩放句柄绘制为矩形
            gc.setFill(HANDLE_FILL);
            gc.fillRect(screenX - halfSize, screenY - halfSize, HANDLE_SIZE, HANDLE_SIZE);

            gc.setStroke(HANDLE_COLOR);
            gc.setLineWidth(1.5);
            gc.strokeRect(screenX - halfSize, screenY - halfSize, HANDLE_SIZE, HANDLE_SIZE);
        }
    }

    /**
     * 命中测试
     *
     * @param worldX    测试点的世界X坐标
     * @param worldY    测试点的世界Y坐标
     * @param tolerance 容差(世界坐标)
     * @return 是否命中
     */
    public boolean hitTest(double worldX, double worldY, double tolerance) {
        double dx = Math.abs(worldX - this.worldX);
        double dy = Math.abs(worldY - this.worldY);
        return dx <= tolerance && dy <= tolerance;
    }

    /**
     * 获取光标类型
     * 根据句柄位置返回相应的光标样式
     */
    public Cursor getCursor() {
        return switch (position) {
            case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
            case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
            case TOP, BOTTOM -> Cursor.N_RESIZE;
            case LEFT, RIGHT -> Cursor.E_RESIZE;
            case ROTATE -> Cursor.HAND; // 旋转使用手型光标
        };
    }

    @Override
    public String toString() {
        return "ResizeHandle{" + position + " at (" + worldX + ", " + worldY + ")}";
    }

    /**
     * 句柄位置枚举
     */
    public enum HandlePosition {
        TOP_LEFT,       // 左上
        TOP,            // 上
        TOP_RIGHT,      // 右上
        RIGHT,          // 右
        BOTTOM_RIGHT,   // 右下
        BOTTOM,         // 下
        BOTTOM_LEFT,    // 左下
        LEFT,           // 左
        ROTATE          // 旋转句柄
    }
}
