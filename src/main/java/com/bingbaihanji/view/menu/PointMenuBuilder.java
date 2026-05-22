package com.bingbaihanji.view.menu;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.ConstraintUtils;
import com.bingbaihanji.util.I18nUtil;
import com.bingbaihanji.util.PointReuseGroup;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.List;

/**
 * 点右键菜单构建器
 *
 * @author bingbaihanji
 * @date 2026-05-23
 */
public final class PointMenuBuilder {

    private PointMenuBuilder() {
    }

    public static ContextMenu createPointMenu(
            PointGeo point, GridChartView canvas, DrawingController controller) {
        ContextMenu menu = new ContextMenu();

        MenuItem renameItem = new MenuItem(I18nUtil.getString("geo.menu.rename"));
        renameItem.setOnAction(e -> ContextMenuDialogs.showRenameDialog(point, canvas));

        MenuItem colorItem = new MenuItem(I18nUtil.getString("geo.menu.changeColor"));
        colorItem.setOnAction(e -> ContextMenuDialogs.showColorPickerDialog(point, canvas));

        MenuItem positionItem = new MenuItem(I18nUtil.getString("geo.menu.position"));
        positionItem.setOnAction(e -> ContextMenuDialogs.showPositionDialog(point, canvas, controller));

        menu.getItems().addAll(renameItem, colorItem, positionItem);

        List<WorldObject> usingShapes = ConstraintUtils.findShapesUsingPoint(point, canvas.getObjects());
        if (!usingShapes.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem usageInfoItem = new MenuItem(
                    I18nUtil.getString("geo.menu.pointUsedBy", usingShapes.size()));
            usageInfoItem.setDisable(true);
            menu.getItems().add(usageInfoItem);

            int displayCount = Math.min(usingShapes.size(), 5);
            for (int i = 0; i < displayCount; i++) {
                WorldObject shape = usingShapes.get(i);
                String shapeName = ConstraintUtils.getShapeDisplayName(shape);
                MenuItem shapeItem = new MenuItem("  • " + shapeName);
                shapeItem.setDisable(true);
                menu.getItems().add(shapeItem);
            }
            if (usingShapes.size() > 5) {
                MenuItem moreItem = new MenuItem("  ... 还有 " + (usingShapes.size() - 5) + " 个");
                moreItem.setDisable(true);
                menu.getItems().add(moreItem);
            }
        }

        menu.getItems().add(new SeparatorMenuItem());

        if (point.isConstrained()) {
            MenuItem removeConstraintItem = new MenuItem(I18nUtil.getString("geo.menu.removeConstraint"));
            removeConstraintItem.setOnAction(e -> {
                point.setConstraint(null);
                canvas.redraw();
            });
            menu.getItems().add(removeConstraintItem);
        } else {
            Menu addConstraintMenu = new Menu(I18nUtil.getString("geo.menu.addConstraint"));
            List<WorldObject> nearbyShapes = ConstraintUtils.findNearbyConstrainableShapes(point, canvas, controller);

            if (nearbyShapes.isEmpty()) {
                MenuItem noShapeItem = new MenuItem(I18nUtil.getString("geo.menu.noConstraintTarget"));
                noShapeItem.setDisable(true);
                addConstraintMenu.getItems().add(noShapeItem);
            } else {
                for (WorldObject shape : nearbyShapes) {
                    String shapeName = ConstraintUtils.getShapeDisplayName(shape);
                    MenuItem shapeItem = new MenuItem(shapeName);
                    shapeItem.setOnAction(e -> ConstraintUtils.addConstraintToPoint(point, shape, controller, canvas));
                    addConstraintMenu.getItems().add(shapeItem);
                }
            }
            menu.getItems().add(addConstraintMenu);
        }

        menu.getItems().add(new SeparatorMenuItem());
        addReuseMenuItems(menu, point, canvas, controller);

        menu.getItems().add(new SeparatorMenuItem());
        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> ContextMenuDialogs.deleteObject(point, canvas, controller));
        menu.getItems().add(deleteItem);

