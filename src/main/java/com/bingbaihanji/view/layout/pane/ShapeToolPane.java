package com.bingbaihanji.view.layout.pane;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.util.StyleManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.net.URL;

/**
 * 图形工具面板（左侧）
 *
 * @author bingbaihanji
 */
public class ShapeToolPane extends VBox {

    /*状态属性*/

    private final ObjectProperty<DrawMode> drawMode =
            new SimpleObjectProperty<>(DrawMode.NONE);

    /**
     * 撤销回调
     */
    private Runnable onUndo;

    /**
     * 恢复回调
     */
    private Runnable onRedo;

    /**
     * 清空回调
     */
    private Runnable onClear;

    /**
     * 函数绘制回调
     */
    private Runnable onFunctionClick;



    /*构造*/

    public ShapeToolPane() {

        setPrefWidth(200);
        setMinWidth(180);
        setPadding(new Insets(12));
        setSpacing(8);
        setStyle(StyleManager.getPanelStyle());

        ToggleGroup group = new ToggleGroup();
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 0;");

        /*  基本工具  */

        TilePane basicTools = createToolGrid();
        basicTools.getChildren().addAll(
                createTool("geo.point", DrawMode.POINT, group),
                createTool("geo.segment", DrawMode.LINE, group),
                createTool("geo.line", DrawMode.INFINITE_LINE, group),
                createTool("geo.circle", DrawMode.CIRCLE, group),
                createTool("geo.polygon", DrawMode.POLYGON, group),
                createTool("geo.handpainted", DrawMode.FREEHAND, group)
        );

        content.getChildren().add(
                createSection("geo.section.basic", basicTools)
        );

        /*  编辑  */

        TilePane editTools = createToolGrid();
        editTools.getChildren().addAll(
                createActionButton("geo.revoke", this::handleUndo),
                createActionButton("geo.restore", this::handleRedo),
                createActionButton("geo.empty", this::handleClear)
        );

        content.getChildren().add(
                createSection("geo.section.edit", editTools)
        );

        /*  作图  */

        TilePane drawTools = createToolGrid();
        drawTools.getChildren().addAll(
                createTool("geo.midpoint", DrawMode.MIDPOINT, group),
                createTool("geo.perpendicular", DrawMode.PERPENDICULAR, group),
                createTool("geo.perpendicularBisector", DrawMode.PERPENDICULAR_BISECTOR, group),
                createTool("geo.parallel", DrawMode.PARALLEL, group),
                createTool("geo.tangent", DrawMode.TANGENT, group),
                createTool("geo.rotating", DrawMode.ROTATE, group),
                createActionButton("geo.function", this::handleFunctionClick)

        );

        content.getChildren().add(
                createSection("geo.section.draw", drawTools)
        );

        /*  Scroll  */

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(StyleManager.getScrollPaneStyle());

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    /**
     * 获取图标文件路径
     */
    private String getIconPath(String textKey) {
        return switch (textKey) {
            case "geo.point" -> "icon/point.png";
            case "geo.segment" -> "icon/segment.png";
            case "geo.circle" -> "icon/circle.png";
            case "geo.polygon" -> "icon/rectangle.png";
            case "geo.restore" -> "icon/restore.png";
            case "geo.revoke" -> "icon/revoke.png";
            case "geo.empty" -> "icon/empty.png";
            case "geo.line" -> "icon/line.png";
            case "geo.handpainted" -> "icon/handpainted.png";
            case "geo.constrainedPoint" -> "icon/point.png"; // 暂时使用普通点图标
            case "geo.midpoint" -> "icon/midpoint.png";
            case "geo.perpendicular" -> "icon/perpendicular.png";
            case "geo.perpendicularBisector" -> "icon/perpendicularBisector.png";
            case "geo.parallel" -> "icon/parallel.png";
            case "geo.tangent" -> "icon/tangent.png";
            case "geo.rotating" -> "icon/rotating.png";
            case "geo.function" -> "icon/function.png";
            default -> null;
        };
    }



    /*UI 构建方法*/

    /**
     * 分组标题 + 内容
     */
    private VBox createSection(String titleKey, Node content) {
        Label title = new Label(I18nUtil.getString(titleKey));
        title.setStyle(StyleManager.getSectionTitleStyle());

        VBox separator = new VBox();
        separator.setStyle(StyleManager.getSeparatorStyle());
        separator.setPrefHeight(1);

        VBox box = new VBox(6, title, separator, content);
        box.setPadding(new Insets(0, 0, 8, 0));
        box.setStyle("-fx-spacing: 6;");
        return box;
    }

    /**
     * 工具按钮网格
     */
    private TilePane createToolGrid() {
        TilePane pane = new TilePane();
        pane.setPrefColumns(3);
        pane.setHgap(6);
        pane.setVgap(6);
        pane.setPadding(new Insets(0));
        pane.setStyle("-fx-padding: 0;");
        return pane;
    }

    /**
     * 创建动作按钮（不是切换按钮）
     */
    private Button createActionButton(String textKey, Runnable action) {
        String tooltipText = I18nUtil.getString(textKey);
        Node iconNode = loadIconNode(textKey);

        Label text = new Label(tooltipText);
        text.setStyle(StyleManager.getTextLabelStyle());

        VBox graphic = new VBox(3, iconNode, text);
        graphic.setAlignment(Pos.CENTER);
        graphic.setPrefSize(50, 50);

        Button button = new Button();
        button.setGraphic(graphic);
        button.setPrefSize(60, 65);
        button.setMinSize(60, 65);
        button.setMaxSize(60, 65);
        button.setFocusTraversable(false);

        button.setStyle(StyleManager.getButtonStyle("normal"));

        button.setOnMouseEntered(e -> {
            button.setStyle(StyleManager.getButtonStyle("hover"));
        });

        button.setOnMouseExited(e -> {
            button.setStyle(StyleManager.getButtonStyle("normal"));
        });

        button.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });

        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle(StyleManager.getTooltipStyle());
        Tooltip.install(button, tooltip);

