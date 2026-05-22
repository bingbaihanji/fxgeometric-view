package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LabelRenderer;
import com.bingbaihanji.util.PointNameManager;
import com.bingbaihanji.util.PointReuseGroup;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class PointGeo extends AbstractWorldObject {

    private double x;
    private double y;

    private PointConstraint constraint = null; // 点的约束(可选)

    /**
     * 标记此点是否是多边形内部创建的顶点
     * 如果是内部顶点,多边形负责绘制它；
     * 如果是外部复用的点,点自己绘制自己
     */
    private boolean polygonVertex = false;

    /**
     * 点所属的复用组
     * 当多个点重合并启用复用时,它们会被添加到同一个复用组
     * 移动任意一个点,组内其他点会同步移动
     */
    private PointReuseGroup reuseGroup = null;

    /**
     * 标记是否正在同步更新位置(防止递归调用)
     */
    private transient boolean isSyncingPosition = false;

    public PointGeo(double x, double y) {
        super(ObjectType.POINT_FREE); // 默认为自由点
        this.x = x;
        this.y = y;
        // 自动分配名称
        this.label = PointNameManager.getInstance().assignName(x, y);
        this.color = StyleManager.GEOMETRY_DEFAULT; // 默认颜色
    }

    public PointGeo(ObjectType type, double x, double y, boolean autoName) {
        super(type);
        this.x = x;
        this.y = y;
        if (autoName) {
            this.label = PointNameManager.getInstance().assignName(x, y);
        }
        this.color = StyleManager.GEOMETRY_DEFAULT;
    }

    public PointGeo(double x, double y, boolean autoName) {
        super(ObjectType.POINT_FREE); // 默认为自由点
        this.x = x;
        this.y = y;
        // 根据参数决定是否自动命名
        if (autoName) {
            this.label = PointNameManager.getInstance().assignName(x, y);
        }
        this.color = StyleManager.GEOMETRY_DEFAULT;
    }

    // Getter methods
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // 获取点的名称(兼容旧代码)
    public String getName() {
        return label;
    }

    // 设置点的名称(兼容旧代码)
    public void setName(String name) {
        this.label = name;
    }

    // 获取点的约束
    public PointConstraint getConstraint() {
        return constraint;
    }

    // 设置点的约束
    public void setConstraint(PointConstraint constraint) {
        this.constraint = constraint;
    }

    // 判断点是否被约束
    public boolean isConstrained() {
        return constraint != null;
    }

    /**
     * 判断是否是多边形内部创建的顶点
     */
    public boolean isPolygonVertex() {
        return polygonVertex;
    }

    /**
     * 设置是否是多边形内部顶点
     */
    public void setPolygonVertex(boolean polygonVertex) {
        this.polygonVertex = polygonVertex;
    }

    // 更新点的位置(支持约束处理和复用组同步)
    public void updatePosition(double newX, double newY) {
        // 保存旧位置
        double oldX = this.x;
        double oldY = this.y;

        if (constraint != null) {
            // 如果有约束,计算新参数并根据参数更新位置
            double newParameter = constraint.calculateParameter(newX, newY);
            constraint.setParameter(newParameter);
            Point2D newPos = constraint.getPointFromParameter();
            this.x = newPos.getX();
            this.y = newPos.getY();
        } else {
            // 无约束,直接更新坐标
            this.x = newX;
            this.y = newY;
        }

        // 如果该点有名称,更新PointNameManager中的映射
        if (this.label != null && !this.label.isEmpty()) {
            PointNameManager.getInstance().updatePosition(oldX, oldY, this.x, this.y);
        }

        // 同步复用组内的其他点(防止递归)
        if (!isSyncingPosition && reuseGroup != null && reuseGroup.isEnabled()) {
            reuseGroup.syncMove(this.x, this.y, this);
        }
    }

    /**
     * 直接设置位置(不触发复用组同步,用于复用组内部调用)
     *
     * @param newX 新X坐标
     * @param newY 新Y坐标
     */
    public void setPositionDirectly(double newX, double newY) {
        isSyncingPosition = true;
        try {
            // 保存旧位置
            double oldX = this.x;
            double oldY = this.y;

            if (constraint != null) {
                // 如果有约束,计算新参数并根据参数更新位置
                double newParameter = constraint.calculateParameter(newX, newY);
                constraint.setParameter(newParameter);
                Point2D newPos = constraint.getPointFromParameter();
                this.x = newPos.getX();
                this.y = newPos.getY();
            } else {
                this.x = newX;
                this.y = newY;
            }

            // 更新名称映射
            if (this.label != null && !this.label.isEmpty()) {
                PointNameManager.getInstance().updatePosition(oldX, oldY, this.x, this.y);
            }
        } finally {
            isSyncingPosition = false;
        }
    }

    // 复用组相关方法

    /**
     * 获取点所属的复用组
     */
    public PointReuseGroup getReuseGroup() {
        return reuseGroup;
    }

    /**
     * 设置点的复用组
     */
    public void setReuseGroup(PointReuseGroup reuseGroup) {
        this.reuseGroup = reuseGroup;
    }

    /**
     * 判断点是否在复用组中
     */
    public boolean isInReuseGroup() {
        return reuseGroup != null;
    }

    /**
     * 判断点是否正在同步位置
     */
    public boolean isSyncingPosition() {
        return isSyncingPosition;
    }

    @Override
    public void paint(GraphicsContext gc, WorldTransform t, double w, double h) {
        if (!visible || polygonVertex) {
            return;
        }

        double sx = t.worldToScreenX(x);
        double sy = t.worldToScreenY(y);

        // 使用有效颜色(考虑选中/悬停状态)
        Color displayColor = getEffectiveColor();

        // 约束点使用深蓝色
        if (isConstrained()) {
            displayColor = StyleManager.GEOMETRY_CONSTRAINED;
        }

        // 复用组内的点使用特殊颜色标识
        if (isInReuseGroup() && reuseGroup.isEnabled()) {
            displayColor = StyleManager.GEOMETRY_REUSED;
        }

        gc.setFill(displayColor);

        double r = (hover || selected) ? 6 : 4;
        gc.fillOval(sx - r, sy - r, r * 2, r * 2);

        // 使用LabelRenderer绘制点的名称
        LabelRenderer.renderLabel(gc, this, sx, sy);
    }

    @Override
    public boolean hitTest(double wx, double wy, double tol) {
        return Math.hypot(wx - x, wy - y) < tol;
    }

    @Override
    public void onClick(double wx, double wy) {
        System.out.println("点被点击：" + x + ", " + y);
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 点本身可拖动
        return List.of(
                new DraggablePoint(x, y, (newX, newY) -> {
                    // 使用 updatePosition 以支持约束和复用组同步
                    updatePosition(newX, newY);
                })
        );
    }

    @Override
    public void rotateAroundPoint(double centerX, double centerY, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = x - centerX;
        double dy = y - centerY;
        double newX = centerX + dx * cos - dy * sin;
        double newY = centerY + dx * sin + dy * cos;

        // 使用 updatePosition 以支持复用组同步
        updatePosition(newX, newY);
    }

    @Override
    public double[] getBoundingBox() {
        // 点的边界框是一个很小的区域
        double margin = 0.1;
        return new double[]{x - margin, x + margin, y - margin, y + margin};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitPoint(this);
    }
}