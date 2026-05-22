package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.*;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形几何图形
 * <p>
 * 支持依次选点绘制多边形,当终点与起点重合时完成绘制
 * 多边形直接引用PointGeo对象作为顶点,实现点复用
 *
 * @author bingbaihanji
 * @date 2025-12-23
 */
public class PolygonGeo extends AbstractWorldObject {

    /**
     * 多边形顶点列表(直接引用PointGeo对象)
     * 多边形不再自己管理顶点坐标,而是从引用的PointGeo获取
     */
    private final List<PointGeo> vertexPoints;

    /**
     * 构造函数
     *
     * @param vertices 顶点坐标数组 [x1, y1, x2, y2, ...]
     */
    public PolygonGeo(double... vertices) {
        this(ObjectType.POLYGON, vertices);
    }

    /**
     * 构造函数(指定类型) - 用于反序列化还原三角形/矩形等子类型
     *
     * @param type     对象类型
     * @param vertices 顶点坐标数组 [x1, y1, x2, y2, ...]
     */
    public PolygonGeo(ObjectType type, double... vertices) {
        super(type);
        if (vertices.length < 6) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }
        if (vertices.length % 2 != 0) {
            throw new IllegalArgumentException("顶点坐标数组长度必须是偶数");
        }

        this.vertexPoints = new ArrayList<>();
        this.color = StyleManager.GEOMETRY_LINE;

        // 为每个坐标创建内部顶点点对象(不显示名称,作为多边形内部顶点)
        for (int i = 0; i < vertices.length; i += 2) {
            PointGeo point = new PointGeo(vertices[i], vertices[i + 1]);
            point.setPolygonVertex(true); // 标记为多边形内部顶点
            this.vertexPoints.add(point);
        }
    }

    /**
     * 构造函数(从PointGeo引用列表)
     * 直接复用已有的点对象
     *
     * @param pointRefs 点对象引用列表
     */
    public PolygonGeo(List<PointGeo> pointRefs) {
        super(ObjectType.POLYGON);
        if (pointRefs.size() < 3) {
            throw new IllegalArgumentException("多边形至少需要3个顶点");
        }

        this.vertexPoints = new ArrayList<>(pointRefs);
        this.color = StyleManager.GEOMETRY_LINE;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (!visible || vertexPoints.isEmpty()) return;

        // 从引用的点对象获取坐标
        double[] xPoints = new double[vertexPoints.size()];
        double[] yPoints = new double[vertexPoints.size()];

        for (int i = 0; i < vertexPoints.size(); i++) {
            PointGeo point = vertexPoints.get(i);
            xPoints[i] = transform.worldToScreenX(point.getX());
            yPoints[i] = transform.worldToScreenY(point.getY());
        }

        // 先绘制填充
        FillRenderer.fillPolygon(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, xPoints, yPoints, vertexPoints.size());

        // 再绘制多边形边框
        // 应用线型
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokePolygon(xPoints, yPoints, vertexPoints.size());

        // 重置线型
        LineStyleUtil.resetLineStyle(gc);

        // 只绘制多边形自己创建的内部顶点(isPolygonVertex=true的点)
        // 复用的外部点由它们自己负责绘制
        gc.setFill(getEffectiveColor());
        double pointRadius = 3;
        for (int i = 0; i < vertexPoints.size(); i++) {
            PointGeo point = vertexPoints.get(i);
            if (point.isPolygonVertex()) {
                // 这是多边形内部创建的点,需要绘制
                gc.fillOval(xPoints[i] - pointRadius, yPoints[i] - pointRadius,
                        pointRadius * 2, pointRadius * 2);
                // 绘制名称
                String name = point.getName();
                if (name != null && !name.isEmpty()) {
                    LabelRenderer.renderLabel(gc, name, xPoints[i], yPoints[i]);
                }
            }
            // 复用的外部点不在这里绘制,它们作为独立对象会自己绘制
        }
    }

    @Override
    public boolean hitTest(double wx, double wy, double tol) {
        // 简单的命中测试：检查是否在多边形边界附近或内部
        for (int i = 0; i < vertexPoints.size(); i++) {
            PointGeo p1 = vertexPoints.get(i);
            PointGeo p2 = vertexPoints.get((i + 1) % vertexPoints.size());

            // 检查点到线段的距离
            double dist = MathCalculationUtils.pointToSegmentDistance(wx, wy, p1.getX(), p1.getY(), p2.getX(), p2.getY());
            if (dist < tol) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(double wx, double wy) {
        System.out.println("多边形被点击");
    }

    /**
     * 获取顶点数量
     */
    public int getVertexCount() {
        return vertexPoints.size();
    }

    /**
     * 获取指定索引的顶点坐标
     */
    public javafx.geometry.Point2D getVertex(int index) {
        PointGeo p = vertexPoints.get(index);
        return new javafx.geometry.Point2D(p.getX(), p.getY());
    }

    /**
     * 获取指定索引的顶点点对象
     */
    public PointGeo getVertexPoint(int index) {
        return vertexPoints.get(index);
    }

    /**
     * 获取所有顶点点对象
     */
    public List<PointGeo> getVertexPoints() {
        return vertexPoints;
    }

    /**
     * 获取多边形的所有边(作为线段)
     *
     * @return 线段列表
     */
    public List<LineGeo> getEdges() {
        if (!edgesDirty && cachedEdges != null) {
            return cachedEdges;
        }
        cachedEdges = new ArrayList<>();
        for (int i = 0; i < vertexPoints.size(); i++) {
            PointGeo p1 = vertexPoints.get(i);
            PointGeo p2 = vertexPoints.get((i + 1) % vertexPoints.size());
            cachedEdges.add(new LineGeo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), false));
        }
        edgesDirty = false;
        return cachedEdges;
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 所有顶点都可拖动
        List<DraggablePoint> points = new ArrayList<>();
        for (int i = 0; i < vertexPoints.size(); i++) {
            final int index = i;
            PointGeo vertexPoint = vertexPoints.get(index);

            points.add(new DraggablePoint(vertexPoint.getX(), vertexPoint.getY(), (newX, newY) -> {
                // 直接更新点对象的位置
                // 如果点有约束,updatePosition会自动处理约束逻辑
                vertexPoint.updatePosition(newX, newY);
                invalidateEdgeCache();
            }));
        }
        return points;
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // 旋转所有顶点(只旋转多边形自己创建的内部顶点)
        for (PointGeo point : vertexPoints) {
            if (point.isPolygonVertex() && !point.isConstrained()) {
                double dx = point.getX() - centerX;
                double dy = point.getY() - centerY;
                double newX = centerX + dx * cos - dy * sin;
                double newY = centerY + dx * sin + dy * cos;
                point.updatePosition(newX, newY);
            }
            // 有约束的点或外部复用的点不参与旋转
        }
        invalidateEdgeCache();
    }

    @Override
    public double[] getBoundingBox() {
        return computeBoundingBox(vertexPoints, PointGeo::getX, PointGeo::getY);
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitPolygon(this);
    }
}
