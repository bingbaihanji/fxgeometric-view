package com.bingbaihanji.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface FxTools {


    // 截图
    static void screenshots(Stage primaryStage, Node node) {

        // 创建快照
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT); // 使用透明背景
        WritableImage image = node.snapshot(snapshotParameters, null);

        // 保存截图到剪切板
        Clipboard systemClipboard = Clipboard.getSystemClipboard(); // 获取系统剪切板
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putImage(image);
        systemClipboard.setContent(clipboardContent);

        // 保存到文件
        BufferedImage png = SwingFXUtils.fromFXImage(image, null);
        FileChooser fileChooser = new FileChooser();
        // 设置默认文件名：当前日期时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss.SSS");
        fileChooser.setInitialFileName(formatter.format(now));

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image File", ".png", "*.png")
        );
        File save = fileChooser.showSaveDialog(primaryStage);
        if (save != null) {
            try {
                ImageIO.write(png, "png", save);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
