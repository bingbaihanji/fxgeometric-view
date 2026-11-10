package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.constant.UnitLabelType;
import com.bingbaihanji.geometry.SpecialPointManager.SpecialPoint;
import com.bingbaihanji.geometry.WorldObject;
import com.bingbaihanji.geometry.WorldTransform;
import com.bingbaihanji.i18n.I18nRefreshable;
import com.bingbaihanji.i18n.I18nUtil;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 格点图视图
 * <p>
 * 作为画布的协调者: 组合画布层, 坐标变换, 对象集合, 渲染器与各交互控制器.
 * 具体的绘制调度交给 {@link ViewRenderer}, 对象集合管理交给 {@link ObjectCollection},
 * 光标交给 {@link ViewCursor}, 视口操作委托给 {@link ViewportController}.
 *
 * @author bingbaihanji
 * @date 2025-12-20 16:12:42
 */
public class GridChartView extends Pane implements I18nRefreshable {

    /**
     * 坐标系统
     */
    private final WorldTransform transform = new WorldTransform();

    /**
     * 对象集合
     */
    private final ObjectCollection objects;

    /**
     * 视图配置
     */
    private final EuclidianViewSettings settings = new EuclidianViewSettings();

    /**
     * 三层画布渲染器
     */
    private final ViewRenderer renderer;

    /**
     * 光标管理
     */
    private final ViewCursor cursor = new ViewCursor();

    private final SnapCalculator snapCalculator;
    private final ViewportController viewportController;

    /**
     * 点击涟漪动效(顶层透明覆盖层)
     */
    private final ClickRippleEffect clickRippleEffect;

    /**
     * 悬停对象
     */
    private WorldObject hoverObject;

    /**
     * 初始化状态标志: 首次布局时只居中一次
     */
    private boolean initialized;

    // 监听器引用, 供 dispose() 清理
    private javafx.beans.value.ChangeListener<Number> widthListener;
    private javafx.beans.value.ChangeListener<Number> heightListener;

    private final Runnable settingsListener = () -> {
        invalidateBackground();
        redraw();
    };

    /**
     * 构造函数
     */
    public GridChartView() {
        this(new ArrayList<>());
    }

    /**
     * 构造函数(与另一个视图共享对象列表)
     * <p>
     * 直接共享列表本体, 两个窗口的增删互相同步。
     * 注意不能传 {@link #getObjects()} 的只读视图, 否则新增对象会抛 UnsupportedOperationException。
     *
     * @param sourceView 源视图
     */
    public GridChartView(GridChartView sourceView) {
        this(sourceView.objects.sharedList());
    }

    /**
     * 构造函数(支持共享对象列表)
     *
     * @param sharedObjects 共享的对象列表, 用于多窗口同步
     */
    public GridChartView(List<WorldObject> sharedObjects) {
        this.objects = new ObjectCollection(sharedObjects);
        this.snapCalculator = new SnapCalculator(settings, transform, this);
        HoverTooltipManager tooltipManager = new HoverTooltipManager(
                this, snapCalculator, transform, objects::get, this::redrawInteraction);

        CanvasManager canvasManager = new CanvasManager();
        this.renderer = new ViewRenderer(canvasManager, new BackgroundBuffer(),
                new CoordSystemRenderer(), transform, settings, objects::get);

        canvasManager.bindToPane(this);
        canvasManager.bindSize(this);

        // 点击涟漪覆盖层: 置于三层画布之上, mouseTransparent 不拦截事件
        clickRippleEffect = new ClickRippleEffect(this);
        getChildren().add(clickRippleEffect.getCanvas());

        widthListener = (obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        };
        heightListener = (obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        };
        widthProperty().addListener(widthListener);
        heightProperty().addListener(heightListener);

        new ViewInteractionHandler(this, transform, snapCalculator, tooltipManager).install();
        viewportController = new ViewportController(this, transform, settings);

        redraw();

        applySettingsToTransform();

        // 注册设置变更监听器: 任何视觉设置变更后自动标脏背景缓存
        settings.addSettingsChangeListener(settingsListener);

        cursor.install(this, "/icon/mouseStyle.png");

        tooltipManager.install(this);
    }

