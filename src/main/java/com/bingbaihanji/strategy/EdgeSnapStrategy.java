package com.bingbaihanji.strategy;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.visitor.EdgeSnapVisitor;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

/**
 * 边吸附策略
 * <p>
 * 中等优先级的吸附策略，吸附到几何图形的边缘
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class EdgeSnapStrategy implements SnapStrategy {

    @Override
    public double[] snap(double x, double y, DrawingContext context) {
        // 计算吸附阈值
        double scale = context.getTransform().getScale();
        double threshold = GeometryConfig.Snapping.EDGE_SNAP_THRESHOLD_PIXELS / scale;

        // 使用访问者模式查找最近的边
        EdgeSnapVisitor visitor = new EdgeSnapVisitor(x, y, threshold);
        EdgeSnapVisitor.SnapResult bestSnap = null;

        for (WorldObject obj : context.getObjects()) {
            EdgeSnapVisitor.SnapResult snap = obj.accept(visitor);
            if (snap != null && (bestSnap == null || snap.distance < bestSnap.distance)) {
                bestSnap = snap;
            }
        }

        if (bestSnap != null) {
            return new double[]{bestSnap.x, bestSnap.y};
        }

        return null; // 未找到可吸附的边
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public String getName() {
        return "EdgeSnap";
    }
}
