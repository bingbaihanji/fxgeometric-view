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

    private static final int CLIP_LEFT = 1;
    private static final int CLIP_RIGHT = 2;
    private static final int CLIP_BOTTOM = 4;
    private static final int CLIP_TOP = 8;

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
     * 分段采样缓存(世界坐标)
     * <p>
     * 用于显式表示函数断点、渐近线或视图裁剪造成的不连续片段。
     * sampledPoints 保留为平铺缓存,供旧代码兼容使用。
     */
    protected List<List<Point2D>> sampledSegments = new ArrayList<>();

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
     * 当前绘制视口对应的世界边界。
     */
    private double[] currentDrawBounds;

    /**
     * 构造函数
     */
    protected FunctionGeo() {
        super(ObjectType.FUNCTION);
        this.color = StyleManager.defaultLineColor;
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
        currentDrawBounds = viewBounds;
        double currentScale = transform.getScale();

        // 检查是否需要重新采样(缩放变化或视图范围变化)
        if (needsResamplingCheck(currentScale, viewBounds)) {
            sampledSegments.clear();
            samplePoints(viewBounds[0], viewBounds[1],
                    viewBounds[2], viewBounds[3],
                    currentScale);
            normalizeSampleSegments();
            if (sampledSegments.isEmpty() && !sampledPoints.isEmpty()) {
                rebuildSampleSegmentsFromPoints();
            }
            needsResampling = false;
            lastSampleScale = currentScale;
            lastViewBounds = viewBounds.clone();
        }

        // 应用线型
        LineStyleUtil.applyLineStyle(gc, lineType);
        gc.setStroke(getEffectiveColor());
        // 悬停时增加线宽以提供视觉反馈
        double mainLineWidth = hover ? getEffectiveLineWidth() + 1.5 : getEffectiveLineWidth();
        gc.setLineWidth(mainLineWidth);
        gc.setGlobalAlpha(opacity);

        // 发光通道（稍宽、半透明、实线）
        if (StyleManager.GLOW_ENABLED) {
            gc.save();
            LineStyleUtil.resetLineStyle(gc);
            gc.setGlobalAlpha(opacity * StyleManager.GLOW_ALPHA);
            gc.setLineWidth(mainLineWidth + StyleManager.GLOW_WIDTH_BONUS);
            drawCurve(gc, transform);
            gc.restore();
        }

        // 主描边
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
     * 绘制曲线（路径批处理优化版）
     * <p>
     * 将所有相邻采样点构成的线段汇入单个 beginPath()/stroke() 调用，
     * GPU 绘制调用从 O(n) 降至 O(1)。断点处通过 moveTo 跳过不连续段。
     * 参考 GeoGebra 的 GeneralPathClippedForCurvePlotter 批处理模式。
     */
    protected void drawCurve(GraphicsContext gc, WorldTransform transform) {
        if (sampledPoints.isEmpty()) {
            return;
        }

        gc.beginPath();

        for (List<Point2D> segment : getSampledSegmentsForInternalUse()) {
            Point2D prevPoint = null;
            for (Point2D point : segment) {
                if (prevPoint != null && !hasDiscontinuityBetween(prevPoint, point)) {
                    drawClippedLineSegment(gc, transform, prevPoint, point);
                }
                prevPoint = point;
            }
        }

        gc.stroke();
    }

    /**
     * 检测两点之间是否有断点
     * <p>
     * 如果y值跳变过大,可能是断点
     */
    protected boolean hasDiscontinuityBetween(Point2D p1, Point2D p2) {
        if (!Double.isFinite(p1.getX()) || !Double.isFinite(p1.getY())
                || !Double.isFinite(p2.getX()) || !Double.isFinite(p2.getY())) {
            return true;
        }

        double dy = Math.abs(p2.getY() - p1.getY());
        double dx = Math.abs(p2.getX() - p1.getX());

        // 如果斜率过大(可能是垂直渐近线)
        if (dx > 1e-10 && dy / dx > 1000) {
            double midX = (p1.getX() + p2.getX()) / 2.0;
            double midY = evaluate(midX);
            if (!Double.isFinite(midY)) {
                return true;
            }
            double linearMidY = (p1.getY() + p2.getY()) / 2.0;
            return Math.abs(midY - linearMidY) > Math.max(100, dy);
        }

        // 如果y值跳变超过视图高度的2倍
        if (dy > getVisibleYJumpThreshold()) {
            double midX = (p1.getX() + p2.getX()) / 2.0;
            double midY = evaluate(midX);
            if (!Double.isFinite(midY)) {
                return true;
            }
            double linearMidY = (p1.getY() + p2.getY()) / 2.0;
            return Math.abs(midY - linearMidY) > Math.max(getVisibleYJumpThreshold(), dy);
        }

        return false;
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
        for (List<Point2D> segment : getSampledSegmentsForInternalUse()) {
            for (int i = 0; i < segment.size() - 1; i++) {
                Point2D p1 = segment.get(i);
                Point2D p2 = segment.get(i + 1);
                if (hasDiscontinuityBetween(p1, p2)) {
                    continue;
                }

                double dist = pointToSegmentDistance(worldX, worldY,
                        p1.getX(), p1.getY(), p2.getX(), p2.getY());
                if (dist < tolerance) {
                    return true;
                }
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
     * 检查y值是否有限且不会让 JavaFX Canvas 接收极端坐标。
     */
    protected boolean isDrawableFiniteY(double y, double viewMinY, double viewMaxY) {
        if (!Double.isFinite(y)) {
            return false;
        }
        double height = Math.max(1.0, viewMaxY - viewMinY);
        double limit = Math.max(1.0E6, height * 1.0E6);
        return y >= viewMinY - limit && y <= viewMaxY + limit;
    }

    /**
     * 清空平铺与分段采样缓存。
     */
    protected void clearSampleCache() {
        sampledPoints.clear();
        sampledSegments.clear();
    }

    /**
     * 将一个采样点加入当前连续片段,同时维护平铺缓存。
     */
    protected void addSamplePoint(Point2D point) {
        if (point == null || !Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
            return;
        }
        sampledPoints.add(point);
        if (sampledSegments.isEmpty()) {
            sampledSegments.add(new ArrayList<>());
        }
        sampledSegments.get(sampledSegments.size() - 1).add(point);
    }

    /**
     * 显式开始一个新的连续片段。
     */
    protected void startNewSampleSegment() {
        if (sampledSegments.isEmpty() || sampledSegments.get(sampledSegments.size() - 1).isEmpty()) {
            return;
        }
        sampledSegments.add(new ArrayList<>());
    }

    /**
     * 从平铺采样点重建分段缓存,用于兼容仍直接写 sampledPoints 的函数子类。
     */
    protected void rebuildSampleSegmentsFromPoints() {
        sampledSegments.clear();
        if (sampledPoints.isEmpty()) {
            return;
        }

        List<Point2D> currentSegment = new ArrayList<>();
        Point2D previous = null;
        for (Point2D point : sampledPoints) {
            if (point == null || !Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
                if (!currentSegment.isEmpty()) {
                    sampledSegments.add(currentSegment);
                    currentSegment = new ArrayList<>();
                }
                previous = null;
                continue;
            }

            if (previous != null && hasDiscontinuityBetween(previous, point)) {
                if (!currentSegment.isEmpty()) {
                    sampledSegments.add(currentSegment);
                }
                currentSegment = new ArrayList<>();
            }

            currentSegment.add(point);
            previous = point;
        }

        if (!currentSegment.isEmpty()) {
            sampledSegments.add(currentSegment);
        }
    }

    /**
     * 移除空片段,避免后续绘制和命中测试处理无效列表。
     */
    protected void normalizeSampleSegments() {
        sampledSegments.removeIf(List::isEmpty);
    }

    /**
     * 内部使用的分段缓存视图。
     */
    protected List<List<Point2D>> getSampledSegmentsForInternalUse() {
        if (sampledSegments.isEmpty() && !sampledPoints.isEmpty()) {
            rebuildSampleSegmentsFromPoints();
        }
        return sampledSegments;
    }

    private void drawClippedLineSegment(GraphicsContext gc, WorldTransform transform,
                                        Point2D p1, Point2D p2) {
        double[] segment = clipToCurrentDrawBounds(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        if (segment == null) {
            return;
        }

        double sx1 = transform.worldToScreenX(segment[0]);
        double sy1 = transform.worldToScreenY(segment[1]);
        double sx2 = transform.worldToScreenX(segment[2]);
        double sy2 = transform.worldToScreenY(segment[3]);

        gc.moveTo(sx1, sy1);
        gc.lineTo(sx2, sy2);
    }

    private double[] clipToCurrentDrawBounds(double x1, double y1, double x2, double y2) {
        if (currentDrawBounds == null) {
            return new double[]{x1, y1, x2, y2};
        }

        double minX = currentDrawBounds[0];
        double maxX = currentDrawBounds[1];
        double minY = currentDrawBounds[2];
        double maxY = currentDrawBounds[3];

        int code1 = computeOutCode(x1, y1, minX, maxX, minY, maxY);
        int code2 = computeOutCode(x2, y2, minX, maxX, minY, maxY);

        while (true) {
            if ((code1 | code2) == 0) {
                return new double[]{x1, y1, x2, y2};
            }
            if ((code1 & code2) != 0) {
                return null;
            }

            int outCode = code1 != 0 ? code1 : code2;
            double x;
            double y;

            if ((outCode & CLIP_TOP) != 0) {
                x = x1 + (x2 - x1) * (maxY - y1) / (y2 - y1);
                y = maxY;
            } else if ((outCode & CLIP_BOTTOM) != 0) {
                x = x1 + (x2 - x1) * (minY - y1) / (y2 - y1);
                y = minY;
            } else if ((outCode & CLIP_RIGHT) != 0) {
                y = y1 + (y2 - y1) * (maxX - x1) / (x2 - x1);
                x = maxX;
            } else {
                y = y1 + (y2 - y1) * (minX - x1) / (x2 - x1);
                x = minX;
            }

            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                return null;
            }

            if (outCode == code1) {
                x1 = x;
                y1 = y;
                code1 = computeOutCode(x1, y1, minX, maxX, minY, maxY);
            } else {
                x2 = x;
                y2 = y;
                code2 = computeOutCode(x2, y2, minX, maxX, minY, maxY);
            }
        }
    }

    private int computeOutCode(double x, double y,
                               double minX, double maxX,
                               double minY, double maxY) {
        int code = 0;
        if (x < minX) {
            code |= CLIP_LEFT;
        } else if (x > maxX) {
            code |= CLIP_RIGHT;
        }
        if (y < minY) {
            code |= CLIP_BOTTOM;
        } else if (y > maxY) {
            code |= CLIP_TOP;
        }
        return code;
    }

    private double getVisibleYJumpThreshold() {
        if (currentDrawBounds == null) {
            return 100;
        }
        double visibleHeight = currentDrawBounds[3] - currentDrawBounds[2];
        return Math.max(100, visibleHeight * 2);
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
        return computeBoundingBox(sampledPoints, Point2D::getX, Point2D::getY);
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

    public List<List<Point2D>> getSampledSegments() {
        List<List<Point2D>> source = getSampledSegmentsForInternalUse();
        List<List<Point2D>> copy = new ArrayList<>(source.size());
        for (List<Point2D> segment : source) {
            copy.add(new ArrayList<>(segment));
        }
        return copy;
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