    /**
     * JavaFX 布局回调: 首次拿到非零尺寸时把世界原点居中
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

    /**
     * 窗口尺寸变化时保持视图中心的世界点不跳变
     */
    private void updateOffsetFromCenterWorld() {
        double centerWorldX = transform.screenToWorldX(getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(getHeight() / 2);
        transform.centerWorldAt(centerWorldX, centerWorldY, getWidth(), getHeight());
    }

    /**
     * 语言切换时刷新所有文本(背景层标签 + 图形标签 + 对象层)
     */
    @Override
    public void refreshI18n() {
        invalidateBackground();
        redraw();
    }

    /**
     * 释放资源, 移除所有监听器, 防止内存泄漏
     */
    public void dispose() {
        if (widthListener != null) {
            widthProperty().removeListener(widthListener);
        }
        if (heightListener != null) {
            heightProperty().removeListener(heightListener);
        }
        settings.removeSettingsChangeListener(settingsListener);
        // 摘除全局 I18n 注册, 否则语言上下文会一直持有已销毁视图的强引用
        I18nUtil.removeRefreshable(this);
        // 停掉可能在跑的缩放动画, 它的回调会继续驱动已销毁的视图
        viewportController.stopZoomAnimation();
        clickRippleEffect.stop();
    }

    //  重绘(委托给 ViewRenderer)

    /**
     * 重绘整个画布(防抖)
     */
    public void redraw() {
        renderer.redraw();
    }

    /**
     * 仅重绘对象层
     */
    public void redrawObjects() {
        renderer.redrawObjects();
    }

    /**
     * 仅重绘交互层
     */
    public void redrawInteraction() {
        renderer.redrawInteraction();
    }

    /**
     * 标脏背景缓存
     */
    public void invalidateBackground() {
        renderer.invalidateBackground();
    }

    // 对象管理(委托给 ObjectCollection)

    public void addObject(WorldObject obj) {
        objects.add(obj);
        snapCalculator.invalidateSpecialPointCache();
        updateAxisUnitTypeBasedOnFunctions();
        redraw();
    }

    public void removeObject(WorldObject obj) {
        objects.remove(obj);
        snapCalculator.invalidateSpecialPointCache();
        updateAxisUnitTypeBasedOnFunctions();
        redraw();
    }

    /**
     * 清除所有图形对象
     */
    public void clearAllObjects() {
        objects.clear();
        snapCalculator.invalidateSpecialPointCache();
        updateAxisUnitTypeBasedOnFunctions();
        redraw();
    }

    /**
     * 获取所有图形对象的只读视图(零分配)
     */
    public List<WorldObject> getObjects() {
        return objects.get();
    }

    /**
     * 根据是否存在三角函数自动调整坐标轴单位标签
     */
    private void updateAxisUnitTypeBasedOnFunctions() {
        settings.setUnitLabelType(objects.hasTrigonometricFunction() ? UnitLabelType.PI : UnitLabelType.NUMERIC);
    }

    // 悬停与吸附状态

    public WorldObject getHoverObject() {
        return hoverObject;
    }

    public void setHoverObject(WorldObject hoverObject) {
        this.hoverObject = hoverObject;
    }

    /**
     * 获取当前特殊点吸附提示
     */
    public SpecialPoint getNearbySpecialPoint() {
        return renderer.getNearbySpecialPoint();
    }

    /**
     * 设置特殊点吸附提示
     */
    public void setNearbySpecialPoint(SpecialPoint nearbySpecialPoint) {
        renderer.setNearbySpecialPoint(nearbySpecialPoint);
    }

    /**
     * 获取当前轴向吸附状态
     */
    public SnapCalculator.AxisSnapInfo getAxisSnapInfo() {
        return renderer.getAxisSnapInfo();
    }

    /**
     * 设置轴向吸附状态
     */
    public void setAxisSnapInfo(SnapCalculator.AxisSnapInfo axisSnapInfo) {
        renderer.setAxisSnapInfo(axisSnapInfo);
    }

    // 预览与包围盒绘制器

    public void setPreviewPainter(BiConsumer<GraphicsContext, WorldTransform> previewPainter) {
        renderer.setPreviewPainter(previewPainter);
    }

    // 视图配置

    public EuclidianViewSettings getSettings() {
        return settings;
    }

    /**
     * 应用配置(刷新视图)
     */
    public void applySettings() {
        applySettingsToTransform();
        invalidateBackground();
        redraw();
    }

    /**
     * 将配置同步到坐标变换
     */
    private void applySettingsToTransform() {
        transform.setScaleX(settings.getXScale());
        transform.setScaleY(settings.getYScale());

        // 极坐标要求 X/Y 轴比例锁定为 1:1, 确保极轴保持水平
        if (settings.getGridType() == GridType.POLAR) {
            double avgScale = (transform.getScaleX() + transform.getScaleY()) / 2;
            transform.setScaleX(avgScale);
            transform.setScaleY(avgScale);
            settings.silentUpdate(s -> {
                s.setXScale(avgScale);
                s.setYScale(avgScale);
            });
        }
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

    /**
     * 获取吸附计算器
     */
    public SnapCalculator getSnapCalculator() {
        return snapCalculator;
    }

    // 背景颜色

    public Color getBackgroundColor() {
        return renderer.getBackgroundColor();
    }

    public void setBackgroundColor(Color color) {
        renderer.setBackgroundColor(color);
        invalidateBackground();
        redraw();
    }

    /**
     * 设置是否显示吸附辅助线
     */
    public void setShowSnapGuideLines(boolean show) {
        renderer.setShowSnapGuideLines(show);
    }

    // 点击涟漪

    /**
     * 在指定屏幕坐标生成点击涟漪(由 RippleHandler 调用)
     *
     * @param screenX 屏幕 X 坐标(相对画布)
     * @param screenY 屏幕 Y 坐标(相对画布)
     */
    public void spawnClickRipple(double screenX, double screenY) {
        clickRippleEffect.spawn(screenX, screenY);
    }

    // 视口控制(委托给 ViewportController)

    /**
     * 停止正在进行的缩放动画
     */
    public void stopZoomAnimation() {
        viewportController.stopZoomAnimation();
    }

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

    /**
     * 获取默认光标
     */
    public Cursor getDefaultCursor() {
        return cursor.getDefaultCursor();
    }
}
