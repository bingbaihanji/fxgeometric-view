package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.UnitLabelType;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.util.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.WorldPainter;
import com.bingbaihanji.view.layout.draw.geometry.impl.AxesPainter;
import com.bingbaihanji.view.layout.draw.geometry.impl.GridPainter;
import com.bingbaihanji.view.layout.draw.geometry.impl.TrigonometricFunctionGeo;
import com.bingbaihanji.view.layout.draw.tools.CircleDrawingTool;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * @author bingbaihanji
 * @date 2025-12-20 16:12:42
 * @description 格点图视图
 */
public class GridChartView extends Pane {

    // 画布管理器
    private final CanvasManager canvasManager = new CanvasManager();
    //  坐标系统
    private final WorldTransform transform = new WorldTransform();
    private final List<WorldPainter> painters = new ArrayList<>();
    private final List<WorldObject> objects;
    private final CircleDrawingTool circleTool;
    //  视图配置
    private final EuclidianViewSettings settings = new EuclidianViewSettings();
    // 对象变化监听器列表(观察者模式)
    private final List<Runnable> objectChangeListeners = new ArrayList<>();
    private final SnapCalculator snapCalculator;
    private final HoverTooltipManager tooltipManager;
    private final ViewportController viewportController;
    private WorldObject hoverObject = null;
    // 预览绘制器
    private BiConsumer<GraphicsContext, WorldTransform> previewPainter;
    // BoundingBox 绘制器
    private BiConsumer<GraphicsContext, WorldTransform> boundingBoxPainter;
    /**
     * 初始化状态标志
     * <p>
     * 用于跟踪组件是否已完成首次坐标初始化
     * 确保layoutChildren()中的初始化逻辑只执行一次
     * 避免每次布局时都重置用户可能已经调整过的原点位置
     */
    private boolean initialized = false;
    // 当前鼠标附近的特殊点(用于视觉反馈)
    private SpecialPoint nearbySpecialPoint = null;
    private Runnable onTransformChanged;
    // 轴向吸附状态(用于视觉反馈)
    private SnapCalculator.AxisSnapInfo axisSnapInfo = null;
    /**
     * 是否显示吸附辅助线(默认false,只在绘制模式下显示)
     */
    private boolean showSnapGuideLines = false;
    // 画布背景颜色
    private Color backgroundColor = Color.WHITE;
    // 自定义默认光标
    private Cursor customDefaultCursor = null;
    // 重绘防抖标记：避免同帧内多次redraw()导致重复绘制
    private boolean redrawScheduled = false;
    /** 背景层离屏缓存，避免每帧重复绘制网格和坐标轴 */
    private final BackgroundBuffer backgroundBuffer = new BackgroundBuffer();
    private int trigFunctionCount = 0;

    //  构造
    public GridChartView() {
        this(new ArrayList<>());
    }

    /**
     * 构造函数(支持共享对象列表)
     *
     * @param sharedObjects 共享的对象列表,用于多窗口同步
     */
    public GridChartView(List<WorldObject> sharedObjects) {
        this.objects = sharedObjects;
        circleTool = new CircleDrawingTool();
        snapCalculator = new SnapCalculator(settings, transform, this);
        tooltipManager = new HoverTooltipManager(this, snapCalculator, transform, () -> objects, this::redrawInteraction);

        canvasManager.bindToPane(this);
        canvasManager.bindSize(this);

        widthProperty().addListener((obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        });
        heightProperty().addListener((obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        });

        new ViewInteractionHandler(this, transform, snapCalculator, tooltipManager).install();
        viewportController = new ViewportController(this, transform, settings);

        redraw();

        addPainter(new GridPainter(settings));
        addPainter(new AxesPainter(true, settings));

        applySettingsToTransform();

        setCustomCursorForPane(this, "/icon/mouseStyle.png");

        tooltipManager.install(this);
    }

    public List<WorldPainter> getPainters() {
        return painters;
    }

