package com.bingbaihanji.util.test;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * JavaFX 工具类
 * <p>
 * 提供 JavaFX 常用的通用工具方法，包括:
 * <ul>
 *     <li>文件选择器 - 单选/多选文件、目录选择、文件类型过滤</li>
 *     <li>系统剪贴板 - 文本、图像、富文本操作</li>
 *     <li>信息对话框 - 确认、警告、错误、输入对话框</li>
 *     <li>进度提示 - 进度条、无限进度、任务取消</li>
 *     <li>表格操作 - 排序、列宽、复制数据</li>
 *     <li>树形控件 - 展开/折叠、搜索筛选</li>
 *     <li>文本控件 - 查找替换、撤销/重做</li>
 *     <li>窗口管理 - 居中、置顶、模态控制</li>
 *     <li>布局助手 - 动态调整、响应式布局</li>
 *     <li>图像处理 - 缩放、裁剪、格式转换</li>
 *     <li>系统信息 - 屏幕分辨率、多显示器检测</li>
 *     <li>动画效果 - 淡入淡出、滑动、缩放效果</li>
 * </ul>
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public interface FxTools {


    /**
     * 打开单个文件
     *
     * @param owner 父窗口
     * @param title 对话框标题
     * @return 选中的文件，如果取消则返回 null
     */
    static File openFile(Window owner, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        return fileChooser.showOpenDialog(owner);
    }

    /**
     * 打开单个文件（带文件类型过滤）
     *
     * @param owner       父窗口
     * @param title       对话框标题
     * @param description 文件类型描述
     * @param extensions  文件扩展名（例如："*.txt", "*.pdf"）
     * @return 选中的文件，如果取消则返回 null
     */
    static File openFile(Window owner, String title, String description, String... extensions) {
        FileChooser fileChooser = createFileChooser(title, null, description, extensions);
        return fileChooser.showOpenDialog(owner);
    }

    /**
     * 打开多个文件
     *
     * @param owner 父窗口
     * @param title 对话框标题
     * @return 选中的文件列表，如果取消则返回 null
     */
    static List<File> openMultipleFiles(Window owner, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        return fileChooser.showOpenMultipleDialog(owner);
    }

    /**
     * 打开多个文件（带文件类型过滤）
     *
     * @param owner       父窗口
     * @param title       对话框标题
     * @param description 文件类型描述
     * @param extensions  文件扩展名（例如："*.txt", "*.pdf"）
     * @return 选中的文件列表，如果取消则返回 null
     */
    static List<File> openMultipleFiles(Window owner, String title, String description, String... extensions) {
        FileChooser fileChooser = createFileChooser(title, null, description, extensions);
        return fileChooser.showOpenMultipleDialog(owner);
    }

    /**
     * 保存文件
     *
     * @param owner           父窗口
     * @param title           对话框标题
     * @param initialFileName 默认文件名
     * @return 选中的文件路径，如果取消则返回 null
     */
    static File saveFile(Window owner, String title, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        if (initialFileName != null) {
            fileChooser.setInitialFileName(initialFileName);
        }
        return fileChooser.showSaveDialog(owner);
    }

    /**
     * 保存文件（带文件类型过滤）
     *
     * @param owner           父窗口
     * @param title           对话框标题
     * @param initialFileName 默认文件名
     * @param description     文件类型描述
     * @param extensions      文件扩展名（例如："*.txt", "*.pdf"）
     * @return 选中的文件路径，如果取消则返回 null
     */
    static File saveFile(Window owner, String title, String initialFileName, String description, String... extensions) {
        FileChooser fileChooser = createFileChooser(title, initialFileName, description, extensions);
        return fileChooser.showSaveDialog(owner);
    }

    /**
     * 选择目录
     *
     * @param owner 父窗口
     * @param title 对话框标题
     * @return 选中的目录，如果取消则返回 null
     */
    static File chooseDirectory(Window owner, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        return directoryChooser.showDialog(owner);
    }

    /**
     * 选择目录（带初始目录）
     *
     * @param owner            父窗口
     * @param title            对话框标题
     * @param initialDirectory 初始目录
     * @return 选中的目录，如果取消则返回 null
     */
    static File chooseDirectory(Window owner, String title, File initialDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        if (initialDirectory != null && initialDirectory.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }
        return directoryChooser.showDialog(owner);
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

    //  系统剪贴板 

    /**
     * 复制文本到剪贴板
     *
     * @param text 要复制的文本
     */
    static void copyTextToClipboard(String text) {
        if (text != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        }
    }

    /**
     * 从剪贴板获取文本
     *
     * @return 剪贴板中的文本，如果没有则返回 null
     */
    static String getTextFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.getString();
    }

    /**
     * 复制 HTML 到剪贴板
     *
     * @param html HTML 内容
     */
    static void copyHtmlToClipboard(String html) {
        if (html != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putHtml(html);
            clipboard.setContent(content);
        }
    }

    /**
     * 从剪贴板获取 HTML
     *
     * @return 剪贴板中的 HTML，如果没有则返回 null
     */
    static String getHtmlFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.getHtml();
    }

    /**
     * 复制富文本（RTF）到剪贴板
     *
     * @param rtf RTF 内容
     */
    static void copyRtfToClipboard(String rtf) {
        if (rtf != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.put(DataFormat.RTF, rtf);
            clipboard.setContent(content);
        }
    }

    /**
     * 检查剪贴板是否有文本
     *
     * @return true 如果剪贴板包含文本
     */
    static boolean hasTextInClipboard() {
        return Clipboard.getSystemClipboard().hasString();
    }

    /**
     * 检查剪贴板是否有图像
     *
     * @return true 如果剪贴板包含图像
     */
    static boolean hasImageInClipboard() {
        return Clipboard.getSystemClipboard().hasImage();
    }

    /**
     * 从剪贴板获取图像
     *
     * @return 剪贴板中的图像，如果没有则返回 null
     */
    static Image getImageFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.getImage();
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

    //  信息对话框 

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
     * 显示确认对话框（是/否/取消三个按钮）
     *
     * @param title   标题
     * @param message 消息
     * @return 用户选择的按钮类型
     */
    static Optional<ButtonType> showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        return alert.showAndWait();
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

    /**
     * 显示输入对话框
     *
     * @param title        标题
     * @param headerText   头部文本
     * @param contentText  提示文本
     * @param defaultValue 默认值
     * @return 用户输入的文本，如果取消则返回 null
     */
    static String showInputDialog(String title, String headerText, String contentText, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * 显示输入对话框（简化版本）
     *
     * @param title       标题
     * @param contentText 提示文本
     * @return 用户输入的文本，如果取消则返回 null
     */
    static String showInputDialog(String title, String contentText) {
        return showInputDialog(title, null, contentText, "");
    }

    /**
     * 显示选择对话框
     *
     * @param title         标题
     * @param headerText    头部文本
     * @param contentText   提示文本
     * @param choices       选项列表
     * @param defaultChoice 默认选项
     * @param <T>           选项类型
     * @return 用户选择的选项，如果取消则返回 null
     */
    static <T> T showChoiceDialog(String title, String headerText, String contentText,
                                  List<T> choices, T defaultChoice) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        Optional<T> result = dialog.showAndWait();
        return result.orElse(null);
    }

    //  进度提示 

    /**
     * 进度对话框包装类
     */
    class ProgressDialogWrapper {
        private final Dialog<Void> dialog;
        private final ProgressBar progressBar;
        private final ProgressIndicator progressIndicator;
        private final Label messageLabel;
        private final BooleanProperty cancelled = new SimpleBooleanProperty(false);

        public ProgressDialogWrapper(Dialog<Void> dialog, ProgressBar progressBar,
                                     ProgressIndicator progressIndicator, Label messageLabel) {
            this.dialog = dialog;
            this.progressBar = progressBar;
            this.progressIndicator = progressIndicator;
            this.messageLabel = messageLabel;
        }

        /**
         * 更新进度
         *
         * @param progress 进度值（0.0 到 1.0）
         */
        public void updateProgress(double progress) {
            Platform.runLater(() -> {
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                }
                if (progressIndicator != null) {
                    progressIndicator.setProgress(progress);
                }
            });
        }

        /**
         * 更新消息
         *
         * @param message 消息文本
         */
        public void updateMessage(String message) {
            Platform.runLater(() -> {
                if (messageLabel != null) {
                    messageLabel.setText(message);
                }
            });
        }

        /**
         * 关闭对话框
         */
        public void close() {
            Platform.runLater(() -> dialog.close());
        }

        /**
         * 检查是否已取消
         *
         * @return true 如果用户点击了取消
         */
        public boolean isCancelled() {
            return cancelled.get();
        }

        /**
         * 获取取消属性
         *
         * @return 取消属性
         */
        public BooleanProperty cancelledProperty() {
            return cancelled;
        }
    }

    /**
     * 显示进度条对话框
     *
     * @param title   标题
     * @param message 初始消息
     * @return ProgressDialogWrapper 对象，用于更新进度
     */
    static ProgressDialogWrapper showProgressDialog(String title, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        Label messageLabel = new Label(message);
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        vbox.getChildren().addAll(messageLabel, progressBar);

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        ProgressDialogWrapper wrapper = new ProgressDialogWrapper(dialog, progressBar, null, messageLabel);

        // 取消按钮处理
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setOnAction(e -> {
            wrapper.cancelled.set(true);
            dialog.close();
        });

        // 非模态显示
        dialog.show();

        return wrapper;
    }

    /**
     * 显示无限进度指示器
     *
     * @param title   标题
     * @param message 消息
     * @return ProgressDialogWrapper 对象
     */
    static ProgressDialogWrapper showIndeterminateProgress(String title, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        Label messageLabel = new Label(message);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        vbox.getChildren().addAll(messageLabel, progressIndicator);

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        ProgressDialogWrapper wrapper = new ProgressDialogWrapper(dialog, null, progressIndicator, messageLabel);

        // 取消按钮处理
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setOnAction(e -> {
            wrapper.cancelled.set(true);
            dialog.close();
        });

        dialog.show();

        return wrapper;
    }

    //  表格操作 

    /**
     * 自动调整表格列宽
     *
     * @param tableView 表格视图
     */
    static void autoResizeColumns(TableView<?> tableView) {
        Platform.runLater(() -> {
            tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            for (TableColumn<?, ?> column : tableView.getColumns()) {
                Text text = new Text(column.getText());
                double maxWidth = text.getLayoutBounds().getWidth() + 20;

                for (int i = 0; i < tableView.getItems().size(); i++) {
                    if (column.getCellData(i) != null) {
                        text = new Text(column.getCellData(i).toString());
                        double width = text.getLayoutBounds().getWidth() + 20;
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    }
                }
                column.setPrefWidth(maxWidth);
            }
        });
    }

    /**
     * 复制表格选中行的数据到剪贴板（制表符分隔）
     *
     * @param tableView 表格视图
     * @param <T>       数据类型
     */
    static <T> void copySelectedRowsToClipboard(TableView<T> tableView) {
        List<T> selectedItems = tableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        // 添加表头
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            sb.append(tableView.getColumns().get(i).getText());
            if (i < tableView.getColumns().size() - 1) {
                sb.append("\t");
            }
        }
        sb.append("\n");

        // 添加数据行
        for (T item : selectedItems) {
            for (int i = 0; i < tableView.getColumns().size(); i++) {
                Object cellData = tableView.getColumns().get(i).getCellData(item);
                sb.append(cellData != null ? cellData.toString() : "");
                if (i < tableView.getColumns().size() - 1) {
                    sb.append("\t");
                }
            }
            sb.append("\n");
        }

        copyTextToClipboard(sb.toString());
    }

    /**
     * 启用表格多选功能
     *
     * @param tableView 表格视图
     */
    static void enableMultiSelection(TableView<?> tableView) {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * 清空表格选择
     *
     * @param tableView 表格视图
     */
    static void clearTableSelection(TableView<?> tableView) {
        tableView.getSelectionModel().clearSelection();
    }

    /**
     * 对表格进行排序
     *
     * @param tableView 表格视图
     * @param column    要排序的列
     * @param ascending 是否升序
     * @param <T>       数据类型
     */
    static <T> void sortTable(TableView<T> tableView, TableColumn<T, ?> column, boolean ascending) {
        column.setSortType(ascending ? TableColumn.SortType.ASCENDING : TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().clear();
        tableView.getSortOrder().add(column);
    }

    //  树形控件 

    /**
     * 展开树形控件的所有节点
     *
     * @param root 根节点
     * @param <T>  数据类型
     */
    static <T> void expandAllTreeItems(TreeItem<T> root) {
        if (root != null) {
            root.setExpanded(true);
            for (TreeItem<T> child : root.getChildren()) {
                expandAllTreeItems(child);
            }
        }
    }

    /**
     * 折叠树形控件的所有节点
     *
     * @param root 根节点
     * @param <T>  数据类型
     */
    static <T> void collapseAllTreeItems(TreeItem<T> root) {
        if (root != null) {
            root.setExpanded(false);
            for (TreeItem<T> child : root.getChildren()) {
                collapseAllTreeItems(child);
            }
        }
    }

    /**
     * 在树形控件中搜索节点
     *
     * @param root      根节点
     * @param predicate 搜索条件
     * @param <T>       数据类型
     * @return 找到的第一个匹配节点，如果没有则返回 null
     */
    static <T> TreeItem<T> findTreeItem(TreeItem<T> root, Predicate<T> predicate) {
        if (root != null) {
            if (predicate.test(root.getValue())) {
                return root;
            }
            for (TreeItem<T> child : root.getChildren()) {
                TreeItem<T> result = findTreeItem(child, predicate);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 过滤树形控件节点
     *
     * @param root      根节点
     * @param predicate 过滤条件
     * @param <T>       数据类型
     * @return 过滤后的节点列表
     */
    static <T> List<TreeItem<T>> filterTreeItems(TreeItem<T> root, Predicate<T> predicate) {
        return filterTreeItemsRecursive(root, predicate);
    }

    /**
     * 递归过滤树节点（内部方法）
     */
    static <T> List<TreeItem<T>> filterTreeItemsRecursive(TreeItem<T> item, Predicate<T> predicate) {
        List<TreeItem<T>> result = new java.util.ArrayList<>();
        if (item != null) {
            if (predicate.test(item.getValue())) {
                result.add(item);
            }
            for (TreeItem<T> child : item.getChildren()) {
                result.addAll(filterTreeItemsRecursive(child, predicate));
            }
        }
        return result;
    }

    //  文本控件 

    /**
     * 在文本控件中查找文本
     *
     * @param textArea   文本区域
     * @param searchText 要搜索的文本
     * @param fromIndex  起始索引
     * @return 找到的文本索引，如果没有找到则返回 -1
     */
    static int findTextInTextArea(TextArea textArea, String searchText, int fromIndex) {
        String content = textArea.getText();
        return content.indexOf(searchText, fromIndex);
    }

    /**
     * 在文本控件中替换所有文本
     *
     * @param textArea    文本区域
     * @param searchText  要搜索的文本
     * @param replaceText 替换文本
     * @return 替换的次数
     */
    static int replaceAllTextInTextArea(TextArea textArea, String searchText, String replaceText) {
        String content = textArea.getText();
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(searchText, index)) != -1) {
            count++;
            index += searchText.length();
        }
        textArea.setText(content.replace(searchText, replaceText));
        return count;
    }

    /**
     * 高亮显示文本控件中的文本
     *
     * @param textArea   文本区域
     * @param searchText 要高亮的文本
     */
    static void highlightTextInTextArea(TextArea textArea, String searchText) {
        String content = textArea.getText();
        int index = content.indexOf(searchText);
        if (index != -1) {
            textArea.selectRange(index, index + searchText.length());
            textArea.requestFocus();
        }
    }

    /**
     * 为文本控件添加撤销/重做功能（快捷键支持）
     *
     * @param textArea 文本区域
     */
    static void enableUndoRedo(TextArea textArea) {
        textArea.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case Z:
                        textArea.undo();
                        event.consume();
                        break;
                    case Y:
                        textArea.redo();
                        event.consume();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    //  窗口管理 

    /**
     * 将窗口居中显示
     *
     * @param stage 舞台
     */
    static void centerStage(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }

    /**
     * 将窗口在指定屏幕上居中显示
     *
     * @param stage  舞台
     * @param screen 屏幕
     */
    static void centerStageOnScreen(Stage stage, Screen screen) {
        Rectangle2D screenBounds = screen.getVisualBounds();
        stage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - stage.getHeight()) / 2);
    }

    /**
     * 设置窗口置顶
     *
     * @param stage       舞台
     * @param alwaysOnTop 是否置顶
     */
    static void setAlwaysOnTop(Stage stage, boolean alwaysOnTop) {
        stage.setAlwaysOnTop(alwaysOnTop);
    }

    /**
     * 最小化窗口
     *
     * @param stage 舞台
     */
    static void minimizeStage(Stage stage) {
        stage.setIconified(true);
    }

    /**
     * 最大化/恢复窗口
     *
     * @param stage 舞台
     */
    static void toggleMaximize(Stage stage) {
        stage.setMaximized(!stage.isMaximized());
    }

    /**
     * 创建模态窗口
     *
     * @param owner    父窗口
     * @param title    标题
     * @param content  内容节点（必须是 Parent 类型）
     * @param modality 模态类型
     * @return 新创建的舞台
     */
    static Stage createModalStage(Window owner, String title, Region content, Modality modality) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(modality);
        stage.setTitle(title);

        Scene scene = new Scene(content);
        stage.setScene(scene);

        return stage;
    }

    /**
     * 创建模态对话框窗口
     *
     * @param owner   父窗口
     * @param title   标题
     * @param content 内容节点
     * @return 新创建的舞台
     */
    static Stage createModalDialog(Window owner, String title, Region content) {
        return createModalStage(owner, title, content, Modality.APPLICATION_MODAL);
    }

    //  布局助手 

    /**
     * 设置节点的边距
     *
     * @param node   节点
     * @param top    上边距
     * @param right  右边距
     * @param bottom 下边距
     * @param left   左边距
     */
    static void setMargins(Node node, double top, double right, double bottom, double left) {
        if (node.getParent() instanceof VBox) {
            VBox.setMargin(node, new Insets(top, right, bottom, left));
        } else if (node.getParent() instanceof HBox) {
            HBox.setMargin(node, new Insets(top, right, bottom, left));
        } else if (node.getParent() instanceof BorderPane) {
            BorderPane.setMargin(node, new Insets(top, right, bottom, left));
        } else if (node.getParent() instanceof GridPane) {
            GridPane.setMargin(node, new Insets(top, right, bottom, left));
        }
    }

    /**
     * 设置节点的填充
     *
     * @param region 区域
     * @param top    上填充
     * @param right  右填充
     * @param bottom 下填充
     * @param left   左填充
     */
    static void setPadding(Region region, double top, double right, double bottom, double left) {
        region.setPadding(new Insets(top, right, bottom, left));
    }

    /**
     * 绑定节点大小到父容器
     *
     * @param node   节点
     * @param parent 父容器
     */
    static void bindSizeToParent(Region node, Region parent) {
        node.prefWidthProperty().bind(parent.widthProperty());
        node.prefHeightProperty().bind(parent.heightProperty());
    }

    /**
     * 使滚动面板的内容自适应宽度
     *
     * @param scrollPane 滚动面板
     */
    static void fitScrollPaneContent(ScrollPane scrollPane) {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
    }

    /**
     * 隐藏滚动条
     *
     * @param scrollPane 滚动面板
     */
    static void hideScrollBars(ScrollPane scrollPane) {
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    /**
     * 动态调整 GridPane 列宽（平均分配）
     *
     * @param gridPane    网格面板
     * @param columnCount 列数
     */
    static void setGridPaneEqualColumns(GridPane gridPane, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / columnCount);
            gridPane.getColumnConstraints().add(column);
        }
    }

    /**
     * 动态调整 GridPane 行高（平均分配）
     *
     * @param gridPane 网格面板
     * @param rowCount 行数
     */
    static void setGridPaneEqualRows(GridPane gridPane, int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / rowCount);
            gridPane.getRowConstraints().add(row);
        }
    }

    //  图像处理 

    /**
     * 缩放图像
     *
     * @param source        原始图像
     * @param newWidth      新宽度
     * @param newHeight     新高度
     * @param preserveRatio 是否保持宽高比
     * @return 缩放后的图像
     */
    static Image resizeImage(Image source, double newWidth, double newHeight, boolean preserveRatio) {
        ImageView imageView = new ImageView(source);
        imageView.setPreserveRatio(preserveRatio);
        imageView.setFitWidth(newWidth);
        imageView.setFitHeight(newHeight);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return imageView.snapshot(params, null);
    }

    /**
     * 裁剪图像
     *
     * @param source 原始图像
     * @param x      起始 X 坐标
     * @param y      起始 Y 坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图像
     */
    static WritableImage cropImage(Image source, int x, int y, int width, int height) {
        return new WritableImage(source.getPixelReader(), x, y, width, height);
    }

    /**
     * 保存图像到文件
     *
     * @param image      图像
     * @param file       文件
     * @param formatName 格式名称（例如："png", "jpg"）
     * @throws IOException 如果保存失败
     */
    static void saveImage(Image image, File file, String formatName) throws IOException {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bufferedImage, formatName, file);
    }

    /**
     * 从文件加载图像
     *
     * @param file 文件
     * @return 加载的图像
     */
    static Image loadImage(File file) {
        return new Image(file.toURI().toString());
    }

    /**
     * 创建图像预览（缩略图）
     *
     * @param source        原始图像
     * @param thumbnailSize 缩略图大小
     * @return 缩略图
     */
    static Image createThumbnail(Image source, double thumbnailSize) {
        return resizeImage(source, thumbnailSize, thumbnailSize, true);
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

    //  系统信息 

    /**
     * 获取主屏幕分辨率
     *
     * @return 屏幕分辨率（宽x高）
     */
    static Rectangle2D getPrimaryScreenBounds() {
        return Screen.getPrimary().getBounds();
    }

    /**
     * 获取主屏幕可视区域（排除任务栏等）
     *
     * @return 屏幕可视区域
     */
    static Rectangle2D getPrimaryScreenVisualBounds() {
        return Screen.getPrimary().getVisualBounds();
    }

    /**
     * 获取所有屏幕
     *
     * @return 屏幕列表
     */
    static List<Screen> getAllScreens() {
        return Screen.getScreens();
    }

    /**
     * 检测显示器数量
     *
     * @return 显示器数量
     */
    static int getScreenCount() {
        return Screen.getScreens().size();
    }

    /**
     * 获取屏幕 DPI
     *
     * @return DPI 值
     */
    static double getScreenDpi() {
        return Screen.getPrimary().getDpi();
    }

    /**
     * 检测系统是否支持高 DPI
     *
     * @return true 如果 DPI 大于 96
     */
    static boolean isHighDpi() {
        return getScreenDpi() > 96;
    }

    //  动画效果 

    /**
     * 淡入动画
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     */
    static void fadeIn(Node node, double duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * 淡出动画
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     */
    static void fadeOut(Node node, double duration) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.play();
    }

    /**
     * 淡出动画（带回调）
     *
     * @param node       节点
     * @param duration   持续时间（毫秒）
     * @param onFinished 完成后的回调
     */
    static void fadeOut(Node node, double duration, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(Duration.millis(duration), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fade.play();
    }

    /**
     * 淡入淡出切换
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     */
    static void toggleFade(Node node, double duration) {
        if (node.getOpacity() == 0.0) {
            fadeIn(node, duration);
        } else {
            fadeOut(node, duration);
        }
    }

    /**
     * 滑动进入动画（从左侧）
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     * @param distance 滑动距离
     */
    static void slideInFromLeft(Node node, double duration, double distance) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(duration), node);
        transition.setFromX(-distance);
        transition.setToX(0);
        transition.play();
    }

    /**
     * 滑动进入动画（从右侧）
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     * @param distance 滑动距离
     */
    static void slideInFromRight(Node node, double duration, double distance) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(duration), node);
        transition.setFromX(distance);
        transition.setToX(0);
        transition.play();
    }

    /**
     * 滑动进入动画（从上方）
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     * @param distance 滑动距离
     */
    static void slideInFromTop(Node node, double duration, double distance) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(duration), node);
        transition.setFromY(-distance);
        transition.setToY(0);
        transition.play();
    }

    /**
     * 滑动进入动画（从下方）
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     * @param distance 滑动距离
     */
    static void slideInFromBottom(Node node, double duration, double distance) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(duration), node);
        transition.setFromY(distance);
        transition.setToY(0);
        transition.play();
    }

    /**
     * 缩放动画
     *
     * @param node      节点
     * @param duration  持续时间（毫秒）
     * @param fromScale 起始缩放比例
     * @param toScale   结束缩放比例
     */
    static void scale(Node node, double duration, double fromScale, double toScale) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(duration), node);
        scale.setFromX(fromScale);
        scale.setFromY(fromScale);
        scale.setToX(toScale);
        scale.setToY(toScale);
        scale.play();
    }

    /**
     * 弹跳缩放动画
     *
     * @param node     节点
     * @param duration 持续时间（毫秒）
     */
    static void bounceScale(Node node, double duration) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(duration), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.2);
        scale.setToY(1.2);
        scale.setCycleCount(2);
        scale.setAutoReverse(true);
        scale.play();
    }

    /**
     * 旋转动画
     *
     * @param node      节点
     * @param duration  持续时间（毫秒）
     * @param fromAngle 起始角度
     * @param toAngle   结束角度
     */
    static void rotate(Node node, double duration, double fromAngle, double toAngle) {
        RotateTransition rotate = new RotateTransition(Duration.millis(duration), node);
        rotate.setFromAngle(fromAngle);
        rotate.setToAngle(toAngle);
        rotate.play();
    }

    /**
     * 组合动画（顺序播放）
     *
     * @param transitions 动画列表
     * @return SequentialTransition 对象
     */
    static SequentialTransition createSequentialTransition(Transition... transitions) {
        return new SequentialTransition(transitions);
    }

    /**
     * 组合动画（并行播放）
     *
     * @param transitions 动画列表
     * @return ParallelTransition 对象
     */
    static ParallelTransition createParallelTransition(Transition... transitions) {
        return new ParallelTransition(transitions);
    }

    /**
     * 抖动动画
     *
     * @param node 节点
     */
    static void shake(Node node) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(50), node);
        transition.setFromX(0);
        transition.setByX(10);
        transition.setCycleCount(4);
        transition.setAutoReverse(true);
        transition.play();
    }

    /**
     * 脉冲动画（缩放循环）
     *
     * @param node 节点
     */
    static void pulse(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);
        scale.play();
    }

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

    //  线程安全 

    /**
     * 线程安全地在 JavaFX UI 线程中执行
     *
     * @param action 要执行的操作
     */
    static void runLaterSafe(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

}
