package com.binbaihanji.view.layout.core;

import com.binbaihanji.constant.GridMode;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import com.binbaihanji.view.layout.draw.geometry.WorldPainter;
import com.binbaihanji.view.layout.draw.geometry.impl.AxesPainter;
import com.binbaihanji.view.layout.draw.geometry.impl.GridPainter;
import com.binbaihanji.view.layout.draw.tools.CircleDrawingTool;
import javafx.animation.PauseTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * @author bingbaihanji
 * @date 2025-12-20 16:12:42
 * @description 格点图视图
 */
public class GridChartPane extends Pane {


    //  基础组件

    private final Canvas canvas = new Canvas();

    //  坐标系统
    private final WorldTransform transform = new WorldTransform();


    private final List<WorldPainter> painters = new ArrayList<>();

    private final List<WorldObject> objects = new ArrayList<>();
    private WorldObject hoverObject = null;

    private CircleDrawingTool circleTool;

    // 预览绘制器
    private BiConsumer<GraphicsContext, WorldTransform> previewPainter;


    /**
     * 初始化状态标志
     * <p>
     * 用于跟踪组件是否已完成首次坐标初始化
     * 确保layoutChildren()中的初始化逻辑只执行一次
     * 避免每次布局时都重置用户可能已经调整过的原点位置
     */
    private boolean initialized = false;


    //   鼠标悬停气泡
    private final Tooltip hoverTooltip = new Tooltip();

    private final PauseTransition hoverTimer =
            new PauseTransition(Duration.seconds(0.5));

    private double lastHoverX;
    private double lastHoverY;

    // 判定
    private static final double HOVER_MOVE_THRESHOLD = 3; // 像素

    private Runnable onTransformChanged;

