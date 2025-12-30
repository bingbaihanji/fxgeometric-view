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
    private final boolean isCircle;

    /**
     * 为普通图形创建属性对话框（仅支持颜色修改）
     */
    public ShapePropertiesDialog(Color currentColor) {
        this(currentColor, 0, false);
    }

    /**
     * 为圆形创建属性对话框（支持颜色和半径修改）
     */
    public ShapePropertiesDialog(Color currentColor, double currentRadius) {
        this(currentColor, currentRadius, true);
    }

    /**
     * 内部构造函数
     */
    private ShapePropertiesDialog(Color currentColor, double currentRadius, boolean isCircle) {
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

        // 如果是圆形，添加半径输入框
        radiusField = new TextField();
        if (isCircle) {
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
        }

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Color selectedColor = colorPicker.getValue();
                double radius = 0;
                
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
                }
                
                return new ShapePropertiesResult(selectedColor, radius);
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
}