    public Runnable getOnTransformChanged() {
        return onTransformChanged;
    }

    // 回掉接口
    public void setOnTransformChanged(Runnable runable) {
        this.onTransformChanged = runable;
    }

    /**
     * 布局子节点方法 - 在JavaFX布局系统中自动调用
     */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (!initialized && getWidth() > 0 && getHeight() > 0) {
            transform.centerWorldAt(0, 0, getWidth(), getHeight());
            initialized = true;
            redraw();
        }
    }

    //  坐标变换
    public double worldToScreenX(double x) {
        return transform.worldToScreenX(x);
    }

    public double worldToScreenY(double y) {
        return transform.worldToScreenY(y);
    }

    public double screenToWorldX(double x) {
        return transform.screenToWorldX(x);
    }

    public double screenToWorldY(double y) {
        return transform.screenToWorldY(y);
    }

    //  重绘

    /**
     * 保证窗口尺寸变化时,视图中心的世界点不发生跳变
     */
    private void updateOffsetFromCenterWorld() {
        double centerWorldX = transform.screenToWorldX(getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(getHeight() / 2);
        transform.centerWorldAt(centerWorldX, centerWorldY, getWidth(), getHeight());
    }

    /**
     * 重绘整个画布(防抖：同帧内多次调用只触发一次实际重绘)
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
     * 立即重绘(不经过防抖，用于需要同步绘制结果的场景)
     */
    public void redrawImmediate() {
        redrawScheduled = false;
        redrawBackground();
        redrawObjects();
        redrawInteraction();
    }

    /**
     * 仅重绘背景层（网格 + 坐标轴），使用离屏缓存避免重复绘制
     */
    public void redrawBackground() {
        double w = canvasManager.getWidth();
        double h = canvasManager.getHeight();

        if (!backgroundBuffer.isValid(w, h)) {
            GraphicsContext gc = backgroundBuffer.beginDraw(w, h);
            gc.setFill(backgroundColor);
            gc.fillRect(0, 0, w, h);
            for (WorldPainter painter : painters) {
                painter.paint(gc, transform, w, h);
            }
            backgroundBuffer.endDraw();
        }

        // 统一屏幕复制（缓存命中或重绘后都执行）
        GraphicsContext screenGc = canvasManager.getBackgroundGC();
        screenGc.clearRect(0, 0, w, h);
        screenGc.setFill(backgroundColor);
        screenGc.fillRect(0, 0, w, h);
        backgroundBuffer.copyTo(screenGc);
    }

    /**
     * 标脏背景缓存，坐标变换或设置变更后调用
     */
    public void invalidateBackground() {
        backgroundBuffer.invalidate();
    }

    /**
     * 仅重绘对象层
     * <p>
     * 按图层（layer）升序排列后绘制，确保高图层对象在低图层对象之上。
     * 参考 GeoGebra 的 DrawableList —— 所有 Drawable 按 z-order 排序后遍历绘制。
     */
    public void redrawObjects() {
        GraphicsContext gc = canvasManager.getObjectGC();
        gc.clearRect(0, 0, canvasManager.getWidth(), canvasManager.getHeight());

        double w = canvasManager.getWidth();
        double h = canvasManager.getHeight();

        // 按图层排序：低 layer 先绘制（在底层），高 layer 后绘制（在上层）
        List<WorldObject> sortedObjects = new ArrayList<>(objects);
        sortedObjects.sort((a, b) -> Integer.compare(a.getLayer(), b.getLayer()));

        for (WorldObject obj : sortedObjects) {
            if (obj.isVisible()) {
                obj.paint(gc, transform, w, h);
            }
        }
    }

    /**
     * 仅重绘交互层（预览、吸附提示、BoundingBox）
     */
    public void redrawInteraction() {
        GraphicsContext gc = canvasManager.getInteractionGC();
        gc.clearRect(0, 0, canvasManager.getWidth(), canvasManager.getHeight());

        if (boundingBoxPainter != null) {
            boundingBoxPainter.accept(gc, transform);
        }

        if (previewPainter != null) {
            previewPainter.accept(gc, transform);
        }

        if (circleTool != null) {
            circleTool.paintPreview(gc, transform);
        }

        if (nearbySpecialPoint != null) {
            drawSpecialPointHint(gc);
        }

        if (axisSnapInfo != null) {
            drawAxisSnapGuideLine(gc);
        }
    }

    /**
     * 绘制特殊点吸附提示(高亮圈)
     */
    private void drawSpecialPointHint(GraphicsContext gc) {
        double sx = transform.worldToScreenX(nearbySpecialPoint.getX());
        double sy = transform.worldToScreenY(nearbySpecialPoint.getY());

        gc.setStroke(GeometryConfig.Colors.SNAP_HINT);
        gc.setLineWidth(2);
        gc.setLineDashes(null);

        double radius1 = 8;
        double radius2 = 12;
        gc.strokeOval(sx - radius1, sy - radius1, radius1 * 2, radius1 * 2);
        gc.strokeOval(sx - radius2, sy - radius2, radius2 * 2, radius2 * 2);

        gc.setFill(GeometryConfig.Colors.SNAP_HINT_FILL);
        double centerRadius = 3;
        gc.fillOval(sx - centerRadius, sy - centerRadius, centerRadius * 2, centerRadius * 2);
    }

    // 对象管理

    /**
     * 绘制轴向吸附辅助线
     */
    private void drawAxisSnapGuideLine(GraphicsContext gc) {
        if (!showSnapGuideLines || axisSnapInfo == null) {
            return;
        }

        gc.setStroke(GeometryConfig.Colors.SNAP_GUIDE_LINE);
        gc.setLineWidth(1.5);
        gc.setLineDashes(5, 5);

        gc.strokeLine(axisSnapInfo.lineStartX, axisSnapInfo.lineStartY,
                axisSnapInfo.lineEndX, axisSnapInfo.lineEndY);

        gc.setLineDashes(null);
    }

    public void addObject(WorldObject obj) {
        objects.add(obj);
        incrementTrigCountIfApplicable(obj);
        updateAxisUnitTypeBasedOnFunctions();
        notifyObjectChange();
        redraw();
    }

    public void removeObject(WorldObject obj) {
        objects.remove(obj);
        decrementTrigCountIfApplicable(obj);
        updateAxisUnitTypeBasedOnFunctions();
        notifyObjectChange();
        redraw();
    }

    /**
     * 清除所有图形对象
     */
    public void clearAllObjects() {
        objects.clear();
        trigFunctionCount = 0;
        updateAxisUnitTypeBasedOnFunctions();
        redraw();
    }

    /**
     * 获取所有图形对象的只读视图(零分配)
     */
    public List<WorldObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    /**
     * 获取所有图形对象的副本(需要修改列表时使用)
     */
    public List<WorldObject> getObjectsCopy() {
        return new ArrayList<>(objects);
    }

    /**
     * 根据当前对象列表中是否有三角函数来自动调整坐标轴单位类型
     * 增量更新模式：通过计数器避免每次O(n)扫描
     */
    private void updateAxisUnitTypeBasedOnFunctions() {
        settings.setUnitLabelType(trigFunctionCount > 0 ? UnitLabelType.PI : UnitLabelType.NUMERIC);
    }

    private void incrementTrigCountIfApplicable(WorldObject obj) {
        if (obj instanceof TrigonometricFunctionGeo) {
            trigFunctionCount++;
        }
    }

    private void decrementTrigCountIfApplicable(WorldObject obj) {
        if (obj instanceof TrigonometricFunctionGeo) {
            trigFunctionCount--;
        }
    }

    // 监听器

    /**
     * 添加对象变化监听器(观察者模式)
     */
    public void addObjectChangeListener(Runnable listener) {
        objectChangeListeners.add(listener);
    }

    /**
     * 移除对象变化监听器
     */
    public void removeObjectChangeListener(Runnable listener) {
        objectChangeListeners.remove(listener);
    }

    /**
     * 通知所有监听器对象已变化
     */
    private void notifyObjectChange() {
        for (Runnable listener : objectChangeListeners) {
            listener.run();
        }
    }

    // Hover / Snap 状态 getter/setter (供 ViewInteractionHandler 使用)

    public WorldObject getHoverObject() {
        return hoverObject;
    }

    public void setHoverObject(WorldObject hoverObject) {
        this.hoverObject = hoverObject;
    }

    public SpecialPoint getNearbySpecialPoint() {
        return nearbySpecialPoint;
    }

    public void setNearbySpecialPoint(SpecialPoint nearbySpecialPoint) {
        this.nearbySpecialPoint = nearbySpecialPoint;
    }

    public SnapCalculator.AxisSnapInfo getAxisSnapInfo() {
        return axisSnapInfo;
    }

    public void setAxisSnapInfo(SnapCalculator.AxisSnapInfo axisSnapInfo) {
        this.axisSnapInfo = axisSnapInfo;
    }

    // 预览 / BoundingBox 绘制器

    public void setPreviewPainter(BiConsumer<GraphicsContext, WorldTransform> previewPainter) {
        this.previewPainter = previewPainter;
    }

    public void setBoundingBoxPainter(BiConsumer<GraphicsContext, WorldTransform> boundingBoxPainter) {
        this.boundingBoxPainter = boundingBoxPainter;
    }

    // 视图配置

    /**
     * 获取视图配置
     */
    public EuclidianViewSettings getSettings() {
        return settings;
    }

    /**
     * 应用配置(刷新视图)
     */
    public void applySettings() {
        applySettingsToTransform();
        redraw();
    }

    /**
     * 将配置同步到 Transform
     */
    private void applySettingsToTransform() {
        transform.setScaleX(settings.getXScale());
        transform.setScaleY(settings.getYScale());
    }

    /**
     * 获取当前缩放比例
     */
    public double getScale() {
        return transform.getScale();
    }

    /**
     * 获取世界坐标变换对象
     */
    public WorldTransform getTransform() {
        return transform;
    }

    public void addPainter(WorldPainter painter) {
        painters.add(painter);
        invalidateBackground();
        redraw();
    }

    public void removePainter(WorldPainter painter) {
        painters.remove(painter);
        invalidateBackground();
        redraw();
    }

    // 背景颜色

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        invalidateBackground();
        redraw();
    }

    /**
     * 设置是否显示吸附辅助线
     */
    public void setShowSnapGuideLines(boolean show) {
        this.showSnapGuideLines = show;
    }

    // 视口控制 (委托给 ViewportController)

    public void zoomToPercent(double percent) {
        viewportController.zoomToPercent(percent);
    }

    public void setAxisRatio(double xRatio, double yRatio) {
        viewportController.setAxisRatio(xRatio, yRatio);
    }

    public void fitAllObjects() {
        viewportController.fitAllObjects();
    }

    public void resetToStandardView() {
        viewportController.resetToStandardView();
    }

    // 光标

    private void setCustomCursorForPane(Pane pane, String imagePath) {
        URL url = getClass().getResource(imagePath);
        if (url == null) {
            pane.setCursor(Cursor.HAND);
            customDefaultCursor = Cursor.HAND;
            return;
        }

        Image cursorImage = new Image(url.toExternalForm());
        Cursor customCursor = new ImageCursor(cursorImage, 0, 0);
        customDefaultCursor = customCursor;

        pane.setOnMouseEntered(e -> pane.setCursor(customCursor));
        pane.setOnMouseExited(e -> pane.setCursor(customCursor));
    }

    /**
     * 获取默认的自定义光标
     */
    public Cursor getDefaultCursor() {
        return customDefaultCursor != null ? customDefaultCursor : Cursor.DEFAULT;
    }
}
