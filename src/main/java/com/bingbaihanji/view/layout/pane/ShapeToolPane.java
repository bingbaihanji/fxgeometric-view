package com.bingbaihanji.view.layout.pane;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.util.I18nRefreshable;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.util.StyleManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;

/**
 * 图形工具面板（左侧），实现 {@link I18nRefreshable} 支持语言切换
 *
 * @author bingbaihanji
 * @date 2025-12-20
 * @updated 2026-06-23 实现 I18nRefreshable，语言切换时无需重建界面
 */
public class ShapeToolPane extends VBox implements I18nRefreshable {

    /** 组件 / Tooltip → i18n key 映射 */
    private final Map<Object, String> i18nKeyMap = new HashMap<>();

    private final ObjectProperty<DrawMode> drawMode =
            new SimpleObjectProperty<>(DrawMode.NONE);

    private Runnable onUndo;
    private Runnable onRedo;
    private Runnable onClear;
    private Runnable onFunctionClick;
    private Runnable onRegularPolygonClick;

    /* 构造 */

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
                createTool("geo.ellipse", DrawMode.ELLIPSE, group),
                createTool("geo.polygon", DrawMode.POLYGON, group),
                createTool("geo.regularPolygon", DrawMode.REGULAR_POLYGON, group),
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

    // ======================== I18nRefreshable ========================

    @Override
    public void refreshI18n() {
        for (var entry : i18nKeyMap.entrySet()) {
            String text = I18nUtil.getString(entry.getValue());
            if (entry.getKey() instanceof Labeled labeled) {
                labeled.setText(text);
            } else if (entry.getKey() instanceof Tooltip tooltip) {
                tooltip.setText(text);
            }
        }
    }

    // ======================== i18n 注册辅助 ========================

    /**
     * 为 Labeled（Label / Button 等）设置 i18n 文本并注册到刷新映射
     */
    private void registerI18n(Labeled labeled, String key) {
        labeled.setText(I18nUtil.getString(key));
        i18nKeyMap.put(labeled, key);
    }

    /**
     * 为 Tooltip 设置 i18n 文本并注册
     */
    private void registerTooltip(Tooltip tooltip, String key) {
        tooltip.setText(I18nUtil.getString(key));
        i18nKeyMap.put(tooltip, key);
    }

    // ======================== UI 构建方法 ========================

    /**
     * 分组标题 + 内容
     */
    private VBox createSection(String titleKey, Node content) {
        Label title = new Label();
        registerI18n(title, titleKey);
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
     * 单个工具按钮（图标 + 文本）
     */
    private ToggleButton createTool(String textKey, DrawMode mode, ToggleGroup group) {
        Node iconNode = loadIconNode(textKey);

        Label text = new Label();
        registerI18n(text, textKey);

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

        Tooltip tooltip = new Tooltip();
        registerTooltip(tooltip, textKey);
        tooltip.setStyle(StyleManager.getTooltipStyle());
        Tooltip.install(button, tooltip);

        button.setOnAction(e -> {
            if (button.isSelected()) {
                if (mode == DrawMode.REGULAR_POLYGON && onRegularPolygonClick != null) {
                    onRegularPolygonClick.run();
                }
                drawMode.set(mode);
            } else {
                drawMode.set(DrawMode.NONE);
            }
        });

        return button;
    }

    /**
     * 创建动作按钮（不是切换按钮）
     */
    private Button createActionButton(String textKey, Runnable action) {
        Node iconNode = loadIconNode(textKey);

        Label text = new Label();
        registerI18n(text, textKey);

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

        Tooltip tooltip = new Tooltip();
        registerTooltip(tooltip, textKey);
        tooltip.setStyle(StyleManager.getTooltipStyle());
        Tooltip.install(button, tooltip);

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

    /**
     * 获取图标文件路径
     */
    private String getIconPath(String textKey) {
        return switch (textKey) {
            case "geo.point" -> "icon/point.png";
            case "geo.segment" -> "icon/segment.png";
            case "geo.circle" -> "icon/circle.png";
            case "geo.ellipse" -> "icon/elliptic.png";
            case "geo.polygon" -> "icon/rectangle.png";
            case "geo.regularPolygon" -> "icon/regularPolygons.png";
            case "geo.restore" -> "icon/restore.png";
            case "geo.revoke" -> "icon/revoke.png";
            case "geo.empty" -> "icon/empty.png";
            case "geo.line" -> "icon/line.png";
            case "geo.handpainted" -> "icon/handpainted.png";
            case "geo.constrainedPoint" -> "icon/point.png";
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

    // ======================== 公开方法 ========================

    public ObjectProperty<DrawMode> drawModeProperty() {
        return drawMode;
    }

    public DrawMode getDrawMode() {
        return drawMode.get();
    }

    public void setOnUndo(Runnable callback) {
        this.onUndo = callback;
    }

    public void setOnRedo(Runnable callback) {
        this.onRedo = callback;
    }

    public void setOnClear(Runnable callback) {
        this.onClear = callback;
    }

    public void setOnFunctionClick(Runnable callback) {
        this.onFunctionClick = callback;
    }

    public void setOnRegularPolygonClick(Runnable callback) {
        this.onRegularPolygonClick = callback;
    }

    private void handleUndo() {
        if (onUndo != null) {
            onUndo.run();
        }
    }

    private void handleRedo() {
        if (onRedo != null) {
            onRedo.run();
        }
    }

    private void handleClear() {
        if (onClear != null) {
            onClear.run();
        }
    }

    private void handleFunctionClick() {
        if (onFunctionClick != null) {
            onFunctionClick.run();
        }
    }
}
