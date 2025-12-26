package com.bingbaihanji.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;


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
        super.stop();
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    Platform.exit();
                    System.out.println("程序已退出");
                })
        );

    }

    @Override
    public void start(Stage stage) throws Exception {
        Stage init = new InitView(stage).init();
        init.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/logo.png")).toExternalForm()));
        init.show();
    }
}
