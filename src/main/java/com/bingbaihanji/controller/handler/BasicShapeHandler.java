package com.bingbaihanji.controller.handler;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.geometry.EdgeSnapManager;
import com.bingbaihanji.controller.GeometryCommand;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.geometry.PointReuseManager;
import com.bingbaihanji.geometry.constraint.PointConstraint;
import com.bingbaihanji.geometry.WorldTransform;
import com.bingbaihanji.geometry.WorldObject;
import com.bingbaihanji.geometry.impl.*;
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

    // ==== 预览状态 ====

    /**
     * 线段/直线/角度射线预览是否激活
     */
    private boolean linePreviewActive;
    /**
     * 线段预览是否为无限直线
     */
    private boolean linePreviewInfinite;
    /**
     * 射线起点
     */
    private double linePreviewStartX;
    private double linePreviewStartY;
    /**
     * 射线跟随鼠标的终点
     */
    private double linePreviewEndX;
    private double linePreviewEndY;

    /**
     * 圆预览是否激活
     */
    private boolean circlePreviewActive;
    /**
     * 圆心
     */
    private double circlePreviewCenterX;
    private double circlePreviewCenterY;
    /**
     * 鼠标位置与对应半径
     */
    private double circlePreviewMouseX;
    private double circlePreviewMouseY;
    private double circlePreviewRadius;

    /**
     * 椭圆预览是否激活
     */
    private boolean ellipsePreviewActive;
    /**
     * 椭圆焦点2是否已确定
     */
    private boolean ellipsePreviewF2Set;
    private double ellipsePreviewF1x;
    private double ellipsePreviewF1y;
    private double ellipsePreviewF2x;
    private double ellipsePreviewF2y;
    /**
     * 鼠标位置(用于计算 2a)
     */
    private double ellipsePreviewMouseX;
    private double ellipsePreviewMouseY;

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
                mode == DrawMode.ELLIPSE || mode == DrawMode.ANGLE ||
                mode == DrawMode.TEXT;
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

            // 更新圆形预览状态
            if (circlePreviewActive) {
                circlePreviewMouseX = worldX;
                circlePreviewMouseY = worldY;
                circlePreviewRadius = previewRadius;
            }
        } else if (context.getDrawMode() == DrawMode.ELLIPSE) {
            if (ellipsePreviewActive) {
                ellipsePreviewMouseX = worldX;
                ellipsePreviewMouseY = worldY;
            }
        } else if (context.getDrawMode() == DrawMode.ANGLE) {
            // 角度预览：FIRST_CLICK 时鼠标位置作为射线1终点，SECOND_CLICK 时作为射线2终点
            if (linePreviewActive) {
                linePreviewEndX = worldX;
                linePreviewEndY = worldY;
            }
        } else if (context.getDrawMode() == DrawMode.LINE || context.getDrawMode() == DrawMode.INFINITE_LINE) {
            // 更新线段/直线预览
            if (linePreviewActive) {
                linePreviewEndX = worldX;
                linePreviewEndY = worldY;
            }
        }

        // 重绘以显示预览
        context.redraw();
        return true;
    }


    /**
     * 处理文本模式的单次点击（弹出对话框后创建文本标注）
     */
    private void handleTextDrawing(double worldX, double worldY, IDrawingContext context) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("自由文本标注");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入文本内容:");
        dialog.showAndWait().ifPresent(text -> {
            if (!text.isBlank()) {
                TextGeo textGeo = new TextGeo(worldX, worldY, text.trim());
                context.executeCommand(new GeometryCommand(context, textGeo));
                context.redraw();
            }
        });
        context.setState(DrawingState.IDLE);
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

        // 进入预览状态
        if (context.getDrawMode() == DrawMode.CIRCLE) {
            startCirclePreview(firstPointX, firstPointY);
        } else if (context.getDrawMode() == DrawMode.ELLIPSE) {
            startEllipsePreview(firstPointX, firstPointY);
        } else if (context.getDrawMode() == DrawMode.LINE) {
            startLinePreview(firstPointX, firstPointY, false);
        } else if (context.getDrawMode() == DrawMode.INFINITE_LINE) {
            startLinePreview(firstPointX, firstPointY, true);
        } else if (context.getDrawMode() == DrawMode.ANGLE) {
            // 角度：第一次点击设置顶点，使用直线预览显示射线
            startLinePreview(firstPointX, firstPointY, false);
        } else if (context.getDrawMode() == DrawMode.TEXT) {
            // 文本：单击后弹出对话框创建文本标注
            handleTextDrawing(worldX, worldY, context);
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

                // 清除预览状态
                stopCirclePreview();
                firstPointRef = null;
            }
            case ANGLE -> {
                // 角度：第二次点击设置射线1上的点，进入三阶段模式
                secondPointX = worldX;
                secondPointY = worldY;
                this.secondPointRef = secondPointRef;
                firstPointRef = null;
                context.setState(DrawingState.SECOND_CLICK);
                context.redraw();
                return; // 不执行 setState(IDLE)
            }
            case ELLIPSE -> {
                // 椭圆：第二次点击设置焦点2，进入三阶段模式
                secondPointX = worldX;
                secondPointY = worldY;
                secondPointRef = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);

                if (ellipsePreviewActive) {
                    setEllipsePreviewFocus2(worldX, worldY);
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

                // 清除预览状态
                stopLinePreview();
                firstPointRef = null;
            }
            case INFINITE_LINE -> {
                // 创建无限直线,复用已有的端点
                InfiniteLineGeo newLine = new InfiniteLineGeo(firstPointRef, firstPointX, firstPointY,
                        secondPointRef, worldX, worldY);

                context.executeCommand(new GeometryCommand(context, newLine));

                // 清除预览状态
                stopLinePreview();
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
        // 角度：第三次点击设置射线2上的点，创建角度图形
        if (context.getDrawMode() == DrawMode.ANGLE) {
            // 支持点复用：如点击位置已有 PointGeo，则引用之
            double scale = context.getTransform().getScale();
            PointGeo ray2Ref = PointReuseManager.getExistingPointOrNull(worldX, worldY, context.getObjects(), scale);
            AngleGeo angle = new AngleGeo(firstPointRef, firstPointX, firstPointY,
                    secondPointRef, secondPointX, secondPointY,
                    ray2Ref, worldX, worldY);
            // 设置标签显示度数
            angle.setLabel(String.format("%.1f°", angle.getAngleDegrees()));
            angle.setLabelVisible(true);
            context.executeCommand(new GeometryCommand(context, angle));
            reset();
            context.setState(DrawingState.IDLE);
            context.redraw();
            return;
        }

        if (context.getDrawMode() != DrawMode.ELLIPSE || !ellipsePreviewActive) {
            context.setState(DrawingState.IDLE);
            return;
        }

        double twoA = ellipsePreviewTwoA();
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

        // 清除预览状态
        stopEllipsePreview();
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

        // 清除预览状态
        stopCirclePreview();
        stopLinePreview();
        stopEllipsePreview();
    }

    // ==== 预览状态与绘制 ====

    /**
     * 开始线段/直线/射线预览
     *
     * @param x        起点 X
     * @param y        起点 Y
     * @param infinite 是否为无限直线
     */
    private void startLinePreview(double x, double y, boolean infinite) {
        linePreviewActive = true;
        linePreviewInfinite = infinite;
        linePreviewStartX = x;
        linePreviewStartY = y;
        linePreviewEndX = x;
        linePreviewEndY = y;
    }

    /**
     * 结束线段/直线/射线预览
     */
    private void stopLinePreview() {
        linePreviewActive = false;
    }

    /**
     * 开始圆预览
     *
     * @param x 圆心 X
     * @param y 圆心 Y
     */
    private void startCirclePreview(double x, double y) {
        circlePreviewActive = true;
        circlePreviewCenterX = x;
        circlePreviewCenterY = y;
        circlePreviewMouseX = x;
        circlePreviewMouseY = y;
        circlePreviewRadius = 0;
    }

    /**
     * 结束圆预览
     */
    private void stopCirclePreview() {
        circlePreviewActive = false;
        circlePreviewRadius = 0;
    }

    /**
     * 开始椭圆预览
     *
     * @param f1x 焦点1 X
     * @param f1y 焦点1 Y
     */
    private void startEllipsePreview(double f1x, double f1y) {
        ellipsePreviewActive = true;
        ellipsePreviewF2Set = false;
        ellipsePreviewF1x = f1x;
        ellipsePreviewF1y = f1y;
        ellipsePreviewMouseX = f1x;
        ellipsePreviewMouseY = f1y;
    }

    /**
     * 确定椭圆焦点2
     *
     * @param x 焦点2 X
     * @param y 焦点2 Y
     */
    private void setEllipsePreviewFocus2(double x, double y) {
        ellipsePreviewF2x = x;
        ellipsePreviewF2y = y;
        ellipsePreviewF2Set = true;
    }

    /**
     * 结束椭圆预览
     */
    private void stopEllipsePreview() {
        ellipsePreviewActive = false;
        ellipsePreviewF2Set = false;
    }

    /**
     * 当前 2a 值 = 鼠标到两焦点距离之和
     *
     * @return 2a, 焦点2未确定时为 0
     */
    private double ellipsePreviewTwoA() {
        if (!ellipsePreviewF2Set) {
            return 0;
        }
        return Math.hypot(ellipsePreviewMouseX - ellipsePreviewF1x, ellipsePreviewMouseY - ellipsePreviewF1y)
                + Math.hypot(ellipsePreviewMouseX - ellipsePreviewF2x, ellipsePreviewMouseY - ellipsePreviewF2y);
    }

    /**
     * 绘制线段/直线/射线预览
     */
    private void paintLinePreview(GraphicsContext gc, WorldTransform transform) {
        if (!linePreviewActive) {
            return;
        }

        double sx1 = transform.worldToScreenX(linePreviewStartX);
        double sy1 = transform.worldToScreenY(linePreviewStartY);
        double sx2 = transform.worldToScreenX(linePreviewEndX);
        double sy2 = transform.worldToScreenY(linePreviewEndY);

        gc.save();
        gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
        gc.setLineWidth(1.5);
        gc.setLineDashes(6);

        if (linePreviewInfinite) {
            // 扩展直线到画布边缘
            double dx = sx2 - sx1;
            double dy = sy2 - sy1;
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 1e-6) {
                double t = 10000 / length;
                sx1 = sx1 - t * dx;
                sy1 = sy1 - t * dy;
                sx2 = sx1 + 2 * t * dx;
                sy2 = sy1 + 2 * t * dy;
            }
        }

        gc.strokeLine(sx1, sy1, sx2, sy2);
        gc.restore();
    }

    /**
     * 绘制圆预览(圆环 + 半径线)
     */
    private void paintCirclePreview(GraphicsContext gc, WorldTransform transform) {
        if (!circlePreviewActive || circlePreviewRadius < 1e-6) {
            return;
        }

        double screenCx = transform.worldToScreenX(circlePreviewCenterX);
        double screenCy = transform.worldToScreenY(circlePreviewCenterY);
        double screenRadius = circlePreviewRadius * transform.getScale();

        gc.save();
        gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
        gc.setLineWidth(1.5);
        gc.setLineDashes(6);
        gc.strokeOval(screenCx - screenRadius, screenCy - screenRadius,
                screenRadius * 2, screenRadius * 2);

        // 半径线：从圆心指向鼠标位置
        gc.setLineDashes(2);
        gc.strokeLine(screenCx, screenCy,
                transform.worldToScreenX(circlePreviewMouseX),
                transform.worldToScreenY(circlePreviewMouseY));

        gc.restore();
    }

    /**
     * 绘制椭圆预览(焦点、连线、椭圆轮廓、跟随点)
     */
    private void paintEllipsePreview(GraphicsContext gc, WorldTransform transform) {
        if (!ellipsePreviewActive) {
            return;
        }

        gc.save();
        gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
        gc.setLineWidth(1.5);

        // 焦点 1
        drawPreviewFocusDot(gc, transform, ellipsePreviewF1x, ellipsePreviewF1y);

        if (ellipsePreviewF2Set) {
            // 焦点 2 与焦点连线
            drawPreviewFocusDot(gc, transform, ellipsePreviewF2x, ellipsePreviewF2y);
            gc.setLineDashes(2);
            gc.strokeLine(
                    transform.worldToScreenX(ellipsePreviewF1x), transform.worldToScreenY(ellipsePreviewF1y),
                    transform.worldToScreenX(ellipsePreviewF2x), transform.worldToScreenY(ellipsePreviewF2y));

            double a = ellipsePreviewTwoA() / 2.0;
            double c = Math.hypot(ellipsePreviewF2x - ellipsePreviewF1x, ellipsePreviewF2y - ellipsePreviewF1y) / 2.0;
            if (a > c && a > 1e-9) {
                double b = Math.sqrt(a * a - c * c);
                double cx = (ellipsePreviewF1x + ellipsePreviewF2x) / 2.0;
                double cy = (ellipsePreviewF1y + ellipsePreviewF2y) / 2.0;
                double angle = Math.atan2(ellipsePreviewF2y - ellipsePreviewF1y,
                        ellipsePreviewF2x - ellipsePreviewF1x);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);

                gc.setLineDashes(6);
                int n = 128;
                double[] sx = new double[n];
                double[] sy = new double[n];
                for (int i = 0; i < n; i++) {
                    double t = 2 * Math.PI * i / n;
                    double xt = a * Math.cos(t);
                    double yt = b * Math.sin(t);
                    sx[i] = transform.worldToScreenX(cx + xt * cos - yt * sin);
                    sy[i] = transform.worldToScreenY(cy + xt * sin + yt * cos);
                }
                gc.strokePolygon(sx, sy, n);
            }
        }

        // 跟随鼠标的半透明点
        double mouseSx = transform.worldToScreenX(ellipsePreviewMouseX);
        double mouseSy = transform.worldToScreenY(ellipsePreviewMouseY);
        gc.setStroke(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(1.5);
        gc.setLineDashes(null);
        gc.strokeOval(mouseSx - 6, mouseSy - 6, 12, 12);
        gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
        gc.fillOval(mouseSx - 4, mouseSy - 4, 8, 8);

        gc.restore();
    }

    /**
     * 绘制椭圆预览的焦点圆点
     */
    private void drawPreviewFocusDot(GraphicsContext gc, WorldTransform transform, double wx, double wy) {
        double sx = transform.worldToScreenX(wx);
        double sy = transform.worldToScreenY(wy);
        gc.setFill(GeometryConfig.Colors.PREVIEW);
        gc.fillOval(sx - 4, sy - 4, 8, 8);
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, IDrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        // 图形轮廓预览(线段/圆/椭圆)
        paintLinePreview(gc, transform);
        paintCirclePreview(gc, transform);
        paintEllipsePreview(gc, transform);

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

        // 角度预览：绘制从顶点出发的无限射线 + 角度弧线
        if (context.getDrawMode() == DrawMode.ANGLE && context.getState() != DrawingState.IDLE) {
            drawAnglePreview(gc, transform, context);
        }
    }

    /**
     * 角度预览：无限射线 + 弧线
     */
    private void drawAnglePreview(GraphicsContext gc, WorldTransform transform, IDrawingContext context) {
        double vsx = transform.worldToScreenX(firstPointX);
        double vsy = transform.worldToScreenY(firstPointY);
        double msx = transform.worldToScreenX(context.getCurrentMouseX());
        double msy = transform.worldToScreenY(context.getCurrentMouseY());
        double w = context.getGridChartPane().getWidth();
        double h = context.getGridChartPane().getHeight();

        gc.setStroke(GeometryConfig.Colors.PREVIEW);
        gc.setLineWidth(1.5);
        gc.setLineDashes(6);

        // 射线1：从顶点出发，经过鼠标位置，延伸到屏幕边缘
        double[] ends1 = extendToScreen(vsx, vsy, msx, msy, w, h);
        gc.strokeLine(vsx, vsy, ends1[0], ends1[1]);

        if (context.getState() == DrawingState.SECOND_CLICK) {
            // 射线2：从顶点出发，经过 ray1 方向，延伸到屏幕边缘
            double r1sx = transform.worldToScreenX(secondPointX);
            double r1sy = transform.worldToScreenY(secondPointY);
            double[] ends2 = extendToScreen(vsx, vsy, r1sx, r1sy, w, h);
            gc.strokeLine(vsx, vsy, ends2[0], ends2[1]);

            // 弧线：在 arcR 像素处
            double arcR = 20;
            double len1 = Math.hypot(ends1[0] - vsx, ends1[1] - vsy);
            double len2 = Math.hypot(ends2[0] - vsx, ends2[1] - vsy);
            double ax = vsx + (ends1[0] - vsx) / len1 * arcR;
            double ay = vsy + (ends1[1] - vsy) / len1 * arcR;
            double bx = vsx + (ends2[0] - vsx) / len2 * arcR;
            double by = vsy + (ends2[1] - vsy) / len2 * arcR;
            drawAngleArc(gc, vsx, vsy, ax, ay, bx, by);

            // 显示预览角度值（currentMouse 已经是世界坐标）
            double mx = context.getCurrentMouseX();
            double my = context.getCurrentMouseY();
            double dx1 = secondPointX - firstPointX, dy1 = secondPointY - firstPointY;
            double dx2 = mx - firstPointX, dy2 = my - firstPointY;
            double dot = dx1 * dx2 + dy1 * dy2;
            double l1 = Math.hypot(dx1, dy1), l2 = Math.hypot(dx2, dy2);
            if (l1 > 1e-10 && l2 > 1e-10) {
                double deg = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot / (l1 * l2)))));
                String text = String.format("%.1f°", deg);
                gc.setFill(GeometryConfig.Colors.PREVIEW);
                gc.fillText(text, vsx + 25, vsy - 10);
            }
        }
        gc.setLineDashes(null);
    }

    /**
     * 从起点沿目标方向延伸到屏幕边缘
     */
    private double[] extendToScreen(double sx, double sy, double tx, double ty, double w, double h) {
        double dx = tx - sx, dy = ty - sy;
        if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) return new double[]{tx, ty};
        double maxDim = Math.max(w, h) * 2;
        double len = Math.hypot(dx, dy);
        return new double[]{sx + dx / len * maxDim, sy + dy / len * maxDim};
    }

    /**
     * 绘制角度预览弧线
     */
    private void drawAngleArc(GraphicsContext gc, double cx, double cy,
                              double ax, double ay, double bx, double by) {
        double startAngle = Math.toDegrees(Math.atan2(-(ay - cy), ax - cx));
        double endAngle = Math.toDegrees(Math.atan2(-(by - cy), bx - cx));
        if (startAngle < 0) startAngle += 360;
        if (endAngle < 0) endAngle += 360;
        double arcExtent = endAngle - startAngle;
        if (Math.abs(arcExtent) > 180)
            arcExtent = arcExtent > 0 ? arcExtent - 360 : arcExtent + 360;
        double radius = 20;
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                startAngle, arcExtent, javafx.scene.shape.ArcType.OPEN);
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
