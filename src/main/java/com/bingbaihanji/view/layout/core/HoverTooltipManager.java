package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.controller.SnapCalculator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Supplier;

/**
 * 悬停 Tooltip 管理器
 * <p>
 * 封装鼠标悬停坐标气泡的生命周期管理
 */
public class HoverTooltipManager {

    private static final double HOVER_MOVE_THRESHOLD = 3; // 像素

    private final Node host;
    private final SnapCalculator snapCalculator;
    private final WorldTransform transform;
    private final Supplier<List<WorldObject>> objectsSupplier;
    private final Runnable onNeedsRedraw;

    private final Tooltip hoverTooltip = new Tooltip();
    private final PauseTransition hoverTimer =
            new PauseTransition(Duration.seconds(0.5));

    private double lastHoverX;
    private double lastHoverY;

    public HoverTooltipManager(Node host,
                               SnapCalculator snapCalculator,
                               WorldTransform transform,
                               Supplier<List<WorldObject>> objectsSupplier,
                               Runnable onNeedsRedraw) {
        this.host = host;
        this.snapCalculator = snapCalculator;
        this.transform = transform;
        this.objectsSupplier = objectsSupplier;
        this.onNeedsRedraw = onNeedsRedraw;

        hoverTooltip.setAutoHide(true);
        hoverTooltip.setStyle("""
                -fx-font-size: 13px;
                -fx-background-color: rgba(255, 224, 178, 0.9);
                -fx-text-fill: #b055a5;
                -fx-background-radius: 6;
                -fx-padding: 6 8 6 8;
                """);
    }

    /**
     * 将鼠标监听注册到指定节点
     */
    public void install(Node node) {
        node.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        node.addEventHandler(MouseEvent.MOUSE_EXITED, this::onMouseExited);
    }

    /**
     * 立即取消计时器并隐藏 Tooltip
     */
    public void cancel() {
        hoverTimer.stop();
        hoverTooltip.hide();
    }

    private void onMouseMoved(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();

        double dx = x - lastHoverX;
        double dy = y - lastHoverY;

        if (Math.hypot(dx, dy) > HOVER_MOVE_THRESHOLD) {
            hoverTimer.stop();
            hoverTooltip.hide();

            lastHoverX = x;
            lastHoverY = y;

            hoverTimer.setOnFinished(ev -> showHoverTooltip(x, y));
            hoverTimer.playFromStart();
        }
    }

    private void onMouseExited(MouseEvent e) {
        hoverTimer.stop();
        hoverTooltip.hide();
    }

    private void showHoverTooltip(double screenX, double screenY) {
        double rawX = transform.screenToWorldX(screenX);
        double rawY = transform.screenToWorldY(screenY);

        SnapCalculator.SnapResult snap = snapCalculator.calculate(rawX, rawY, objectsSupplier.get());
        double x = snap.x;
        double y = snap.y;

        // 极坐标网格下显示极坐标 (r, θ°)，其他网格显示笛卡尔坐标 (x, y)
        String text;
        EuclidianViewSettings settings = snapCalculator.getSettings();
        if (settings.getGridType() == GridType.POLAR) {
            double r = Math.sqrt(x * x + y * y);
            double thetaDeg = Math.toDegrees(Math.atan2(y, x));
            thetaDeg = Math.round(thetaDeg * 10) / 10.0; // 消除浮点精度噪声
            text = String.format("(r=%.2f, θ=%.1f°)", r, thetaDeg);
        } else {
            text = String.format("(%.2f, %.2f)", x, y);
        }
        hoverTooltip.setText(text);

        hoverTooltip.show(
                host,
                host.localToScreen(screenX + 12, screenY + 24).getX(),
                host.localToScreen(screenX + 12, screenY + 24).getY()
        );

        // 触发重绘以显示吸附提示
        if (onNeedsRedraw != null) {
            onNeedsRedraw.run();
        }
    }
}
