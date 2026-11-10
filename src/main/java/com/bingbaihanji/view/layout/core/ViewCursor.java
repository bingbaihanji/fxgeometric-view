package com.bingbaihanji.view.layout.core;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import java.net.URL;

/**
 * 视图光标管理
 * <p>
 * 加载自定义光标图片并应用到画布; 图片缺失时回落到手型光标.
 */
public class ViewCursor {

    /**
     * 默认光标
     */
    private Cursor defaultCursor = Cursor.DEFAULT;

    /**
     * 安装自定义光标
     *
     * @param pane      目标容器
     * @param imagePath 光标图片路径(classpath)
     */
    public void install(Pane pane, String imagePath) {
        Cursor loaded = Cursor.HAND;
        URL url = ViewCursor.class.getResource(imagePath);
        if (url != null) {
            loaded = new ImageCursor(new Image(url.toExternalForm()), 0, 0);
        }
        final Cursor applied = loaded;
        defaultCursor = applied;
        pane.setOnMouseEntered(e -> pane.setCursor(applied));
        pane.setOnMouseExited(e -> pane.setCursor(applied));
    }

    /**
     * 获取默认光标
     *
     * @return 自定义光标, 未安装时为默认光标
     */
    public Cursor getDefaultCursor() {
        return defaultCursor;
    }
}
