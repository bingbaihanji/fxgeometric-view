package com.bingbaihanji.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;


/**
 *
 * @author bingbaihanji
 * @date 2025-12-20 15:18:33
 * @description 首页
 */
public class FXGeometricView extends Application {


    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public void stop() throws Exception {
        // 清理资源
        try {
            // 保存用户配置（如果有）
            // TODO: 实现配置保存逻辑
            
            super.stop();
        } finally {
            Platform.exit();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // 设置应用图标
            URL logoUrl = getClass().getResource("/logo.png");
            if (logoUrl != null) {
                stage.getIcons().add(new Image(logoUrl.toExternalForm()));
            }
            
            // 初始化主界面
            InitView initView = new InitView(stage);
            initView.init();
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("应用启动失败", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
