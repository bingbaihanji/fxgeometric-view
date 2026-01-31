package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * 线段几何图形
 * <p>
 * 支持点复用：如果端点位置已有PointGeo，直接引用而不是创建新点
 */
public class LineGeo extends AbstractWorldObject {

    // 端点引用（如果复用已有点）
    private PointGeo startPointRef;
    private PointGeo endPointRef;

    // 内部坐标（当没有引用时使用）
    private double startX;
    private double startY;
    private double endX;
    private double endY;

    private String startPointName; // 起点名称
    private String endPointName;   // 终点名称

    // 标记端点是否是内部创建的（需要由线段绘制）
    private boolean startIsInternal = true;
    private boolean endIsInternal = true;

    /**
     * 基础构造函数（坐标方式）
     */
    public LineGeo(double startX, double startY, double endX, double endY) {
        this(startX, startY, endX, endY, true);
    }

    /**
     * 构造函数（坐标方式，可选自动命名）
     */
    public LineGeo(double startX, double startY, double endX, double endY, boolean autoName) {
        super(ObjectType.SEGMENT);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.color = StyleManager.GEOMETRY_LINE;
        if (autoName) {
            PointNameManager manager = PointNameManager.getInstance();
            this.startPointName = manager.assignName(startX, startY);
            this.endPointName = manager.assignName(endX, endY);
        }
    }

    /**
     * 构造函数（点引用方式）- 复用已有点
     *
     * @param startPoint 起点引用（可为null，表示内部创建）
     * @param startX     起点X坐标
     * @param startY     起点Y坐标
     * @param endPoint   终点引用（可为null，表示内部创建）
     * @param endX       终点X坐标
     * @param endY       终点Y坐标
     */
    public LineGeo(PointGeo startPoint, double startX, double startY,
                   PointGeo endPoint, double endX, double endY) {
        super(ObjectType.SEGMENT);
        this.startPointRef = startPoint;
        this.endPointRef = endPoint;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.color = StyleManager.GEOMETRY_LINE;

        // 如果有引用，使用引用点的名称；否则分配新名称
        PointNameManager manager = PointNameManager.getInstance();
        if (startPoint != null) {
            this.startPointName = startPoint.getName();
            this.startIsInternal = false; // 复用外部点，不由线段绘制
        } else {
            this.startPointName = manager.assignName(startX, startY);
            this.startIsInternal = true;
        }

        if (endPoint != null) {
            this.endPointName = endPoint.getName();
            this.endIsInternal = false;
        } else {
            this.endPointName = manager.assignName(endX, endY);
            this.endIsInternal = true;
        }
    }


    public double getStartX() {
        return startPointRef != null ? startPointRef.getX() : startX;
    }

    public double getStartY() {
        return startPointRef != null ? startPointRef.getY() : startY;
    }

    public double getEndX() {
        return endPointRef != null ? endPointRef.getX() : endX;
    }

    public double getEndY() {
        return endPointRef != null ? endPointRef.getY() : endY;
    }

    public PointGeo getStartPointRef() {
        return startPointRef;
    }

    public PointGeo getEndPointRef() {
        return endPointRef;
    }

    public String getStartPointName() {
        return startPointName;
    }

    public String getEndPointName() {
        return endPointName;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        double sx1 = transform.worldToScreenX(getStartX());
        double sy1 = transform.worldToScreenY(getStartY());
        double sx2 = transform.worldToScreenX(getEndX());
        double sy2 = transform.worldToScreenY(getEndY());

        // 应用线型
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        gc.setLineWidth(getEffectiveLineWidth());
        gc.strokeLine(sx1, sy1, sx2, sy2);

        // 重置线型
        LineStyleUtil.resetLineStyle(gc);

        // 只绘制内部创建的端点，复用的外部点由它们自己绘制
        gc.setFill(getEffectiveColor());
        double pointRadius = hover ? 5 : 4;

        if (startIsInternal) {
            gc.fillOval(sx1 - pointRadius, sy1 - pointRadius, pointRadius * 2, pointRadius * 2);
            // 绘制起点名称
            if (startPointName != null && !startPointName.isEmpty()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(startPointName, sx1 + 8, sy1 - 8);
                gc.setFill(getEffectiveColor());
            }
        }

        if (endIsInternal) {
            gc.fillOval(sx2 - pointRadius, sy2 - pointRadius, pointRadius * 2, pointRadius * 2);
            // 绘制终点名称
            if (endPointName != null && !endPointName.isEmpty()) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.LEFT);
                gc.fillText(endPointName, sx2 + 8, sy2 - 8);
            }
        }
    }

    @Override
    public boolean hitTest(double x, double y, double tolerance) {
        double sX = getStartX();
        double sY = getStartY();
        double eX = getEndX();
        double eY = getEndY();

        // 点到线段的距离计算
        double dx = eX - sX;
        double dy = eY - sY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return Math.hypot(x - sX, y - sY) <= tolerance;
        }

        double distance = Math.abs(dy * x - dx * y + eX * sY - eY * sX) / length;
        return distance <= tolerance;
    }

    @Override
    public void onClick(double x, double y) {
        // 线段本身暂时不响应点击
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 线段的两个端点可拖动
        return List.of(
                new DraggablePoint(getStartX(), getStartY(), (newX, newY) -> {
                    if (startPointRef != null) {
                        // 复用的外部点，更新其位置
                        startPointRef.updatePosition(newX, newY);
                    } else {
                        startX = newX;
                        startY = newY;
                    }
                }),
                new DraggablePoint(getEndX(), getEndY(), (newX, newY) -> {
                    if (endPointRef != null) {
                        endPointRef.updatePosition(newX, newY);
                    } else {
                        endX = newX;
                        endY = newY;
                    }
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // 旋转起点（只旋转内部点）
        if (startPointRef != null && !startPointRef.isConstrained()) {
            double dx1 = startPointRef.getX() - centerX;
            double dy1 = startPointRef.getY() - centerY;
            startPointRef.updatePosition(
                    centerX + dx1 * cos - dy1 * sin,
                    centerY + dx1 * sin + dy1 * cos
            );
        } else if (startPointRef == null) {
            double dx1 = startX - centerX;
            double dy1 = startY - centerY;
            startX = centerX + dx1 * cos - dy1 * sin;
            startY = centerY + dx1 * sin + dy1 * cos;
        }

        // 旋转终点
        if (endPointRef != null && !endPointRef.isConstrained()) {
            double dx2 = endPointRef.getX() - centerX;
            double dy2 = endPointRef.getY() - centerY;
            endPointRef.updatePosition(
                    centerX + dx2 * cos - dy2 * sin,
                    centerY + dx2 * sin + dy2 * cos
            );
        } else if (endPointRef == null) {
            double dx2 = endX - centerX;
            double dy2 = endY - centerY;
            endX = centerX + dx2 * cos - dy2 * sin;
            endY = centerY + dx2 * sin + dy2 * cos;
        }
    }

    @Override
    public double[] getBoundingBox() {
        double sX = getStartX();
        double sY = getStartY();
        double eX = getEndX();
        double eY = getEndY();
        double minX = Math.min(sX, eX);
        double maxX = Math.max(sX, eX);
        double minY = Math.min(sY, eY);
        double maxY = Math.max(sY, eY);
        return new double[]{minX, maxX, minY, maxY};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitLine(this);
    }
}