package com.binbaihanji.view.layout.pane;

import com.binbaihanji.constant.DrawMode;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 图形工具面板
 * <p>
 * 左侧工具栏，用于选择绘制模式和设置多边形参数
 *
 * @author bingbaihanji
 * @date 2025-12-21
 */
public class ShapeToolPane extends VBox {

    private final ObjectProperty<DrawMode> drawMode =
            new SimpleObjectProperty<>(DrawMode.NONE);

    /**
     * 多边形边数属性
     */
    private final IntegerProperty polygonSides =
            new SimpleIntegerProperty(5);

    private ToggleButton polygonButton;

    public ShapeToolPane() {

        setSpacing(10);
        setPadding(new Insets(12));
        setPrefWidth(160);
        setStyle("""
                -fx-background-color: #f4f4f4;
                -fx-border-color: #d0d0d0;
                -fx-border-width: 0 1 0 0;
                """);

        ToggleGroup group = new ToggleGroup();

        // 基础工具按钮
        getChildren().addAll(
                createTool("点", DrawMode.POINT, group),
                createTool("线段", DrawMode.LINE, group),
                createTool("圆", DrawMode.CIRCLE, group),
                createTool("三角形", DrawMode.TRIANGLE, group),
                createTool("矩形", DrawMode.RECTANGLE, group),
                createTool("手绘线", DrawMode.FREEHAND, group)
        );

        // 分隔线
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        getChildren().add(separator);

        // 多边形工具（带参数）
        polygonButton = createTool("多边形", DrawMode.POLYGON, group);
        getChildren().add(polygonButton);

        // 多边形边数设置
        Label sidesLabel = new Label("边数:");
        sidesLabel.setStyle("-fx-font-size: 12px;");

        Spinner<Integer> sidesSpinner = new Spinner<>(3, 20, 5);
        sidesSpinner.setPrefWidth(80);
        sidesSpinner.setEditable(true);

        // 绑定边数属性
        sidesSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                polygonSides.set(newVal);
            }
        });
        polygonSides.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(sidesSpinner.getValue())) {
                sidesSpinner.getValueFactory().setValue(newVal.intValue());
            }
        });

        HBox sidesBox = new HBox(8, sidesLabel, sidesSpinner);
        sidesBox.setAlignment(Pos.CENTER_LEFT);
        sidesBox.setPadding(new Insets(0, 0, 0, 8));
        getChildren().add(sidesBox);

        // 分隔线
        Separator separator2 = new Separator();
        separator2.setPadding(new Insets(10, 0, 5, 0));
        getChildren().add(separator2);

        // 清除按钮
        Button clearButton = new Button("清除全部");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
        getChildren().add(clearButton);

        // 清除按钮事件会在外部绑定
        clearButton.setOnAction(e -> onClearAll());
    }

    private ToggleButton createTool(
            String text,
            DrawMode mode,
            ToggleGroup group
    ) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(e -> {
            if (btn.isSelected()) {
                drawMode.set(mode);
            } else {
                drawMode.set(DrawMode.NONE);
            }
        });

        return btn;
    }

    /**
     * 清除按钮回调（由外部设置）
     */
    private Runnable clearAllCallback;

    public void setClearAllCallback(Runnable callback) {
        this.clearAllCallback = callback;
    }

    private void onClearAll() {
        if (clearAllCallback != null) {
            clearAllCallback.run();
        }
    }

    public ObjectProperty<DrawMode> drawModeProperty() {
        return drawMode;
    }

    public DrawMode getDrawMode() {
        return drawMode.get();
    }

    public IntegerProperty polygonSidesProperty() {
        return polygonSides;
    }

    public int getPolygonSides() {
        return polygonSides.get();
    }
}
