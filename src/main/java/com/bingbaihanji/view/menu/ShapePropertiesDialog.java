package com.bingbaihanji.view.menu;

import com.bingbaihanji.util.I18nUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/**
 * 几何图形属性对话框
 * <p>
 * 支持修改几何图形的颜色，对于圆形还可以修改半径
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class ShapePropertiesDialog extends Dialog<ShapePropertiesResult> {

    private final ColorPicker colorPicker;
    private final TextField radiusField;
    private final TextField centerNameField; // 圆心名称输入框
    private final boolean isCircle;

    /**
     * 为普通图形创建属性对话框（仅支持颜色修改）
     */
    public ShapePropertiesDialog(Color currentColor) {
        this(currentColor, 0, null, false);
    }

    /**
     * 为圆形创建属性对话框（支持颜色、半径和圆心名称修改）
     */
    public ShapePropertiesDialog(Color currentColor, double currentRadius, String currentCenterName) {
        this(currentColor, currentRadius, currentCenterName, true);
    }

    /**
     * 内部构造函数
     */
    private ShapePropertiesDialog(Color currentColor, double currentRadius, String currentCenterName, boolean isCircle) {
        this.isCircle = isCircle;

        setTitle(I18nUtil.getString("geo.dialog.properties.title"));
        setHeaderText(I18nUtil.getString("geo.dialog.properties.header"));

        // 创建内容面板
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        // 颜色选择器
        Label colorLabel = new Label(I18nUtil.getString("geo.dialog.properties.color"));
        colorPicker = new ColorPicker(currentColor);
        colorPicker.setPrefWidth(200);
        grid.add(colorLabel, 0, 0);
        grid.add(colorPicker, 1, 0);

        // 如果是圆形，添加半径和圆心名称输入框
        radiusField = new TextField();
        centerNameField = new TextField();
        if (isCircle) {
            // 半径输入框
            Label radiusLabel = new Label(I18nUtil.getString("geo.dialog.properties.radius"));
            radiusField.setText(String.format("%.2f", currentRadius));
            radiusField.setPrefWidth(200);

            // 添加输入验证
            radiusField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*\\.?\\d*")) {
                    radiusField.setText(oldVal);
                }
            });

            grid.add(radiusLabel, 0, 1);
            grid.add(radiusField, 1, 1);

            // 圆心名称输入框
            Label centerNameLabel = new Label(I18nUtil.getString("geo.dialog.properties.centerName"));
            centerNameField.setText(currentCenterName != null ? currentCenterName : "");
            centerNameField.setPrefWidth(200);
            centerNameField.setPromptText(I18nUtil.getString("geo.dialog.properties.centerName.prompt"));

            grid.add(centerNameLabel, 0, 2);
            grid.add(centerNameField, 1, 2);
        }

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Color selectedColor = colorPicker.getValue();
                double radius = 0;
                String centerName = null;

                if (isCircle) {
                    try {
                        radius = Double.parseDouble(radiusField.getText());
                        if (radius <= 0) {
                            showErrorAlert(I18nUtil.getString("geo.dialog.properties.error.radius"));
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showErrorAlert(I18nUtil.getString("geo.dialog.properties.error.invalid"));
                        return null;
                    }

                    // 获取圆心名称
                    centerName = centerNameField.getText().trim();
                    if (centerName.isEmpty()) {
                        centerName = null; // 空名称设为 null
                    }

                    return new ShapePropertiesResult(selectedColor, radius, centerName);
                }

                return new ShapePropertiesResult(selectedColor);
            }
            return null;
        });
    }

    /**
     * 显示错误提示
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nUtil.getString("geo.dialog.properties.error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean isCircle() {
        return isCircle;
    }
}
