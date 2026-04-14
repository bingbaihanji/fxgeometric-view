package com.bingbaihanji.view;

import com.bingbaihanji.config.ConfigManager;
import com.bingbaihanji.util.ExceptionHandler;
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
            // 保存用户配置
            ConfigManager.getInstance().saveConfig();

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
            ExceptionHandler.handleExceptionWithDialog(
                    "应用启动失败",
                    "启动应用程序时发生错误,请查看详细信息。",
                    e
            );
            throw e;
        }
    }
}
