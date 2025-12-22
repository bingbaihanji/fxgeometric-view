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
    exports com.binbaihanji;
    exports com.binbaihanji.view;
    exports com.binbaihanji.util;
    exports com.binbaihanji.view.layout.draw.geometry;
    exports com.binbaihanji.view.layout.draw.tools;
}