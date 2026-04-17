package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.HandDrawnParameters;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * 绘制设置对话框
 * <p>
 * 用于调节手绘线的平滑参数
 *
 * @author bingbaihanji
 * @date 2026-03-29
 */
public class DrawingSettingsDialog extends Dialog<DrawingSettingsDialog.SettingsResult> {

    // 滑块组件
    private Slider simplifyEpsilonSlider;
    private Slider smoothSegmentsSlider;
    private Slider tensionSlider;
    private Slider minPointDistanceSlider;
    private CheckBox enableSmoothingCheckBox;

    // 值标签
    private Label simplifyEpsilonValueLabel;
    private Label smoothSegmentsValueLabel;
    private Label tensionValueLabel;
    private Label minPointDistanceValueLabel;

    // 当前设置值
    private final double simplifyEpsilon;
    private final int smoothSegments;
    private final double tension;
    private final double minPointDistance;
    private final boolean enableSmoothing;

    public DrawingSettingsDialog(double simplifyEpsilon, int smoothSegments,
                                 double tension, double minPointDistance,
                                 boolean enableSmoothing) {
        this.simplifyEpsilon = simplifyEpsilon;
        this.smoothSegments = smoothSegments;
        this.tension = tension;
        this.minPointDistance = minPointDistance;
        this.enableSmoothing = enableSmoothing;

        initDialog();
    }

    private void initDialog() {
        // 设置对话框标题和模态
        setTitle(I18nUtil.getString("settings.drawing.title"));
        setHeaderText(I18nUtil.getString("settings.drawing.header"));
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        // 设置对话框图标
        FxTools.setDialogIcon(this, "/icon/setting.png");

        // 创建主面板
        VBox mainVBox = new VBox(15);
        mainVBox.setPadding(new Insets(20, 20, 20, 20));
        mainVBox.setAlignment(Pos.CENTER_LEFT);

        // 创建网格面板放置滑块
        GridPane gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.CENTER_LEFT);

        int row = 0;

        // 1. 启用平滑复选框
        enableSmoothingCheckBox = new CheckBox(I18nUtil.getString("settings.drawing.enableSmoothing"));
        enableSmoothingCheckBox.setSelected(enableSmoothing);
        gridPane.add(enableSmoothingCheckBox, 0, row++, 3, 1);

        // 添加分隔线
        row++;

        // 2. 简化容差滑块
        Label simplifyLabel = new Label(I18nUtil.getString("settings.drawing.simplifyEpsilon"));
        simplifyLabel.setTooltip(new Tooltip(I18nUtil.getString("settings.drawing.simplifyEpsilon.tooltip")));

        simplifyEpsilonSlider = createSlider(0.1, 3.0, simplifyEpsilon, 0.1);
        simplifyEpsilonValueLabel = new Label(String.format("%.2f", simplifyEpsilon));
        simplifyEpsilonValueLabel.setPrefWidth(50);

        gridPane.add(simplifyLabel, 0, row);
        gridPane.add(simplifyEpsilonSlider, 1, row);
        gridPane.add(simplifyEpsilonValueLabel, 2, row);
        row++;

        // 3. 平滑细分数滑块
        Label segmentsLabel = new Label(I18nUtil.getString("settings.drawing.smoothSegments"));
        segmentsLabel.setTooltip(new Tooltip(I18nUtil.getString("settings.drawing.smoothSegments.tooltip")));

        smoothSegmentsSlider = createSlider(5, 30, smoothSegments, 1);
        smoothSegmentsValueLabel = new Label(String.valueOf(smoothSegments));
        smoothSegmentsValueLabel.setPrefWidth(50);

        gridPane.add(segmentsLabel, 0, row);
        gridPane.add(smoothSegmentsSlider, 1, row);
        gridPane.add(smoothSegmentsValueLabel, 2, row);
        row++;

        // 4. 张力系数滑块
        Label tensionLabel = new Label(I18nUtil.getString("settings.drawing.tension"));
        tensionLabel.setTooltip(new Tooltip(I18nUtil.getString("settings.drawing.tension.tooltip")));

        tensionSlider = createSlider(0.0, 1.0, tension, 0.05);
        tensionValueLabel = new Label(String.format("%.2f", tension));
        tensionValueLabel.setPrefWidth(50);

