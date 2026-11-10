package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.geometry.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.geometry.WorldObject;
import com.bingbaihanji.geometry.WorldTransform;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 视图渲染器
 * <p>
 * 负责三层画布的绘制调度: 背景层(网格/坐标轴), 对象层, 交互层.
 * 背景使用离屏缓存, 仅在变换或设置变更后重绘;
 * 对象层按图层排序绘制; 交互层绘制预览, 吸附提示与包围盒.
 */
public class ViewRenderer {

    private final CanvasManager canvasManager;
    private final BackgroundBuffer backgroundBuffer;
    private final CoordSystemRenderer coordSystemRenderer;
    private final WorldTransform transform;
    private final EuclidianViewSettings settings;
    private final Supplier<List<WorldObject>> objectsSupplier;

    /**
     * 按图层排序的绘制缓冲, 复用避免每帧分配
     */
    private final List<WorldObject> sortedObjectsBuffer = new ArrayList<>();

    /**
     * 画布背景颜色
     */
    private Color backgroundColor = Color.WHITE;

    /**
     * 预览绘制器
     */
    private BiConsumer<GraphicsContext, WorldTransform> previewPainter;

    /**
     * 当前鼠标附近的特殊点(用于视觉反馈)
     */
    private SpecialPoint nearbySpecialPoint;

    /**
     * 轴向吸附状态(用于视觉反馈)
     */
    private SnapCalculator.AxisSnapInfo axisSnapInfo;

    /**
     * 是否显示吸附辅助线
     */
    private boolean showSnapGuideLines;

    /**
     * 重绘防抖标记
     */
    private boolean redrawScheduled;

    /**
     * 构造函数
     *
     * @param canvasManager       画布管理器
     * @param backgroundBuffer    背景离屏缓存
     * @param coordSystemRenderer 坐标系统渲染器
     * @param transform           坐标变换
     * @param settings            视图配置
     * @param objectsSupplier     对象列表提供者
     */
    public ViewRenderer(CanvasManager canvasManager, BackgroundBuffer backgroundBuffer,
                        CoordSystemRenderer coordSystemRenderer, WorldTransform transform,
                        EuclidianViewSettings settings, Supplier<List<WorldObject>> objectsSupplier) {
        this.canvasManager = canvasManager;
        this.backgroundBuffer = backgroundBuffer;
        this.coordSystemRenderer = coordSystemRenderer;
        this.transform = transform;
        this.settings = settings;
        this.objectsSupplier = objectsSupplier;
    }

    /**
     * 重绘整个画布(防抖: 同帧内多次调用只触发一次实际重绘)
     */
    public void redraw() {
        if (redrawScheduled) {
            return;
        }
        redrawScheduled = true;
        Platform.runLater(() -> {
            redrawScheduled = false;
            redrawBackground();
            redrawObjects();
            redrawInteraction();
        });
    }

    /**
     * 仅重绘背景层(网格 + 坐标轴), 使用离屏缓存
     */
    public void redrawBackground() {
        double w = canvasManager.getWidth();
        double h = canvasManager.getHeight();

        if (!backgroundBuffer.isValid(w, h)) {
            GraphicsContext gc = backgroundBuffer.beginDraw(w, h);
            gc.setFill(backgroundColor);
            gc.fillRect(0, 0, w, h);
            coordSystemRenderer.render(gc, transform, settings, w, h);
            backgroundBuffer.endDraw();
        }

        // 统一屏幕复制(缓存命中或重绘后都执行)
        GraphicsContext screenGc = canvasManager.getBackgroundGC();
        screenGc.clearRect(0, 0, w, h);
        screenGc.setFill(backgroundColor);
        screenGc.fillRect(0, 0, w, h);
        backgroundBuffer.copyTo(screenGc);
    }

    /**
     * 仅重绘对象层, 按图层升序绘制
     */
    public void redrawObjects() {
        GraphicsContext gc = canvasManager.getObjectGC();
        double w = canvasManager.getWidth();
        double h = canvasManager.getHeight();
        gc.clearRect(0, 0, w, h);

        // 低 layer 先绘制(在底层), 高 layer 后绘制(在上层)
        sortedObjectsBuffer.clear();
        sortedObjectsBuffer.addAll(objectsSupplier.get());
        sortedObjectsBuffer.sort(Comparator.comparingInt(WorldObject::getLayer));

        for (WorldObject obj : sortedObjectsBuffer) {
            if (obj.isVisible()) {
                obj.paint(gc, transform, w, h);
            }
        }
    }