        return menu;
    }

    /**
     * 添加复用功能菜单项
     */
    public static void addReuseMenuItems(ContextMenu menu, PointGeo point, GridChartView canvas, DrawingController controller) {
        if (point.isInReuseGroup()) {
            PointReuseGroup group = point.getReuseGroup();

            MenuItem groupInfoItem = new MenuItem(
                    I18nUtil.getString("geo.menu.reuseGroupInfo", group.getMemberCount()) +
                            " - " + group.getMembersInfo());
            groupInfoItem.setDisable(true);
            menu.getItems().add(groupInfoItem);

            String toggleText = group.isEnabled() ?
                    I18nUtil.getString("geo.menu.disableReuse") :
                    I18nUtil.getString("geo.menu.enableReuseToggle");
            MenuItem toggleReuseItem = new MenuItem(toggleText);
            toggleReuseItem.setOnAction(e -> {
                group.setEnabled(!group.isEnabled());
                canvas.redraw();
            });
            menu.getItems().add(toggleReuseItem);

            MenuItem removeFromGroupItem = new MenuItem(I18nUtil.getString("geo.menu.removeFromReuseGroup"));
            removeFromGroupItem.setOnAction(e -> {
                PointReuseManager.disableReuse(point);
                canvas.redraw();
            });
            menu.getItems().add(removeFromGroupItem);

            if (group.getMemberCount() > 1) {
                MenuItem dissolveGroupItem = new MenuItem(I18nUtil.getString("geo.menu.dissolveReuseGroup"));
                dissolveGroupItem.setOnAction(e -> {
                    group.dissolve();
                    canvas.redraw();
                });
                menu.getItems().add(dissolveGroupItem);
            }
        } else {
            double scale = canvas.getTransform().getScale();
            double threshold = GeometryConfig.Tolerance.VERTEX_HIT_TEST_PIXELS / scale;
            List<PointGeo> overlappingPoints = PointReuseManager.findOverlappingPoints(
                    point, canvas.getObjects(), threshold);

            if (!overlappingPoints.isEmpty()) {
                Menu enableReuseMenu = new Menu(I18nUtil.getString("geo.menu.enableReuse") +
                        " (" + overlappingPoints.size() + ")");

                for (PointGeo overlapping : overlappingPoints) {
                    String pointName = overlapping.getName() != null && !overlapping.getName().isEmpty() ?
                            overlapping.getName() :
                            String.format("点(%.2f, %.2f)", overlapping.getX(), overlapping.getY());
                    if (overlapping.isInReuseGroup()) {
                        pointName += " [已复用]";
                    }
                    MenuItem pointItem = new MenuItem(I18nUtil.getString("geo.menu.reuseWith", pointName));
                    pointItem.setOnAction(e -> {
                        PointReuseManager.enableReuse(point, overlapping);
                        canvas.redraw();
                    });
                    enableReuseMenu.getItems().add(pointItem);
                }

                if (overlappingPoints.size() > 1) {
                    enableReuseMenu.getItems().add(new SeparatorMenuItem());
                    MenuItem reuseAllItem = new MenuItem(I18nUtil.getString("geo.menu.reuseWithAll"));
                    reuseAllItem.setOnAction(e -> {
                        PointReuseGroup group = PointReuseGroup.getManager().createGroup();
                        group.addMember(point);
                        for (PointGeo overlapping : overlappingPoints) {
                            group.addMember(overlapping);
                        }
                        canvas.redraw();
                    });
                    enableReuseMenu.getItems().add(reuseAllItem);
                }
                menu.getItems().add(enableReuseMenu);
            } else {
                MenuItem noOverlapItem = new MenuItem(I18nUtil.getString("geo.menu.noOverlappingPoints"));
                noOverlapItem.setDisable(true);
                menu.getItems().add(noOverlapItem);
            }
        }
    }

    /**
     * 添加约束功能菜单项
     */
    public static void addConstraintMenuItems(ContextMenu menu, PointGeo point, GridChartView canvas, DrawingController controller) {
        if (point.isConstrained()) {
            MenuItem removeConstraintItem = new MenuItem(I18nUtil.getString("geo.menu.removeConstraint"));
            removeConstraintItem.setOnAction(e -> {
                point.setConstraint(null);
                canvas.redraw();
            });
            menu.getItems().add(removeConstraintItem);
        } else {
            Menu addConstraintMenu = new Menu(I18nUtil.getString("geo.menu.addConstraint"));
            List<WorldObject> nearbyShapes = ConstraintUtils.findNearbyConstrainableShapes(point, canvas, controller);

            if (nearbyShapes.isEmpty()) {
                MenuItem noShapeItem = new MenuItem(I18nUtil.getString("geo.menu.noConstraintTarget"));
                noShapeItem.setDisable(true);
                addConstraintMenu.getItems().add(noShapeItem);
            } else {
                for (WorldObject shape : nearbyShapes) {
                    String shapeName = ConstraintUtils.getShapeDisplayName(shape);
                    MenuItem shapeItem = new MenuItem(shapeName);
                    shapeItem.setOnAction(e -> ConstraintUtils.addConstraintToPoint(point, shape, controller, canvas));
                    addConstraintMenu.getItems().add(shapeItem);
                }
            }
            menu.getItems().add(addConstraintMenu);
        }
    }
}