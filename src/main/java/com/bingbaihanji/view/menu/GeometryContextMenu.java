package com.bingbaihanji.view.menu;

import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.util.*;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.DetachedCanvasWindow;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 几何图形右键菜单管理器
 * <p>
 * 提供点、图形和画布的右键菜单功能
 * 支持窗口层级管理：从任意窗口打开的子窗口会自动注册到父窗口的控制器中
 *
 * @author bingbaihanji
 * @date 2025-12-30
 */
public class GeometryContextMenu {

    /**
     * 为点创建右键菜单
     */
    public static ContextMenu createPointMenu(
            PointGeo point,
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        // 修改名称
        MenuItem renameItem = new MenuItem(I18nUtil.getString("geo.menu.rename"));
        renameItem.setOnAction(e -> showRenameDialog(point, canvas));

        // 修改颜色
        MenuItem colorItem = new MenuItem(I18nUtil.getString("geo.menu.changeColor"));
        colorItem.setOnAction(e -> showColorPickerDialog(point, canvas));

        // 设置位置
        MenuItem positionItem = new MenuItem(I18nUtil.getString("geo.menu.position"));
        positionItem.setOnAction(e -> showPositionDialog(point, canvas, controller));

        menu.getItems().addAll(renameItem, colorItem, positionItem);

        // 检查有多少图形在使用这个点
        List<WorldObject> usingShapes = findShapesUsingPoint(point, canvas.getObjects());
        System.out.println("[DEBUG] 点 " + point.getName() + " 被 " + usingShapes.size() + " 个图形使用");
        if (!usingShapes.isEmpty()) {
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem usageInfoItem = new MenuItem(
                    I18nUtil.getString("geo.menu.pointUsedBy", usingShapes.size()));
            usageInfoItem.setDisable(true);
            menu.getItems().add(usageInfoItem);

            // 显示使用该点的图形列表（最多显示5个）
            int displayCount = Math.min(usingShapes.size(), 5);
            for (int i = 0; i < displayCount; i++) {
                WorldObject shape = usingShapes.get(i);
                String shapeName = getShapeDisplayName(shape);
                MenuItem shapeItem = new MenuItem("  \u2022 " + shapeName);
                shapeItem.setDisable(true);
                menu.getItems().add(shapeItem);
            }
            if (usingShapes.size() > 5) {
                MenuItem moreItem = new MenuItem("  ... 还有 " + (usingShapes.size() - 5) + " 个");
                moreItem.setDisable(true);
                menu.getItems().add(moreItem);
            }
        }

        // ==================== 约束属性菜单 ====================
        menu.getItems().add(new SeparatorMenuItem());

        // 移除约束（仅约束点显示）
        if (point.isConstrained()) {
            MenuItem removeConstraintItem = new MenuItem(I18nUtil.getString("geo.menu.removeConstraint"));
            removeConstraintItem.setOnAction(e -> {
                point.setConstraint(null);
                canvas.redraw();
            });
            menu.getItems().add(removeConstraintItem);
        } else {
            // 添加约束菜单（选择附近的几何图形作为约束对象）
            Menu addConstraintMenu = new Menu(I18nUtil.getString("geo.menu.addConstraint"));
            List<WorldObject> nearbyShapes = findNearbyConstrainableShapes(point, canvas, controller);

            if (nearbyShapes.isEmpty()) {
                MenuItem noShapeItem = new MenuItem(I18nUtil.getString("geo.menu.noConstraintTarget"));
                noShapeItem.setDisable(true);
                addConstraintMenu.getItems().add(noShapeItem);
            } else {
                for (WorldObject shape : nearbyShapes) {
                    String shapeName = getShapeDisplayName(shape);
                    MenuItem shapeItem = new MenuItem(shapeName);
                    shapeItem.setOnAction(e -> {
                        addConstraintToPoint(point, shape, controller, canvas);
                    });
                    addConstraintMenu.getItems().add(shapeItem);
                }
            }
            menu.getItems().add(addConstraintMenu);
        }

        // ==================== 复用属性菜单 ====================
        menu.getItems().add(new SeparatorMenuItem());

        if (point.isInReuseGroup()) {
            // 已在复用组中，显示禁用复用选项
            PointReuseGroup group = point.getReuseGroup();

            // 复用组信息 - 显示更详细的信息
            MenuItem groupInfoItem = new MenuItem(
                    I18nUtil.getString("geo.menu.reuseGroupInfo", group.getMemberCount()) +
                            " - " + group.getMembersInfo());
            groupInfoItem.setDisable(true);
            menu.getItems().add(groupInfoItem);

            // 启用/禁用复用切换
            String toggleText = group.isEnabled() ?
                    I18nUtil.getString("geo.menu.disableReuse") :
                    I18nUtil.getString("geo.menu.enableReuseToggle");
            MenuItem toggleReuseItem = new MenuItem(toggleText);
            toggleReuseItem.setOnAction(e -> {
                group.setEnabled(!group.isEnabled());
                canvas.redraw();
            });
            menu.getItems().add(toggleReuseItem);

            // 从复用组移除（不解散组）
            MenuItem removeFromGroupItem = new MenuItem(I18nUtil.getString("geo.menu.removeFromReuseGroup"));
            removeFromGroupItem.setOnAction(e -> {
                PointReuseManager.disableReuse(point);
                canvas.redraw();
            });
            menu.getItems().add(removeFromGroupItem);

            // 解散复用组
            if (group.getMemberCount() > 1) {
                MenuItem dissolveGroupItem = new MenuItem(I18nUtil.getString("geo.menu.dissolveReuseGroup"));
                dissolveGroupItem.setOnAction(e -> {
                    group.dissolve();
                    canvas.redraw();
                });
                menu.getItems().add(dissolveGroupItem);
            }
        } else {
            // 未在复用组中，检查是否有重合的点
            double scale = canvas.getTransform().getScale();
            double threshold = 10.0 / scale;
            List<PointGeo> overlappingPoints = PointReuseManager.findOverlappingPoints(
                    point, canvas.getObjects(), threshold);

            if (!overlappingPoints.isEmpty()) {
                // 启用复用子菜单
                Menu enableReuseMenu = new Menu(I18nUtil.getString("geo.menu.enableReuse") +
                        " (" + overlappingPoints.size() + ")");

                for (PointGeo overlapping : overlappingPoints) {
                    String pointName = overlapping.getName() != null && !overlapping.getName().isEmpty() ?
                            overlapping.getName() :
                            String.format("点(%.2f, %.2f)", overlapping.getX(), overlapping.getY());

                    // 显示点是否已在其他复用组
                    if (overlapping.isInReuseGroup()) {
                        pointName += " [已复用]";
                    }

                    MenuItem pointItem = new MenuItem(
                            I18nUtil.getString("geo.menu.reuseWith", pointName));
                    pointItem.setOnAction(e -> {
                        PointReuseManager.enableReuse(point, overlapping);
                        canvas.redraw();
                    });
                    enableReuseMenu.getItems().add(pointItem);
                }

                // 与所有重合点启用复用
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
                // 没有重合的点
                MenuItem noOverlapItem = new MenuItem(I18nUtil.getString("geo.menu.noOverlappingPoints"));
                noOverlapItem.setDisable(true);
                menu.getItems().add(noOverlapItem);
            }
        }

        // 删除
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> deleteObject(point, canvas, controller));
        menu.getItems().add(deleteItem);

