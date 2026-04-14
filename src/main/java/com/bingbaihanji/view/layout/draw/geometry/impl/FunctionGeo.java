package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.LabelRenderer;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数几何对象抽象基类
 * <p>
 * 所有函数类型的基类,提供统一的采样、绘制、交互逻辑
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public abstract class FunctionGeo extends AbstractWorldObject {

    /**
     * 默认采样密度系数(每单位世界坐标的采样点数)
     */
    protected static final int DEFAULT_SAMPLES_PER_UNIT = 5;

    /**
     * 最小采样点数
     */
    protected static final int MIN_SAMPLES = 100;

    /**
     * 最大采样点数(防止性能问题)
     */
    protected static final int MAX_SAMPLES = 2000;

    /**
     * 视图边界余量系数
     */
    protected static final double VIEW_MARGIN_FACTOR = 0.5;

    /**
     * 函数表达式(用于显示)
     */
    protected String expression = "";

    /**
     * 定义域最小值
     */
    protected double domainMin = Double.NEGATIVE_INFINITY;

    /**
     * 定义域最大值
     */
    protected double domainMax = Double.POSITIVE_INFINITY;

    /**
     * 采样点缓存(世界坐标)
     */
    protected List<Point2D> sampledPoints = new ArrayList<>();

    /**
     * 是否需要重新采样
     */
    protected boolean needsResampling = true;

    /**
     * 上次采样时的缩放比例
     */
    protected double lastSampleScale = -1;

    /**
     * 上次采样时的视图范围(用于检测视图变化)
     */
    protected double[] lastViewBounds = null;

    /**
     * 构造函数
     */
    protected FunctionGeo() {
        super(ObjectType.FUNCTION);
        this.color = StyleManager.GEOMETRY_LINE;
        this.lineWidth = 2.0;
        this.labelVisible = true;
    }

    /**
     * 抽象方法：采样函数曲线
     * <p>
     * 子类实现此方法来计算函数的离散点
     *
     * @param viewMinX 视图范围最小x
     * @param viewMaxX 视图范围最大x
     * @param viewMinY 视图范围最小y
     * @param viewMaxY 视图范围最大y
     * @param scale    缩放比例
     */
    protected abstract void samplePoints(double viewMinX, double viewMaxX,
                                         double viewMinY, double viewMaxY,
                                         double scale);

    /**
     * 获取函数表达式字符串
     *
     * @return 表达式
     */
    public abstract String getExpression();

    /**
     * 创建该函数的约束
     *
     * @return 约束对象
     */
    public abstract PointConstraint createConstraint();

    /**
     * 计算给定x的函数值(显函数)
     * <p>
     * 子类应该重写此方法,参数方程类(如椭圆)可以返回NaN
     *
     * @param x x坐标
     * @return y坐标,如果不是显函数则返回NaN
     */
    public double evaluate(double x) {
        return Double.NaN;
    }

    /**
     * 检测是否有断点(如反比例函数在x=0处)
     * <p>
     * 子类可以重写此方法
     *
     * @param x x坐标
     * @return 是否有断点
     */
    protected boolean hasDiscontinuity(double x) {
        return false;
    }

    /**
     * 更新表达式字符串
     * <p>
     * 子类应该实现此方法来生成表达式,并在参数变化时调用
     */
    protected abstract void updateExpression();

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double width, double height) {
        if (!visible) {
            return;
        }

        // 计算当前视图范围
        double[] viewBounds = calculateViewBounds(transform, width, height);
        double currentScale = transform.getScale();

        // 检查是否需要重新采样(缩放变化或视图范围变化)
        if (needsResamplingCheck(currentScale, viewBounds)) {
            samplePoints(viewBounds[0], viewBounds[1],
                    viewBounds[2], viewBounds[3],
                    currentScale);
            needsResampling = false;
            lastSampleScale = currentScale;
            lastViewBounds = viewBounds.clone();
        }

        // 应用线型
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        // 悬停时增加线宽以提供视觉反馈
        gc.setLineWidth(hover ? getEffectiveLineWidth() + 1.5 : getEffectiveLineWidth());
        gc.setGlobalAlpha(opacity);

        // 绘制曲线(处理断点)
        drawCurve(gc, transform);

        // 重置状态
        gc.setGlobalAlpha(1.0);
        LineStyleUtil.resetLineStyle(gc);

        // 绘制函数表达式标签
        if (labelVisible && !sampledPoints.isEmpty()) {
            drawFunctionLabel(gc, transform);
        }
    }

    /**
     * 绘制曲线(处理断点)
     */
    protected void drawCurve(GraphicsContext gc, WorldTransform transform) {
        if (sampledPoints.isEmpty()) {
            return;
        }

        Point2D prevPoint = null;
        for (Point2D point : sampledPoints) {
            if (prevPoint != null && !hasDiscontinuityBetween(prevPoint, point)) {
                double sx1 = transform.worldToScreenX(prevPoint.getX());
                double sy1 = transform.worldToScreenY(prevPoint.getY());
                double sx2 = transform.worldToScreenX(point.getX());
                double sy2 = transform.worldToScreenY(point.getY());

                gc.strokeLine(sx1, sy1, sx2, sy2);
            }
            prevPoint = point;
        }
    }

    /**
     * 检测两点之间是否有断点
     * <p>
     * 如果y值跳变过大,可能是断点
     */
    protected boolean hasDiscontinuityBetween(Point2D p1, Point2D p2) {
        double dy = Math.abs(p2.getY() - p1.getY());
        double dx = Math.abs(p2.getX() - p1.getX());

        // 如果斜率过大(可能是垂直渐近线)
        if (dx > 1e-10 && dy / dx > 1000) {
            return true;
        }

        // 如果y值跳变超过视图高度的2倍
        return dy > 100;
    }

    /**
     * 绘制函数表达式标签
     */
    protected void drawFunctionLabel(GraphicsContext gc, WorldTransform transform) {
        // 在曲线中点位置显示表达式
        int midIndex = sampledPoints.size() / 2;
        if (midIndex < sampledPoints.size()) {
            Point2D midPoint = sampledPoints.get(midIndex);
            double sx = transform.worldToScreenX(midPoint.getX());
            double sy = transform.worldToScreenY(midPoint.getY());

            // 在曲线上方显示标签
            LabelRenderer.renderLabel(gc, this, sx, sy - 15);
        }
    }

    @Override
    public boolean hitTest(double worldX, double worldY, double tolerance) {
        if (!selectable || !visible) {
            return false;
        }

        // 检查点到曲线的距离
        for (int i = 0; i < sampledPoints.size() - 1; i++) {
            Point2D p1 = sampledPoints.get(i);
            Point2D p2 = sampledPoints.get(i + 1);

            double dist = pointToSegmentDistance(worldX, worldY,
                    p1.getX(), p1.getY(), p2.getX(), p2.getY());
            if (dist < tolerance) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算点到线段的距离
     */
    protected double pointToSegmentDistance(double px, double py,
                                            double x1, double y1, double x2, double y2) {
        return MathCalculationUtils.pointToSegmentDistance(px, py, x1, y1, x2, y2);
    }

    /**
     * 计算视图范围
     */
    protected double[] calculateViewBounds(WorldTransform transform, double width, double height) {
        double minX = transform.screenToWorldX(0);
        double maxX = transform.screenToWorldX(width);
        double minY = transform.screenToWorldY(height);
        double maxY = transform.screenToWorldY(0);

        return new double[]{minX, maxX, minY, maxY};
    }

    /**
     * 检查是否需要重新采样
     *
     * @param currentScale 当前缩放比例
     * @param viewBounds   当前视图范围
     * @return 是否需要重新采样
     */
    protected boolean needsResamplingCheck(double currentScale, double[] viewBounds) {
        // 显式标记需要重新采样
        if (needsResampling) {
            return true;
        }

        // 缩放比例变化
        if (Math.abs(currentScale - lastSampleScale) > 0.01) {
            return true;
        }

        // 视图范围变化(平移)
        if (lastViewBounds == null) {
            return true;
        }

        double viewWidth = viewBounds[1] - viewBounds[0];
        double viewHeight = viewBounds[3] - viewBounds[2];
        double threshold = Math.max(viewWidth, viewHeight) * 0.1; // 10%的变化阈值

        return Math.abs(viewBounds[0] - lastViewBounds[0]) > threshold ||
                Math.abs(viewBounds[1] - lastViewBounds[1]) > threshold ||
                Math.abs(viewBounds[2] - lastViewBounds[2]) > threshold ||
                Math.abs(viewBounds[3] - lastViewBounds[3]) > threshold;
    }

    /**
     * 计算采样点数
     *
     * @param scale 缩放比例
     * @param range 采样范围
     * @return 采样点数
     */
    protected int calculateSampleCount(double scale, double range) {
        int samples = (int) (scale * range * DEFAULT_SAMPLES_PER_UNIT);
        return Math.max(MIN_SAMPLES, Math.min(samples, MAX_SAMPLES));
    }

    /**
     * 应用定义域限制
     *
     * @param viewMin 视图最小值
     * @param viewMax 视图最大值
     * @return [实际最小值, 实际最大值]
     */
    protected double[] applyDomainLimits(double viewMin, double viewMax) {
        double x1 = Double.isFinite(domainMin) ? Math.max(viewMin, domainMin) : viewMin;
        double x2 = Double.isFinite(domainMax) ? Math.min(viewMax, domainMax) : viewMax;
        return new double[]{x1, x2};
    }

    /**
     * 检查y值是否在视图范围内(带余量)
     *
     * @param y        y值
     * @param viewMinY 视图最小y
     * @param viewMaxY 视图最大y
     * @return 是否在范围内
     */
    protected boolean isYInViewRange(double y, double viewMinY, double viewMaxY) {
        if (!Double.isFinite(y)) {
            return false;
        }
        double margin = (viewMaxY - viewMinY) * VIEW_MARGIN_FACTOR;
        return y >= viewMinY - margin && y <= viewMaxY + margin;
    }

    /**
     * 标记参数已更改,需要更新表达式和重新采样
     */
    protected void onParameterChanged() {
        updateExpression();
        needsResampling = true;
    }

    @Override
    public double[] getBoundingBox() {
        if (sampledPoints.isEmpty()) {
            return new double[]{0, 0, 0, 0};
        }

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Point2D p : sampledPoints) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        return new double[]{minX, maxX, minY, maxY};
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        // 函数曲线本身不可拖动,返回空列表
        return new ArrayList<>();
    }

    // Getter/Setter

    public void setDomainRange(double min, double max) {
        this.domainMin = min;
        this.domainMax = max;
        this.needsResampling = true;
    }

    public void markNeedsResampling() {
        this.needsResampling = true;
    }

    public double getDomainMin() {
        return domainMin;
    }

    public double getDomainMax() {
        return domainMax;
    }

    public List<Point2D> getSampledPoints() {
        return new ArrayList<>(sampledPoints);
    }

    /**
     * 获取有效颜色(重写父类方法,使用函数专用的悬停颜色)
     */
    @Override
    protected javafx.scene.paint.Color getEffectiveColor() {
        if (selected) {
            return StyleManager.GEOMETRY_HOVER;
        } else if (hover) {
            return color.brighter();
        } else {
            return color;
        }
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }
}
