package com.bingbaihanji.geometry.impl;

import java.util.ArrayList;
import java.util.List;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.geometry.LabelRenderer;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.geometry.WorldTransform;
import com.bingbaihanji.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 角度几何图形
 * <p>
 * 由顶点 O 和两条射线上的点 A、B 定义。两条边作为无限射线绘制，
 * 从顶点向两侧延伸至屏幕边缘。角内部绘制弧线和角度值标签。
 * 三击流程：顶点 → 射线1上一点 → 射线2上一点。
 * </p>
 *
 * @author bingbaihanji
 * @date 2026-06-23
 */
public class AngleGeo extends AbstractWorldObject {

    private static final double ARC_RADIUS = 28;

    private final ReusableCoordinate vertex;
    private final ReusableCoordinate ray1;
    private final ReusableCoordinate ray2;

    public AngleGeo(double ox, double oy, double ax, double ay, double bx, double by) {
        super(ObjectType.ANGLE);
        this.vertex = new ReusableCoordinate(ox, oy, "O");
        this.ray1 = new ReusableCoordinate(ax, ay, null);
        this.ray2 = new ReusableCoordinate(bx, by, null);
    }

    /**
     * 支持点复用的构造函数（与 LineGeo/CircleGeo/EllipseGeo 一致）
     */
    public AngleGeo(PointGeo vertexRef, double ox, double oy,
                    PointGeo ray1Ref, double ax, double ay,
                    PointGeo ray2Ref, double bx, double by) {
        super(ObjectType.ANGLE);
        this.vertex = vertexRef != null ? new ReusableCoordinate(vertexRef, ox, oy) : new ReusableCoordinate(ox, oy, "O");
        this.ray1 = ray1Ref != null ? new ReusableCoordinate(ray1Ref, ax, ay) : new ReusableCoordinate(ax, ay, null);
        this.ray2 = ray2Ref != null ? new ReusableCoordinate(ray2Ref, bx, by) : new ReusableCoordinate(bx, by, null);
    }

    public double getVertexX() { return vertex.getX(); }
    public double getVertexY() { return vertex.getY(); }
    public double getRay1X() { return ray1.getX(); }
    public double getRay1Y() { return ray1.getY(); }
    public double getRay2X() { return ray2.getX(); }
    public double getRay2Y() { return ray2.getY(); }

