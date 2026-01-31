package com.bingbaihanji.util;

import com.bingbaihanji.view.BoundingBox;
import com.bingbaihanji.view.ResizeHandle;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;

import java.util.List;
import java.util.Map;

/**
 * BoundingBox变换工具类
 * <p>
 * 处理BoundingBox的缩放和旋转操作，类似Word中的图片操作
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */

@Deprecated // 该类已弃用,功能实现比较复杂,等后续有精力和时间再重构
public class BoundingBoxTransform {

    /**
     * 执行缩放变换（相对于初始位置）
     *
     * @param boundingBox        边界框对象
     * @param handle             被拖动的句柄
     * @param worldX             当前鼠标世界坐标X
     * @param worldY             当前鼠标世界坐标Y
     * @param initialPositions   初始位置映射
     * @param circleInitialRadii 圆的初始半径
     * @param maintainAspect     是否保持宽高比（Shift键）
     */
    public static void applyResize(BoundingBox boundingBox, ResizeHandle handle,
                                   double worldX, double worldY,
                                   Map<WorldObject.DraggablePoint, double[]> initialPositions,
                                   Map<CircleGeo, Double> circleInitialRadii,
                                   boolean maintainAspect) {
        if (handle == null || boundingBox.isEmpty() || initialPositions.isEmpty()) {
            return;
        }

        List<WorldObject> objects = boundingBox.getObjects();

        // 计算初始边界框
        double oldMinX = Double.MAX_VALUE;
        double oldMaxX = -Double.MAX_VALUE;
        double oldMinY = Double.MAX_VALUE;
        double oldMaxY = -Double.MAX_VALUE;

        for (double[] pos : initialPositions.values()) {
            oldMinX = Math.min(oldMinX, pos[0]);
            oldMaxX = Math.max(oldMaxX, pos[0]);
            oldMinY = Math.min(oldMinY, pos[1]);
            oldMaxY = Math.max(oldMaxY, pos[1]);
        }

        double centerX = (oldMinX + oldMaxX) / 2;
        double centerY = (oldMinY + oldMaxY) / 2;
        double oldWidth = oldMaxX - oldMinX;
        double oldHeight = oldMaxY - oldMinY;

        // 计算缩放因子
        double scaleX = 1.0;
        double scaleY = 1.0;

        switch (handle.getPosition()) {
            case TOP_LEFT:
                scaleX = (oldMaxX - worldX) / oldWidth;
                scaleY = (worldY - oldMinY) / oldHeight;
                break;
            case TOP:
                scaleY = (worldY - oldMinY) / oldHeight;
                break;
            case TOP_RIGHT:
                scaleX = (worldX - oldMinX) / oldWidth;
                scaleY = (worldY - oldMinY) / oldHeight;
                break;
            case RIGHT:
                scaleX = (worldX - oldMinX) / oldWidth;
                break;
            case BOTTOM_RIGHT:
                scaleX = (worldX - oldMinX) / oldWidth;
                scaleY = (oldMaxY - worldY) / oldHeight;
                break;
            case BOTTOM:
                scaleY = (oldMaxY - worldY) / oldHeight;
                break;
            case BOTTOM_LEFT:
                scaleX = (oldMaxX - worldX) / oldWidth;
                scaleY = (oldMaxY - worldY) / oldHeight;
                break;
            case LEFT:
                scaleX = (oldMaxX - worldX) / oldWidth;
                break;
        }

        // 保持宽高比
        if (maintainAspect) {
            double avgScale = (Math.abs(scaleX) + Math.abs(scaleY)) / 2;
            scaleX = Math.copySign(avgScale, scaleX);
            scaleY = Math.copySign(avgScale, scaleY);
        }

        // 防止缩放到0或负数
        if (Math.abs(scaleX) < 0.01) scaleX = Math.copySign(0.01, scaleX);
        if (Math.abs(scaleY) < 0.01) scaleY = Math.copySign(0.01, scaleY);

        // 对所有对象应用缩放（基于初始位置）
        for (WorldObject obj : objects) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                double[] initialPos = initialPositions.get(point);
                if (initialPos == null) continue;

                double oldX = initialPos[0];
                double oldY = initialPos[1];

                // 相对于中心点的坐标
                double relX = oldX - centerX;
                double relY = oldY - centerY;

                // 应用缩放
                double newRelX = relX * scaleX;
                double newRelY = relY * scaleY;

                // 转换回世界坐标
                double newX = centerX + newRelX;
                double newY = centerY + newRelY;

                point.updatePosition(newX, newY);
            }

            // 特殊处理：圆需要缩放半径
            if (obj instanceof CircleGeo circle && circleInitialRadii != null) {
                Double initialRadius = circleInitialRadii.get(circle);
                if (initialRadius != null && initialRadius > 0) {
                    double avgScale = (Math.abs(scaleX) + Math.abs(scaleY)) / 2;
                    double newR = initialRadius * avgScale;
                    if (newR > 0.01) {
                        circle.setR(newR);
                    }
                }
            }
        }

        // 更新边界框
        boundingBox.setObjects(objects);
    }

    /**
     * 执行旋转变换（相对于初始位置）
     *
     * @param boundingBox      边界框对象
     * @param worldX           当前鼠标世界坐标X
     * @param worldY           当前鼠标世界坐标Y
     * @param startX           拖动起始世界坐标X
     * @param startY           拖动起始世界坐标Y
     * @param initialPositions 初始位置映射
     */
    public static void applyRotation(BoundingBox boundingBox, double worldX, double worldY,
                                     double startX, double startY,
                                     Map<WorldObject.DraggablePoint, double[]> initialPositions) {
        if (boundingBox.isEmpty() || initialPositions.isEmpty()) {
            return;
        }

        List<WorldObject> objects = boundingBox.getObjects();

        // 计算初始边界框中心
        double centerX = 0;
        double centerY = 0;
        for (double[] pos : initialPositions.values()) {
            centerX += pos[0];
            centerY += pos[1];
        }
        centerX /= initialPositions.size();
        centerY /= initialPositions.size();

        // 计算旋转角度
        double startAngle = Math.atan2(startY - centerY, startX - centerX);
        double currentAngle = Math.atan2(worldY - centerY, worldX - centerX);
        double deltaAngle = currentAngle - startAngle;

        double cos = Math.cos(deltaAngle);
        double sin = Math.sin(deltaAngle);

        // 对所有对象应用旋转（基于初始位置）
        for (WorldObject obj : objects) {
            for (WorldObject.DraggablePoint point : obj.getDraggablePoints()) {
                double[] initialPos = initialPositions.get(point);
                if (initialPos == null) continue;

                double oldX = initialPos[0];
                double oldY = initialPos[1];

                // 相对于中心点的坐标
                double relX = oldX - centerX;
                double relY = oldY - centerY;

                // 应用旋转矩阵
                double newRelX = relX * cos - relY * sin;
                double newRelY = relX * sin + relY * cos;

                // 转换回世界坐标
                double newX = centerX + newRelX;
                double newY = centerY + newRelY;

                point.updatePosition(newX, newY);
            }
        }

        // 更新边界框
        boundingBox.setObjects(objects);
    }
}
