package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.controller.PreviewManager;
import com.bingbaihanji.util.EdgeSnapManager;
import com.bingbaihanji.util.GeometryCommand;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.util.PointReuseManager;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * 基础图形绘制处理器
 * <p>
 * 处理点、线段、无限直线、圆的绘制
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class BasicShapeHandler extends AbstractDrawingHandler {

    /**
     * 圆形预览对象
     */
    private PreviewManager.CirclePreview circlePreview = null;

    /**
     * 线段/直线预览对象
     */
    private PreviewManager.LinePreview linePreview = null;

    /**
     * 椭圆预览对象
     */
    private PreviewManager.EllipsePreview ellipsePreview = null;

    /**
     * 第一个点的世界坐标(用于绘制圆、线段、椭圆焦点1等)
     */
    private double firstPointX;
    private double firstPointY;
    /**
     * 第一次点击时复用的点引用
     */
    private PointGeo firstPointRef = null;

    /**
     * 第二个点的世界坐标（用于椭圆焦点2）
     */
    private double secondPointX;
    private double secondPointY;
    /**
     * 第二次点击时复用的点引用（椭圆焦点2）
     */
    private PointGeo secondPointRef = null;

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.POINT || mode == DrawMode.LINE ||
                mode == DrawMode.INFINITE_LINE || mode == DrawMode.CIRCLE ||
                mode == DrawMode.ELLIPSE;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, IDrawingContext context) {
        // 只处理左键
        if (e.getButton() != MouseButton.PRIMARY || !canHandle(context.getDrawMode())) {
            return false;
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用特殊点磁性吸附
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        // Shift 键约束：在第二次点击时也应用角度约束
        if (e.isShiftDown() && context.getState() == DrawingState.FIRST_CLICK) {
            double[] constrained = applyShiftConstraint(worldX, worldY);
            worldX = constrained[0];
            worldY = constrained[1];
        }

        if (context.getDrawMode() == DrawMode.POINT) {
            handlePointDrawing(worldX, worldY, context);
        } else if (context.getState() == DrawingState.IDLE) {
            handleFirstClick(rawX, rawY, worldX, worldY, context);
        } else if (context.getState() == DrawingState.FIRST_CLICK) {
            handleSecondClick(worldX, worldY, context);
        } else if (context.getState() == DrawingState.SECOND_CLICK) {
            handleThirdClick(worldX, worldY, context);
        }

        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, IDrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用网格吸附(所有模式都应用)
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        // Shift 键约束：在绘制线段/直线/圆时,强制吸附到特定角度(0°, 45°, 90°等)
        if (e.isShiftDown() && context.getState() == DrawingState.FIRST_CLICK) {
            double[] constrained = applyShiftConstraint(worldX, worldY);
            worldX = constrained[0];
            worldY = constrained[1];
        }

        // 保存当前鼠标位置用于预览
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 点模式：检测是否靠近可约束的图形,改变光标
        if (context.getDrawMode() == DrawMode.POINT) {
            double scale = context.getTransform().getScale();
            double snapDistance = GeometryConfig.Snapping.POINT_SNAP_THRESHOLD_PIXELS / scale; // 吸附距离阈值

            // 查找是否靠近可约束图形
            boolean nearConstrainableShape = false;
            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo || obj instanceof InfiniteLineGeo ||
                        obj instanceof CircleGeo || obj instanceof EllipseGeo ||
                        obj instanceof PolygonGeo || obj instanceof PathGeo ||
                        obj instanceof FunctionGeo) {
                    PointConstraint tempConstraint = context.getConstraintHandler().createConstraint(obj);
                    double distance = tempConstraint.distanceToShape(rawX, rawY);

                    if (distance < snapDistance) {
                        nearConstrainableShape = true;
                        break;
                    }
                }
            }

            // 根据是否靠近图形设置光标
            if (nearConstrainableShape) {
                context.getGridChartPane().setCursor(javafx.scene.Cursor.HAND);
            } else {
                context.getGridChartPane().setCursor(context.getGridChartPane().getDefaultCursor());
            }

            // 重绘以显示吸附预览点
            context.redraw();
            return true;
        }

        // 其他模式：预览逻辑
        if (context.getState() != DrawingState.FIRST_CLICK && context.getState() != DrawingState.SECOND_CLICK) {
            // 即使在 IDLE 状态,也重绘以显示吸附预览点
            context.redraw();
            return true;
        }

        if (context.getDrawMode() == DrawMode.CIRCLE) {
            // 计算预览半径
            double previewRadius = MathCalculationUtils.sqrt(
                    MathCalculationUtils.pow(worldX - firstPointX, 2) + MathCalculationUtils.pow(worldY - firstPointY, 2)
            );

            // 检测圆相切吸附
            double tangentThreshold = GeometryConfig.Snapping.CIRCLE_TANGENT_THRESHOLD_PIXELS / context.getTransform().getScale();
            EdgeSnapManager.CircleTangentResult tangentResult =
                    EdgeSnapManager.findCircleTangentSnap(firstPointX, firstPointY, previewRadius,
                            context.getObjects(), tangentThreshold);

            // 如果找到相切吸附,使用调整后的半径
            if (tangentResult != null) {
                previewRadius = tangentResult.getRadius();
            }

            // 更新圆形预览
            if (circlePreview != null) {
                circlePreview.updatePreview(worldX, worldY);
            }
        } else if (context.getDrawMode() == DrawMode.ELLIPSE) {
            if (ellipsePreview != null) {
                ellipsePreview.updatePreview(worldX, worldY);
            }
        } else if (context.getDrawMode() == DrawMode.LINE || context.getDrawMode() == DrawMode.INFINITE_LINE) {
            // 更新线段/直线预览
            if (linePreview != null) {
                linePreview.updatePreview(worldX, worldY);
            }
        }

        // 重绘以显示预览
        context.redraw();
        return true;
    }


    /**
     * 处理点模式的绘制
     */
    private void handlePointDrawing(double worldX, double worldY, IDrawingContext context) {
        // 点模式：检测是否靠近图形(吸附)
        double scale = context.getTransform().getScale();
        double snapDistance = GeometryConfig.Snapping.POINT_SNAP_THRESHOLD_PIXELS / scale; // 吸附距离阈值

        // 查找最近的可约束图形
        WorldObject nearestShape = null;
        double minDistance = snapDistance;

        for (WorldObject obj : context.getObjects()) {
            if (obj instanceof LineGeo || obj instanceof InfiniteLineGeo ||
                    obj instanceof CircleGeo || obj instanceof EllipseGeo ||
                    obj instanceof PolygonGeo || obj instanceof PathGeo) {
                // 创建临时约束来计算距离
                PointConstraint tempConstraint = context.getConstraintHandler().createConstraint(obj);
                double distance = tempConstraint.distanceToShape(worldX, worldY);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestShape = obj;
                }
            }
        }

        PointGeo newPoint;
        if (nearestShape != null) {
            // 先创建点
            newPoint = new PointGeo(worldX, worldY);

            // 创建约束并自动检测是否为顶点
            PointConstraint constraint = context.getConstraintHandler().createConstraint(nearestShape, newPoint);

            // 如果不是顶点约束,计算参数
            if (!constraint.isVertexConstraint()) {
                double parameter = constraint.calculateParameter(worldX, worldY);
                constraint.setParameter(parameter);
            }

            // 更新点位置到约束位置
            Point2D constrainedPos = constraint.getPointFromParameter();
            newPoint.updatePosition(constrainedPos.getX(), constrainedPos.getY());
            newPoint.setConstraint(constraint);
        } else {
            // 创建普通点
            newPoint = new PointGeo(worldX, worldY);
        }

        context.executeCommand(new GeometryCommand(context, newPoint));

        context.setState(DrawingState.IDLE);
    }

    /**
     * 处理第一次点击
     */
    private void handleFirstClick(double rawX, double rawY, double worldX, double worldY, IDrawingContext context) {
        // 第一次点击：记录起点,进入预览状态
        firstPointX = worldX;
        firstPointY = worldY;
        context.setState(DrawingState.FIRST_CLICK);

        // 检查第一次点击位置是否已有点对象(用于复用)
        double scale = context.getTransform().getScale();
        firstPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        // 创建预览对象并注册到 PreviewManager
        if (context.getDrawMode() == DrawMode.CIRCLE) {
            circlePreview = new PreviewManager.CirclePreview();
            circlePreview.setCenterPoint(firstPointX, firstPointY);
            context.getPreviewManager().addPreviewable(circlePreview);
        } else if (context.getDrawMode() == DrawMode.ELLIPSE) {
            ellipsePreview = new PreviewManager.EllipsePreview();
            ellipsePreview.setFocus1(firstPointX, firstPointY);
            context.getPreviewManager().addPreviewable(ellipsePreview);
        } else if (context.getDrawMode() == DrawMode.LINE) {
            linePreview = new PreviewManager.LinePreview(false); // 普通线段
            linePreview.setStartPoint(firstPointX, firstPointY);
            context.getPreviewManager().addPreviewable(linePreview);
        } else if (context.getDrawMode() == DrawMode.INFINITE_LINE) {
            linePreview = new PreviewManager.LinePreview(true); // 无限直线
            linePreview.setStartPoint(firstPointX, firstPointY);
            context.getPreviewManager().addPreviewable(linePreview);
        }
    }

    /**
     * 处理第二次点击
     */
    private void handleSecondClick(double worldX, double worldY, IDrawingContext context) {
        double scale = context.getTransform().getScale();

        // 检查第二次点击位置是否已有点对象(用于复用)
        PointGeo secondPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

        switch (context.getDrawMode()) {
            case CIRCLE -> {
                double radius = MathCalculationUtils.sqrt(
                        MathCalculationUtils.pow(worldX - firstPointX, 2) + MathCalculationUtils.pow(worldY - firstPointY, 2)
                );

                // 检测圆相切吸附
                double tangentThreshold = GeometryConfig.Snapping.CIRCLE_TANGENT_THRESHOLD_PIXELS / scale;
                EdgeSnapManager.CircleTangentResult tangentResult =
                        EdgeSnapManager.findCircleTangentSnap(firstPointX, firstPointY, radius,
                                context.getObjects(), tangentThreshold);

                double finalRadius = (tangentResult != null) ? tangentResult.getRadius() : radius;

                // 创建圆,复用已有的圆心点
                CircleGeo newCircle = new CircleGeo(firstPointRef, firstPointX, firstPointY, finalRadius);

                context.executeCommand(new GeometryCommand(context, newCircle));

                // 清除预览对象
                if (circlePreview != null) {
                    context.getPreviewManager().removePreviewable(circlePreview);
                    circlePreview = null;
                }
                firstPointRef = null;
            }
            case ELLIPSE -> {
                // 椭圆：第二次点击设置焦点2，进入三阶段模式
                secondPointX = worldX;
                secondPointY = worldY;
                secondPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

                if (ellipsePreview != null) {
                    ellipsePreview.setFocus2(worldX, worldY);
                }
                firstPointRef = null;
                context.setState(DrawingState.SECOND_CLICK);
                context.redraw();
                return; // 不执行后面的 setState(IDLE)
            }
            case LINE -> {
                // 创建线段,复用已有的端点
                LineGeo newLine = new LineGeo(firstPointRef, firstPointX, firstPointY,
                        secondPointRef, worldX, worldY);

                context.executeCommand(new GeometryCommand(context, newLine));

                // 清除预览对象
                if (linePreview != null) {
                    context.getPreviewManager().removePreviewable(linePreview);
                    linePreview = null;
                }
                firstPointRef = null;
            }
            case INFINITE_LINE -> {
                // 创建无限直线,复用已有的端点
                InfiniteLineGeo newLine = new InfiniteLineGeo(firstPointRef, firstPointX, firstPointY,
                        secondPointRef, worldX, worldY);

                context.executeCommand(new GeometryCommand(context, newLine));

                // 清除预览对象
                if (linePreview != null) {
                    context.getPreviewManager().removePreviewable(linePreview);
                    linePreview = null;
                }
                firstPointRef = null;
            }
        }

        context.setState(DrawingState.IDLE);
        context.redraw();
    }

    /**
     * 处理第三次点击（椭圆确认：确定椭圆上一点）
     */
    private void handleThirdClick(double worldX, double worldY, IDrawingContext context) {
        if (context.getDrawMode() != DrawMode.ELLIPSE || ellipsePreview == null) {
            context.setState(DrawingState.IDLE);
            return;
        }

        double twoA = ellipsePreview.getTwoA();
        if (twoA <= 0) {
            context.setState(DrawingState.IDLE);
            return;
        }

        // 确保 2a > 焦距
        double focalDist = Math.hypot(secondPointX - firstPointX, secondPointY - firstPointY);
        if (twoA <= focalDist) {
            context.setState(DrawingState.IDLE);
            return;
        }

        // 创建椭圆
        EllipseGeo ellipse = new EllipseGeo(firstPointRef, firstPointX, firstPointY,
                secondPointRef, secondPointX, secondPointY, twoA);

        context.executeCommand(new GeometryCommand(context, ellipse));

        // 清除预览对象
        if (ellipsePreview != null) {
            context.getPreviewManager().removePreviewable(ellipsePreview);
            ellipsePreview = null;
        }
        firstPointRef = null;
        secondPointRef = null;

        context.setState(DrawingState.IDLE);
        context.redraw();
    }

    /**
     * 应用 Shift 键角度约束
     * <p>
     * 将当前点约束到从第一个点出发的特定角度上(0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°)
     *
     * @param worldX 当前鼠标的世界坐标 X
     * @param worldY 当前鼠标的世界坐标 Y
     * @return 约束后的坐标 [x, y]
     */
    private double[] applyShiftConstraint(double worldX, double worldY) {
        double dx = worldX - firstPointX;
        double dy = worldY - firstPointY;

        // 计算当前角度
        double angle = Math.atan2(dy, dx);

        // 将角度吸附到最近的 45 度倍数(π/4 弧度)
        double snappedAngle = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);

        // 计算到起点的距离
        double length = MathCalculationUtils.hypot(dx, dy);

        // 使用约束后的角度计算新坐标
        double constrainedX = firstPointX + length * Math.cos(snappedAngle);
        double constrainedY = firstPointY + length * Math.sin(snappedAngle);

        return new double[]{constrainedX, constrainedY};
    }

    @Override
    public void reset() {
        firstPointX = 0;
        firstPointY = 0;
        firstPointRef = null;
        secondPointX = 0;
        secondPointY = 0;
        secondPointRef = null;

        // 清除预览对象
        circlePreview = null;
        linePreview = null;
        ellipsePreview = null;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, IDrawingContext context) {
        // PreviewManager 统一管理,此处只绘制补充效果(吸附预览点)
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        double pointRadius = 4;

        // 在 IDLE 状态下,显示吸附预览点
        if (context.getState() == DrawingState.IDLE) {
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            gc.setStroke(GeometryConfig.Colors.PREVIEW);
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);

            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
            return;
        }

        // 在 FIRST_CLICK 状态下,绘制第一个点和第二个点的预览
        if (context.getState() == DrawingState.FIRST_CLICK) {
            // 绘制第一个点(实心点)
            double firstPointScreenX = transform.worldToScreenX(firstPointX);
            double firstPointScreenY = transform.worldToScreenY(firstPointY);
            gc.setFill(GeometryConfig.Colors.PREVIEW);
            gc.fillOval(firstPointScreenX - pointRadius, firstPointScreenY - pointRadius, pointRadius * 2, pointRadius * 2);

            // 椭圆模式下，F1 已固定，显示焦点标签
            if (context.getDrawMode() == DrawMode.ELLIPSE) {
                drawFocusLabel(gc, transform, firstPointX, firstPointY, "F1");
            }

            // 绘制第二个点的预览(半透明点,跟随鼠标)
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());

            // 外圈光晕效果
            gc.setStroke(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);

            // 内部半透明填充点
            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }

        // 在 SECOND_CLICK 状态下（椭圆），绘制 F1 和 F2
        if (context.getState() == DrawingState.SECOND_CLICK && context.getDrawMode() == DrawMode.ELLIPSE) {
            drawFocusLabel(gc, transform, firstPointX, firstPointY, "F1");
            drawFocusLabel(gc, transform, secondPointX, secondPointY, "F2");

            // 绘制鼠标跟随点
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            gc.setStroke(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(1.5);
            gc.strokeOval(mouseScreenX - 6, mouseScreenY - 6, 12, 12);
            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }
    }

    /**
     * 绘制焦点标签
     */
    private void drawFocusLabel(GraphicsContext gc, WorldTransform transform,
                                double wx, double wy, String label) {
        double sx = transform.worldToScreenX(wx);
        double sy = transform.worldToScreenY(wy);
        gc.setFill(GeometryConfig.Colors.PREVIEW);
        gc.fillOval(sx - 4, sy - 4, 8, 8);
        gc.setStroke(GeometryConfig.Colors.PREVIEW);
        gc.setLineWidth(1);
        gc.strokeText(label, sx + 6, sy - 6);
    }
}
