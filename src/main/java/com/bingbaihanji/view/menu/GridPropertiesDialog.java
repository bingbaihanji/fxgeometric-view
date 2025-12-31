package com.bingbaihanji.view.menu;

import com.bingbaihanji.constant.GridType;
import com.bingbaihanji.constant.LineType;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.view.layout.core.EuclidianViewSettings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 网格属性对话框
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 配置网格的各种属性
 */
public class GridPropertiesDialog extends Dialog<ButtonType> {

    private final EuclidianViewSettings settings;

    // UI控件
    private CheckBox cbShowGrid;
    private ComboBox<GridType> cmbGridType;
    private CheckBox cbAutoGridDistance;
    private TextField tfGridDistance;
    private TextField tfPolarAngleStep;
    private ComboBox<LineType> cmbLineType;
    private ColorPicker gridColorPicker;
    private ColorPicker subGridColorPicker;

    // 新增：同步相关控件
    private CheckBox cbSyncGridWithAxes;
    private Slider sliderGridDistFactor;
    private Label lblGridDistFactorValue;

    public GridPropertiesDialog(EuclidianViewSettings settings) {
        this.settings = settings;

        setTitle(I18nUtil.getString("menu.gridProperties"));
        setHeaderText(I18nUtil.getString("menu.gridProperties.header"));

        // 创建UI
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        content.getChildren().addAll(
                createVisibilitySection(),
                new Separator(),
                createTypeSection(),
                new Separator(),
                createDistanceSection(),
                new Separator(),
                createStyleSection()
        );

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 加载当前配置
        loadSettings();

        // 监听网格类型变化，显示/隐藏相关选项
        cmbGridType.valueProperty().addListener((obs, old, val) -> {
            updateUIForGridType(val);
        });

        // OK按钮处理
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                saveSettings();
            }
            return buttonType;
        });
    }

    /**
     * 创建可见性设置区
     */
    private VBox createVisibilitySection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("grid.visibility"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        cbShowGrid = new CheckBox(I18nUtil.getString("grid.showGrid"));

        section.getChildren().addAll(title, cbShowGrid);
        return section;
    }

    /**
     * 创建网格类型设置区
     */
    private VBox createTypeSection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("grid.type"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        Label lblType = new Label(I18nUtil.getString("grid.gridType") + ":");
        cmbGridType = new ComboBox<>();
        cmbGridType.getItems().addAll(GridType.values());
        cmbGridType.setPrefWidth(200);

        grid.add(lblType, 0, 0);
        grid.add(cmbGridType, 1, 0);

        section.getChildren().addAll(title, grid);
        return section;
    }

    /**
     * 创建间距设置区
     */
    private VBox createDistanceSection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("grid.distance"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        int row = 0;

        // 网格与坐标轴同步（新增）
        Label lblSync = new Label(I18nUtil.getString("grid.syncWithAxes") + ":");
        cbSyncGridWithAxes = new CheckBox(I18nUtil.getString("grid.enableSync"));
        cbSyncGridWithAxes.setTooltip(new Tooltip(I18nUtil.getString("grid.syncWithAxes.hint")));

        grid.add(lblSync, 0, row);
        grid.add(cbSyncGridWithAxes, 1, row);
        row++;

        // 网格密度因子（新增，仅在同步模式下启用）
        Label lblFactor = new Label(I18nUtil.getString("grid.distanceFactor") + ":");
        sliderGridDistFactor = new Slider(0.1, 5.0, 1.0);
        sliderGridDistFactor.setShowTickLabels(false);
        sliderGridDistFactor.setShowTickMarks(true);
        sliderGridDistFactor.setMajorTickUnit(1.0);
        sliderGridDistFactor.setMinorTickCount(9);
        sliderGridDistFactor.setPrefWidth(150);

        lblGridDistFactorValue = new Label("1.0");
        lblGridDistFactorValue.setStyle("-fx-font-weight: bold;");

        Label lblFactorHint = new Label("(1.0 = " + I18nUtil.getString("grid.factorHint") + ")");
        lblFactorHint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        HBox factorBox = new HBox(10, sliderGridDistFactor, lblGridDistFactorValue, lblFactorHint);

        grid.add(lblFactor, 0, row);
        grid.add(factorBox, 1, row);
        row++;

        // 监听滑块变化
        sliderGridDistFactor.valueProperty().addListener((obs, oldV, newV) -> {
            lblGridDistFactorValue.setText(String.format("%.1f", newV.doubleValue()));
        });

        // 网格间距（手动模式）
        Label lblDistance = new Label(I18nUtil.getString("grid.gridDistance") + ":");
        cbAutoGridDistance = new CheckBox(I18nUtil.getString("grid.auto"));
        tfGridDistance = new TextField();
        tfGridDistance.setPrefWidth(80);

        HBox distanceBox = new HBox(5, cbAutoGridDistance, tfGridDistance);

        grid.add(lblDistance, 0, row);
        grid.add(distanceBox, 1, row);
        row++;

        // 极坐标角度步长（仅极坐标网格显示）
        Label lblAngle = new Label(I18nUtil.getString("grid.polarAngle") + ":");
        tfPolarAngleStep = new TextField();
        tfPolarAngleStep.setPrefWidth(80);
        Label lblAngleHint = new Label("(" + I18nUtil.getString("grid.polarAngleHint") + ")");
        lblAngleHint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        HBox angleBox = new HBox(5, tfPolarAngleStep, lblAngleHint);

        grid.add(lblAngle, 0, row);
        grid.add(angleBox, 1, row);
        row++;

        // 动态UI逻辑：同步模式下禁用手动间距，启用密度因子
        cbSyncGridWithAxes.selectedProperty().addListener((obs, old, val) -> {
            updateDistanceControlsState();
        });

        cbAutoGridDistance.selectedProperty().addListener((obs, old, val) -> {
            tfGridDistance.setDisable(val);
        });

        section.getChildren().addAll(title, grid);
        return section;
    }

    /**
     * 更新间距控件的启用/禁用状态
     */
    private void updateDistanceControlsState() {
        boolean isSyncEnabled = cbSyncGridWithAxes.isSelected();

        // 同步模式下：启用密度因子，禁用手动间距
        sliderGridDistFactor.setDisable(!isSyncEnabled);
        lblGridDistFactorValue.setDisable(!isSyncEnabled);

        if (isSyncEnabled) {
            // 同步模式下强制使用自动间距
            cbAutoGridDistance.setSelected(true);
            cbAutoGridDistance.setDisable(true);
            tfGridDistance.setDisable(true);
        } else {
            // 独立模式下恢复手动控制
            cbAutoGridDistance.setDisable(false);
            tfGridDistance.setDisable(cbAutoGridDistance.isSelected());
        }
    }

    /**
     * 创建样式设置区
     */
    private VBox createStyleSection() {
        VBox section = new VBox(10);

        Label title = new Label(I18nUtil.getString("grid.style"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // 线型
        Label lblLineType = new Label(I18nUtil.getString("grid.lineType") + ":");
        cmbLineType = new ComboBox<>();
        cmbLineType.getItems().addAll(LineType.values());
        cmbLineType.setPrefWidth(150);

        grid.add(lblLineType, 0, 0);
        grid.add(cmbLineType, 1, 0);

        // 主网格颜色
        Label lblColor = new Label(I18nUtil.getString("grid.color") + ":");
        gridColorPicker = new ColorPicker();
        gridColorPicker.setPrefWidth(150);

        grid.add(lblColor, 0, 1);
        grid.add(gridColorPicker, 1, 1);

        // 次网格颜色（仅带次网格模式显示）
        Label lblSubColor = new Label(I18nUtil.getString("grid.subColor") + ":");
        subGridColorPicker = new ColorPicker();
        subGridColorPicker.setPrefWidth(150);

        grid.add(lblSubColor, 0, 2);
        grid.add(subGridColorPicker, 1, 2);

        section.getChildren().addAll(title, grid);
        return section;
    }

    /**
     * 根据网格类型更新UI可见性
     */
    private void updateUIForGridType(GridType gridType) {
        // 极坐标网格时显示角度步长输入
        boolean isPolar = (gridType == GridType.POLAR);
        tfPolarAngleStep.setVisible(isPolar);
        tfPolarAngleStep.setManaged(isPolar);

        // 带次网格模式时显示次网格颜色
        boolean hasSubGrid = (gridType == GridType.CARTESIAN_WITH_SUBGRID);
        subGridColorPicker.setVisible(hasSubGrid);
        subGridColorPicker.setManaged(hasSubGrid);
    }

    /**
     * 加载当前设置到UI
     */
    private void loadSettings() {
        cbShowGrid.setSelected(settings.isShowGrid());
        cmbGridType.setValue(settings.getGridType());

        // 新增：加载同步设置
        cbSyncGridWithAxes.setSelected(settings.isSyncGridWithAxes());
        sliderGridDistFactor.setValue(settings.getGridDistanceFactor());
        lblGridDistFactorValue.setText(String.format("%.1f", settings.getGridDistanceFactor()));

        cbAutoGridDistance.setSelected(settings.isAutoGridDistance());
        tfGridDistance.setText(String.valueOf(settings.getGridDistance()));
        tfGridDistance.setDisable(settings.isAutoGridDistance());

        // 极坐标角度步长（转换为度数显示）
        double angleDegrees = Math.toDegrees(settings.getPolarAngleStep());
        tfPolarAngleStep.setText(String.valueOf((int) angleDegrees));

        cmbLineType.setValue(settings.getGridLineType());
        gridColorPicker.setValue(settings.getGridColor());
        subGridColorPicker.setValue(settings.getSubGridColor());

        // 初始化UI可见性
        updateUIForGridType(settings.getGridType());
        // 初始化间距控件状态
        updateDistanceControlsState();
    }

    /**
     * 保存UI设置到配置
     */
    private void saveSettings() {
        settings.setShowGrid(cbShowGrid.isSelected());
        settings.setGridType(cmbGridType.getValue());

        // 新增：保存同步设置
        settings.setSyncGridWithAxes(cbSyncGridWithAxes.isSelected());
        settings.setGridDistanceFactor(sliderGridDistFactor.getValue());

        settings.setAutoGridDistance(cbAutoGridDistance.isSelected());
        if (!cbAutoGridDistance.isSelected()) {
            try {
                settings.setGridDistance(Double.parseDouble(tfGridDistance.getText()));
            } catch (NumberFormatException e) {
                // 保持原值
            }
        }

        // 保存极坐标角度步长（转换为弧度）
        if (cmbGridType.getValue() == GridType.POLAR) {
            try {
                double angleDegrees = Double.parseDouble(tfPolarAngleStep.getText());
                settings.setPolarAngleStep(Math.toRadians(angleDegrees));
            } catch (NumberFormatException e) {
                // 保持原值
            }
        }

        settings.setGridLineType(cmbLineType.getValue());
        settings.setGridColor(gridColorPicker.getValue());
        settings.setSubGridColor(subGridColorPicker.getValue());
    }
}
