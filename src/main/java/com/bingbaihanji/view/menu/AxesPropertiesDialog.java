package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.AxisArrowType;
import com.bingbaihanji.constant.AxisTickStyle;
import com.bingbaihanji.constant.LineType;
import com.bingbaihanji.constant.UnitLabelType;
import com.bingbaihanji.util.FxTools;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 坐标轴属性对话框
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 配置坐标轴的各种属性
 */
public class AxesPropertiesDialog extends Dialog<ButtonType> {

    private final EuclidianViewSettings settings;

    // UI控件
    private CheckBox cbShowXAxis;
    private CheckBox cbShowYAxis;
    private CheckBox cbShowAxesNumbers;

    private TextField tfXScale;
    private TextField tfYScale;

    private CheckBox cbAutoXTick;
    private TextField tfXTickDistance;
    private CheckBox cbAutoYTick;
    private TextField tfYTickDistance;

    private ComboBox<AxisTickStyle> cmbXTickStyle;
    private ComboBox<AxisTickStyle> cmbYTickStyle;

    private ComboBox<AxisArrowType> cmbXArrowType;
    private ComboBox<AxisArrowType> cmbYArrowType;

    private ComboBox<UnitLabelType> cmbUnitLabelType;

    private ComboBox<LineType> cmbLineType;
    private ColorPicker colorPicker;