    /**
     * 仅重绘交互层(包围盒, 预览, 吸附提示)
     */
    public void redrawInteraction() {
        GraphicsContext gc = canvasManager.getInteractionGC();
        gc.clearRect(0, 0, canvasManager.getWidth(), canvasManager.getHeight());

        if (previewPainter != null) {
            previewPainter.accept(gc, transform);
        }
        if (nearbySpecialPoint != null) {
            drawSpecialPointHint(gc);
        }
        if (axisSnapInfo != null) {
            drawAxisSnapGuideLine(gc);
        }
    }

    /**
     * 标脏背景缓存, 坐标变换或设置变更后调用
     */
    public void invalidateBackground() {
        backgroundBuffer.invalidate();
    }

    /**
     * 绘制特殊点吸附提示(高亮圈)
     */
    private void drawSpecialPointHint(GraphicsContext gc) {
        double sx = transform.worldToScreenX(nearbySpecialPoint.getX());
        double sy = transform.worldToScreenY(nearbySpecialPoint.getY());

        gc.setStroke(GeometryConfig.Colors.SNAP_HINT);
        gc.setLineWidth(2);
        gc.setLineDashes();

        double radius1 = 8;
        double radius2 = 12;
        gc.strokeOval(sx - radius1, sy - radius1, radius1 * 2, radius1 * 2);
        gc.strokeOval(sx - radius2, sy - radius2, radius2 * 2, radius2 * 2);

        gc.setFill(GeometryConfig.Colors.SNAP_HINT_FILL);
        double centerRadius = 3;
        gc.fillOval(sx - centerRadius, sy - centerRadius, centerRadius * 2, centerRadius * 2);
    }

    /**
     * 绘制轴向吸附辅助线
     */
    private void drawAxisSnapGuideLine(GraphicsContext gc) {
        if (!showSnapGuideLines) {
            return;
        }

        gc.setStroke(GeometryConfig.Colors.SNAP_GUIDE_LINE);
        gc.setLineWidth(1.5);
        gc.setLineDashes(5, 5);
        gc.strokeLine(axisSnapInfo.lineStartX, axisSnapInfo.lineStartY,
                axisSnapInfo.lineEndX, axisSnapInfo.lineEndY);
        gc.setLineDashes();
    }

    /**
     * 设置画布背景颜色
     *
     * @param color 背景色
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    /**
     * 获取画布背景颜色
     *
     * @return 背景色
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * 设置预览绘制器
     *
     * @param previewPainter 预览绘制器
     */
    public void setPreviewPainter(BiConsumer<GraphicsContext, WorldTransform> previewPainter) {
        this.previewPainter = previewPainter;
    }

    /**
     * 设置特殊点吸附提示
     *
     * @param specialPoint 特殊点, 可为 null
     */
    public void setNearbySpecialPoint(SpecialPoint specialPoint) {
        this.nearbySpecialPoint = specialPoint;
    }

    /**
     * 设置轴向吸附状态
     *
     * @param axisSnapInfo 吸附状态, 可为 null
     */
    public void setAxisSnapInfo(SnapCalculator.AxisSnapInfo axisSnapInfo) {
        this.axisSnapInfo = axisSnapInfo;
    }

    /**
     * 获取当前的轴向吸附状态
     *
     * @return 吸附状态, 无则为 null
     */
    public SnapCalculator.AxisSnapInfo getAxisSnapInfo() {
        return axisSnapInfo;
    }

    /**
     * 获取当前的特殊点吸附提示
     *
     * @return 特殊点, 无则为 null
     */
    public SpecialPoint getNearbySpecialPoint() {
        return nearbySpecialPoint;
    }

    /**
     * 设置是否显示吸附辅助线
     *
     * @param show 是否显示
     */
    public void setShowSnapGuideLines(boolean show) {
        this.showSnapGuideLines = show;
    }
}