    /**
     * 初始化鼠标悬停坐标气泡
     * <p>
     * 鼠标在同一位置停留超过 2 秒时，显示该点的世界坐标
     */
    private void initMouseHoverTooltip() {

        hoverTooltip.setAutoHide(true);
        hoverTooltip.setStyle("""
                -fx-font-size: 13px;
                -fx-background-color: rgba(255, 224, 178, 0.9);
                -fx-text-fill: #b055a5;
                -fx-background-radius: 6;
                -fx-padding: 6 8 6 8;
                """);

        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {

            double x = e.getX();
            double y = e.getY();

            double dx = x - lastHoverX;
            double dy = y - lastHoverY;

            if (Math.hypot(dx, dy) > HOVER_MOVE_THRESHOLD) {

                hoverTimer.stop();
                hoverTooltip.hide();

                lastHoverX = x;
                lastHoverY = y;

                hoverTimer.setOnFinished(ev ->
                        showHoverTooltip(x, y)
                );
                hoverTimer.playFromStart();
            }
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hoverTimer.stop();
            hoverTooltip.hide();
        });
    }


    /**
     * 显示悬停点的坐标气泡
     */
    private void showHoverTooltip(double screenX, double screenY) {

        double rawX = screenToWorldX(screenX);
        double rawY = screenToWorldY(screenY);

        double step = chooseAxisStep();

        double x = snapToInteger(rawX);
        x = snapToGrid(x, step);
        x = stabilize(x);

        double y = snapToInteger(rawY);
        y = snapToGrid(y, step);
        y = stabilize(y);

        String text = String.format("(%.2f, %.2f)", x, y);
        hoverTooltip.setText(text);

        hoverTooltip.show(
                this,
                localToScreen(screenX + 12, screenY + 12).getX(),
                localToScreen(screenX + 12, screenY + 12).getY()
        );
    }

    public Runnable getOnTransformChanged() {
        return onTransformChanged;
    }


    //  视图拖拽状态
    private boolean panning = false;
    private double lastMouseX;
    private double lastMouseY;

    //  构造
    public GridChartPane() {
        circleTool = new CircleDrawingTool();

        getChildren().add(canvas);
        bindSize(); // 尺寸绑定
        initMouseZoom();// 鼠标滚轮缩放
        initMousePan(); // 鼠标滚轮键按下时开始拖拽
        redraw(); // 绘制 格点/网格
        initMouseClickOutput(); // 点击控制台显示坐标
        initMouseHoverTooltip(); // 悬浮气泡显示坐标
        initMouseObjectHover();
        addPainter(new GridPainter(GridMode.DOT));
        addPainter(new AxesPainter());
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


    //  初始化

    /**
     * 初始化画布尺寸绑定和监听器
     */
    private void bindSize() {
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        });
        heightProperty().addListener((obs, o, n) -> {
            updateOffsetFromCenterWorld();
            redraw();
        });
    }

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

            cancelHoverTooltip();

            if (e.isMiddleButtonDown()) {
                panning = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {

            cancelHoverTooltip();

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


    //  缩放

    private void handleZoom(ScrollEvent e) {

        cancelHoverTooltip();

        double oldScale = transform.getScale();
        double newScale = oldScale;

        if (e.getDeltaY() > 0) {
            newScale *= 1.1;
        } else {
            newScale *= 0.9;
        }

        newScale = clamp(newScale, 5, 500);

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

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
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
     * 保证窗口尺寸变化时，视图中心的世界点不发生跳变
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


    /**
     * 重绘整个画布
     * <p>
     * 清空画布后，依次调用所有已注册的 WorldPainter 进行绘制
     */
    public void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double w = canvas.getWidth();
        double h = canvas.getHeight();

        for (WorldPainter painter : painters) {
            painter.paint(gc, transform, w, h);
        }

        for (WorldObject obj : objects) {
            obj.paint(gc, transform, w, h);
        }

        // 绘制预览图形
        if (previewPainter != null) {
            previewPainter.accept(gc, transform);
        }

        // ：交互预览
        if (circleTool != null) {
            circleTool.paintPreview(gc, transform);
        }
    }


    //  坐标轴 绘制

    /**
     * 根据当前缩放比例选择合适的坐标轴刻度步长
     *
     * @return 坐标轴刻度步长（世界单位）
     */
    private double chooseAxisStep() {
        double scale = transform.getScale();
        if (scale > 200) return 0.5;
        if (scale > 100) return 1;
        if (scale > 50) return 2;
        if (scale > 25) return 5;
        return 10;
    }


    /**
     * 初始化鼠标点击输出与世界对象交互
     * <p>
     * 点击优先命中 WorldObject；
     * 若未命中任何对象，则输出点击点的世界坐标
     */
    private void initMouseClickOutput() {

        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

            // 屏幕 → 世界
            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());

            double tolerance = 5 / transform.getScale();

            // 优先命中对象
            for (int i = objects.size() - 1; i >= 0; i--) {
                WorldObject obj = objects.get(i);

                if (obj.hitTest(worldX, worldY, tolerance)) {
                    obj.onClick(worldX, worldY);
                    redraw();
                    return;
                }
            }

            // 否则输出世界坐标
            double step = chooseAxisStep();

            double x = stabilize(
                    snapToGrid(
                            snapToInteger(worldX),
                            step
                    )
            );

            double y = stabilize(
                    snapToGrid(
                            snapToInteger(worldY),
                            step
                    )
            );

            System.out.printf(
                    "point(x = %.2f, y = %.2f)%n",
                    x, y
            );
        });
    }


    private void initMouseObjectHover() {

        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {

            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());

            double tolerance = 5 / transform.getScale();

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

                redraw();
            }
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {

            if (hoverObject != null) {
                hoverObject.setHover(false);
                hoverObject = null;
                redraw();
            }
        });
    }


    /**
     * 消除浮点抖动
     * <p>
     * 将接近整数的浮点数修正为整数，避免浮点运算误差
     *
     * @param v 待稳定化的数值
     * @return 稳定化后的数值
     */
    private double stabilize(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) {
            return Math.round(v);
        }
        return v;
    }

    /**
     * 吸附到最近整数点（优先级最高）
     * <p>
     * 当世界坐标值接近整数时，自动吸附到该整数点
     *
     * @param worldValue 世界坐标值
     * @return 吸附后的坐标值
     */
    private double snapToInteger(double worldValue) {
        double scale = transform.getScale();
        double nearest = Math.round(worldValue);
        double pixelDistance = Math.abs(worldValue - nearest) * scale;

        // 整数磁力更强： 5像素
        if (pixelDistance < 5) {
            return nearest;
        }
        return worldValue;
    }


    /**
     * 吸附到最近网格点（基于当前轴刻度）
     * <p>
     * 当世界坐标值接近网格点时，自动吸附到该网格点
     *
     * @param worldValue 世界坐标值
     * @param step       网格步长
     * @return 吸附后的坐标值
     */
    private double snapToGrid(double worldValue, double step) {
        double scale = transform.getScale();
        double snapped = Math.round(worldValue / step) * step;

        double pixelDistance = Math.abs(worldValue - snapped) * scale;

        if (pixelDistance < 5) {
            return snapped;
        }
        return worldValue;
    }


    //  对外接口

    /**
     * 立即取消悬停气泡显示
     */
    private void cancelHoverTooltip() {
        hoverTimer.stop();
        hoverTooltip.hide();
    }


    // 回掉接口
    public void setOnTransformChanged(Runnable runable) {
        this.onTransformChanged = runable;
    }


    /**
     * 获取当前缩放比例
     *
     * @return 当前缩放比例（世界单位到像素的比例）
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


    public void addObject(WorldObject obj) {
        objects.add(obj);
        redraw();
    }

    public void removeObject(WorldObject obj) {
        objects.remove(obj);
        redraw();
    }

    /**
     * 清除所有图形对象
     */
    public void clearAllObjects() {
        objects.clear();
        redraw();
    }

    /**
     * 设置预览绘制器
     */
    public void setPreviewPainter(BiConsumer<GraphicsContext, WorldTransform> previewPainter) {
        this.previewPainter = previewPainter;
    }
}