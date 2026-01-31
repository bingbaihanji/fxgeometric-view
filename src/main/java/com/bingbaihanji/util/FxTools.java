package com.bingbaihanji.util;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX 工具类
 * <p>
 * 提供 JavaFX 常用的通用工具方法，包括：
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public interface FxTools {

    //  截图与导出 

    /**
     * 截图功能：截取节点内容，保存到剪切板并允许保存为文件
     *
     * @param primaryStage 主舞台
     * @param node         要截图的节点
     */
    static void screenshots(Stage primaryStage, Node node) {

        // 创建快照
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT); // 使用透明背景
        WritableImage image = node.snapshot(snapshotParameters, null);

        // 保存截图到剪切板
        copyImageToClipboard(image);

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


    /**
     * 将图片复制到剪切板
     *
     * @param image 图片
     */
    static void copyImageToClipboard(WritableImage image) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putImage(image);
        clipboard.setContent(content);
    }


    /**
     * 快照节点为图片
     *
     * @param node        要截图的节点
     * @param transparent 是否使用透明背景
     * @return WritableImage 图片对象
     */
    static WritableImage snapshotNode(Node node, boolean transparent) {
        SnapshotParameters params = new SnapshotParameters();
        if (transparent) {
            params.setFill(Color.TRANSPARENT);
        }
        return node.snapshot(params, null);
    }

    /**
     * 创建配置好的 FileChooser
     *
     * @param title           标题
     * @param initialFileName 默认文件名
     * @param description     文件类型描述
     * @param extensions      文件扩展名（例如："*.png", ".png"）
     * @return 配置好的 FileChooser
     */
    static FileChooser createFileChooser(String title, String initialFileName, String description, String... extensions) {
        FileChooser fileChooser = new FileChooser();
        if (title != null) {
            fileChooser.setTitle(title);
        }
        if (initialFileName != null) {
            fileChooser.setInitialFileName(initialFileName);
        }
        if (description != null && extensions != null && extensions.length > 0) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(description, extensions)
            );
        }
        return fileChooser;
    }


    //  对话框工具

    /**
     * 为 Dialog 设置图标（通用方法）
     * <p>
     * 使用监听器在对话框显示后获取 Stage 并设置图标
     *
     * @param dialog   对话框
     * @param iconPath 图标路径（相对于 resources 目录，例如："/icon/setting.png"）
     */
    static void setDialogIcon(Dialog<?> dialog, String iconPath) {
        setDialogIcon(dialog, iconPath, null);
    }

    /**
     * 为 Dialog 设置图标（通用方法）
     * <p>
     * 使用监听器在对话框显示后获取 Stage 并设置图标
     *
     * @param dialog        对话框
     * @param iconPath      图标路径（相对于 resources 目录，例如："/icon/setting.png"）
     * @param resourceClass 用于加载资源的类（如果为 null，使用 FxTools.class）
     */
    static void setDialogIcon(Dialog<?> dialog, String iconPath, Class<?> resourceClass) {
        dialog.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // 对话框正在显示
                Window window = dialog.getDialogPane().getScene().getWindow();
                if (window instanceof Stage stage) {
                    try {
                        Class<?> loader = resourceClass != null ? resourceClass : FxTools.class;
                        var iconUrl = loader.getResource(iconPath);
                        if (iconUrl != null) {
                            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
                        }
                    } catch (Exception e) {
                        Logger.getLogger(FxTools.class).warn("加载对话框图标失败: " + iconPath, e);
                    }
                }
            }
        });
    }

    //  Alert 快捷创建 

    /**
     * 显示错误对话框
     *
     * @param title   标题
     * @param message 消息
     */
    static void showErrorAlert(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, null, message);
    }

    /**
     * 显示错误对话框（带异常堆栈）
     *
     * @param title   标题
     * @param message 消息
     * @param e       异常
     */
    static void showErrorAlert(String title, String message, Throwable e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.setContentText(e.getMessage());

            // 添加详细异常信息
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            alert.getDialogPane().setExpandableContent(textArea);
            alert.showAndWait();
        });
    }

    /**
     * 显示警告对话框
     *
     * @param title   标题
     * @param message 消息
     */
    static void showWarningAlert(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, null, message);
    }

    /**
     * 显示信息对话框
     *
     * @param title   标题
     * @param message 消息
     */
    static void showInfoAlert(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, null, message);
    }

    /**
     * 显示确认对话框
     *
     * @param title   标题
     * @param message 消息
     * @return 用户是否点击了 OK
     */
    static boolean showConfirmAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * 通用 Alert 创建方法
     *
     * @param type       类型
     * @param title      标题
     * @param headerText 头部文本
     * @param content    内容
     */
    static void showAlert(Alert.AlertType type, String title, String headerText, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(headerText);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    //  Tooltip 工具 

    /**
     * 创建并安装 Tooltip
     *
     * @param node 节点
     * @param text 提示文本
     */
    static void installTooltip(Node node, String text) {
        if (text != null && !text.isEmpty()) {
            Tooltip tooltip = new Tooltip(text);
            Tooltip.install(node, tooltip);
        }
    }

    /**
     * 创建带样式的 Tooltip
     *
     * @param node  节点
     * @param text  提示文本
     * @param style CSS 样式
     */
    static void installTooltip(Node node, String text, String style) {
        if (text != null && !text.isEmpty()) {
            Tooltip tooltip = new Tooltip(text);
            if (style != null) {
                tooltip.setStyle(style);
            }
            Tooltip.install(node, tooltip);
        }
    }


}
