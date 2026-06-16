package com.bingbaihanji.util;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class FormulaCalculator extends Application {

    private TextField formulaDisplay;   // 公式显示框
    private Label resultLabel;          // 结果显示标签

    @Override
    public void start(Stage primaryStage) {
        // ---------- 顶部显示区域 ----------
        formulaDisplay = new TextField();
        formulaDisplay.setEditable(false);
        formulaDisplay.setPrefHeight(50);
        formulaDisplay.setStyle("-fx-font-size: 18px; -fx-alignment: center-right;");

        resultLabel = new Label();
        resultLabel.setPrefHeight(30);
        resultLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333; -fx-alignment: center-right;");
        resultLabel.setMaxWidth(Double.MAX_VALUE);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);

        VBox topBox = new VBox(5, formulaDisplay, resultLabel);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1;");

        // ---------- 按钮区域 ----------
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(5);
        buttonGrid.setVgap(5);
        buttonGrid.setPadding(new Insets(10));
        buttonGrid.setAlignment(Pos.CENTER);

        // 定义按钮文字（按行）
        String[][] buttonTexts = {
            {"C", "⌫", "(", ")", "÷"},
            {"7", "8", "9", "*", "sin"},
            {"4", "5", "6", "-", "cos"},
            {"1", "2", "3", "+", "tan"},
            {"0", ".", "^", "log", "√"},
            {"π", "e", "=", "", ""}   // 最后两个空位不添加按钮
        };

        // 生成按钮并绑定事件
        for (int row = 0; row < buttonTexts.length; row++) {
            for (int col = 0; col < buttonTexts[row].length; col++) {
                String text = buttonTexts[row][col];
                if (text.isEmpty()) continue;   // 跳过空位

                Button btn = new Button(text);
                btn.setPrefSize(60, 40);
                btn.setStyle("-fx-font-size: 14px;");
                btn.setOnAction(e -> handleButtonClick(text));

                buttonGrid.add(btn, col, row);
            }
        }

        // 整体布局
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(buttonGrid);

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setTitle("公式输入组件 - exp4j");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * 处理按钮点击事件
     */
    private void handleButtonClick(String command) {
        switch (command) {
            case "C":
                formulaDisplay.clear();
                resultLabel.setText("");
                break;
            case "⌫":
                String current = formulaDisplay.getText();
                if (!current.isEmpty()) {
                    formulaDisplay.setText(current.substring(0, current.length() - 1));
                }
                break;
            case "=":
                evaluateExpression();
                break;
            case "÷":
                appendToFormula("/");
                break;
            case "√":
                // 输入 sqrt( 并等待用户补全右括号
                appendToFormula("sqrt(");
                break;
            case "π":
                appendToFormula("pi");
                break;
            case "e":
                appendToFormula("e");
                break;
            // 函数类按钮 (sin, cos, tan, log)
            default:
                if (command.matches("sin|cos|tan|log")) {
                    appendToFormula(command + "(");
                } else {
                    appendToFormula(command);
                }
                break;
        }
    }

    /**
     * 向公式显示框中追加字符
     */
    private void appendToFormula(String text) {
        formulaDisplay.appendText(text);
    }

    /**
     * 使用 exp4j 计算当前公式并显示结果
     */
    private void evaluateExpression() {
        String expr = formulaDisplay.getText().trim();
        if (expr.isEmpty()) {
            resultLabel.setText("请输入公式");
            return;
        }
        try {
            Expression expression = new ExpressionBuilder(expr).build();
            double result = expression.evaluate();
            // 如果结果是整数，去掉小数点
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                resultLabel.setText(String.valueOf((long) result));
            } else {
                resultLabel.setText(String.valueOf(result));
            }
        } catch (ArithmeticException e) {
            resultLabel.setText("数学错误：" + e.getMessage());
        } catch (Exception e) {
            resultLabel.setText("语法错误");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}