    public AxesPropertiesDialog(EuclidianViewSettings settings) {
        this.settings = settings;

        setTitle(I18nUtil.getString("menu.axesProperties"));
        setHeaderText(I18nUtil.getString("menu.axesProperties.header"));

        // 设置对话框图标
        FxTools.setDialogIcon(this, "/icon/axes.png");

        // 创建UI
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        content.getChildren().addAll(
                createVisibilitySection(),
                new Separator(),
                createScaleSection(),
                new Separator(),
                createTickSection(),
                new Separator(),
                createStyleSection()
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        getDialogPane().setContent(scrollPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 加载当前配置
        loadSettings();

        // OK按钮处理
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                saveSettings();
            }
            return buttonType;
        });
    }

    /**
     * 创建可见性设置区
     */
    private VBox createVisibilitySection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("axes.visibility"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        cbShowXAxis = new CheckBox(I18nUtil.getString("axes.showXAxis"));
        cbShowYAxis = new CheckBox(I18nUtil.getString("axes.showYAxis"));
        cbShowAxesNumbers = new CheckBox(I18nUtil.getString("axes.showNumbers"));

        section.getChildren().addAll(title, cbShowXAxis, cbShowYAxis, cbShowAxesNumbers);
        return section;
    }

    /**
     * 创建比例设置区
     */
    private VBox createScaleSection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("axes.scale"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        Label lblXScale = new Label(I18nUtil.getString("axes.xScale") + ":");
        tfXScale = new TextField();
        tfXScale.setPrefWidth(100);

        Label lblYScale = new Label(I18nUtil.getString("axes.yScale") + ":");
        tfYScale = new TextField();
        tfYScale.setPrefWidth(100);

        grid.add(lblXScale, 0, 0);
        grid.add(tfXScale, 1, 0);
        grid.add(lblYScale, 0, 1);
        grid.add(tfYScale, 1, 1);

        section.getChildren().addAll(title, grid);
        return section;
    }

    /**
     * 创建刻度设置区
     */
    private VBox createTickSection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("axes.ticks"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // X轴刻度
        Label lblXTick = new Label(I18nUtil.getString("axes.xTick") + ":");
        cbAutoXTick = new CheckBox(I18nUtil.getString("axes.auto"));
        tfXTickDistance = new TextField();
        tfXTickDistance.setPrefWidth(80);

        cmbXTickStyle = new ComboBox<>();
        cmbXTickStyle.getItems().addAll(AxisTickStyle.values());
        cmbXTickStyle.setPrefWidth(150);

        // 自动模式时禁用距离输入
        cbAutoXTick.selectedProperty().addListener((obs, old, val) -> {
            tfXTickDistance.setDisable(val);
        });

        HBox xTickBox = new HBox(5, cbAutoXTick, tfXTickDistance);

        grid.add(lblXTick, 0, 0);
        grid.add(xTickBox, 1, 0);
        grid.add(new Label(I18nUtil.getString("axes.xTickStyle") + ":"), 0, 1);
        grid.add(cmbXTickStyle, 1, 1);

        // Y轴刻度
        Label lblYTick = new Label(I18nUtil.getString("axes.yTick") + ":");
        cbAutoYTick = new CheckBox(I18nUtil.getString("axes.auto"));
        tfYTickDistance = new TextField();
        tfYTickDistance.setPrefWidth(80);

        cmbYTickStyle = new ComboBox<>();
        cmbYTickStyle.getItems().addAll(AxisTickStyle.values());
        cmbYTickStyle.setPrefWidth(150);

        cbAutoYTick.selectedProperty().addListener((obs, old, val) -> {
            tfYTickDistance.setDisable(val);
        });

        HBox yTickBox = new HBox(5, cbAutoYTick, tfYTickDistance);

        grid.add(lblYTick, 0, 2);
        grid.add(yTickBox, 1, 2);
        grid.add(new Label(I18nUtil.getString("axes.yTickStyle") + ":"), 0, 3);
        grid.add(cmbYTickStyle, 1, 3);

        section.getChildren().addAll(title, grid);
        return section;
    }

    /**
     * 创建样式设置区
     */
    private VBox createStyleSection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("axes.style"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // 箭头类型
        Label lblXArrow = new Label(I18nUtil.getString("axes.xArrow") + ":");
        cmbXArrowType = new ComboBox<>();
        cmbXArrowType.getItems().addAll(AxisArrowType.values());
        cmbXArrowType.setPrefWidth(150);

        Label lblYArrow = new Label(I18nUtil.getString("axes.yArrow") + ":");
        cmbYArrowType = new ComboBox<>();
        cmbYArrowType.getItems().addAll(AxisArrowType.values());
        cmbYArrowType.setPrefWidth(150);

        grid.add(lblXArrow, 0, 0);
        grid.add(cmbXArrowType, 1, 0);
        grid.add(lblYArrow, 0, 1);
        grid.add(cmbYArrowType, 1, 1);

        // 单位类型
        Label lblUnit = new Label(I18nUtil.getString("axes.unitLabel") + ":");
        cmbUnitLabelType = new ComboBox<>();
        cmbUnitLabelType.getItems().addAll(UnitLabelType.values());
        cmbUnitLabelType.setPrefWidth(150);

        grid.add(lblUnit, 0, 2);
        grid.add(cmbUnitLabelType, 1, 2);

        // 线型
        Label lblLineType = new Label(I18nUtil.getString("axes.lineType") + ":");
        cmbLineType = new ComboBox<>();
        cmbLineType.getItems().addAll(LineType.values());
        cmbLineType.setPrefWidth(150);

        grid.add(lblLineType, 0, 3);
        grid.add(cmbLineType, 1, 3);

        // 颜色
        Label lblColor = new Label(I18nUtil.getString("axes.color") + ":");
        colorPicker = new ColorPicker();
        colorPicker.setPrefWidth(150);

        grid.add(lblColor, 0, 4);
        grid.add(colorPicker, 1, 4);

        section.getChildren().addAll(title, grid);
        return section;
    }

    /**
     * 加载当前设置到UI
     */
    private void loadSettings() {
        cbShowXAxis.setSelected(settings.isShowXAxis());
        cbShowYAxis.setSelected(settings.isShowYAxis());
        cbShowAxesNumbers.setSelected(settings.isShowAxesNumbers());

        tfXScale.setText(String.valueOf(settings.getXScale()));
        tfYScale.setText(String.valueOf(settings.getYScale()));

        cbAutoXTick.setSelected(settings.isAutoXTickDistance());
        tfXTickDistance.setText(String.valueOf(settings.getXTickDistance()));
        tfXTickDistance.setDisable(settings.isAutoXTickDistance());

        cbAutoYTick.setSelected(settings.isAutoYTickDistance());
        tfYTickDistance.setText(String.valueOf(settings.getYTickDistance()));
        tfYTickDistance.setDisable(settings.isAutoYTickDistance());

        cmbXTickStyle.setValue(settings.getXTickStyle());
        cmbYTickStyle.setValue(settings.getYTickStyle());

        cmbXArrowType.setValue(settings.getXArrowType());
        cmbYArrowType.setValue(settings.getYArrowType());

        cmbUnitLabelType.setValue(settings.getUnitLabelType());

        cmbLineType.setValue(settings.getAxesLineType());
        colorPicker.setValue(settings.getAxesColor());
    }

    /**
     * 保存UI设置到配置
     */
    private void saveSettings() {
        settings.batchUpdate(s -> {
            s.setShowXAxis(cbShowXAxis.isSelected());
            s.setShowYAxis(cbShowYAxis.isSelected());
            s.setShowAxesNumbers(cbShowAxesNumbers.isSelected());

            try {
                s.setXScale(Double.parseDouble(tfXScale.getText()));
                s.setYScale(Double.parseDouble(tfYScale.getText()));
            } catch (NumberFormatException e) {
                // 保持原值
            }

            s.setAutoXTickDistance(cbAutoXTick.isSelected());
            if (!cbAutoXTick.isSelected()) {
                try {
                    s.setXTickDistance(Double.parseDouble(tfXTickDistance.getText()));
                } catch (NumberFormatException e) {
                    // 保持原值
                }
            }

            s.setAutoYTickDistance(cbAutoYTick.isSelected());
            if (!cbAutoYTick.isSelected()) {
                try {
                    s.setYTickDistance(Double.parseDouble(tfYTickDistance.getText()));
                } catch (NumberFormatException e) {
                    // 保持原值
                }
            }

            s.setXTickStyle(cmbXTickStyle.getValue());
            s.setYTickStyle(cmbYTickStyle.getValue());

            s.setXArrowType(cmbXArrowType.getValue());
            s.setYArrowType(cmbYArrowType.getValue());

            s.setUnitLabelType(cmbUnitLabelType.getValue());

            s.setAxesLineType(cmbLineType.getValue());
            s.setAxesColor(colorPicker.getValue());
        });
    }
}
