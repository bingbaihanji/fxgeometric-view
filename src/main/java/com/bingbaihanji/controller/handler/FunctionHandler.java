package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.view.layout.draw.geometry.impl.FunctionGeo;
import com.bingbaihanji.view.menu.FunctionInputDialog;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.Optional;

/**
 * 函数绘制处理器
 * <p>
 * 处理函数绘制模式的鼠标事件
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public class FunctionHandler extends AbstractDrawingHandler {

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.FUNCTION;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, IDrawingContext context) {
        if (e.getButton() != MouseButton.PRIMARY || !canHandle(context.getDrawMode())) {
            return false;
        }

        // 在JavaFX线程上显示对话框
        Platform.runLater(() -> {
            // 显示函数输入对话框
            double width = context.getGridChartPane().getWidth();
            double height = context.getGridChartPane().getHeight();

            FunctionInputDialog dialog = new FunctionInputDialog(
                    context.getTransform(),
                    width,
                    height
            );

            Optional<FunctionInputResult> result = dialog.showAndWait();

            if (result.isPresent()) {
                FunctionInputResult input = result.get();
                try {
                    FunctionGeo function = com.bingbaihanji.factory.FunctionFactory.createFunction(input);
                    // 设置定义域
                    if (!input.autoRange()) {
                        function.setDomainRange(input.domainMin(), input.domainMax());
                    }
                    // 通过命令历史添加到画布(支持撤销)
                    context.executeCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            context.addObject(function);
                        }

                        @Override
                        public void undo() {
                            context.removeObject(function);
                        }
                    });
                    context.redraw();
                } catch (com.bingbaihanji.factory.FunctionCreationException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("函数创建失败");
                    alert.setHeaderText("无法创建函数图像");
                    alert.setContentText(ex.getMessage());
                    alert.show();
                }
            }
        });

        e.consume();
        return true;
    }


    @Override
    public void reset() {
        // 函数模式无需重置状态
    }
}
