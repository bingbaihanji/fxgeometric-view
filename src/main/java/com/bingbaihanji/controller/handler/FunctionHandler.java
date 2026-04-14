package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.Logger;
import com.bingbaihanji.view.layout.draw.geometry.impl.FunctionGeo;
import com.bingbaihanji.view.menu.FunctionInputDialog;
import javafx.application.Platform;
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

    private static final Logger logger = Logger.getLogger(FunctionHandler.class);

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.FUNCTION;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, DrawingContext context) {
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
                FunctionGeo function = createFunction(input);

                if (function != null) {
                    // 设置定义域
                    if (!input.isAutoRange()) {
                        function.setDomainRange(input.getDomainMin(), input.getDomainMax());
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
                }
            }
        });

        e.consume();
        return true;
    }

    /**
     * 根据输入结果创建函数对象
     * <p>
     * 委托给 FunctionFactory 工厂类创建,避免代码重复
     */
    private FunctionGeo createFunction(FunctionInputResult input) {
        try {
            return com.bingbaihanji.factory.FunctionFactory.createFunction(input);
        } catch (Exception e) {
            logger.error("创建函数对象时发生错误", e);
            return null;
        }
    }

    @Override
    public void reset() {
        // 函数模式无需重置状态
    }
}