        gridPane.add(tensionLabel, 0, row);
        gridPane.add(tensionSlider, 1, row);
        gridPane.add(tensionValueLabel, 2, row);
        row++;

        // 5. 最小采样距离滑块
        Label minDistanceLabel = new Label(I18nUtil.getString("settings.drawing.minPointDistance"));
        minDistanceLabel.setTooltip(new Tooltip(I18nUtil.getString("settings.drawing.minPointDistance.tooltip")));

        minPointDistanceSlider = createSlider(0.01, 0.2, minPointDistance, 0.01);
        minPointDistanceValueLabel = new Label(String.format("%.2f", minPointDistance));
        minPointDistanceValueLabel.setPrefWidth(50);

        gridPane.add(minDistanceLabel, 0, row);
        gridPane.add(minPointDistanceSlider, 1, row);
        gridPane.add(minPointDistanceValueLabel, 2, row);

        // 添加值变化监听器
        setupValueListeners();

        // 创建按钮面板
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button resetButton = new Button(I18nUtil.getString("settings.drawing.reset"));
        resetButton.setOnAction(e -> resetToDefaults());

        buttonBox.getChildren().add(resetButton);

        // 组装主面板
        mainVBox.getChildren().addAll(gridPane, buttonBox);

        // 设置对话框内容
        getDialogPane().setContent(mainVBox);
        getDialogPane().setPrefWidth(450);

        // 添加按钮
        ButtonType applyButtonType = new ButtonType(I18nUtil.getString("settings.dialog.apply"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(I18nUtil.getString("settings.dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(applyButtonType, cancelButtonType);

        // 应用按钮事件处理
        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                return new SettingsResult(
                        simplifyEpsilonSlider.getValue(),
                        (int) smoothSegmentsSlider.getValue(),
                        tensionSlider.getValue(),
                        minPointDistanceSlider.getValue(),
                        enableSmoothingCheckBox.isSelected()
                );
            }
            return null;
        });
    }

    /**
     * 创建滑块组件
     */
    private Slider createSlider(double min, double max, double value, double step) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit((max - min) / 5);
        slider.setBlockIncrement(step);
        slider.setPrefWidth(250);
        return slider;
    }

    /**
     * 设置值变化监听器
     */
    private void setupValueListeners() {
        simplifyEpsilonSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simplifyEpsilonValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        smoothSegmentsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            smoothSegmentsValueLabel.setText(String.valueOf(newVal.intValue()));
        });

        tensionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            tensionValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        minPointDistanceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            minPointDistanceValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });
    }

    /**
     * 重置为默认值
     */
    private void resetToDefaults() {
        simplifyEpsilonSlider.setValue(HandDrawnParameters.DEFAULT_SIMPLIFY_EPSILON.getValue().doubleValue());
        smoothSegmentsSlider.setValue(HandDrawnParameters.DEFAULT_SMOOTH_SEGMENTS.getValue().intValue());
        tensionSlider.setValue(HandDrawnParameters.DEFAULT_TENSION.getValue().doubleValue());
        minPointDistanceSlider.setValue(HandDrawnParameters.DEFAULT_MIN_POINT_DISTANCE.getValue().doubleValue());
        enableSmoothingCheckBox.setSelected(true);
    }

    /**
     * 设置结果类
     */
    public static class SettingsResult {
        private final double simplifyEpsilon;
        private final int smoothSegments;
        private final double tension;
        private final double minPointDistance;
        private final boolean enableSmoothing;

        public SettingsResult(double simplifyEpsilon, int smoothSegments,
                              double tension, double minPointDistance,
                              boolean enableSmoothing) {
            this.simplifyEpsilon = simplifyEpsilon;
            this.smoothSegments = smoothSegments;
            this.tension = tension;
            this.minPointDistance = minPointDistance;
            this.enableSmoothing = enableSmoothing;
        }

        public double getSimplifyEpsilon() {
            return simplifyEpsilon;
        }

        public int getSmoothSegments() {
            return smoothSegments;
        }

        public double getTension() {
            return tension;
        }

        public double getMinPointDistance() {
            return minPointDistance;
        }

        public boolean isEnableSmoothing() {
            return enableSmoothing;
        }
    }
}
