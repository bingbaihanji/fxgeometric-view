package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.FunctionType;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.model.FunctionParameter;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * 函数输入对话框
 * <p>
 * 允许用户选择函数类型并输入参数
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class FunctionInputDialog extends Dialog<FunctionInputResult> {

    private final ComboBox<FunctionType> functionTypeCombo;
    private final VBox parametersPane;
    private final Map<String, TextField> parameterFields = new HashMap<>();

    // 定义域设置
    private CheckBox autoRangeCheckBox;
    private TextField domainMinField;
    private TextField domainMaxField;

    /**
     * 构造函数
     *
     * @param transform    世界坐标变换器
     * @param canvasWidth  画布宽度
     * @param canvasHeight 画布高度
     */
    public FunctionInputDialog(WorldTransform transform, double canvasWidth, double canvasHeight) {
        setTitle(I18nUtil.getString("function.dialog.title"));
        setHeaderText(I18nUtil.getString("function.dialog.header"));

        // 创建主布局
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));

        // 函数类型选择
        Label typeLabel = new Label(I18nUtil.getString("function.type"));
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        functionTypeCombo = new ComboBox<>();
        functionTypeCombo.getItems().addAll(FunctionType.values());
        functionTypeCombo.setValue(FunctionType.LINEAR);
        functionTypeCombo.setPrefWidth(300);

        VBox typeSection = new VBox(10, typeLabel, functionTypeCombo);

        // 参数输入区（动态）
        parametersPane = new VBox(10);

        // 定义域设置区
        VBox domainSection = createDomainSection(transform, canvasWidth, canvasHeight);

        mainContent.getChildren().addAll(typeSection, parametersPane, domainSection);

        // 监听类型变化，动态更新参数输入区
        functionTypeCombo.valueProperty().addListener((obs, old, newVal) -> {
            updateParametersPane(newVal);
        });

        // 初始化参数面板
        updateParametersPane(FunctionType.LINEAR);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);

        getDialogPane().setContent(scrollPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 设置结果转换器
        setResultConverter(this::convertResult);
    }

    /**
     * 更新参数输入面板
     */
    private void updateParametersPane(FunctionType type) {
        parametersPane.getChildren().clear();
        parameterFields.clear();

        Label title = new Label(I18nUtil.getString("function.parameters"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        parametersPane.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(5, 0, 0, 0));

        int row = 0;
        for (FunctionParameter param : type.getParameters()) {
            Label label = new Label(param.getLabel() + ":");
            TextField field = new TextField(param.getDefaultValue());
            field.setPrefWidth(200);
            field.setPromptText(param.getDescription());

            // 添加数值验证
            field.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.matches("-?\\d*\\.?\\d*")) {
                    field.setText(old);
                }
            });

            grid.add(label, 0, row);
            grid.add(field, 1, row);

            parameterFields.put(param.getName(), field);
            row++;
        }

        parametersPane.getChildren().add(grid);
    }

    /**
     * 创建定义域设置区
     */
    private VBox createDomainSection(WorldTransform transform, double w, double h) {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("function.domain"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        autoRangeCheckBox = new CheckBox(I18nUtil.getString("function.domain.auto"));
        autoRangeCheckBox.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(5, 0, 0, 0));

        // 计算当前视图范围
        double viewMinX = transform.screenToWorldX(0);
        double viewMaxX = transform.screenToWorldX(w);

        Label minLabel = new Label(I18nUtil.getString("function.domain.min") + ":");
        domainMinField = new TextField(String.format("%.2f", viewMinX));
        domainMinField.setPrefWidth(150);
        domainMinField.setDisable(true);

        // 添加数值验证
        domainMinField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("-?\\d*\\.?\\d*")) {
                domainMinField.setText(old);
            }
        });

        Label maxLabel = new Label(I18nUtil.getString("function.domain.max") + ":");
        domainMaxField = new TextField(String.format("%.2f", viewMaxX));
        domainMaxField.setPrefWidth(150);
        domainMaxField.setDisable(true);

        // 添加数值验证
        domainMaxField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("-?\\d*\\.?\\d*")) {
                domainMaxField.setText(old);
            }
        });

        // 自动模式切换
        autoRangeCheckBox.selectedProperty().addListener((obs, old, val) -> {
            domainMinField.setDisable(val);
            domainMaxField.setDisable(val);
        });

        grid.add(minLabel, 0, 0);
        grid.add(domainMinField, 1, 0);
        grid.add(maxLabel, 0, 1);
        grid.add(domainMaxField, 1, 1);

        section.getChildren().addAll(title, autoRangeCheckBox, grid);
        return section;
    }

    /**
     * 结果转换器
     */
    private FunctionInputResult convertResult(ButtonType buttonType) {
        if (buttonType != ButtonType.OK) {
            return null;
        }

        try {
            FunctionType type = functionTypeCombo.getValue();
            Map<String, Double> parameters = new HashMap<>();

            // 读取所有参数
            for (Map.Entry<String, TextField> entry : parameterFields.entrySet()) {
                String paramName = entry.getKey();
                String value = entry.getValue().getText();
                if (value == null || value.trim().isEmpty()) {
                    showError(I18nUtil.getString("function.error.emptyParameter"));
                    return null;
                }
                parameters.put(paramName, Double.parseDouble(value));
            }

            // 验证参数有效性
            if (!validateParameters(type, parameters)) {
                return null;
            }

            // 读取定义域
            boolean autoRange = autoRangeCheckBox.isSelected();
            double domainMin = autoRange ? Double.NEGATIVE_INFINITY
                    : Double.parseDouble(domainMinField.getText());
            double domainMax = autoRange ? Double.POSITIVE_INFINITY
                    : Double.parseDouble(domainMaxField.getText());

            // 验证定义域
            if (!autoRange && domainMin >= domainMax) {
                showError(I18nUtil.getString("function.error.invalidDomain"));
                return null;
            }

            return new FunctionInputResult(type, parameters, domainMin, domainMax, autoRange);

        } catch (NumberFormatException e) {
            showError(I18nUtil.getString("function.error.invalidNumber"));
            return null;
        }
    }

    /**
     * 验证参数有效性
     */
    private boolean validateParameters(FunctionType type, Map<String, Double> params) {
        switch (type) {
            case QUADRATIC:
                // 二次项系数不能为0
                Double a = params.get("a");
                if (a != null && Math.abs(a) < 1e-10) {
                    showError("二次项系数a不能为0");
                    return false;
                }
                break;

            case RECIPROCAL:
                // 反比例系数不能为0
                Double k = params.get("k");
                if (k != null && Math.abs(k) < 1e-10) {
                    showError("系数k不能为0");
                    return false;
                }
                break;

            case EXPONENTIAL:
            case LOGARITHMIC:
                // 底数必须大于0且不等于1
                Double base = params.get("a");
                if (base != null && (base <= 0 || Math.abs(base - 1.0) < 1e-10)) {
                    showError("底数a必须大于0且不等于1");
                    return false;
                }
                break;

            case ELLIPSE:
            case HYPERBOLA:
                // 长半轴和短半轴必须大于0
                Double aAxis = params.get("a");
                Double bAxis = params.get("b");
                if (aAxis != null && aAxis <= 0) {
                    showError("长半轴a必须大于0");
                    return false;
                }
                if (bAxis != null && bAxis <= 0) {
                    showError("短半轴b必须大于0");
                    return false;
                }
                break;

            case PARABOLA_CONIC:
                // 焦参数必须大于0
                Double p = params.get("p");
                if (p != null && p <= 0) {
                    showError("焦参数p必须大于0");
                    return false;
                }
                break;
        }

        return true;
    }

    /**
     * 显示错误提示
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nUtil.getString("function.error.title"));
        alert.setContentText(message);
        alert.showAndWait();
    }
}