        return button;
    }

    /**
     * 单个工具按钮（图标 + 文本）
     */
    private ToggleButton createTool(
            String textKey,
            DrawMode mode,
            ToggleGroup group
    ) {
        String tooltipText = I18nUtil.getString(textKey);
        Node iconNode = loadIconNode(textKey);

        Label text = new Label(tooltipText);
        text.setStyle(StyleManager.getTextLabelStyle());

        VBox graphic = new VBox(3, iconNode, text);
        graphic.setAlignment(Pos.CENTER);
        graphic.setPrefSize(50, 50);

        ToggleButton button = new ToggleButton();
        button.setGraphic(graphic);
        button.setToggleGroup(group);
        button.setPrefSize(60, 65);
        button.setMinSize(60, 65);
        button.setMaxSize(60, 65);
        button.setFocusTraversable(false);

        button.setStyle(StyleManager.getButtonStyle("normal"));

        button.setOnMouseEntered(e -> {
            if (!button.isSelected()) {
                button.setStyle(StyleManager.getButtonStyle("hover"));
            }
        });

        button.setOnMouseExited(e -> {
            if (!button.isSelected()) {
                button.setStyle(StyleManager.getButtonStyle("normal"));
            }
        });

        button.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                button.setStyle(StyleManager.getButtonStyle("selected"));
            } else {
                button.setStyle(StyleManager.getButtonStyle("normal"));
            }
        });

        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle(StyleManager.getTooltipStyle());
        Tooltip.install(button, tooltip);

        button.setOnAction(e ->
                drawMode.set(button.isSelected() ? mode : DrawMode.NONE)
        );

        return button;
    }

    /**
     * 加载图标节点
     */
    private Node loadIconNode(String textKey) {
        String iconPath = getIconPath(textKey);

        ImageView iconView = new ImageView();
        iconView.setFitWidth(24);
        iconView.setFitHeight(24);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        if (iconPath != null) {
            try {
                URL url = getClass().getResource("/" + iconPath);
                if (url != null) {
                    iconView.setImage(new Image(url.toExternalForm()));
                    return iconView;
                } else {
                    System.err.println("Icon not found: /" + iconPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Label fallback = new Label("◉");
        fallback.setStyle(StyleManager.getFallbackLabelStyle());
        return fallback;
    }


    public ObjectProperty<DrawMode> drawModeProperty() {
        return drawMode;
    }

    public DrawMode getDrawMode() {
        return drawMode.get();
    }

    /**
     * 设置撤销回调
     */
    public void setOnUndo(Runnable callback) {
        this.onUndo = callback;
    }

    /**
     * 设置恢复回调
     */
    public void setOnRedo(Runnable callback) {
        this.onRedo = callback;
    }

    /**
     * 处理撤销
     */
    private void handleUndo() {
        if (onUndo != null) {
            onUndo.run();
        }
    }

    /**
     * 处理恢复
     */
    private void handleRedo() {
        if (onRedo != null) {
            onRedo.run();
        }
    }

    /**
     * 设置清空回调
     */
    public void setOnClear(Runnable callback) {
        this.onClear = callback;
    }

    /**
     * 处理清空
     */
    private void handleClear() {
        if (onClear != null) {
            onClear.run();
        }
    }

    /**
     * 设置函数绘制回调
     */
    public void setOnFunctionClick(Runnable callback) {
        this.onFunctionClick = callback;
    }

    /**
     * 处理函数点击
     */
    private void handleFunctionClick() {
        if (onFunctionClick != null) {
            onFunctionClick.run();
        }
    }


}
