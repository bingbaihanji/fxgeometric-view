package com.bingbaihanji.view.menu;

import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.util.StyleManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;

/**
 * 线条样式设置对话框
 * <p>
 * 可设置默认线条颜色、线宽、发光描边等效果。
 *
 * @author bingbaihanji
 */
public class LineStyleSettingsDialog extends Dialog<LineStyleSettingsDialog.SettingsResult> {

    // 当前设置值
    private final Color currentColor;
    private final double currentLineWidth;
    private final boolean currentGlowEnabled;
    private final double currentGlowAlpha;
    private final double currentGlowWidth;

    // 控件
    private ColorPicker colorPicker;
    private Slider lineWidthSlider;
    private Label lineWidthValueLabel;
    private CheckBox glowEnabledCheckBox;
    private Slider glowAlphaSlider;
    private Label glowAlphaValueLabel;
    private Slider glowWidthSlider;
    private Label glowWidthValueLabel;

    public LineStyleSettingsDialog() {
        this(StyleManager.defaultLineColor, StyleManager.defaultLineWidth,
                StyleManager.GLOW_ENABLED, StyleManager.GLOW_ALPHA,
                StyleManager.GLOW_WIDTH_BONUS);
    }

    public LineStyleSettingsDialog(Color color, double lineWidth,
                                   boolean glowEnabled, double glowAlpha,
                                   double glowWidth) {
        this.currentColor = color;
        this.currentLineWidth = lineWidth;
        this.currentGlowEnabled = glowEnabled;
        this.currentGlowAlpha = glowAlpha;
        this.currentGlowWidth = glowWidth;
        initDialog();
    }

    private void initDialog() {
        setTitle(I18nUtil.getString("settings.lineStyle.title"));
        setHeaderText(I18nUtil.getString("settings.lineStyle.header"));
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        FxTools.setDialogIcon(this, "/icon/setting.png");

        VBox mainVBox = new VBox(15);
        mainVBox.setPadding(new Insets(20, 20, 20, 20));
        mainVBox.setAlignment(Pos.CENTER_LEFT);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(15);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.CENTER_LEFT);

        int row = 0;

        // 1. 默认颜色
        Label colorLabel = new Label(I18nUtil.getString("settings.lineStyle.defaultColor"));
        colorPicker = new ColorPicker(currentColor);
        colorPicker.setPrefWidth(100);
        gridPane.add(colorLabel, 0, row);
        gridPane.add(colorPicker, 1, row, 2, 1);
        row++;

        // 2. 默认线宽
        Label lineWidthLabel = new Label(I18nUtil.getString("settings.lineStyle.lineWidth"));
        lineWidthSlider = createSlider(0.5, 5.0, currentLineWidth, 0.5);
        lineWidthValueLabel = new Label(String.format("%.1f", currentLineWidth));
        lineWidthValueLabel.setPrefWidth(40);
        gridPane.add(lineWidthLabel, 0, row);
        gridPane.add(lineWidthSlider, 1, row);
        gridPane.add(lineWidthValueLabel, 2, row);
        row++;

        // 分隔
        row++;

        // 3. 启用发光
        glowEnabledCheckBox = new CheckBox(I18nUtil.getString("settings.lineStyle.glowEnabled"));
        glowEnabledCheckBox.setSelected(currentGlowEnabled);
        gridPane.add(glowEnabledCheckBox, 0, row++, 3, 1);
        row++;

        // 4. 发光透明度
        Label glowAlphaLabel = new Label(I18nUtil.getString("settings.lineStyle.glowAlpha"));
        glowAlphaSlider = createSlider(0.05, 0.8, currentGlowAlpha, 0.05);
        glowAlphaValueLabel = new Label(String.format("%.2f", currentGlowAlpha));
        glowAlphaValueLabel.setPrefWidth(40);
        gridPane.add(glowAlphaLabel, 0, row);
        gridPane.add(glowAlphaSlider, 1, row);
        gridPane.add(glowAlphaValueLabel, 2, row);
        row++;

        // 5. 发光宽度
        Label glowWidthLabel = new Label(I18nUtil.getString("settings.lineStyle.glowWidth"));
        glowWidthSlider = createSlider(1.0, 6.0, currentGlowWidth, 0.5);
        glowWidthValueLabel = new Label(String.format("%.1f", currentGlowWidth));
        glowWidthValueLabel.setPrefWidth(40);
        gridPane.add(glowWidthLabel, 0, row);
        gridPane.add(glowWidthSlider, 1, row);
        gridPane.add(glowWidthValueLabel, 2, row);
        row++;

        setupValueListeners();

        // 重置按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button resetButton = new Button(I18nUtil.getString("settings.drawing.reset"));
        resetButton.setOnAction(e -> resetToDefaults());
        buttonBox.getChildren().add(resetButton);

        mainVBox.getChildren().addAll(gridPane, buttonBox);
        getDialogPane().setContent(mainVBox);
        getDialogPane().setPrefWidth(480);

        ButtonType applyButtonType = new ButtonType(
                I18nUtil.getString("settings.dialog.apply"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(
                I18nUtil.getString("settings.dialog.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(applyButtonType, cancelButtonType);

        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                return new SettingsResult(
                        colorPicker.getValue(),
                        lineWidthSlider.getValue(),
                        glowEnabledCheckBox.isSelected(),
                        glowAlphaSlider.getValue(),
                        glowWidthSlider.getValue()
                );
            }
            return null;
        });
    }

    private Slider createSlider(double min, double max, double value, double step) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit((max - min) / 5);
        slider.setBlockIncrement(step);
        slider.setPrefWidth(220);
        return slider;
    }

    private void setupValueListeners() {
        lineWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lineWidthValueLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });
        glowAlphaSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            glowAlphaValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });
        glowWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            glowWidthValueLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });
    }

    private void resetToDefaults() {
        colorPicker.setValue(Color.LIGHTSLATEGRAY);
        lineWidthSlider.setValue(2.0);
        glowEnabledCheckBox.setSelected(true);
        glowAlphaSlider.setValue(0.75);
        glowWidthSlider.setValue(2.0);
    }

    /** 设置结果 */
    public static class SettingsResult {
        private final Color color;
        private final double lineWidth;
        private final boolean glowEnabled;
        private final double glowAlpha;
        private final double glowWidth;

        public SettingsResult(Color color, double lineWidth, boolean glowEnabled,
                             double glowAlpha, double glowWidth) {
            this.color = color;
            this.lineWidth = lineWidth;
            this.glowEnabled = glowEnabled;
            this.glowAlpha = glowAlpha;
            this.glowWidth = glowWidth;
        }

        public Color getColor() { return color; }
        public double getLineWidth() { return lineWidth; }
        public boolean isGlowEnabled() { return glowEnabled; }
        public double getGlowAlpha() { return glowAlpha; }
        public double getGlowWidth() { return glowWidth; }
    }
}