    /**
     * 获取角度度数（0° ~ 180°）
     */
    public double getAngleDegrees() {
        double dx1 = ray1.getX() - vertex.getX();
        double dy1 = ray1.getY() - vertex.getY();
        double dx2 = ray2.getX() - vertex.getX();
        double dy2 = ray2.getY() - vertex.getY();
        double dot = dx1 * dx2 + dy1 * dy2;
        double len1 = Math.hypot(dx1, dy1);
        double len2 = Math.hypot(dx2, dy2);
        if (len1 < 1e-10 || len2 < 1e-10) return 0;
        double cosAngle = dot / (len1 * len2);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosAngle))));
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        double svx = transform.worldToScreenX(vertex.getX());
        double svy = transform.worldToScreenY(vertex.getY());

        // 计算两条射线的方向向量（世界坐标）
        double dx1 = ray1.getX() - vertex.getX();
        double dy1 = ray1.getY() - vertex.getY();
        double dx2 = ray2.getX() - vertex.getX();
        double dy2 = ray2.getY() - vertex.getY();

        // 将射线延伸到屏幕边缘
        double[] ray1Ends = extendRayToScreenBounds(svx, svy, dx1, dy1, transform, w, h);
        double[] ray2Ends = extendRayToScreenBounds(svx, svy, dx2, dy2, transform, w, h);

        double srx1 = ray1Ends[0], sry1 = ray1Ends[1];
        double srx2 = ray2Ends[0], sry2 = ray2Ends[1];

        // 弧线端点：ARC_RADIUS 像素处的点
        double len1 = Math.hypot(srx1 - svx, sry1 - svy);
        double len2 = Math.hypot(srx2 - svx, sry2 - svy);
        double arx1 = svx + (srx1 - svx) / len1 * ARC_RADIUS;
        double ary1 = svy + (sry1 - svy) / len1 * ARC_RADIUS;
        double arx2 = svx + (srx2 - svx) / len2 * ARC_RADIUS;
        double ary2 = svy + (sry2 - svy) / len2 * ARC_RADIUS;

        // 发光层（弧线先画，避免与射线重叠处变深）
        if (StyleManager.GLOW_ENABLED) {
            gc.save();
            LineStyleUtil.resetLineStyle(gc);
            gc.setGlobalAlpha(StyleManager.GLOW_ALPHA);
            gc.setLineWidth(getEffectiveLineWidth() + StyleManager.GLOW_WIDTH_BONUS);
            gc.setStroke(getEffectiveColor());
            drawArc(gc, svx, svy, arx1, ary1, arx2, ary2);
            gc.strokeLine(svx, svy, srx1, sry1);
            gc.strokeLine(svx, svy, srx2, sry2);
            gc.restore();
        }

        // 主描边（弧线先画，射线覆盖弧线端点避免重叠变深）
        LineStyleUtil.applyLineStyle(gc, getLineType());
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        drawArc(gc, svx, svy, arx1, ary1, arx2, ary2);
        gc.strokeLine(svx, svy, srx1, sry1);
        gc.strokeLine(svx, svy, srx2, sry2);
        LineStyleUtil.resetLineStyle(gc);

        // 顶点
        if (vertex.isInternal()) {
            gc.setFill(getEffectiveColor());
            double pr = 3;
            gc.fillOval(svx - pr, svy - pr, pr * 2, pr * 2);
        }

        // 角度值标签（显示在弧线外侧）
        if (isLabelVisible() && getLabel() != null && !getLabel().isEmpty()) {
            // 弧线中点方向，向外偏移（屏幕 Y 向下，sin 取反）
            double midAngle = Math.atan2(-(ary1 + ary2 - 2 * svy), (arx1 + arx2 - 2 * svx));
            double labelR = ARC_RADIUS + 15;
            double lx = svx + Math.cos(midAngle) * labelR;
            double ly = svy - Math.sin(midAngle) * labelR;
            LabelRenderer.renderLabel(gc, getLabel(), lx, ly, getLabelPosition(), getLabelFontSize(),
                    getLabelColor() != null ? getLabelColor() : getEffectiveColor());
        }
    }

    /**
     * 从顶点沿世界方向延伸足够远后再转屏幕坐标（确保射线始终超出屏幕边界）
     */
    private double[] extendRayToScreenBounds(double svx, double svy,
            double dxWorld, double dyWorld, WorldTransform transform, double w, double h) {
        double len = Math.hypot(dxWorld, dyWorld);
        if (len < 1e-12) return new double[]{svx + 1, svy};
        double ux = dxWorld / len, uy = dyWorld / len;
        // 屏幕对角线长度作为最小延伸距离
        double far = Math.max(w, h) * 3;
        double sx = svx, sy = svy;
        // 逐步延伸直到超出屏幕边界
        for (int i = 0; i < 100; i++) {
            double wx = transform.screenToWorldX(sx);
            double wy = transform.screenToWorldY(sy);
            double nx = wx + ux * far, ny = wy + uy * far;
            double nsx = transform.worldToScreenX(nx);
            double nsy = transform.worldToScreenY(ny);
            if (nsx < -w || nsx > w * 2 || nsy < -h || nsy > h * 2) {
                return new double[]{nsx, nsy};
            }
            far *= 2;
        }
        return new double[]{sx + ux * far, sy + uy * far};
    }

    /**
     * 在两条射线之间绘制弧线（屏幕坐标，从 arx1/ary1 到 arx2/ary2）
     */
    private void drawArc(GraphicsContext gc, double cx, double cy,
                         double ax, double ay, double bx, double by) {
        double startAngle = Math.toDegrees(Math.atan2(-(ay - cy), ax - cx));
        double endAngle = Math.toDegrees(Math.atan2(-(by - cy), bx - cx));
        if (startAngle < 0) startAngle += 360;
        if (endAngle < 0) endAngle += 360;

        double arcExtent = endAngle - startAngle;
        // 取较小的弧
        if (Math.abs(arcExtent) > 180) {
            arcExtent = arcExtent > 0 ? arcExtent - 360 : arcExtent + 360;
        }

        gc.strokeArc(cx - ARC_RADIUS, cy - ARC_RADIUS, ARC_RADIUS * 2, ARC_RADIUS * 2,
                startAngle, arcExtent, javafx.scene.shape.ArcType.OPEN);
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        // 距离两条无限射线的最近距离
        double d1 = distanceToRay(x, y, vertex.getX(), vertex.getY(), ray1.getX(), ray1.getY());
        double d2 = distanceToRay(x, y, vertex.getX(), vertex.getY(), ray2.getX(), ray2.getY());
        return Math.min(d1, d2) <= tolerance;
    }

    /**
     * 点到无限射线的距离（射线从 (vx,vy) 出发，经过 (px,py) 向无限远延伸）
     */
    private double distanceToRay(double x, double y, double vx, double vy, double px, double py) {
        double dx = px - vx, dy = py - vy;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1e-12) return Math.hypot(x - vx, y - vy);
        // 投影参数 t，限制为 >= 0（射线单向无限）
        double t = ((x - vx) * dx + (y - vy) * dy) / len2;
        if (t <= 0) return Math.hypot(x - vx, y - vy);
        return Math.hypot(x - (vx + t * dx), y - (vy + t * dy));
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        List<DraggablePoint> points = new ArrayList<>();
        points.add(new DraggablePoint(vertex.getX(), vertex.getY(), (nx, ny) -> {
            vertex.updatePosition(nx, ny);
        }));
        points.add(new DraggablePoint(ray1.getX(), ray1.getY(), (nx, ny) -> {
            ray1.updatePosition(nx, ny);
        }));
        points.add(new DraggablePoint(ray2.getX(), ray2.getY(), (nx, ny) -> {
            ray2.updatePosition(nx, ny);
        }));
        return points;
    }

    @Override
    public void rotateAroundPoint(double cx, double cy, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        rotateCoord(vertex, cx, cy, cos, sin);
        rotateCoord(ray1, cx, cy, cos, sin);
        rotateCoord(ray2, cx, cy, cos, sin);
    }

    private void rotateCoord(ReusableCoordinate coord, double cx, double cy, double cos, double sin) {
        double dx = coord.getX() - cx, dy = coord.getY() - cy;
        coord.updatePosition(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos);
    }

    @Override
    public double[] getBoundingBox() {
        double minX = Math.min(Math.min(vertex.getX(), ray1.getX()), ray2.getX());
        double maxX = Math.max(Math.max(vertex.getX(), ray1.getX()), ray2.getX());
        double minY = Math.min(Math.min(vertex.getY(), ray1.getY()), ray2.getY());
        double maxY = Math.max(Math.max(vertex.getY(), ray1.getY()), ray2.getY());
        return new double[]{minX, minY, maxX, maxY};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitAngle(this);
    }
}
