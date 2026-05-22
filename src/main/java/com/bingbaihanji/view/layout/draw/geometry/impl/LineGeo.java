package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * 线段几何图形
 * <p>
 * 支持点复用：如果端点位置已有PointGeo,直接引用而不是创建新点
 */
public class LineGeo extends AbstractWorldObject {

    private final ReusableCoordinate startPoint;
    private final ReusableCoordinate endPoint;

    /**
     * 基础构造函数(坐标方式)
     */
    public LineGeo(double startX, double startY, double endX, double endY) {
        this(startX, startY, endX, endY, true);
    }

    /**
     * 构造函数(坐标方式,可选自动命名)
     */
    public LineGeo(double startX, double startY, double endX, double endY, boolean autoName) {
        this(ObjectType.SEGMENT, startX, startY, endX, endY, autoName);
    }

    /**
     * 构造函数(指定类型、可选自动命名) - 用于反序列化还原子类型
     */
    public LineGeo(ObjectType type, double startX, double startY, double endX, double endY, boolean autoName) {
        super(type);
        PointNameManager manager = PointNameManager.getInstance();
        this.startPoint = new ReusableCoordinate(startX, startY,
                autoName ? manager.assignName(startX, startY) : null);
        this.endPoint = new ReusableCoordinate(endX, endY,
                autoName ? manager.assignName(endX, endY) : null);
        this.color = StyleManager.GEOMETRY_LINE;
    }

    /**
     * 构造函数(点引用方式)- 复用已有点
     */
    public LineGeo(PointGeo startPointRef, double startX, double startY,
                   PointGeo endPointRef, double endX, double endY) {
        super(ObjectType.SEGMENT);
        PointNameManager manager = PointNameManager.getInstance();
        this.startPoint = new ReusableCoordinate(startPointRef, startX, startY);
        if (startPointRef == null) {
            this.startPoint.setName(manager.assignName(startX, startY));
        }
        this.endPoint = new ReusableCoordinate(endPointRef, endX, endY);
        if (endPointRef == null) {
            this.endPoint.setName(manager.assignName(endX, endY));
        }
        this.color = StyleManager.GEOMETRY_LINE;
    }

    public double getStartX() {
        return startPoint.getX();
    }

    public double getStartY() {
        return startPoint.getY();
    }

    public double getEndX() {
        return endPoint.getX();
    }

    public double getEndY() {
        return endPoint.getY();
    }

    public PointGeo getStartPointRef() {
        return startPoint.getRef();
    }

    public PointGeo getEndPointRef() {
        return endPoint.getRef();
    }

    public String getStartPointName() {
        return startPoint.getName();
    }

    public void setStartPointName(String name) {
        startPoint.setName(name);
    }

    public String getEndPointName() {
        return endPoint.getName();
    }

    public void setEndPointName(String name) {
        endPoint.setName(name);
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (!visible) return;
        double sx1 = transform.worldToScreenX(getStartX());
        double sy1 = transform.worldToScreenY(getStartY());
        double sx2 = transform.worldToScreenX(getEndX());
        double sy2 = transform.worldToScreenY(getEndY());

        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokeLine(sx1, sy1, sx2, sy2);
        LineStyleUtil.resetLineStyle(gc);

        gc.setFill(getEffectiveColor());
        double pointRadius = hover ? 5 : 4;

        if (startPoint.isInternal()) {
            gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
            if (startPoint.getName() != null && !startPoint.getName().isEmpty()) {
                gc.setFill(GeometryConfig.Colors.LABEL_TEXT);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(startPoint.getName(), sx1 + 8, sy1 - 8);
                gc.setFill(getEffectiveColor());
            }
        }

        if (endPoint.isInternal()) {
            gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
            if (endPoint.getName() != null && !endPoint.getName().isEmpty()) {
                gc.setFill(GeometryConfig.Colors.LABEL_TEXT);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(endPoint.getName(), sx2 + 8, sy2 - 8);
                gc.setFill(getEffectiveColor());
            }
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        return MathCalculationUtils.pointToSegmentDistance(x, y, getStartX(), getStartY(), getEndX(), getEndY()) <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        return List.of(
                new DraggablePoint(getStartX(), getStartY(), (newX, newY) -> startPoint.updatePosition(newX, newY)),
                new DraggablePoint(getEndX(), getEndY(), (newX, newY) -> endPoint.updatePosition(newX, newY))
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        startPoint.rotateAround(centerX, centerY, cos, sin);
        endPoint.rotateAround(centerX, centerY, cos, sin);
    }

    @Override
    public double[] getBoundingBox() {
        double sX = getStartX();
        double sY = getStartY();
        double eX = getEndX();
        double eY = getEndY();
        return new double[]{Math.min(sX, eX), Math.max(sX, eX), Math.min(sY, eY), Math.max(sY, eY)};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitLine(this);
    }
}