        return menu;
    }

    /**
     * 添加复用功能菜单项
     */
    private static void addReuseMenuItems(ContextMenu menu, PointGeo point, GridChartView canvas, DrawingController controller) {
        if (point.isInReuseGroup()) {
            // 已在复用组中
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
            // 未在复用组中
            double scale = canvas.getTransform().getScale();
            double threshold = 10.0 / scale;
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

                    MenuItem pointItem = new MenuItem(
                            I18nUtil.getString("geo.menu.reuseWith", pointName));
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
    private static void addConstraintMenuItems(ContextMenu menu, PointGeo point, GridChartView canvas, DrawingController controller) {
        if (point.isConstrained()) {
            MenuItem removeConstraintItem = new MenuItem(I18nUtil.getString("geo.menu.removeConstraint"));
            removeConstraintItem.setOnAction(e -> {
                point.setConstraint(null);
                canvas.redraw();
            });
            menu.getItems().add(removeConstraintItem);
        } else {
            Menu addConstraintMenu = new Menu(I18nUtil.getString("geo.menu.addConstraint"));
            List<WorldObject> nearbyShapes = findNearbyConstrainableShapes(point, canvas, controller);

            if (nearbyShapes.isEmpty()) {
                MenuItem noShapeItem = new MenuItem(I18nUtil.getString("geo.menu.noConstraintTarget"));
                noShapeItem.setDisable(true);
                addConstraintMenu.getItems().add(noShapeItem);
            } else {
                for (WorldObject shape : nearbyShapes) {
                    String shapeName = getShapeDisplayName(shape);
                    MenuItem shapeItem = new MenuItem(shapeName);
                    shapeItem.setOnAction(e -> {
                        addConstraintToPoint(point, shape, controller, canvas);
                    });
                    addConstraintMenu.getItems().add(shapeItem);
                }
            }
            menu.getItems().add(addConstraintMenu);
        }
    }

    /**
     * 查找附近可以作为约束对象的几何图形
     */
    private static List<WorldObject> findNearbyConstrainableShapes(
            PointGeo point, GridChartView canvas, DrawingController controller) {
        List<WorldObject> result = new java.util.ArrayList<>();
        double scale = canvas.getTransform().getScale();
        double threshold = 20.0 / scale; // 距离阈值

        for (WorldObject obj : canvas.getObjects()) {
            if (obj == point) continue;

            // 检查是否是可约束的图形类型
            if (isConstrainableShape(obj)) {
                // 检查点是否在图形附近
                if (isPointNearShape(point.getX(), point.getY(), obj, threshold)) {
                    result.add(obj);
                }
            }
        }

        return result;
    }

    /**
     * 判断图形是否可以作为约束对象
     */
    private static boolean isConstrainableShape(WorldObject obj) {
        return obj instanceof LineGeo ||
                obj instanceof InfiniteLineGeo ||
                obj instanceof CircleGeo ||
                obj instanceof PolygonGeo ||
                obj instanceof PathGeo ||
                obj instanceof FunctionGeo;
    }

    /**
     * 检查点是否在图形附近
     */
    private static boolean isPointNearShape(double x, double y, WorldObject shape, double threshold) {
        if (shape instanceof LineGeo line) {
            return pointToLineDistance(x, y, line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY()) < threshold;
        } else if (shape instanceof InfiniteLineGeo infLine) {
            return pointToInfiniteLineDistance(x, y, infLine) < threshold;
        } else if (shape instanceof CircleGeo circle) {
            double dist = Math.abs(Math.hypot(x - circle.getCx(), y - circle.getCy()) - circle.getR());
            return dist < threshold;
        } else if (shape instanceof PolygonGeo polygon) {
            for (int i = 0; i < polygon.getVertexCount(); i++) {
                javafx.geometry.Point2D v1 = polygon.getVertex(i);
                javafx.geometry.Point2D v2 = polygon.getVertex((i + 1) % polygon.getVertexCount());
                if (pointToLineDistance(x, y, v1.getX(), v1.getY(), v2.getX(), v2.getY()) < threshold) {
                    return true;
                }
            }
            return false;
        } else if (shape instanceof FunctionGeo function) {
            // 简化处理：检查y坐标是否接近函数值
            try {
                double fy = function.evaluate(x);
                return Double.isFinite(fy) && Math.abs(y - fy) < threshold;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 计算点到线段的距离
     */
    private static double pointToLineDistance(double px, double py,
                                              double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lenSq = dx * dx + dy * dy;

        if (lenSq < 1e-10) {
            return Math.hypot(px - x1, py - y1);
        }

        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lenSq));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(px - projX, py - projY);
    }

    /**
     * 计算点到无限直线的距离
     */
    private static double pointToInfiniteLineDistance(double px, double py, InfiniteLineGeo line) {
        double x1 = line.getPoint1X();
        double y1 = line.getPoint1Y();
        double x2 = line.getPoint2X();
        double y2 = line.getPoint2Y();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);

        if (len < 1e-10) {
            return Math.hypot(px - x1, py - y1);
        }

        // 点到直线的距离公式
        return Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1) / len;
    }

    /**
     * 获取图形的显示名称
     */
    private static String getShapeDisplayName(WorldObject shape) {
        if (shape instanceof LineGeo line) {
            return I18nUtil.getString("geo.shape.segment",
                    line.getStartPointName(), line.getEndPointName());
        } else if (shape instanceof InfiniteLineGeo infLine) {
            return I18nUtil.getString("geo.shape.line",
                    infLine.getPoint1Name(), infLine.getPoint2Name());
        } else if (shape instanceof CircleGeo circle) {
            return I18nUtil.getString("geo.shape.circle", circle.getCenterName());
        } else if (shape instanceof PolygonGeo polygon) {
            return I18nUtil.getString("geo.shape.polygon", polygon.getVertexCount());
        } else if (shape instanceof PathGeo) {
            return I18nUtil.getString("geo.shape.path");
        } else if (shape instanceof FunctionGeo function) {
            return I18nUtil.getString("geo.shape.function", function.getExpression());
        }
        return shape.getClass().getSimpleName();
    }

    /**
     * 为点添加约束
     */
    private static void addConstraintToPoint(PointGeo point, WorldObject shape,
                                             DrawingController controller, GridChartView canvas) {
        try {
            DrawingContext context = controller.getContext();
            // 创建约束并自动检测是否为顶点
            PointConstraint constraint = context.getConstraintHandler().createConstraint(shape, point);

            // 如果不是顶点约束，计算参数
            if (!constraint.isVertexConstraint()) {
                double parameter = constraint.calculateParameter(point.getX(), point.getY());
                constraint.setParameter(parameter);
            }

            // 设置约束
            point.setConstraint(constraint);

            // 更新点位置到约束位置
            javafx.geometry.Point2D newPos = constraint.getPointFromParameter();
            point.updatePosition(newPos.getX(), newPos.getY());

            canvas.redraw();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.getString("geo.dialog.error.title"));
            alert.setContentText(I18nUtil.getString("geo.dialog.constraint.error", e.getMessage()));
            alert.showAndWait();
        }
    }

    /**
     * 为几何图形的顶点创建右键菜单
     */
    public static ContextMenu createVertexMenu(
            WorldObject.DraggablePoint vertex,
            WorldObject parentShape,
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        // 获取顶点位置
        double vx = vertex.getX();
        double vy = vertex.getY();

        // 检查该位置是否已有独立的 PointGeo
        double scale = canvas.getTransform().getScale();
        double threshold = 5.0 / scale;
        PointGeo existingPoint = PointReuseManager.findExistingPoint(vx, vy, canvas.getObjects(), threshold);

        // 检查是否有其他重合的点（包括多边形顶点、线段端点、圆心等）
        // 注意：这里要检查所有图形的关键点，不只是独立的 PointGeo
        boolean hasOverlappingPoints = false;
        if (existingPoint == null) {
            // 创建一个临时点来检测重合
            PointGeo tempPoint = new PointGeo(vx, vy, false);
            // 使用更大的阈值来检测附近的点
            List<PointGeo> overlapping = PointReuseManager.findOverlappingPoints(
                    tempPoint, canvas.getObjects(), 10.0 / scale);

            // 额外检查：是否有多个圆心在此位置（同心圆情况）
            int circleCount = 0;
            for (WorldObject obj : canvas.getObjects()) {
                if (obj instanceof CircleGeo circle) {
                    double dist = Math.hypot(circle.getCx() - vx, circle.getCy() - vy);
                    if (dist < 10.0 / scale) {
                        circleCount++;
                    }
                }
            }

            hasOverlappingPoints = !overlapping.isEmpty() || circleCount > 1;
        }

        if (existingPoint != null) {
            // 已有独立点，创建完整的复用/约束菜单项
            MenuItem convertInfo = new MenuItem("✓ 已有独立点");
            convertInfo.setDisable(true);
            menu.getItems().add(convertInfo);
            menu.getItems().add(new SeparatorMenuItem());

            // 添加复用功能
            addReuseMenuItems(menu, existingPoint, canvas, controller);

            // 添加约束功能
            menu.getItems().add(new SeparatorMenuItem());
            addConstraintMenuItems(menu, existingPoint, canvas, controller);

        } else if (hasOverlappingPoints) {
            // 没有独立点，但有重合的点（如多边形顶点、同心圆等），提供创建选项
            MenuItem convertItem = new MenuItem(I18nUtil.getString("geo.menu.createPointHere"));
            convertItem.setOnAction(e -> {
                // 在此位置创建独立的 PointGeo
                PointGeo newPoint = new PointGeo(vx, vy);
                controller.getContext().addObject(newPoint);
                canvas.redraw();
            });
            menu.getItems().add(convertItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        // 设置位置
        MenuItem positionItem = new MenuItem(I18nUtil.getString("geo.menu.position"));
        positionItem.setOnAction(e -> showVertexPositionDialog(vertex, parentShape, canvas, controller));
        menu.getItems().add(positionItem);

        return menu;
    }

    /**
     * 为几何图形创建右键菜单
     */
    public static ContextMenu createShapeMenu(
            WorldObject shape,
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        // 属性
        MenuItem propertiesItem = new MenuItem(I18nUtil.getString("geo.menu.properties"));
        propertiesItem.setOnAction(e -> showPropertiesDialog(shape, canvas));

        // 删除
        MenuItem deleteItem = new MenuItem(I18nUtil.getString("geo.menu.delete"));
        deleteItem.setOnAction(e -> deleteObject(shape, canvas, controller));

        menu.getItems().addAll(propertiesItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    /**
     * 为选中对象创建右键菜单（BoundingBox菜单）
     *
     * @param canvas     画布视图
     * @param controller 绘制控制器
     * @return 右键菜单
     */
    public static ContextMenu createBoundingBoxMenu(
            GridChartView canvas,
            DrawingController controller
    ) {
        ContextMenu menu = new ContextMenu();

        // 导出PNG
        MenuItem exportPngItem = new MenuItem(I18nUtil.getString("geo.menu.exportPNG"));
        exportPngItem.setOnAction(e -> exportSelectionToPNG(canvas, controller));

        menu.getItems().add(exportPngItem);

        return menu;
    }

    /**
     * 为画布创建右键菜单
     */
    public static ContextMenu createCanvasMenu(
            GridChartView canvas,
            DrawingController controller
    ) {
        return createCanvasMenu(canvas, controller, null);
    }

    /**
     * 为画布创建右键菜单（带父窗口引用）
     *
     * @param canvas       画布视图
     * @param controller   绘制控制器
     * @param parentWindow 父窗口（如果在独立窗口中则传入，否则为null）
     */
    public static ContextMenu createCanvasMenu(
            GridChartView canvas,
            DrawingController controller,
            DetachedCanvasWindow parentWindow
    ) {
        ContextMenu menu = new ContextMenu();

        MenuItem undoItem = new MenuItem(I18nUtil.getString("geo.menu.undo"));
        undoItem.setDisable(!controller.canUndo());
        undoItem.setOnAction(e -> controller.undo());

        MenuItem redoItem = new MenuItem(I18nUtil.getString("geo.menu.redo"));
        redoItem.setDisable(!controller.canRedo());
        redoItem.setOnAction(e -> controller.redo());

        MenuItem clearItem = new MenuItem(I18nUtil.getString("geo.menu.clear"));
        clearItem.setOnAction(e -> controller.clearAll());

        //   视图控制子菜单  

        // 缩放子菜单
        Menu zoomMenu = new Menu(I18nUtil.getString("menu.zoom"));

        MenuItem zoom25 = new MenuItem("25%");
        zoom25.setOnAction(e -> canvas.zoomToPercent(25));

        MenuItem zoom50 = new MenuItem("50%");
        zoom50.setOnAction(e -> canvas.zoomToPercent(50));

        MenuItem zoom100 = new MenuItem("100%");
        zoom100.setOnAction(e -> canvas.zoomToPercent(100));

        MenuItem zoom200 = new MenuItem("200%");
        zoom200.setOnAction(e -> canvas.zoomToPercent(200));

        MenuItem zoom400 = new MenuItem("400%");
        zoom400.setOnAction(e -> canvas.zoomToPercent(400));

        zoomMenu.getItems().addAll(zoom25, zoom50, zoom100, zoom200, zoom400);

        // 轴比例子菜单
        Menu axisRatioMenu = new Menu(I18nUtil.getString("menu.axisRatio"));

        MenuItem ratio11 = new MenuItem("1:1");
        ratio11.setOnAction(e -> canvas.setAxisRatio(1, 1));

        MenuItem ratio12 = new MenuItem("1:2");
        ratio12.setOnAction(e -> canvas.setAxisRatio(1, 2));

        MenuItem ratio21 = new MenuItem("2:1");
        ratio21.setOnAction(e -> canvas.setAxisRatio(2, 1));

        MenuItem ratio14 = new MenuItem("1:4");
        ratio14.setOnAction(e -> canvas.setAxisRatio(1, 4));

        MenuItem ratio41 = new MenuItem("4:1");
        ratio41.setOnAction(e -> canvas.setAxisRatio(4, 1));

        axisRatioMenu.getItems().addAll(ratio11, ratio12, ratio21, ratio14, ratio41);

        // 显示所有对象
        MenuItem fitAllItem = new MenuItem(I18nUtil.getString("menu.showAllObjects"));
        fitAllItem.setOnAction(e -> canvas.fitAllObjects());
        fitAllItem.setDisable(canvas.getObjects().isEmpty());

        // 标准视图
        MenuItem standardViewItem = new MenuItem(I18nUtil.getString("menu.standardView"));
        standardViewItem.setOnAction(e -> canvas.resetToStandardView());

        //   属性配置  

        // 坐标轴属性
        MenuItem axesPropsItem = new MenuItem(I18nUtil.getString("menu.axesProperties"));
        axesPropsItem.setOnAction(e -> {
            AxesPropertiesDialog dialog = new AxesPropertiesDialog(canvas.getSettings());
            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    canvas.applySettings();
                }
            });
        });

        // 网格属性
        MenuItem gridPropsItem = new MenuItem(I18nUtil.getString("menu.gridProperties"));
        gridPropsItem.setOnAction(e -> {
            GridPropertiesDialog dialog = new GridPropertiesDialog(canvas.getSettings());
            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    canvas.applySettings();
                }
            });
        });

        // 背景颜色子菜单
        Menu bgColorMenu = new Menu(I18nUtil.getString("geo.menu.backgroundColor"));

        // 预设颜色选项
        MenuItem whiteItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.white"));
        whiteItem.setOnAction(e -> canvas.setBackgroundColor(Color.WHITE));

        MenuItem grayItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.gray"));
        grayItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(240, 240, 240)));

        MenuItem beigeItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.beige"));
        beigeItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(245, 245, 220)));

        MenuItem blueItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.blue"));
        blueItem.setOnAction(e -> canvas.setBackgroundColor(Color.rgb(230, 240, 255)));

        MenuItem customItem = new MenuItem(I18nUtil.getString("geo.menu.bgColor.custom"));
        customItem.setOnAction(e -> showBackgroundColorPicker(canvas));

        bgColorMenu.getItems().addAll(whiteItem, grayItem, beigeItem, blueItem,
                new SeparatorMenuItem(), customItem);

        // 在新窗口打开
        MenuItem detachItem = new MenuItem(I18nUtil.getString("geo.menu.detachWindow"));
        detachItem.setOnAction(e -> {
            DetachedCanvasWindow detachedWindow = new DetachedCanvasWindow(canvas, parentWindow);
            // 将新窗口注册到当前控制器的子窗口列表中
            controller.addChildWindow(detachedWindow);
            detachedWindow.show();
        });

        menu.getItems().addAll(
                undoItem, redoItem,
                new SeparatorMenuItem(),
                clearItem,
                new SeparatorMenuItem(),
                zoomMenu, axisRatioMenu,
                new SeparatorMenuItem(),
                fitAllItem, standardViewItem,
                new SeparatorMenuItem(),
                axesPropsItem, gridPropsItem,
                new SeparatorMenuItem(),
                bgColorMenu,
                new SeparatorMenuItem(),
                detachItem
        );
        return menu;
    }

    /**
     * 显示重命名对话框
     */
    private static void showRenameDialog(PointGeo point, GridChartView canvas) {
        TextInputDialog dialog = new TextInputDialog(point.getName());
        dialog.setTitle(I18nUtil.getString("geo.dialog.rename.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.rename.header"));
        dialog.setContentText(I18nUtil.getString("geo.dialog.rename.content"));

        // 添加 Escape 键关闭对话框支持
        addEscapeKeyHandler(dialog);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                point.setName(name.trim());
                canvas.redraw();
            }
        });
    }

    /**
     * 显示颜色选择对话框
     */
    private static void showColorPickerDialog(PointGeo point, GridChartView canvas) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.color.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.color.header"));

        ColorPicker picker = new ColorPicker(point.getColor());
        picker.setPrefWidth(200);

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 添加 Escape 键关闭对话框支持
        addEscapeKeyHandler(dialog);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return picker.getValue();
            }
            return null;
        });

        Optional<Color> result = dialog.showAndWait();
        result.ifPresent(color -> {
            point.setColor(color);
            canvas.redraw();
        });
    }

    /**
     * 显示位置设置对话框
     */
    private static void showPositionDialog(PointGeo point, GridChartView canvas, DrawingController controller) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.position.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.position.header"));

        // 创建输入表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField xField = new TextField(String.format("%.2f", point.getX()));
        TextField yField = new TextField(String.format("%.2f", point.getY()));

        grid.add(new Label(I18nUtil.getString("geo.dialog.position.x")), 0, 0);
        grid.add(xField, 1, 0);
        grid.add(new Label(I18nUtil.getString("geo.dialog.position.y")), 0, 1);
        grid.add(yField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 添加 Escape 键关闭对话框支持
        addEscapeKeyHandler(dialog);

        // 处理确定按钮
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double newX = Double.parseDouble(xField.getText());
                double newY = Double.parseDouble(yField.getText());

                // 保存旧位置用于撤销
                final double oldX = point.getX();
                final double oldY = point.getY();

                // 使用命令模式支持撤销/恢复
                controller.getContext().executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        point.updatePosition(newX, newY);
                        canvas.redraw();
                    }

                    @Override
                    public void undo() {
                        point.updatePosition(oldX, oldY);
                        canvas.redraw();
                    }
                });
            } catch (NumberFormatException e) {
                // 显示错误提示
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.getString("geo.dialog.position.error.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nUtil.getString("geo.dialog.position.error.invalid"));
                addEscapeKeyHandler(alert); // Alert 也添加 Escape 键支持
                alert.showAndWait();
            }
        }
    }

    /**
     * 显示顶点位置设置对话框（用于线段、多边形、圆等图形的顶点）
     */
    private static void showVertexPositionDialog(
            WorldObject.DraggablePoint vertex,
            WorldObject parentShape,
            GridChartView canvas,
            DrawingController controller
    ) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.vertex.position.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.vertex.position.header"));

        // 创建输入表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField xField = new TextField(String.format("%.2f", vertex.getX()));
        TextField yField = new TextField(String.format("%.2f", vertex.getY()));

        grid.add(new Label(I18nUtil.getString("geo.dialog.position.x")), 0, 0);
        grid.add(xField, 1, 0);
        grid.add(new Label(I18nUtil.getString("geo.dialog.position.y")), 0, 1);
        grid.add(yField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 添加 Escape 键关闭对话框支持
        addEscapeKeyHandler(dialog);

        // 处理确定按钮
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double newX = Double.parseDouble(xField.getText());
                double newY = Double.parseDouble(yField.getText());

                // 保存旧位置用于撤销
                final double oldX = vertex.getX();
                final double oldY = vertex.getY();

                // 使用命令模式支持撤销/恢复
                controller.getContext().executeCommand(new CommandHistory.Command() {
                    @Override
                    public void execute() {
                        vertex.updatePosition(newX, newY);
                        // 更新约束点和交点
                        controller.getContext().getConstraintHandler().updateAllConstrainedPoints(controller.getContext());
                        controller.getContext().getIntersectionHandler().recalculateAllIntersections(controller.getContext());
                        canvas.redraw();
                    }

                    @Override
                    public void undo() {
                        vertex.updatePosition(oldX, oldY);
                        // 更新约束点和交点
                        controller.getContext().getConstraintHandler().updateAllConstrainedPoints(controller.getContext());
                        controller.getContext().getIntersectionHandler().recalculateAllIntersections(controller.getContext());
                        canvas.redraw();
                    }
                });
            } catch (NumberFormatException e) {
                // 显示错误提示
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.getString("geo.dialog.position.error.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18nUtil.getString("geo.dialog.position.error.invalid"));
                addEscapeKeyHandler(alert); // Alert 也添加 Escape 键支持
                alert.showAndWait();
            }
        }
    }

    /**
     * 显示几何图形属性对话框
     */
    private static void showPropertiesDialog(WorldObject shape, GridChartView canvas) {
        ShapePropertiesDialog dialog;

        // 根据不同的图形类型创建对话框
        if (shape instanceof CircleGeo circle) {
            // 圆形：支持颜色、半径和圆心名称修改
            dialog = new ShapePropertiesDialog(circle.getColor(), circle.getR(), circle.getCenterName());

            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                circle.setColor(props.getColor());
                circle.setR(props.getRadius());
                // 设置圆心名称
                if (props.getCenterName() != null && !props.getCenterName().isEmpty()) {
                    circle.setCenterName(props.getCenterName());
                }
                canvas.redraw();
            });
        } else if (shape instanceof LineGeo line) {
            // 线段：仅支持颜色修改
            dialog = new ShapePropertiesDialog(line.getColor());

            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                line.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof InfiniteLineGeo infiniteLine) {
            // 直线：仅支持颜色修改
            dialog = new ShapePropertiesDialog(infiniteLine.getColor());

            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                infiniteLine.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof PathGeo path) {
            // 手绘路径：仅支持颜色修改
            dialog = new ShapePropertiesDialog(path.getColor());

            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                path.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof PolygonGeo polygon) {
            // 多边形：仅支持颜色修改
            dialog = new ShapePropertiesDialog(polygon.getColor());

            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                polygon.setColor(props.getColor());
                canvas.redraw();
            });
        } else if (shape instanceof FunctionGeo function) {
            // 函数曲线：支持颜色、线宽修改
            dialog = new ShapePropertiesDialog(function.getColor());

            Optional<ShapePropertiesResult> result = dialog.showAndWait();
            result.ifPresent(props -> {
                function.setColor(props.getColor());
                canvas.redraw();
            });
        }
    }

    /**
     * 显示背景颜色选择对话框
     */
    private static void showBackgroundColorPicker(GridChartView canvas) {
        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle(I18nUtil.getString("geo.dialog.bgColor.title"));
        dialog.setHeaderText(I18nUtil.getString("geo.dialog.bgColor.header"));

        // 设置对话框图标
        FxTools.setDialogIcon(dialog, "/icon/rgb.png");
        ColorPicker picker = new ColorPicker(canvas.getBackgroundColor());
        picker.setPrefWidth(200);

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 添加 Escape 键关闭对话框支持
        addEscapeKeyHandler(dialog);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return picker.getValue();
            }
            return null;
        });

        Optional<Color> result = dialog.showAndWait();
        result.ifPresent(canvas::setBackgroundColor);
    }

    /**
     * 删除对象
     */
    private static void deleteObject(WorldObject obj, GridChartView canvas, DrawingController controller) {
        // 查找所有约束到此图形的点，自动移除约束
        for (WorldObject o : canvas.getObjects()) {
            if (o instanceof PointGeo point && point.isConstrained()) {
                if (point.getConstraint().getConstrainedShape() == obj) {
                    point.setConstraint(null);  // 移除约束
                }
            }
        }

        // 删除对象
        canvas.removeObject(obj);
        canvas.redraw();
    }

    /**
     * 查找使用指定点的所有图形
     *
     * @param point   点对象
     * @param objects 所有图形列表
     * @return 使用该点的图形列表
     */
    private static List<WorldObject> findShapesUsingPoint(PointGeo point, List<WorldObject> objects) {
        List<WorldObject> result = new java.util.ArrayList<>();
        double px = point.getX();
        double py = point.getY();
        double threshold = 0.01; // 坐标匹配阈值

        for (WorldObject obj : objects) {
            if (obj == point) continue;

            // 检查线段
            if (obj instanceof LineGeo line) {
                if (line.getStartPointRef() == point || line.getEndPointRef() == point) {
                    result.add(obj);
                }
            }
            // 检查无限直线
            else if (obj instanceof InfiniteLineGeo infLine) {
                if (infLine.getPoint1Ref() == point || infLine.getPoint2Ref() == point) {
                    result.add(obj);
                }
            }
            // 检查圆（既检查引用，也检查坐标）
            else if (obj instanceof CircleGeo circle) {
                // 检查是否直接引用该点
                if (circle.getCenterPointRef() == point) {
                    result.add(obj);
                }
                // 检查圆心坐标是否与点重合（处理内部坐标的情况）
                else if (Math.abs(circle.getCx() - px) < threshold &&
                        Math.abs(circle.getCy() - py) < threshold) {
                    result.add(obj);
                }
            }
            // 检查多边形
            else if (obj instanceof PolygonGeo polygon) {
                for (PointGeo vertex : polygon.getVertexPoints()) {
                    if (vertex == point) {
                        result.add(obj);
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 为对话框添加 Escape 键关闭功能
     * <p>
     * 监听 Escape 键，自动关闭对话框（等同于点击 Cancel 按钮）
     *
     * @param dialog 任意 JavaFX Dialog 对象
     */
    private static void addEscapeKeyHandler(Dialog<?> dialog) {
        dialog.getDialogPane().setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                dialog.close();
                event.consume();
            }
        });
    }

    /**
     * 导出选中对象为PNG
     *
     * @param canvas     画布视图
     * @param controller 绘制控制器
     */
    private static void exportSelectionToPNG(GridChartView canvas, DrawingController controller) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nUtil.getString("geo.dialog.export.title"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );
        fileChooser.setInitialFileName("selection.png");

        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                // 创建快照
                WritableImage image = canvas.snapshot(new SnapshotParameters(), null);

                // 保存为PNG
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

                // 显示成功提示
                FxTools.showInfoAlert(
                        I18nUtil.getString("geo.dialog.export.success.title"),
                        I18nUtil.getString("geo.dialog.export.success.content")
                );
            } catch (IOException e) {
                // 显示错误提示
                FxTools.showErrorAlert(
                        I18nUtil.getString("geo.dialog.export.error.title"),
                        I18nUtil.getString("geo.dialog.export.error.content") + "\n" + e.getMessage()
                );
            }
        }
    }
}
