package com.bingbaihanji.view.menu;

import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 正多边形边数选择对话框
 * <p>
 * 允许用户选择正多边形的边数(3-10)
 *
 * @author bingbaihanji
 * @date 2026-01-10
 */
public class RegularPolygonSidesDialog extends Dialog<Integer> {

    private static final int MIN_SIDES = 3;
    private static final int MAX_SIDES = 10;
    private int currentSides;
    private Label numberLabel;

    /**
     * 构造函数
     *
     * @param defaultSides 默认边数(3-10)
     */
    public RegularPolygonSidesDialog(int defaultSides) {
        this.currentSides = Math.max(MIN_SIDES, Math.min(MAX_SIDES, defaultSides));

        setTitle(I18nUtil.getString("regularPolygon.dialog.title"));
        setHeaderText(I18nUtil.getString("regularPolygon.dialog.header"));

        // 设置对话框图标
        FxTools.setDialogIcon(this, "/icon/regularPolygons.png");

        // 创建主布局
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.CENTER);

        // 创建边数选择区
        HBox sidesSelector = createSidesSelector();
        sidesSelector.setAlignment(Pos.CENTER);

        // 创建范围提示标签
        Label rangeLabel = new Label(I18nUtil.getString("regularPolygon.dialog.range"));
        rangeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        mainContent.getChildren().addAll(sidesSelector, rangeLabel);

        getDialogPane().setContent(mainContent);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 设置最小宽度
        getDialogPane().setMinWidth(350);

        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return currentSides;
            }
            return null;
        });
    }

    /**
     * 创建边数选择器(- 按钮 + 数字标签 + + 按钮)
     */
    private HBox createSidesSelector() {
        HBox selector = new HBox(30);
        selector.setAlignment(Pos.CENTER);

        // 减少按钮
        Button decreaseBtn = new Button("-");

        // -fx-background-radius: 25px 配合宽高50px创建圆形
        decreaseBtn.setStyle(
                """
                        -fx-font-size: 24px;
                        -fx-font-weight: bold;
                        -fx-min-width: 50px;
                        -fx-min-height: 50px;
                        -fx-background-radius: 25px;
                        
                        """
        );

        installGlowEffect(decreaseBtn);
        decreaseBtn.setOnAction(e -> {
            if (currentSides > MIN_SIDES) {
                currentSides--;
                updateLabel();
            }
        });

        // 数字标签
        numberLabel = new Label(String.valueOf(currentSides));
        numberLabel.setStyle(
                """
                        -fx-font-size: 32px;
                        -fx-font-weight: bold;
                        -fx-min-width: 80px;
                        -fx-alignment: center;
                        """
        );

        // 增加按钮
        Button increaseBtn = new Button("+");
        increaseBtn.setStyle(
                """
                        -fx-font-size: 24px;
                        -fx-font-weight: bold;
                        -fx-min-width: 50px;
                        -fx-min-height: 50px;
                        -fx-background-radius: 25px;
                        """
        );

        installGlowEffect(increaseBtn);
        increaseBtn.setOnAction(e -> {
            if (currentSides < MAX_SIDES) {
                currentSides++;
                updateLabel();
            }
        });

        selector.getChildren().addAll(decreaseBtn, numberLabel, increaseBtn);
        return selector;
    }


    private void installGlowEffect(Button btn) {

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#4FA3FF"));
        glow.setRadius(0);
        glow.setSpread(0.15);
        glow.setOffsetX(0);
        glow.setOffsetY(0);

        btn.setEffect(glow);

        // Hover 进入：发光增强
        Timeline glowIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 0),
                        new KeyValue(glow.colorProperty(), Color.web("#4FA3FF", 0.0))
                ),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(glow.radiusProperty(), 14),
                        new KeyValue(glow.colorProperty(), Color.web("#4FA3FF", 0.85))
                )
        );

        // Hover 离开：发光消失
        Timeline glowOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), glow.getRadius()),
                        new KeyValue(glow.colorProperty(), glow.getColor())
                ),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(glow.radiusProperty(), 0),
                        new KeyValue(glow.colorProperty(), Color.web("#4FA3FF", 0.0))
                )
        );

        // 呼吸动画(Hover 停留)
        Timeline breathing = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(glow.radiusProperty(), 12)
                ),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(glow.radiusProperty(), 16)
                )
        );
        breathing.setAutoReverse(true);
        breathing.setCycleCount(Animation.INDEFINITE);

        // 绑定事件
        btn.setOnMouseEntered(e -> {
            glowOut.stop();
            glowIn.playFromStart();
            glowIn.setOnFinished(ev -> breathing.play());
        });

        btn.setOnMouseExited(e -> {
            breathing.stop();
            glowIn.stop();
            glowOut.playFromStart();
        });
    }


    /**
     * 更新数字标签显示
     */
    private void updateLabel() {
        numberLabel.setText(String.valueOf(currentSides));
    }
}
