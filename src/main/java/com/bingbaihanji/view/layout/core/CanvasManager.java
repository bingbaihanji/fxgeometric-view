package com.bingbaihanji.view.layout.core;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

/**
 * 画布管理器
 * <p>
 * 负责三层 Canvas（背景层、对象层、交互层）的生命周期管理
 */
public class CanvasManager {

    // 背景层
    private final Canvas backgroundCanvas = new Canvas();
    // 对象层
    private final Canvas objectCanvas = new Canvas();
    // 交互层
    private final Canvas interactionCanvas = new Canvas();

    public void bindToPane(Pane pane) {
        pane.getChildren().addAll(backgroundCanvas, objectCanvas, interactionCanvas);
    }

    public GraphicsContext getBackgroundGC() {
        return backgroundCanvas.getGraphicsContext2D();
    }

    public GraphicsContext getObjectGC() {
        return objectCanvas.getGraphicsContext2D();
    }

    public GraphicsContext getInteractionGC() {
        return interactionCanvas.getGraphicsContext2D();
    }

    public void clearBackground() {
        GraphicsContext gc = backgroundCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, backgroundCanvas.getWidth(), backgroundCanvas.getHeight());
    }

    public void clearObjects() {
        GraphicsContext gc = objectCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, objectCanvas.getWidth(), objectCanvas.getHeight());
    }

    public void clearInteraction() {
        GraphicsContext gc = interactionCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, interactionCanvas.getWidth(), interactionCanvas.getHeight());
    }

    public void bindSize(Pane pane) {
        backgroundCanvas.widthProperty().bind(pane.widthProperty());
        backgroundCanvas.heightProperty().bind(pane.heightProperty());
        objectCanvas.widthProperty().bind(pane.widthProperty());
        objectCanvas.heightProperty().bind(pane.heightProperty());
        interactionCanvas.widthProperty().bind(pane.widthProperty());
        interactionCanvas.heightProperty().bind(pane.heightProperty());
    }

    public double getWidth() {
        return backgroundCanvas.getWidth();
    }

    public double getHeight() {
        return backgroundCanvas.getHeight();
    }
}
