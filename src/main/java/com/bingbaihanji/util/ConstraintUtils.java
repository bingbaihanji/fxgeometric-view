package com.bingbaihanji.util;

import com.bingbaihanji.controller.DrawingController;
import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.util.constraint.PointConstraint;
import com.bingbaihanji.view.layout.core.GridChartView;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

/**
 * 几何约束工具类
 *
 * @author bingbaihanji
 * @date 2026-05-23
 */
public final class ConstraintUtils {

    private ConstraintUtils() {
    }

    /**
     * 判断图形是否可以作为约束对象
     */
    public static boolean isConstrainableShape(WorldObject obj) {
        return obj instanceof LineGeo ||
                obj instanceof InfiniteLineGeo ||
                obj instanceof CircleGeo ||
                obj instanceof EllipseGeo ||
                obj instanceof PolygonGeo ||
                obj instanceof PathGeo ||
                obj instanceof FunctionGeo;
    }

    /**
     * 检查点是否在图形附近
     */
    public static boolean isPointNearShape(double x, double y, WorldObject shape, double threshold) {
        if (shape instanceof LineGeo line) {
            return MathCalculationUtils.pointToSegmentDistance(x, y,
                    line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY()) < threshold;
        } else if (shape instanceof InfiniteLineGeo infLine) {
            return MathCalculationUtils.pointToInfiniteLineDistance(x, y,
                    infLine.getPoint1X(), infLine.getPoint1Y(),
                    infLine.getPoint2X(), infLine.getPoint2Y()) < threshold;
        } else if (shape instanceof EllipseGeo ellipse) {
            double d1 = Math.hypot(x - ellipse.getF1x(), y - ellipse.getF1y());
            double d2 = Math.hypot(x - ellipse.getF2x(), y - ellipse.getF2y());
            return Math.abs(d1 + d2 - ellipse.getTwoA()) < threshold;
        } else if (shape instanceof CircleGeo circle) {
            double dist = Math.abs(Math.hypot(x - circle.getCx(), y - circle.getCy()) - circle.getR());
            return dist < threshold;
        } else if (shape instanceof PolygonGeo polygon) {
            for (int i = 0; i < polygon.getVertexCount(); i++) {
                javafx.geometry.Point2D v1 = polygon.getVertex(i);
                javafx.geometry.Point2D v2 = polygon.getVertex((i + 1) % polygon.getVertexCount());
                if (MathCalculationUtils.pointToSegmentDistance(x, y,
                        v1.getX(), v1.getY(), v2.getX(), v2.getY()) < threshold) {
                    return true;
                }
            }
            return false;
        } else if (shape instanceof FunctionGeo function) {
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
     * 查找附近可以作为约束对象的几何图形
     */
    public static List<WorldObject> findNearbyConstrainableShapes(
            PointGeo point, GridChartView canvas, DrawingController controller) {
        List<WorldObject> result = new ArrayList<>();
        double scale = canvas.getTransform().getScale();
        double threshold = 20.0 / scale;

        for (WorldObject obj : canvas.getObjects()) {
            if (obj == point) continue;
            if (isConstrainableShape(obj)) {
                if (isPointNearShape(point.getX(), point.getY(), obj, threshold)) {
                    result.add(obj);
                }
            }
        }
        return result;
    }

    /**
     * 获取图形的显示名称
     */
    public static String getShapeDisplayName(WorldObject shape) {
        if (shape instanceof LineGeo line) {
            return I18nUtil.getString("geo.shape.segment",
                    line.getStartPointName(), line.getEndPointName());
        } else if (shape instanceof InfiniteLineGeo infLine) {
            return I18nUtil.getString("geo.shape.line",
                    infLine.getPoint1Name(), infLine.getPoint2Name());
        } else if (shape instanceof EllipseGeo ellipse) {
            return I18nUtil.getString("geo.shape.ellipse", "");
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
    public static void addConstraintToPoint(PointGeo point, WorldObject shape,
                                            DrawingController controller, GridChartView canvas) {
        try {
            IDrawingContext context = controller.getContext();
            PointConstraint constraint = context.getConstraintHandler().createConstraint(shape, point);

            if (!constraint.isVertexConstraint()) {
                double parameter = constraint.calculateParameter(point.getX(), point.getY());
                constraint.setParameter(parameter);
            }

            point.setConstraint(constraint);

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
     * 查找使用指定点的所有图形
     */
    public static List<WorldObject> findShapesUsingPoint(PointGeo point, List<WorldObject> objects) {
        List<WorldObject> result = new ArrayList<>();
        double px = point.getX();
        double py = point.getY();
        double threshold = 0.01;

        for (WorldObject obj : objects) {
            if (obj == point) continue;

            if (obj instanceof LineGeo line) {
                if (line.getStartPointRef() == point || line.getEndPointRef() == point) {
                    result.add(obj);
                }
            } else if (obj instanceof InfiniteLineGeo infLine) {
                if (infLine.getPoint1Ref() == point || infLine.getPoint2Ref() == point) {
                    result.add(obj);
                }
            } else if (obj instanceof EllipseGeo ellipse) {
                if (ellipse.getF1Ref() == point || ellipse.getF2Ref() == point) {
                    result.add(obj);
                } else if (Math.abs(ellipse.getF1x() - px) < threshold &&
                        Math.abs(ellipse.getF1y() - py) < threshold) {
                    result.add(obj);
                } else if (Math.abs(ellipse.getF2x() - px) < threshold &&
                        Math.abs(ellipse.getF2y() - py) < threshold) {
                    result.add(obj);
                }
            } else if (obj instanceof CircleGeo circle) {
                if (circle.getCenterPointRef() == point) {
                    result.add(obj);
                } else if (Math.abs(circle.getCx() - px) < threshold &&
                        Math.abs(circle.getCy() - py) < threshold) {
                    result.add(obj);
                }
            } else if (obj instanceof PolygonGeo polygon) {
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
}