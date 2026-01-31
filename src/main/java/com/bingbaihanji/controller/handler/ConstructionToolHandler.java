package com.bingbaihanji.controller.handler;

import com.bingbaihanji.constant.DrawMode;
import com.bingbaihanji.constant.DrawingState;
import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.CommandHistory;
import com.bingbaihanji.util.IntersectionUtils;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.CircleGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.InfiniteLineGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.LineGeo;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * 作图工具处理器
 * <p>
 * 处理中点、垂线、平行线、垂直平分线、切线等作图工具
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class ConstructionToolHandler extends AbstractDrawingHandler {

    /**
     * 已选中的线段或直线（用于垂线和平行线）
     */
    private WorldObject selectedLine = null;

    @Override
    public boolean canHandle(DrawMode mode) {
        return mode == DrawMode.MIDPOINT || mode == DrawMode.PERPENDICULAR ||
                mode == DrawMode.PARALLEL || mode == DrawMode.PERPENDICULAR_BISECTOR ||
                mode == DrawMode.TANGENT;
    }

    @Override
    public boolean handleMouseClicked(MouseEvent e, DrawingContext context) {
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

        switch (context.getDrawMode()) {
            case MIDPOINT -> handleMidpointClick(worldX, worldY, context);
            case PERPENDICULAR -> handlePerpendicularClick(worldX, worldY, context);
            case PARALLEL -> handleParallelClick(worldX, worldY, context);
            case PERPENDICULAR_BISECTOR -> handlePerpendicularBisectorClick(worldX, worldY, context);
            case TANGENT -> handleTangentClick(worldX, worldY, context);
        }

        e.consume();
        return true;
    }

    @Override
    public boolean handleMouseMoved(MouseEvent e, DrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return false;
        }

        double rawX = context.getGridChartPane().screenToWorldX(e.getX());
        double rawY = context.getGridChartPane().screenToWorldY(e.getY());

        // 应用特殊点磁性吸附
        double[] snapped = context.getSnappingHandler().applySnapping(rawX, rawY, context);
        double worldX = snapped[0];
        double worldY = snapped[1];

        // 保存当前鼠标位置用于预览
        context.setCurrentMouseX(worldX);
        context.setCurrentMouseY(worldY);

        // 重绘以显示预览（IDLE状态下的悬停预览 或 FIRST_CLICK状态下的构造预览）
        if ((context.getDrawMode() == DrawMode.PERPENDICULAR || context.getDrawMode() == DrawMode.PARALLEL) &&
                (context.getState() == DrawingState.IDLE || context.getState() == DrawingState.FIRST_CLICK)) {
            context.redraw();
            return true;
        }

        // 中点、垂直平分线、切线模式：在IDLE状态下显示悬停预览
        if ((context.getDrawMode() == DrawMode.MIDPOINT ||
                context.getDrawMode() == DrawMode.PERPENDICULAR_BISECTOR ||
                context.getDrawMode() == DrawMode.TANGENT) &&
                context.getState() == DrawingState.IDLE) {
            context.redraw();
            return true;
        }

        // 其他模式仅在FIRST_CLICK状态下预览
        if (context.getState() == DrawingState.FIRST_CLICK) {
            context.redraw();
            return true;
        }

        return false;
    }

    @Override
    public void paintPreview(GraphicsContext gc, WorldTransform transform, DrawingContext context) {
        if (!canHandle(context.getDrawMode())) {
            return;
        }

        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale;
        double worldX = context.getCurrentMouseX();
        double worldY = context.getCurrentMouseY();

        // 在垂线和平行线模式下，IDLE状态显示悬停高亮
        if ((context.getDrawMode() == DrawMode.PERPENDICULAR || context.getDrawMode() == DrawMode.PARALLEL) &&
                context.getState() == DrawingState.IDLE) {
            // 高亮显示鼠标悬停的线段/直线
            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo || obj instanceof InfiniteLineGeo) {
                    if (obj.hitTest(worldX, worldY, tolerance)) {
                        // 高亮显示可选择的线
                        obj.setHover(true);
                        obj.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
                        obj.setHover(false);
                        break; // 只高亮最上层的一条线
                    }
                }
            }
            return;
        }

        // 中点模式：高亮悬停的线段/直线，并显示中点预览
        if (context.getDrawMode() == DrawMode.MIDPOINT && context.getState() == DrawingState.IDLE) {
            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        // 高亮显示线段
                        line.setHover(true);
                        line.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
                        line.setHover(false);

                        // 显示中点预览
                        double midX = (line.getStartX() + line.getEndX()) / 2.0;
                        double midY = (line.getStartY() + line.getEndY()) / 2.0;
                        double midScreenX = transform.worldToScreenX(midX);
                        double midScreenY = transform.worldToScreenY(midY);

                        gc.setFill(Color.GREEN);
                        double pointRadius = 4;
                        gc.fillOval(midScreenX - pointRadius, midScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
                        break;
                    }
                } else if (obj instanceof InfiniteLineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        // 高亮显示直线
                        line.setHover(true);
                        line.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
                        line.setHover(false);

                        // 显示定义点的中点预览
                        double midX = (line.getPoint1X() + line.getPoint2X()) / 2.0;
                        double midY = (line.getPoint1Y() + line.getPoint2Y()) / 2.0;
                        double midScreenX = transform.worldToScreenX(midX);
                        double midScreenY = transform.worldToScreenY(midY);

                        gc.setFill(Color.GREEN);
                        double pointRadius = 4;
                        gc.fillOval(midScreenX - pointRadius, midScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
                        break;
                    }
                }
            }
            return;
        }

        // 垂直平分线模式：高亮悬停的线段，并显示垂直平分线预览
        if (context.getDrawMode() == DrawMode.PERPENDICULAR_BISECTOR && context.getState() == DrawingState.IDLE) {
            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        // 高亮显示线段
                        line.setHover(true);
                        line.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
                        line.setHover(false);

                        // 计算并显示垂直平分线预览
                        double midX = (line.getStartX() + line.getEndX()) / 2.0;
                        double midY = (line.getStartY() + line.getEndY()) / 2.0;

                        Point2D[] bisectorLine = IntersectionUtils.getPerpendicularLine(
                                line.getStartX(), line.getStartY(),
                                line.getEndX(), line.getEndY(),
                                midX, midY
                        );

                        // 绘制垂直平分线预览（IntersectionUtils已经返回了扩展后的点，直接使用）
                        double px1 = bisectorLine[0].getX();
                        double py1 = bisectorLine[0].getY();
                        double px2 = bisectorLine[1].getX();
                        double py2 = bisectorLine[1].getY();

                        double sx1 = transform.worldToScreenX(px1);
                        double sy1 = transform.worldToScreenY(py1);
                        double sx2 = transform.worldToScreenX(px2);
                        double sy2 = transform.worldToScreenY(py2);

                        gc.setStroke(Color.valueOf("#759eb2"));
                        gc.setLineWidth(2);
                        gc.setLineDashes(6);
                        gc.strokeLine(sx1, sy1, sx2, sy2);
                        gc.setLineDashes(null);

                        // 绘制中点
                        double midScreenX = transform.worldToScreenX(midX);
                        double midScreenY = transform.worldToScreenY(midY);
                        gc.setFill(Color.valueOf("#759eb2"));
                        double pointRadius = 4;
                        gc.fillOval(midScreenX - pointRadius, midScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
                        break;
                    }
                }
            }
            return;
        }

        // 切线模式：高亮悬停的圆，并显示切点和切线预览
        if (context.getDrawMode() == DrawMode.TANGENT && context.getState() == DrawingState.IDLE) {
            double tangentTolerance = 15.0 / scale;
            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof CircleGeo circle) {
                    // 计算点击位置到圆心的距离
                    double distance = MathCalculationUtils.hypot(worldX - circle.getCx(), worldY - circle.getCy());

                    // 检查是否靠近圆
                    if (MathCalculationUtils.abs(distance - circle.getR()) <= tangentTolerance) {
                        // 高亮显示圆
                        circle.setHover(true);
                        circle.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
                        circle.setHover(false);

                        // 计算切点（投影到圆周上）
                        double dx = worldX - circle.getCx();
                        double dy = worldY - circle.getCy();
                        double len = MathCalculationUtils.hypot(dx, dy);

                        if (len > 1e-10) {
                            double tangentPointX = circle.getCx() + (dx / len) * circle.getR();
                            double tangentPointY = circle.getCy() + (dy / len) * circle.getR();

                            // 绘制切点
                            double tangentScreenX = transform.worldToScreenX(tangentPointX);
                            double tangentScreenY = transform.worldToScreenY(tangentPointY);
                            gc.setFill(Color.ORANGE);
                            double pointRadius = 5;
                            gc.fillOval(tangentScreenX - pointRadius, tangentScreenY - pointRadius, pointRadius * 2, pointRadius * 2);

                            // 计算并绘制切线预览
                            Point2D[] tangentLine = IntersectionUtils.getTangentLine(
                                    circle.getCx(), circle.getCy(), tangentPointX, tangentPointY
                            );

                            double tx1 = tangentLine[0].getX();
                            double ty1 = tangentLine[0].getY();
                            double tx2 = tangentLine[1].getX();
                            double ty2 = tangentLine[1].getY();

                            // IntersectionUtils已经返回了扩展后的点，直接转换为屏幕坐标绘制
                            double tsx1 = transform.worldToScreenX(tx1);
                            double tsy1 = transform.worldToScreenY(ty1);
                            double tsx2 = transform.worldToScreenX(tx2);
                            double tsy2 = transform.worldToScreenY(ty2);

                            gc.setStroke(Color.ORANGE);
                            gc.setLineWidth(2);
                            gc.setLineDashes(6);
                            gc.strokeLine(tsx1, tsy1, tsx2, tsy2);
                            gc.setLineDashes(null);
                        }
                        break;
                    }
                }
            }
            return;
        }

        // 已选中线段的预览逻辑
        if (context.getState() != DrawingState.FIRST_CLICK || selectedLine == null) {
            return;
        }

        // 只有垂线和平行线有预览
        if (context.getDrawMode() != DrawMode.PERPENDICULAR && context.getDrawMode() != DrawMode.PARALLEL) {
            return;
        }

        // 高亮显示已选中的线段/直线
        selectedLine.setHover(true);
        selectedLine.paint(gc, transform, context.getGridChartPane().getWidth(), context.getGridChartPane().getHeight());
        selectedLine.setHover(false);

        // 根据不同的作图模式绘制预览线
        double x1, y1, x2, y2;
        if (selectedLine instanceof LineGeo line) {
            x1 = line.getStartX();
            y1 = line.getStartY();
            x2 = line.getEndX();
            y2 = line.getEndY();
        } else if (selectedLine instanceof InfiniteLineGeo line) {
            x1 = line.getPoint1X();
            y1 = line.getPoint1Y();
            x2 = line.getPoint2X();
            y2 = line.getPoint2Y();
        } else {
            return;
        }

        Point2D[] previewLine = null;
        if (context.getDrawMode() == DrawMode.PERPENDICULAR) {
            previewLine = IntersectionUtils.getPerpendicularLine(x1, y1, x2, y2, context.getCurrentMouseX(), context.getCurrentMouseY());
        } else if (context.getDrawMode() == DrawMode.PARALLEL) {
            previewLine = IntersectionUtils.getParallelLine(x1, y1, x2, y2, context.getCurrentMouseX(), context.getCurrentMouseY());
        }

        if (previewLine != null) {
            // IntersectionUtils已经返回了扩展后的点，直接转换为屏幕坐标绘制
            double px1 = previewLine[0].getX();
            double py1 = previewLine[0].getY();
            double px2 = previewLine[1].getX();
            double py2 = previewLine[1].getY();

            double sx1 = transform.worldToScreenX(px1);
            double sy1 = transform.worldToScreenY(py1);
            double sx2 = transform.worldToScreenX(px2);
            double sy2 = transform.worldToScreenY(py2);

            // 绘制虚线预览
            gc.setStroke(Color.valueOf("#759eb2"));
            gc.setLineWidth(2);
            gc.setLineDashes(6);
            gc.strokeLine(sx1, sy1, sx2, sy2);
            gc.setLineDashes(null);

            // 绘制鼠标位置的点
            double mouseScreenX = transform.worldToScreenX(context.getCurrentMouseX());
            double mouseScreenY = transform.worldToScreenY(context.getCurrentMouseY());
            gc.setFill(Color.LIGHTGRAY);
            double pointRadius = 3;
            gc.fillOval(mouseScreenX - pointRadius, mouseScreenY - pointRadius, pointRadius * 2, pointRadius * 2);
        }
    }

    @Override
    public void reset() {
        selectedLine = null;
    }

    /**
     * 处理中点模式的点击事件
     */
    private void handleMidpointClick(double worldX, double worldY, DrawingContext context) {
        // 查找点击位置附近的线段或直线
        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale;

        for (WorldObject obj : context.getObjects()) {
            if (obj instanceof LineGeo line) {
                if (line.hitTest(worldX, worldY, tolerance)) {
                    // 计算中点
                    Point2D midpoint = IntersectionUtils.getMidpoint(
                            line.getStartX(), line.getStartY(),
                            line.getEndX(), line.getEndY()
                    );

                    // 绘制中点
                    PointGeo newPoint = new PointGeo(midpoint.getX(), midpoint.getY());
                    newPoint.setColor(Color.GREEN);
                    context.executeCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            context.addObject(newPoint);
                        }

                        @Override
                        public void undo() {
                            context.removeObject(newPoint);
                        }
                    });
                    return;
                }
            } else if (obj instanceof InfiniteLineGeo line) {
                if (line.hitTest(worldX, worldY, tolerance)) {
                    // 直线的中点是定义点的中点
                    Point2D midpoint = IntersectionUtils.getMidpoint(
                            line.getPoint1X(), line.getPoint1Y(),
                            line.getPoint2X(), line.getPoint2Y()
                    );

                    // 绘制中点
                    PointGeo newPoint = new PointGeo(midpoint.getX(), midpoint.getY());
                    newPoint.setColor(Color.GREEN);
                    context.executeCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            context.addObject(newPoint);
                        }

                        @Override
                        public void undo() {
                            context.removeObject(newPoint);
                        }
                    });
                    return;
                }
            }
        }
    }

    /**
     * 处理垂线模式的点击事件
     */
    private void handlePerpendicularClick(double worldX, double worldY, DrawingContext context) {
        if (context.getState() == DrawingState.IDLE) {
            // 第一次点击：选择线段或直线
            double scale = context.getTransform().getScale();
            double tolerance = 10.0 / scale;

            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        context.setState(DrawingState.FIRST_CLICK);
                        context.redraw();
                        return;
                    }
                } else if (obj instanceof InfiniteLineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        context.setState(DrawingState.FIRST_CLICK);
                        context.redraw();
                        return;
                    }
                }
            }
        } else if (context.getState() == DrawingState.FIRST_CLICK && selectedLine != null) {
            // 第二次点击：过此点绘制垂线
            double x1, y1, x2, y2;
            if (selectedLine instanceof LineGeo line) {
                x1 = line.getStartX();
                y1 = line.getStartY();
                x2 = line.getEndX();
                y2 = line.getEndY();
            } else if (selectedLine instanceof InfiniteLineGeo line) {
                x1 = line.getPoint1X();
                y1 = line.getPoint1Y();
                x2 = line.getPoint2X();
                y2 = line.getPoint2Y();
            } else {
                return;
            }

            // 计算垂线：过 (worldX, worldY) 点
            Point2D[] perpLine = IntersectionUtils.getPerpendicularLine(x1, y1, x2, y2, worldX, worldY);

            // 创建垂线（使用鼠标点和垂线上的另一点）
            InfiniteLineGeo newLine = new InfiniteLineGeo(
                    worldX, worldY,
                    perpLine[0].getX(), perpLine[0].getY()
            );

            // 计算交点
            List<PointGeo> intersectionPoints = context.getIntersectionHandler().checkIntersections(newLine, context);
            context.executeCommand(new CommandHistory.Command() {
                @Override
                public void execute() {
                    context.addObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        context.addObject(point);
                    }
                }

                @Override
                public void undo() {
                    context.removeObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        context.removeObject(point);
                    }
                }
            });

            // 重置状态
            selectedLine = null;
            context.setState(DrawingState.IDLE);
            context.redraw();
        }
    }

    /**
     * 处理平行线模式的点击事件
     */
    private void handleParallelClick(double worldX, double worldY, DrawingContext context) {
        if (context.getState() == DrawingState.IDLE) {
            // 第一次点击：选择线段或直线
            double scale = context.getTransform().getScale();
            double tolerance = 10.0 / scale;

            for (WorldObject obj : context.getObjects()) {
                if (obj instanceof LineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        context.setState(DrawingState.FIRST_CLICK);
                        context.redraw();
                        return;
                    }
                } else if (obj instanceof InfiniteLineGeo line) {
                    if (line.hitTest(worldX, worldY, tolerance)) {
                        selectedLine = line;
                        context.setState(DrawingState.FIRST_CLICK);
                        context.redraw();
                        return;
                    }
                }
            }
        } else if (context.getState() == DrawingState.FIRST_CLICK && selectedLine != null) {
            // 第二次点击：过此点绘制平行线
            double x1, y1, x2, y2;
            if (selectedLine instanceof LineGeo line) {
                x1 = line.getStartX();
                y1 = line.getStartY();
                x2 = line.getEndX();
                y2 = line.getEndY();
            } else if (selectedLine instanceof InfiniteLineGeo line) {
                x1 = line.getPoint1X();
                y1 = line.getPoint1Y();
                x2 = line.getPoint2X();
                y2 = line.getPoint2Y();
            } else {
                return;
            }

            // 计算平行线
            Point2D[] parallelLine = IntersectionUtils.getParallelLine(x1, y1, x2, y2, worldX, worldY);

            // 创建平行线（使用鼠标点和平行线上的另一点）
            InfiniteLineGeo newLine = new InfiniteLineGeo(
                    worldX, worldY,
                    parallelLine[0].getX(), parallelLine[0].getY()
            );

            // 计算交点
            List<PointGeo> intersectionPoints = context.getIntersectionHandler().checkIntersections(newLine, context);
            context.executeCommand(new CommandHistory.Command() {
                @Override
                public void execute() {
                    context.addObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        context.addObject(point);
                    }
                }

                @Override
                public void undo() {
                    context.removeObject(newLine);
                    for (PointGeo point : intersectionPoints) {
                        context.removeObject(point);
                    }
                }
            });

            // 重置状态
            selectedLine = null;
            context.setState(DrawingState.IDLE);
            context.redraw();
        }
    }

    /**
     * 处理垂直平分线模式的点击事件
     */
    private void handlePerpendicularBisectorClick(double worldX, double worldY, DrawingContext context) {
        // 查找点击位置附近的线段（注意：只有线段才有垂直平分线，直线没有）
        double scale = context.getTransform().getScale();
        double tolerance = 10.0 / scale;

        for (WorldObject obj : context.getObjects()) {
            if (obj instanceof LineGeo line) {
                if (line.hitTest(worldX, worldY, tolerance)) {
                    // 计算线段的中点
                    double midX = (line.getStartX() + line.getEndX()) / 2.0;
                    double midY = (line.getStartY() + line.getEndY()) / 2.0;

                    // 计算垂直平分线（过中点，垂直于线段）
                    Point2D[] bisectorLine = IntersectionUtils.getPerpendicularLine(
                            line.getStartX(), line.getStartY(),
                            line.getEndX(), line.getEndY(),
                            midX, midY
                    );

                    // 创建垂直平分线（使用中点和垂直方向点）
                    InfiniteLineGeo newLine = new InfiniteLineGeo(
                            midX, midY,
                            bisectorLine[0].getX(), bisectorLine[0].getY()
                    );

                    // 计算交点
                    List<PointGeo> intersectionPoints = context.getIntersectionHandler().checkIntersections(newLine, context);
                    context.executeCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            context.addObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                context.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            context.removeObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                context.removeObject(point);
                            }
                        }
                    });
                    return;
                }
            }
            // 注意：不处理 InfiniteLineGeo，因为直线没有垂直平分线
        }
    }

    /**
     * 处理切线模式的点击事件
     */
    private void handleTangentClick(double worldX, double worldY, DrawingContext context) {
        // 查找点击位置附近的圆
        double scale = context.getTransform().getScale();
        double tolerance = 15.0 / scale;

        for (WorldObject obj : context.getObjects()) {
            if (obj instanceof CircleGeo circle) {
                // 计算点击位置到圆心的距离
                double distance = MathCalculationUtils.hypot(worldX - circle.getCx(), worldY - circle.getCy());

                // 检查是否靠近圆（允许一定容差）
                if (MathCalculationUtils.abs(distance - circle.getR()) <= tolerance) {
                    // 计算离点击位置最近的圆周上的点（将点击位置投影到圆周上）
                    double dx = worldX - circle.getCx();
                    double dy = worldY - circle.getCy();
                    double len = MathCalculationUtils.hypot(dx, dy);

                    // 归一化方向向量并乘以半径，得到圆周上的点
                    double tangentPointX = circle.getCx() + (dx / len) * circle.getR();
                    double tangentPointY = circle.getCy() + (dy / len) * circle.getR();

                    // 计算并绘制切线（过圆周上的切点）
                    Point2D[] tangentLine = IntersectionUtils.getTangentLine(
                            circle.getCx(), circle.getCy(), tangentPointX, tangentPointY
                    );

                    // 创建切线（使用切点和切线上的另一点）
                    InfiniteLineGeo newLine = new InfiniteLineGeo(
                            tangentPointX, tangentPointY,
                            tangentLine[0].getX(), tangentLine[0].getY()
                    );

                    // 计算交点
                    List<PointGeo> intersectionPoints = context.getIntersectionHandler().checkIntersections(newLine, context);
                    context.executeCommand(new CommandHistory.Command() {
                        @Override
                        public void execute() {
                            context.addObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                context.addObject(point);
                            }
                        }

                        @Override
                        public void undo() {
                            context.removeObject(newLine);
                            for (PointGeo point : intersectionPoints) {
                                context.removeObject(point);
                            }
                        }
                    });
                    return;
                }
            }
        }
    }
}
