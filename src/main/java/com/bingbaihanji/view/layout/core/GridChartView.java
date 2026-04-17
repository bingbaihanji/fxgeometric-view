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
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * @author bingbaihanji
 * @date 2025-12-20 16:12:42
 * @description 格点图视图
 */
public class GridChartView extends Pane {


    //  基础组件
    private final Canvas backgroundCanvas = new Canvas();
    private final Canvas objectCanvas = new Canvas();
    private final Canvas interactionCanvas = new Canvas();
    //  坐标系统
    private final WorldTransform transform = new WorldTransform();
    private final List<WorldPainter> painters = new ArrayList<>();
    private final List<WorldObject> objects;  // 改为非final,支持共享列表
    private final CircleDrawingTool circleTool;
    //  视图配置
    private final EuclidianViewSettings settings = new EuclidianViewSettings();
    // 对象变化监听器列表(观察者模式)
    private final List<Runnable> objectChangeListeners = new ArrayList<>();
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
    private final SnapCalculator snapCalculator;
    private final HoverTooltipManager tooltipManager;

    /**
     * 是否显示吸附辅助线(默认false,只在绘制模式下显示)
     */
    private boolean showSnapGuideLines = false;
    // 画布背景颜色
    private Color backgroundColor = Color.WHITE;
    //  视图拖拽状态
    private boolean panning = false;
    private double lastMouseX;
    private double lastMouseY;
    // 自定义默认光标
    private Cursor customDefaultCursor = null;

    //  构造
    public GridChartView() {
        this(new ArrayList<>());  // 默认创建新的列表
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

        getChildren().addAll(backgroundCanvas, objectCanvas, interactionCanvas);
        bindSize(); // 尺寸绑定
        initMouseZoom();// 鼠标滚轮缩放
        initMousePan(); // 鼠标滚轮键按下时开始拖拽
        redraw(); // 绘制 格点/网格
        initMouseClickOutput(); // 点击控制台显示坐标
        initMouseObjectHover(); // 鼠标吸附

        // 使用配置初始化 Painters
        addPainter(new GridPainter(settings));
        addPainter(new AxesPainter(true, settings));

        // 同步配置到 transform
        applySettingsToTransform();

        setCustomCursorForPane(this, "/icon/mouseStyle.png");

        tooltipManager = new HoverTooltipManager(this, snapCalculator, transform, () -> objects, this::redrawInteraction);
        tooltipManager.install(this);
    }

    public List<WorldPainter> getPainters() {
        return painters;
    }

    public Runnable getOnTransformChanged() {
        return onTransformChanged;
    }


