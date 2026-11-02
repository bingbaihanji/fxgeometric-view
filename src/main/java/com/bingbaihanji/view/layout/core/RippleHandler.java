package com.bingbaihanji.view.layout.core;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * 点击涟漪处理器
 * <p>
 * 鼠标左键按下时在点击位置生成涟漪动效。
 * 纯视觉反馈，绘制模式下同样生效，与 {@link ClickHandler} 的语义分派互不影响。
 *
 * @author bingbaihanji
 * @date 2026-11-02
 */
public class RippleHandler {

    private final GridChartView view;

    public RippleHandler(GridChartView view) {
        this.view = view;
    }

    public void install() {
        view.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            view.spawnClickRipple(e.getX(), e.getY());
        });
    }
}
