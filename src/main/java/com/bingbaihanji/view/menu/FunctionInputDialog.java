package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.FunctionType;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.model.FunctionParameter;
import com.bingbaihanji.util.FxTools;
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

    // 自定义表达式输入
    private TextInputControl customExpressionField;

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

        // 设置对话框图标
        FxTools.setDialogIcon(this, "/icon/function.png");

        // 创建主布局
        VBox mainContent = new VBox(15);
        mainContent.setPadding(new Insets(20));

        // 函数类型选择
        Label typeLabel = new Label(I18nUtil.getString("function.type"));
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // 函数类型选择下拉框
        functionTypeCombo = new ComboBox<>();
        functionTypeCombo.getItems().addAll(FunctionType.values()); // 添加所有函数类型
        functionTypeCombo.setValue(FunctionType.CUSTOM); // 默认选择自定义函数
        functionTypeCombo.setPrefWidth(300);

        VBox typeSection = new VBox(10, typeLabel, functionTypeCombo);

        // 参数输入区(动态)
        parametersPane = new VBox(10);

        // 定义域设置区
        VBox domainSection = createDomainSection(transform, canvasWidth, canvasHeight);

        mainContent.getChildren().addAll(typeSection, parametersPane, domainSection);

        // 监听类型变化,动态更新参数输入区
        functionTypeCombo.valueProperty().addListener((obs, old, newVal) -> {
            updateParametersPane(newVal);
        });

        // 初始化参数面板
        updateParametersPane(FunctionType.CUSTOM);

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
        customExpressionField = null;

        Label title = new Label(I18nUtil.getString("function.parameters"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        parametersPane.getChildren().add(title);

        if (type == FunctionType.CUSTOM) {
            // 自定义表达式输入区
            Label exprLabel = new Label("表达式 f(x)：");
            customExpressionField = new TextArea();
            customExpressionField.setPrefHeight(100);
            customExpressionField.setPrefWidth(300);
            customExpressionField.setPromptText("例如: sin(x) + x^2 或 2*x^3 - x");

            Label hintLabel = new Label(
                    """
                            +----------------+-------------------------------------------+
                            | 运算符         | +  -  *  /  ^(幂)                          |
                            +----------------+-------------------------------------------+
                            | 内置函数       | sin(正弦)    cos(余弦)    tan(正切)          |
                            |                | asin(反正弦) acos(反余弦) atan(反正切)       |
                            |                | abs(绝对值)  exp(指数)   ceil(向上取整)      |
                            |                | floor(向下取整) log(对数)  log2(以2为底的对数)|
                            |                | log10(以10为底的对数) sqrt(平方根)           |
                            +----------------+-------------------------------------------+
                            | 常量           | pi(圆周率)   e(自然对数的底数)                |
                            +----------------+-------------------------------------------+"""
            );
            hintLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

            parametersPane.getChildren().addAll(exprLabel, customExpressionField, hintLabel);
        } else {
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

            // 读取定义域(对所有类型通用)
            boolean autoRange = autoRangeCheckBox.isSelected();
            double domainMin = autoRange ? Double.NEGATIVE_INFINITY
                    : Double.parseDouble(domainMinField.getText());
            double domainMax = autoRange ? Double.POSITIVE_INFINITY
                    : Double.parseDouble(domainMaxField.getText());

            if (!autoRange && domainMin >= domainMax) {
                showError(I18nUtil.getString("function.error.invalidDomain"));
                return null;
            }

            // 自定义表达式类型单独处理
            if (type == FunctionType.CUSTOM) {
                String expr = customExpressionField == null ? "" : customExpressionField.getText().trim();
                String error = com.bingbaihanji.view.layout.draw.geometry.impl.CustomFunctionGeo.validate(expr);
                if (error != null) {
                    showError(error);
                    return null;
                }
                return new FunctionInputResult(type, new HashMap<>(), domainMin, domainMax, autoRange, expr);
            }

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
        FxTools.showErrorAlert(I18nUtil.getString("function.error.title"), message);
    }
}
