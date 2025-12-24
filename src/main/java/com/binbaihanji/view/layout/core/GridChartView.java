package com.binbaihanji.view.layout.core;

import com.binbaihanji.constant.GridMode;
import com.binbaihanji.util.SpecialPointManager;
import com.binbaihanji.util.SpecialPointManager.SpecialPoint;
import com.binbaihanji.view.layout.draw.geometry.WorldObject;
import com.binbaihanji.view.layout.draw.geometry.WorldPainter;
import com.binbaihanji.view.layout.draw.geometry.impl.AxesPainter;
import com.binbaihanji.view.layout.draw.geometry.impl.GridPainter;
import com.binbaihanji.view.layout.draw.tools.CircleDrawingTool;
import javafx.animation.PauseTransition;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

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


    // 判定
    private static final double HOVER_MOVE_THRESHOLD = 3; // 像素
    //  基础组件
    private final Canvas canvas = new Canvas();
    //  坐标系统
    private final WorldTransform transform = new WorldTransform();
    private final List<WorldPainter> painters = new ArrayList<>();
    private final List<WorldObject> objects = new ArrayList<>();
    private final CircleDrawingTool circleTool;
    //   鼠标悬停气泡
    private final Tooltip hoverTooltip = new Tooltip();
    private final PauseTransition hoverTimer =
            new PauseTransition(Duration.seconds(0.5));
    private WorldObject hoverObject = null;
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
    private double lastHoverX;
    private double lastHoverY;
    // 当前鼠标附近的特殊点（用于视觉反馈）
    private SpecialPoint nearbySpecialPoint = null;
    private Runnable onTransformChanged;
    //  视图拖拽状态
    private boolean panning = false;
    private double lastMouseX;
    private double lastMouseY;


    //  构造
    public GridChartView() {
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
        addPainter(new AxesPainter(true));
        setCustomCursorForPane(this, "/icon/mouseStyle.png");
    }

    public List<WorldPainter> getPainters() {
        return painters;
    }

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

        // 应用磁性吸附效果
        SpecialPoint nearestSpecialPoint = findNearestSpecialPoint(rawX, rawY);
        double x, y;
        if (nearestSpecialPoint != null) {
            x = nearestSpecialPoint.getX();
            y = nearestSpecialPoint.getY();
        } else {
            // 如果没有特殊点吸附，则应用原有的吸附逻辑
            double step = chooseAxisStep();

            x = snapToInteger(rawX);
            x = snapToGrid(x, step);
            x = stabilize(x);

            y = snapToInteger(rawY);
            y = snapToGrid(y, step);
            y = stabilize(y);
        }

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

    // 回掉接口
    public void setOnTransformChanged(Runnable runable) {
        this.onTransformChanged = runable;
    }


    //  初始化

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


    //  缩放

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

    private void handleZoom(ScrollEvent e) {

        cancelHoverTooltip();

        double newScale = transform.getScale();

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
            // 现在CircleDrawingTool可以正确地与DrawingController协同工作
            // 预览绘制逻辑已经修复，可以正常显示圆形预览
            circleTool.paintPreview(gc, transform);
        }

        // 绘制特殊点吸附提示
        if (nearbySpecialPoint != null) {
            drawSpecialPointHint(gc);
        }
    }


    //  坐标轴 绘制

    /**
     * 绘制特殊点吸附提示（高亮圈）
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

            // 应用磁性吸附效果
            SpecialPoint nearestSpecialPoint = findNearestSpecialPoint(worldX, worldY);
            if (nearestSpecialPoint != null) {
                worldX = nearestSpecialPoint.getX();
                worldY = nearestSpecialPoint.getY();
            } else {
                // 如果没有特殊点吸附，则应用原有的吸附逻辑
                double step = chooseAxisStep();

                worldX = stabilize(
                        snapToGrid(
                                snapToInteger(worldX),
                                step
                        )
                );

                worldY = stabilize(
                        snapToGrid(
                                snapToInteger(worldY),
                                step
                        )
                );
            }

            // 优先命中对象
            double tolerance = 5 / transform.getScale();
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

    private void initMouseObjectHover() {

        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {

            double worldX = screenToWorldX(e.getX());
            double worldY = screenToWorldY(e.getY());

            // 应用磁性吸附效果，并更新附近的特殊点用于视觉反馈
            SpecialPoint nearestSpecialPoint = findNearestSpecialPoint(worldX, worldY);
            nearbySpecialPoint = nearestSpecialPoint; // 保存用于绘制提示

            if (nearestSpecialPoint != null) {
                worldX = nearestSpecialPoint.getX();
                worldY = nearestSpecialPoint.getY();
            }

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
            } else if (nearbySpecialPoint != null) {
                // 即使hover对象没变，如果附近有特殊点，也需要重绘以显示提示
                redraw();
            }
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {

            if (hoverObject != null) {
                hoverObject.setHover(false);
                hoverObject = null;
                redraw();
            }

            // 清除特殊点提示
            if (nearbySpecialPoint != null) {
                nearbySpecialPoint = null;
                redraw();
            }
        });
    }

    /**
     * 查找最近的特殊点（用于磁性吸附）
     *
     * @param x 当前鼠标x坐标（世界坐标）
     * @param y 当前鼠标y坐标（世界坐标）
     * @return 最近的特殊点，如果没有找到则返回null
     */
    private SpecialPoint findNearestSpecialPoint(double x, double y) {
        // 获取所有特殊点
        List<SpecialPoint> specialPoints = SpecialPointManager.extractSpecialPoints(objects);

        // 计算吸附阈值（像素距离转换为世界坐标距离）
        double scale = transform.getScale();
        double threshold = 10.0 / scale; // 10像素的吸附范围

        // 查找最近的特殊点
        return SpecialPointManager.findNearestSpecialPoint(x, y, specialPoints, threshold);
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


    //  对外接口

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

    /**
     * 立即取消悬停气泡显示
     */
    private void cancelHoverTooltip() {
        hoverTimer.stop();
        hoverTooltip.hide();
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


    // 设置鼠标样式
    private void setCustomCursorForPane(Pane pane, String imagePath) {
        URL url = getClass().getResource(imagePath);
        if (url == null) {
            // 资源不存在，降级处理
            pane.setCursor(Cursor.HAND);
            return;
        }

        Image cursorImage = new Image(url.toExternalForm());

        // 左上角作为点击点（hotspot）
        Cursor customCursor = new ImageCursor(cursorImage, 0, 0);

        pane.setOnMouseEntered(e -> pane.setCursor(customCursor));
        pane.setOnMouseExited(e -> pane.setCursor(Cursor.DEFAULT));
    }

}
