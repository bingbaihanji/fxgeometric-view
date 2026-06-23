package com.bingbaihanji.view.layout.draw.geometry.impl;

import java.util.ArrayList;
import java.util.List;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LabelRenderer;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 角度几何图形
 * <p>
 * 由顶点 O 和两条射线上的点 A、B 定义，绘制弧线和角度值标签。
 * 三击流程：顶点 → 射线1上一点 → 射线2上一点。
 * </p>
 *
 * @author bingbaihanji
 * @date 2026-06-23
 */
public class AngleGeo extends AbstractWorldObject {

    /** 绘制弧线的近似线段数 */
    private static final int ARC_SEGMENTS = 64;

    private final ReusableCoordinate vertex;
    private final ReusableCoordinate ray1;
    private final ReusableCoordinate ray2;

    public AngleGeo(double ox, double oy, double ax, double ay, double bx, double by) {
        super(ObjectType.ANGLE);
        this.vertex = new ReusableCoordinate(ox, oy, "O");
        this.ray1 = new ReusableCoordinate(ax, ay, null);
        this.ray2 = new ReusableCoordinate(bx, by, null);
    }

    public double getVertexX() { return vertex.getX(); }
    public double getVertexY() { return vertex.getY(); }
    public double getRay1X() { return ray1.getX(); }
    public double getRay1Y() { return ray1.getY(); }
    public double getRay2X() { return ray2.getX(); }
    public double getRay2Y() { return ray2.getY(); }

    /**
     * 获取角度的度数（0° ~ 180°）
     */
    public double getAngleDegrees() {
        double dx1 = ray1.getX() - vertex.getX();
        double dy1 = ray1.getY() - vertex.getY();
        double dx2 = ray2.getX() - vertex.getX();
        double dy2 = ray2.getY() - vertex.getY();
        double dot = dx1 * dx2 + dy1 * dy2;
        double cross = dx1 * dy2 - dy1 * dx2;
        double angle = Math.toDegrees(Math.atan2(Math.abs(cross), dot));
        return Math.min(angle, 180 - angle);
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        double svx = transform.worldToScreenX(vertex.getX());
        double svy = transform.worldToScreenY(vertex.getY());
        double srx1 = transform.worldToScreenX(ray1.getX());
        double sry1 = transform.worldToScreenY(ray1.getY());
        double srx2 = transform.worldToScreenX(ray2.getX());
        double sry2 = transform.worldToScreenY(ray2.getY());

        // 发光层
        if (StyleManager.GLOW_ENABLED) {
            gc.save();
            LineStyleUtil.resetLineStyle(gc);
            gc.setGlobalAlpha(StyleManager.GLOW_ALPHA);
            gc.setLineWidth(getEffectiveLineWidth() + StyleManager.GLOW_WIDTH_BONUS);
            gc.setStroke(getEffectiveColor());
            gc.strokeLine(svx, svy, srx1, sry1);
            gc.strokeLine(svx, svy, srx2, sry2);
            drawArc(gc, svx, svy, srx1, sry1, srx2, sry2);
            gc.restore();
        }

        // 主描边
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokeLine(svx, svy, srx1, sry1);
        gc.strokeLine(svx, svy, srx2, sry2);
        drawArc(gc, svx, svy, srx1, sry1, srx2, sry2);
        LineStyleUtil.resetLineStyle(gc);

        // 顶点
        if (vertex.isInternal()) {
            gc.setFill(getEffectiveColor());
            double pr = 3;
            gc.fillOval(svx - pr, svy - pr, pr * 2, pr * 2);
        }

        // 角度值标签
        if (labelVisible && label != null && !label.isEmpty()) {
            double midX = transform.worldToScreenX(
                    (vertex.getX() + ray1.getX() + ray2.getX()) / 3);
            double midY = transform.worldToScreenY(
                    (vertex.getY() + ray1.getY() + ray2.getY()) / 3);
            Color c = labelColor != null ? labelColor : getEffectiveColor();
            LabelRenderer.renderLabel(gc, label, midX, midY, labelPosition, labelFontSize, c);
        }
    }

    /**
     * 在两条射线之间绘制弧线
     */
    private void drawArc(GraphicsContext gc, double cx, double cy,
                         double ax, double ay, double bx, double by) {
        double startAngle = Math.toDegrees(Math.atan2(-(ay - cy), ax - cx));
        double endAngle = Math.toDegrees(Math.atan2(-(by - cy), bx - cx));
        if (startAngle < 0) startAngle += 360;
        if (endAngle < 0) endAngle += 360;

        double arcExtent = endAngle - startAngle;
        // 确保取较小的弧
        if (Math.abs(arcExtent) > 180) {
            double temp = startAngle;
            startAngle = endAngle;
            endAngle = temp;
            arcExtent = endAngle - startAngle;
        }
        if (arcExtent < 0) arcExtent += 360;

        double radius = 30; // 固定弧线屏幕半径
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                startAngle, arcExtent, javafx.scene.shape.ArcType.OPEN);
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double d1 = hypotFromSegment(x, y, vertex.getX(), vertex.getY(), ray1.getX(), ray1.getY());
        double d2 = hypotFromSegment(x, y, vertex.getX(), vertex.getY(), ray2.getX(), ray2.getY());
        return Math.min(d1, d2) <= tolerance;
    }

    private double hypotFromSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
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
