module FXGeometricView {
    // JavaFX 模块
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.graphics;

    // 日志模块
    requires org.slf4j;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;

    // Lombok
//    requires static lombok;

    // 导出包给外部模块使用
    exports com.bingbaihanji;
    exports com.bingbaihanji.view;
    exports com.bingbaihanji.util;
    exports com.bingbaihanji.view.layout.draw.geometry;
    exports com.bingbaihanji.view.layout.draw.tools;
}