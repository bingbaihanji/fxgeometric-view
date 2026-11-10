module FXGeometricView {
    // JavaFX 模块
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.graphics;

    // 日志模块
    requires org.slf4j;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;
    requires commons.math3;
    requires exp4j;
    requires java.logging;
    requires static lombok;

    // Lombok
//    requires static lombok;

    // 导出包给外部模块使用
    exports com.bingbaihanji;
    exports com.bingbaihanji.view;
    exports com.bingbaihanji.util;
    exports com.bingbaihanji.i18n;
    exports com.bingbaihanji.geometry;
    exports com.bingbaihanji.view.layout.draw.tools;
    exports com.bingbaihanji.view.layout.draw.coordinate;
    exports com.bingbaihanji.view.layout.draw.coordinate.grid;
    exports com.bingbaihanji.view.layout.draw.coordinate.render;
}