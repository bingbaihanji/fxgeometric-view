package com.binbaihanji.view;

import javafx.application.Application;
import javafx.stage.Stage;

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
    }

    @Override
    public void start(Stage stage) throws Exception {
        Stage init = new InitView(stage).init();

        init.show();
    }
}
