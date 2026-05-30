package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.*;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 正多边形几何图形
 * <p>
 * 支持3-10边的正多边形,通过中心点、半径和边数定义
 * 支持中心点复用：如果中心位置已有PointGeo,直接引用而不是创建新点
 */
public class RegularPolygonGeo extends AbstractWorldObject {

    /**
     * 中心坐标(支持 PointGeo 引用复用)
     */
    private final ReusableCoordinate center;
    private double radius;
    private int sideCount;
    private double startAngle;
    /**
     * 顶点点对象列表(内部创建,标记为多边形顶点)
     */
    private List<PointGeo> vertexPoints;

    /**
     * 顶点缓存(从中心+半径+边数计算)
     */
    private List<Point2D> cachedVertices;
    private boolean verticesCacheValid = false;

    /**
     * 基础构造函数(坐标方式)
     */
    public RegularPolygonGeo(double cx, double cy, double radius, int sideCount) {
        super(ObjectType.REGULAR_POLYGON);
        this.radius = radius;
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        this.startAngle = -Math.PI / 2;
        this.center = new ReusableCoordinate(cx, cy,
                PointNameManager.getInstance().assignCenterName(cx, cy));
        this.color = StyleManager.defaultLineColor;
        this.cachedVertices = new ArrayList<>();
        this.vertexPoints = new ArrayList<>();
        createVertexPoints();
    }

    /**
     * 构造函数(坐标方式,可选自动命名)
     */
    public RegularPolygonGeo(double cx, double cy, double radius, int sideCount, boolean autoNameCenter) {
        super(ObjectType.REGULAR_POLYGON);
        this.radius = radius;
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        this.startAngle = -Math.PI / 2;
        this.center = new ReusableCoordinate(cx, cy,
                autoNameCenter ? PointNameManager.getInstance().assignCenterName(cx, cy) : null);
        this.color = StyleManager.defaultLineColor;
        this.cachedVertices = new ArrayList<>();
        this.vertexPoints = new ArrayList<>();
        createVertexPoints();
    }

    /**
     * 构造函数(点引用方式)- 复用已有点作为中心
     */
    public RegularPolygonGeo(PointGeo centerPoint, double cx, double cy, double radius, int sideCount) {
        this(centerPoint, cx, cy, radius, sideCount, -Math.PI / 2);
    }

    /**
     * 构造函数(点引用方式,带起始角度)- 复用已有点作为中心
     */
    public RegularPolygonGeo(PointGeo centerPoint, double cx, double cy, double radius, int sideCount, double startAngle) {
        super(ObjectType.REGULAR_POLYGON);
        this.radius = radius;
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        this.startAngle = startAngle;
        this.center = new ReusableCoordinate(centerPoint, cx, cy);
        if (centerPoint == null) {
            this.center.setName(PointNameManager.getInstance().assignCenterName(cx, cy));
        }
        this.color = StyleManager.defaultLineColor;
        this.cachedVertices = new ArrayList<>();
        this.vertexPoints = new ArrayList<>();
        createVertexPoints();
    }

    public double getCenterX() {
        return center.getX();
    }

    public double getCenterY() {
        return center.getY();
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
        verticesCacheValid = false;
        invalidateEdgeCache();
    }

    public int getSideCount() {
        return sideCount;
    }

    public void setSideCount(int sideCount) {
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        verticesCacheValid = false;
        invalidateEdgeCache();
    }

    /**
     * 获取内部中心X坐标(忽略引用,用于序列化)
     */
    public double getCx() {
        return center.getRawX();
    }

    /**
     * 获取内部中心Y坐标(忽略引用,用于序列化)
     */
    public double getCy() {
        return center.getRawY();
    }

    public double getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(double startAngle) {
        this.startAngle = startAngle;
    }

    public String getCenterName() {
        return center.getName();
    }

    public void setCenterName(String name) {
        center.setName(name);
    }

    public PointGeo getCenterPointRef() {
        return center.getRef();
    }

    private void calculateVertices() {
        if (verticesCacheValid) return;

        cachedVertices.clear();
        double angleStep = 2 * Math.PI / sideCount;

        for (int i = 0; i < sideCount; i++) {
            double angle = startAngle + i * angleStep;
            double x = getCenterX() + radius * Math.cos(angle);
            double y = getCenterY() + radius * Math.sin(angle);
            cachedVertices.add(new Point2D(x, y));
        }

        for (int i = 0; i < vertexPoints.size() && i < cachedVertices.size(); i++) {
            Point2D vertex = cachedVertices.get(i);
            vertexPoints.get(i).updatePosition(vertex.getX(), vertex.getY());
        }

        verticesCacheValid = true;
    }

    private void createVertexPoints() {
        vertexPoints.clear();
        calculateVertices();

        for (Point2D vertex : cachedVertices) {
            PointGeo vertexPoint = new PointGeo(vertex.getX(), vertex.getY());
            vertexPoint.setPolygonVertex(true);
            vertexPoint.setName(PointNameManager.getInstance().assignName(vertex.getX(), vertex.getY()));
            vertexPoints.add(vertexPoint);
        }
    }