    //  初始化

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
            transform.centerWorldAt(
                    0,
                    0,
                    getWidth(),
                    getHeight()
            );
            initialized = true;
            redraw();
        }
    }

    /**
     * 初始化画布尺寸绑定和监听器
     */
    private void bindSize() {
        backgroundCanvas.widthProperty().bind(widthProperty());
        backgroundCanvas.heightProperty().bind(heightProperty());
        objectCanvas.widthProperty().bind(widthProperty());
        objectCanvas.heightProperty().bind(heightProperty());
        interactionCanvas.widthProperty().bind(widthProperty());
        interactionCanvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        });
        heightProperty().addListener((obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        });
    }


    //  缩放

    /**
     * 鼠标滚轮缩放
     */
    private void initMouseZoom() {
        setOnScroll(this::handleZoom);
    }

    /**
     * 鼠标滚轮键按下时开始拖拽
     */
    private void initMousePan() {

        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {

            tooltipManager.cancel();

            if (e.isMiddleButtonDown()) {
                panning = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {

            tooltipManager.cancel();

            if (!panning) return;

            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            transform.setOffset(
                    transform.getOffsetX() + dx,
                    transform.getOffsetY() + dy
            );

            lastMouseX = e.getX();
            lastMouseY = e.getY();

            redraw();
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            panning = false;
        });
    }

    private void handleZoom(ScrollEvent e) {

        tooltipManager.cancel();

        double newScale = transform.getScale();

        if (e.getDeltaY() > 0) {
            newScale *= 1.1;
        } else {
            newScale *= 0.9;
        }

        newScale = clamp(newScale);

        double mouseX = e.getX();
        double mouseY = e.getY();

        double worldX = transform.screenToWorldX(mouseX);
        double worldY = transform.screenToWorldY(mouseY);

        transform.setScale(newScale);

        double newOffsetX = mouseX - worldX * newScale;
        double newOffsetY = mouseY + worldY * newScale;

        transform.setOffset(newOffsetX, newOffsetY);

        redraw();
    }

    /**
     * 将给定的数值限制在指定的闭区间内。
     * <p>
     * 如果 {@code v} 小于 {@code min}，则返回 {@code min}；
     * 如果 {@code v} 大于 {@code max}，则返回 {@code max}；
     * 否则返回 {@code v} 本身。
     * <p>
     * 此方法常用于数值稳定化或边界约束处理。
     *
     * @param v 待限制的输入值
     * @return 限制后的值，确保 {@code min <= result <= max}
     */
    private double clamp(double v) {
        return Math.max(5.0E-5, Math.min(5.0E+5, v));
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
     * 保证窗口尺寸变化时,视图中心的世界点不发生跳变
     */
    private void updateOffsetFromCenterWorld() {
        double centerWorldX = transform.screenToWorldX(getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(getHeight() / 2);

        transform.centerWorldAt(
                centerWorldX,
                centerWorldY,
                getWidth(),
                getHeight()
        );
    }


    //  坐标轴 绘制

    /**
     * 重绘整个画布
     * <p>
     * 清空画布后,依次调用所有已注册的 WorldPainter 进行绘制
     */
    public void redraw() {
        redrawBackground();
        redrawObjects();
        redrawInteraction();
    }

    /**
     * 仅重绘背景层（网格 + 坐标轴）
     */
    private void redrawBackground() {
        GraphicsContext gc = backgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, backgroundCanvas.getWidth(), backgroundCanvas.getHeight());

        // 填充背景色
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, backgroundCanvas.getWidth(), backgroundCanvas.getHeight());

        double w = backgroundCanvas.getWidth();
        double h = backgroundCanvas.getHeight();

        for (WorldPainter painter : painters) {
            painter.paint(gc, transform, w, h);
        }
    }

    /**
     * 仅重绘对象层
     */
    private void redrawObjects() {
        GraphicsContext gc = objectCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, objectCanvas.getWidth(), objectCanvas.getHeight());

        double w = objectCanvas.getWidth();
        double h = objectCanvas.getHeight();

        for (WorldObject obj : objects) {
            obj.paint(gc, transform, w, h);
        }
    }

    /**
     * 仅重绘交互层（预览、吸附提示、BoundingBox）
     */
    private void redrawInteraction() {
        GraphicsContext gc = interactionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, interactionCanvas.getWidth(), interactionCanvas.getHeight());

        // 绘制 BoundingBox(通过回调函数绘制)
        if (boundingBoxPainter != null) {
            boundingBoxPainter.accept(gc, transform);
        }

        // 绘制预览图形
        if (previewPainter != null) {
            previewPainter.accept(gc, transform);
        }

        // 交互预览
        if (circleTool != null) {
            // 现在CircleDrawingTool可以正确地与DrawingController协同工作
            // 预览绘制逻辑已经修复,可以正常显示圆形预览
            circleTool.paintPreview(gc, transform);
        }

        // 绘制特殊点吸附提示
        if (nearbySpecialPoint != null) {
            drawSpecialPointHint(gc);
        }

        // 绘制轴向吸附辅助线
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

        // 绘制吸附提示圈
        gc.setStroke(Color.rgb(255, 165, 0, 0.8)); // 橙色半透明
        gc.setLineWidth(2);
        gc.setLineDashes(null);

        // 绘制两个同心圆作为吸附提示
        double radius1 = 8;
        double radius2 = 12;
        gc.strokeOval(sx - radius1, sy - radius1, radius1 * 2, radius1 * 2);
        gc.strokeOval(sx - radius2, sy - radius2, radius2 * 2, radius2 * 2);

        // 绘制中心点
        gc.setFill(Color.rgb(255, 165, 0, 0.6));
        double centerRadius = 3;
        gc.fillOval(sx - centerRadius, sy - centerRadius, centerRadius * 2, centerRadius * 2);
    }

    /**
     * 初始化鼠标点击输出与世界对象交互
     * <p>
     * 点击优先命中 WorldObject；
     * 若未命中任何对象,则输出点击点的世界坐标
     */
    private void initMouseClickOutput() {

        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

            // 屏幕 → 世界
            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());

            // 应用统一的吸附系统
            SnapCalculator.SnapResult snapped = snapCalculator.calculate(worldX, worldY, objects);
            nearbySpecialPoint = snapped.snappedPoint;
            axisSnapInfo = snapped.axisSnapInfo;
            worldX = snapped.x;
            worldY = snapped.y;

            // 优先命中对象
            double tolerance = GeometryConfig.Tolerance.HIT_TEST_PIXELS / transform.getScale();
            for (int i = objects.size() - 1; i >= 0; i--) {
                WorldObject obj = objects.get(i);

                if (obj.hitTest(worldX, worldY, tolerance)) {
                    obj.onClick(worldX, worldY);
                    redraw();
                    return;
                }
            }

            System.out.printf(
                    "point(x = %.2f, y = %.2f)%n",
                    worldX, worldY
            );
        });
    }

    // 吸附
    private void initMouseObjectHover() {

        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {

            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());

            // 应用统一的吸附系统(更新吸附状态用于视觉反馈)
            SnapCalculator.SnapResult snapped = snapCalculator.calculate(worldX, worldY, objects);
            nearbySpecialPoint = snapped.snappedPoint;
            axisSnapInfo = snapped.axisSnapInfo;
            worldX = snapped.x;
            worldY = snapped.y;

            double tolerance = GeometryConfig.Tolerance.HIT_TEST_PIXELS / transform.getScale();

            WorldObject newHover = null;

            // 从上往下命中
            for (int i = objects.size() - 1; i >= 0; i--) {
                WorldObject obj = objects.get(i);
                if (obj.hitTest(worldX, worldY, tolerance)) {
                    newHover = obj;
                    break;
                }
            }

            // hover 发生变化才更新
            if (newHover != hoverObject) {

                if (hoverObject != null) {
                    hoverObject.setHover(false);
                }

                hoverObject = newHover;

                if (hoverObject != null) {
                    hoverObject.setHover(true);
                }

                redrawObjects();
                redrawInteraction();
            } else if (nearbySpecialPoint != null || axisSnapInfo != null) {
                // 即使hover对象没变,如果有吸附提示,也需要重绘交互层
                redrawInteraction();
            }
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {

            if (hoverObject != null) {
                hoverObject.setHover(false);
                hoverObject = null;
                redrawObjects();
                redrawInteraction();
            }

            // 清除吸附提示
            if (nearbySpecialPoint != null || axisSnapInfo != null) {
                nearbySpecialPoint = null;
                axisSnapInfo = null;
                redrawInteraction();
            }
        });
    }

    /**
     * 获取当前缩放比例
     *
     * @return 当前缩放比例(世界单位到像素的比例)
     */
    public double getScale() {
        return transform.getScale();
    }

    /**
     * 获取世界坐标变换对象
     *
     * @return 世界坐标变换对象
     */
    public WorldTransform getTransform() {
        return transform;
    }

    public void addPainter(WorldPainter painter) {
        painters.add(painter);
        redraw();
    }

    public void removePainter(WorldPainter painter) {
        painters.remove(painter);
        redraw();
    }

    /**
     * 获取画布背景颜色
     *
     * @return 背景颜色
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * 设置画布背景颜色
     *
     * @param color 背景颜色
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        redraw();
    }

    /**
     * 设置是否显示吸附辅助线
     * <p>
     * 在绘制几何图形时可以显示辅助线,在选择/拖动模式下应隐藏
     *
     * @param show 是否显示辅助线
     */
    public void setShowSnapGuideLines(boolean show) {
        this.showSnapGuideLines = show;
    }

    /**
     * 添加对象变化监听器(观察者模式)
     *
     * @param listener 监听器回调
     */
    public void addObjectChangeListener(Runnable listener) {
        objectChangeListeners.add(listener);
    }

    /**
     * 移除对象变化监听器
     *
     * @param listener 监听器回调
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

    public void addObject(WorldObject obj) {
        objects.add(obj);
        updateAxisUnitTypeBasedOnFunctions();  // 自动检测三角函数
        notifyObjectChange();
        redraw();
    }

    public void removeObject(WorldObject obj) {
        objects.remove(obj);
        updateAxisUnitTypeBasedOnFunctions();  // 自动检测三角函数
        notifyObjectChange();
        redraw();
    }

    /**
     * 根据当前对象列表中是否有三角函数来自动调整坐标轴单位类型
     * 如果有三角函数：自动设置为π单位(X轴显示π,Y轴显示数值)
     * 如果没有三角函数：恢复为普通数值单位
     */
    private void updateAxisUnitTypeBasedOnFunctions() {
        boolean hasTrigFunction = objects.stream()
                .anyMatch(obj -> obj instanceof TrigonometricFunctionGeo);

        if (hasTrigFunction) {
            // 有三角函数：设置为π单位模式
            settings.setUnitLabelType(UnitLabelType.PI);
        } else {
            // 没有三角函数：恢复为普通数值模式
            settings.setUnitLabelType(UnitLabelType.NUMERIC);
        }
    }

    /**
     * 清除所有图形对象
     */
    public void clearAllObjects() {
        objects.clear();
        updateAxisUnitTypeBasedOnFunctions();  // 清空后恢复默认坐标轴
        redraw();
    }

    /**
     * 获取所有图形对象的副本
     *
     * @return 图形对象列表的副本
     */
    public List<WorldObject> getObjects() {
        return new ArrayList<>(objects);
    }

    /**
     * 设置预览绘制器
     */
    public void setPreviewPainter(BiConsumer<GraphicsContext, WorldTransform> previewPainter) {
        this.previewPainter = previewPainter;
    }

    /**
     * 设置 BoundingBox 绘制器
     */
    public void setBoundingBoxPainter(BiConsumer<GraphicsContext, WorldTransform> boundingBoxPainter) {
        this.boundingBoxPainter = boundingBoxPainter;
    }

    // 设置鼠标样式
    private void setCustomCursorForPane(Pane pane, String imagePath) {
        URL url = getClass().getResource(imagePath);
        if (url == null) {
            // 资源不存在,降级处理
            pane.setCursor(Cursor.HAND);
            customDefaultCursor = Cursor.HAND;
            return;
        }

        Image cursorImage = new Image(url.toExternalForm());

        // 左上角作为点击点(hotspot)
        Cursor customCursor = new ImageCursor(cursorImage, 0, 0);
        customDefaultCursor = customCursor;

        pane.setOnMouseEntered(e -> pane.setCursor(customCursor));
        pane.setOnMouseExited(e -> pane.setCursor(customCursor));
    }

    /**
     * 获取默认的自定义光标
     * 用于在DrawingController中恢复默认光标样式
     */
    public Cursor getDefaultCursor() {
        return customDefaultCursor != null ? customDefaultCursor : Cursor.DEFAULT;
    }

    //   视图配置和控制方法  

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
     * 缩放到指定百分比
     *
     * @param percent 百分比(如100表示100%)
     */
    public void zoomToPercent(double percent) {
        double newScale = com.bingbaihanji.util.AxisRangeCalculator.getScaleFromPercent(transform.getScale(), percent);

        // 保持中心点不变
        double centerWorldX = transform.screenToWorldX(getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(getHeight() / 2);

        transform.setScale(newScale);
        settings.setXScale(newScale);
        settings.setYScale(newScale);

        transform.centerWorldAt(centerWorldX, centerWorldY, getWidth(), getHeight());
        redraw();
    }

    /**
     * 设置坐标轴比例
     *
     * @param xRatio X轴比例系数
     * @param yRatio Y轴比例系数
     */
    public void setAxisRatio(double xRatio, double yRatio) {
        // 保持中心点不变
        double centerWorldX = transform.screenToWorldX(getWidth() / 2);
        double centerWorldY = transform.screenToWorldY(getHeight() / 2);

        transform.setAxisRatio(xRatio, yRatio);
        settings.setXScale(transform.getScaleX());
        settings.setYScale(transform.getScaleY());

        transform.centerWorldAtWithScales(centerWorldX, centerWorldY, getWidth(), getHeight());
        redraw();
    }

    /**
     * 显示所有对象(自动调整范围)
     */
    public void fitAllObjects() {
        if (objects.isEmpty()) {
            return;
        }

        double[] range = com.bingbaihanji.util.AxisRangeCalculator.fitAllObjects(
                objects, transform, getWidth(), getHeight()
        );

        // 计算所需的缩放比例
        double xRange = range[1] - range[0];
        double yRange = range[3] - range[2];

        double newScaleX = getWidth() / xRange;
        double newScaleY = getHeight() / yRange;

        // 使用较小的缩放比例,确保所有对象都可见
        double newScale = Math.min(newScaleX, newScaleY) * 0.9; // 留10%边距

        transform.setScale(newScale);
        settings.setXScale(newScale);
        settings.setYScale(newScale);

        // 居中显示
        double centerX = (range[0] + range[1]) / 2;
        double centerY = (range[2] + range[3]) / 2;

        transform.centerWorldAt(centerX, centerY, getWidth(), getHeight());
        redraw();
    }

    /**
     * 重置到标准视图(原点居中,比例1:1,缩放100%)
     */
    public void resetToStandardView() {
        double standardScale = 50.0; // 标准缩放(100%)

        transform.setScale(standardScale);
        settings.setXScale(standardScale);
        settings.setYScale(standardScale);

        transform.centerWorldAt(0, 0, getWidth(), getHeight());
        redraw();
    }

    /**
     * 绘制轴向吸附辅助线
     */
    private void drawAxisSnapGuideLine(GraphicsContext gc) {
        // 只在允许显示辅助线且有吸附信息时绘制
        if (!showSnapGuideLines || axisSnapInfo == null) {
            return;
        }

        // 设置辅助线样式
        gc.setStroke(Color.rgb(0, 150, 255, 0.6)); // 蓝色半透明
        gc.setLineWidth(1.5);
        gc.setLineDashes(5, 5);

        // 绘制辅助线
        gc.strokeLine(
                axisSnapInfo.lineStartX,
                axisSnapInfo.lineStartY,
                axisSnapInfo.lineEndX,
                axisSnapInfo.lineEndY
        );

        // 恢复默认样式
        gc.setLineDashes(null);
    }

}

