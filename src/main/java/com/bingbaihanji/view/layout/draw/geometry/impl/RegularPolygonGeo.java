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

    private double radius;     // 外接圆半径
    private int sideCount;     // 边数(3-10)
    private double startAngle; // 起始角度(第一个顶点的角度)

    // 中心引用(如果复用已有点)
    private PointGeo centerPointRef;

    // 内部坐标(当没有引用时使用)
    private double cx;
    private double cy;

    private String centerName; // 中心名称

    // 标记中心是否是内部创建的(需要由正多边形绘制)
    private boolean centerIsInternal = true;

    // 顶点点对象列表(内部创建,标记为多边形顶点)
    private List<PointGeo> vertexPoints;

    // 顶点缓存(从中心+半径+边数计算)
    private List<Point2D> cachedVertices;
    private boolean verticesCacheValid = false;

    /**
     * 基础构造函数(坐标方式)
     */
    public RegularPolygonGeo(double cx, double cy, double radius, int sideCount) {
        super(ObjectType.REGULAR_POLYGON);
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.sideCount = Math.max(3, Math.min(10, sideCount)); // 限制在3-10之间
        this.startAngle = -Math.PI / 2; // 默认从正上方开始
        this.color = StyleManager.GEOMETRY_LINE;
        this.centerName = PointNameManager.getInstance().assignCenterName(cx, cy);
        this.cachedVertices = new ArrayList<>();
        this.vertexPoints = new ArrayList<>();

        // 创建顶点点对象
        createVertexPoints();
    }

    /**
     * 构造函数(坐标方式,可选自动命名)
     */
    public RegularPolygonGeo(double cx, double cy, double radius, int sideCount, boolean autoNameCenter) {
        super(ObjectType.REGULAR_POLYGON);
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        this.startAngle = -Math.PI / 2; // 默认从正上方开始
        this.color = StyleManager.GEOMETRY_LINE;
        if (autoNameCenter) {
            this.centerName = PointNameManager.getInstance().assignCenterName(cx, cy);
        }
        this.cachedVertices = new ArrayList<>();
        this.vertexPoints = new ArrayList<>();

        // 创建顶点点对象
        createVertexPoints();
    }

    /**
     * 构造函数(点引用方式)- 复用已有点作为中心
     *
     * @param centerPoint 中心点引用(可为null,表示内部创建)
     * @param cx          中心X坐标
     * @param cy          中心Y坐标
     * @param radius      外接圆半径
     * @param sideCount   边数(3-10)
     */
    public RegularPolygonGeo(PointGeo centerPoint, double cx, double cy, double radius, int sideCount) {
        this(centerPoint, cx, cy, radius, sideCount, -Math.PI / 2); // 默认从正上方开始
    }

    /**
     * 构造函数(点引用方式,带起始角度)- 复用已有点作为中心
     *
     * @param centerPoint 中心点引用(可为null,表示内部创建)
     * @param cx          中心X坐标
     * @param cy          中心Y坐标
     * @param radius      外接圆半径
     * @param sideCount   边数(3-10)
     * @param startAngle  起始角度(第一个顶点的角度)
     */
    public RegularPolygonGeo(PointGeo centerPoint, double cx, double cy, double radius, int sideCount, double startAngle) {
        super(ObjectType.REGULAR_POLYGON);
        this.centerPointRef = centerPoint;
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        this.startAngle = startAngle;
        this.color = StyleManager.GEOMETRY_LINE;

        if (centerPoint != null) {
            this.centerName = centerPoint.getName();
            this.centerIsInternal = false; // 复用外部点,不由正多边形绘制
        } else {
            this.centerName = PointNameManager.getInstance().assignCenterName(cx, cy);
            this.centerIsInternal = true;
        }
        this.cachedVertices = new ArrayList<>();
        this.vertexPoints = new ArrayList<>();

        // 创建顶点点对象
        createVertexPoints();
    }

    // Getter methods
    public double getCenterX() {
        return centerPointRef != null ? centerPointRef.getX() : cx;
    }

    public double getCenterY() {
        return centerPointRef != null ? centerPointRef.getY() : cy;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
        verticesCacheValid = false;
    }

    public int getSideCount() {
        return sideCount;
    }

    public void setSideCount(int sideCount) {
        this.sideCount = Math.max(3, Math.min(10, sideCount));
        verticesCacheValid = false;
    }

    public double getCx() {
        return cx;
    }

    public double getCy() {
        return cy;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(double startAngle) {
        this.startAngle = startAngle;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public PointGeo getCenterPointRef() {
        return centerPointRef;
    }

    /**
     * 计算正多边形的顶点坐标(极坐标转直角坐标)
     * 第一个顶点的角度由startAngle决定
     */
    private void calculateVertices() {
        if (verticesCacheValid) {
            return;
        }

        cachedVertices.clear();
        double angleStep = 2 * Math.PI / sideCount;

        for (int i = 0; i < sideCount; i++) {
            double angle = startAngle + i * angleStep;
            double x = getCenterX() + radius * Math.cos(angle);
            double y = getCenterY() + radius * Math.sin(angle);
            cachedVertices.add(new Point2D(x, y));
        }

        // 同步更新顶点点对象的位置
        for (int i = 0; i < vertexPoints.size() && i < cachedVertices.size(); i++) {
            Point2D vertex = cachedVertices.get(i);
            vertexPoints.get(i).updatePosition(vertex.getX(), vertex.getY());
        }

        verticesCacheValid = true;
    }

    /**
     * 创建顶点点对象,并按A, B, C...命名
     */
    private void createVertexPoints() {
        vertexPoints.clear();
        calculateVertices();

        for (Point2D vertex : cachedVertices) {
            PointGeo vertexPoint = new PointGeo(vertex.getX(), vertex.getY());
            vertexPoint.setPolygonVertex(true); // 标记为多边形顶点

            // 使用普通的A, B, C...命名
            String name = PointNameManager.getInstance().assignName(vertex.getX(), vertex.getY());
            vertexPoint.setName(name);

            vertexPoints.add(vertexPoint);
        }
    }

    /**
     * 获取顶点点对象列表
     */
    public List<PointGeo> getVertexPoints() {
        return vertexPoints;
    }

    /**
     * 获取顶点列表
     */
    public List<Point2D> getVertices() {
        calculateVertices();
        return new ArrayList<>(cachedVertices);
    }

    /**
     * 获取所有边(作为LineGeo列表)
     */
    public List<LineGeo> getEdges() {
        calculateVertices();
        List<LineGeo> edges = new ArrayList<>();

        for (int i = 0; i < cachedVertices.size(); i++) {
            Point2D p1 = cachedVertices.get(i);
            Point2D p2 = cachedVertices.get((i + 1) % cachedVertices.size());
            edges.add(new LineGeo(p1.getX(), p1.getY(), p2.getX(), p2.getY(), false));
        }

        return edges;
    }

    @Override
    public void paint(GraphicsContext gc,
                      WorldTransform transform,
                      double w,
                      double h) {

        calculateVertices();

        // 转换顶点到屏幕坐标
        double[] xPoints = new double[sideCount];
        double[] yPoints = new double[sideCount];

        for (int i = 0; i < sideCount; i++) {
            Point2D vertex = cachedVertices.get(i);
            xPoints[i] = transform.worldToScreenX(vertex.getX());
            yPoints[i] = transform.worldToScreenY(vertex.getY());
        }

        // 先绘制填充
        FillRenderer.fillPolygon(gc, fillType, fillColor, fillOpacity,
                hatchAngle, hatchDistance, xPoints, yPoints, sideCount);

        // 再绘制边框
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());

        gc.strokePolygon(xPoints, yPoints, sideCount);

        // 重置线型
        LineStyleUtil.resetLineStyle(gc);

        // 只绘制内部创建的中心点,复用的外部点由它们自己绘制
        if (centerIsInternal) {
            double sx = transform.worldToScreenX(getCenterX());
            double sy = transform.worldToScreenY(getCenterY());

            gc.setFill(getEffectiveColor());
            double pointRadius = hover ? 4 : 3;
            gc.fillOval(sx - pointRadius, sy - pointRadius, pointRadius * 2, pointRadius * 2);

            // 使用LabelRenderer绘制中心名称
            if (centerName != null && !centerName.isEmpty()) {
                LabelRenderer.renderLabel(gc, centerName, sx, sy);
            }
        }

        // 绘制顶点点对象
        gc.setFill(getEffectiveColor());
        double pointRadius = 3;
        for (int i = 0; i < vertexPoints.size(); i++) {
            PointGeo vertexPoint = vertexPoints.get(i);
            // 绘制顶点
            gc.fillOval(xPoints[i] - pointRadius, yPoints[i] - pointRadius,
                    pointRadius * 2, pointRadius * 2);
            // 绘制顶点名称
            String name = vertexPoint.getName();
            if (name != null && !name.isEmpty()) {
                LabelRenderer.renderLabel(gc, name, xPoints[i], yPoints[i]);
            }
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        calculateVertices();

        // 方法1: 检查是否在边附近
        for (int i = 0; i < cachedVertices.size(); i++) {
            Point2D p1 = cachedVertices.get(i);
            Point2D p2 = cachedVertices.get((i + 1) % cachedVertices.size());
            double dist = pointToSegmentDistance(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
            if (dist < tolerance) {
                return true;
            }
        }

        // 方法2: 如果有填充,检查是否在内部
        if (fillType != FillType.NONE) {
            return isPointInPolygon(x, y);
        }

        return false;
    }

    /**
     * 计算点到线段的距离
     */
    private double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared < 1e-10) {
            return Math.hypot(px - x1, py - y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(px - projX, py - projY);
    }

    /**
     * 判断点是否在多边形内部(射线法)
     */
    private boolean isPointInPolygon(double x, double y) {
        int intersections = 0;

        for (int i = 0; i < cachedVertices.size(); i++) {
            Point2D p1 = cachedVertices.get(i);
            Point2D p2 = cachedVertices.get((i + 1) % cachedVertices.size());

            if ((p1.getY() > y) != (p2.getY() > y)) {
                double xIntersect = (p2.getX() - p1.getX()) * (y - p1.getY()) / (p2.getY() - p1.getY()) + p1.getX();
                if (x < xIntersect) {
                    intersections++;
                }
            }
        }

        return (intersections % 2) == 1;
    }

    @Override
    public void onClick(double x, double y) {
        // 正多边形本身暂时不响应点击
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 中心可拖动
        return List.of(
                new DraggablePoint(getCenterX(), getCenterY(), (newX, newY) -> {
                    if (centerPointRef != null) {
                        // 复用的外部点,更新其位置
                        centerPointRef.updatePosition(newX, newY);
                    } else {
                        // 内部坐标,更新坐标
                        double oldX = cx;
                        double oldY = cy;
                        cx = newX;
                        cy = newY;
                        // 更新名称映射
                        if (centerName != null && !centerName.isEmpty()) {
                            PointNameManager.getInstance().updatePosition(oldX, oldY, cx, cy);
                        }
                    }
                    // 中心移动后需要重新计算顶点
                    verticesCacheValid = false;
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        // 旋转中心点
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        if (centerPointRef != null && !centerPointRef.isConstrained()) {
            double dx = centerPointRef.getX() - centerX;
            double dy = centerPointRef.getY() - centerY;
            centerPointRef.updatePosition(
                    centerX + dx * cos - dy * sin,
                    centerY + dx * sin + dy * cos
            );
        } else if (centerPointRef == null) {
            double dx = cx - centerX;
            double dy = cy - centerY;
            cx = centerX + dx * cos - dy * sin;
            cy = centerY + dx * sin + dy * cos;
        }

        // 旋转后需要重新计算顶点
        verticesCacheValid = false;
    }

    @Override
    public double[] getBoundingBox() {
        calculateVertices();

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Point2D vertex : cachedVertices) {
            minX = Math.min(minX, vertex.getX());
            maxX = Math.max(maxX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            maxY = Math.max(maxY, vertex.getY());
        }

        return new double[]{minX, maxX, minY, maxY};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitRegularPolygon(this);
    }
}