    public List<PointGeo> getVertexPoints() {
        return vertexPoints;
    }

    public List<Point2D> getVertices() {
        calculateVertices();
        return new ArrayList<>(cachedVertices);
    }

    public List<LineGeo> getEdges() {
        calculateVertices();
        if (!edgesDirty && cachedEdges != null) {
            return cachedEdges;
        }
        cachedEdges = new ArrayList<>();
        for (int i = 0; i < cachedVertices.size(); i++) {
            Point2D p1 = cachedVertices.get(i);
            Point2D p2 = cachedVertices.get((i + 1) % cachedVertices.size());
            cachedEdges.add(new LineGeo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), false));
        }
        edgesDirty = false;
        return cachedEdges;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (!visible) return;
        calculateVertices();

        double[] xPoints = new double[sideCount];
        double[] yPoints = new double[sideCount];

        for (int i = 0; i < sideCount; i++) {
            Point2D vertex = cachedVertices.get(i);
            xPoints[i] = transform.worldToScreenX(vertex.getX());
            yPoints[i] = transform.worldToScreenY(vertex.getY());
        }

        FillRenderer.fillPolygon(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, xPoints, yPoints, sideCount);

        // 发光通道（稍宽、半透明、实线）
        if (StyleManager.GLOW_ENABLED) {
            gc.save();
            LineStyleUtil.resetLineStyle(gc);
            gc.setGlobalAlpha(StyleManager.GLOW_ALPHA);
            gc.setLineWidth(getEffectiveLineWidth() + StyleManager.GLOW_WIDTH_BONUS);
            gc.setStroke(getEffectiveColor());
            gc.strokePolygon(xPoints, yPoints, sideCount);
            gc.restore();
        }

        // 主描边
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokePolygon(xPoints, yPoints, sideCount);
        LineStyleUtil.resetLineStyle(gc);

        if (center.isInternal()) {
            double sx = transform.worldToScreenX(getCenterX());
            double sy = transform.worldToScreenY(getCenterY());
            gc.setFill(getEffectiveColor());
            double pointRadius = hover ? 4 : 3;
            gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);
            if (center.getName() != null && !center.getName().isEmpty()) {
                LabelRenderer.renderLabel(gc, center.getName(), sx, sy);
            }
        }

        gc.setFill(getEffectiveColor());
        double pointRadius = 3;
        for (int i = 0; i < vertexPoints.size(); i++) {
            PointGeo vertexPoint = vertexPoints.get(i);
            gc.fillOval(xPoints[i] - pointRadius, yPoints[i] - pointRadius,
                    pointRadius * 2, pointRadius * 2);
            String name = vertexPoint.getName();
            if (name != null && !name.isEmpty()) {
                LabelRenderer.renderLabel(gc, name, xPoints[i], yPoints[i]);
            }
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        calculateVertices();

        for (int i = 0; i < cachedVertices.size(); i++) {
            Point2D p1 = cachedVertices.get(i);
            Point2D p2 = cachedVertices.get((i + 1) % cachedVertices.size());
            double dist = MathCalculationUtils.pointToSegmentDistance(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
            if (dist < tolerance) return true;
        }

        if (fillType != FillType.NONE) {
            return isPointInPolygon(x, y);
        }
        return false;
    }

    private boolean isPointInPolygon(double x, double y) {
        int intersections = 0;
        for (int i = 0; i < cachedVertices.size(); i++) {
            Point2D p1 = cachedVertices.get(i);
            Point2D p2 = cachedVertices.get((i + 1) % cachedVertices.size());
            if ((p1.getY() > y) != (p2.getY() > y)) {
                double xIntersect = (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY()) + p1.getX();
                if (x < xIntersect) intersections++;
            }
        }
        return (intersections % 2) == 1;
    }

    @Override
    public void onClick(double x, double y) {
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        return List.of(
                new DraggablePoint(getCenterX(), getCenterY(), (newX, newY) -> {
                    if (center.hasRef()) {
                        center.updatePosition(newX, newY);
                    } else {
                        double oldX = center.getX();
                        double oldY = center.getY();
                        center.updatePosition(newX, newY);
                        if (center.getName() != null && !center.getName().isEmpty()) {
                            PointNameManager.getInstance().updatePosition(oldX, oldY, getCenterX(), getCenterY());
                        }
                    }
                    verticesCacheValid = false;
                    invalidateEdgeCache();
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        center.rotateAround(centerX, centerY, cos, sin);
        verticesCacheValid = false;
        invalidateEdgeCache();
    }

    @Override
    public double[] getBoundingBox() {
        calculateVertices();
        return computeBoundingBox(cachedVertices, Point2D::getX, Point2D::getY);
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitRegularPolygon(this);
    }
}