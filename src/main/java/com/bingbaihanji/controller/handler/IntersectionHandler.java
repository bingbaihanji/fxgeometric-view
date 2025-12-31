package com.bingbaihanji.controller.handler;

import com.bingbaihanji.controller.DrawingContext;
import com.bingbaihanji.util.IntersectionUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * 交点计算处理器
 * <p>
 * 负责管理图形间交点的计算和更新
 *
 * @author bingbaihanji
 * @date 2025-12-31
 */
public class IntersectionHandler {

    /**
     * 检查新添加的对象与已有对象的交点
     *
     * @param newObject 新添加的对象
     * @param context   绘制上下文
     * @return 交点列表
     */
    public List<PointGeo> checkIntersections(Object newObject, DrawingContext context) {
        List<WorldObject> allObjects = new ArrayList<>(context.getObjects()); // 创建副本避免并发修改
        List<PointGeo> intersectionPoints = new ArrayList<>(); // 收集所有交点

        for (WorldObject obj : allObjects) {
            // 跳过自身
            if (obj == newObject) continue;

            // 检查不同类型的图形组合
            if (newObject instanceof LineGeo && obj instanceof LineGeo) {
                // 线段与线段的交点
                List<Point2D> intersections = IntersectionUtils.getLineLineIntersections((LineGeo) newObject, (LineGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof LineGeo && obj instanceof CircleGeo) {
                // 线段与圆的交点
                List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections((LineGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof CircleGeo && obj instanceof LineGeo) {
                // 圆与线段的交点
                List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections((LineGeo) obj, (CircleGeo) newObject);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof CircleGeo && obj instanceof CircleGeo) {
                // 圆与圆的交点
                List<Point2D> intersections = IntersectionUtils.getCircleCircleIntersections((CircleGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof InfiniteLineGeo && obj instanceof LineGeo) {
                // 无限直线与线段的交点
                List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) newObject, (LineGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof InfiniteLineGeo && obj instanceof CircleGeo) {
                // 无限直线与圆的交点
                List<Point2D> intersections = IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) newObject, (CircleGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof InfiniteLineGeo && obj instanceof InfiniteLineGeo) {
                // 无限直线与无限直线的交点
                List<Point2D> intersections = IntersectionUtils.getInfiniteLineInfiniteLineIntersections((InfiniteLineGeo) newObject, (InfiniteLineGeo) obj);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    intersectionPoints.add(intersectionPoint);
                }
            } else if (newObject instanceof PolygonGeo polygon) {
                // 多边形与其他图形的交点：遍历多边形的每条边
                for (LineGeo edge : polygon.getEdges()) {
                    if (obj instanceof LineGeo line) {
                        List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, line);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof CircleGeo circle) {
                        List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections(edge, circle);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PolygonGeo otherPolygon) {
                        // 多边形与多边形的交点：遍历两个多边形的所有边
                        for (LineGeo otherEdge : otherPolygon.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, otherEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PathGeo path) {
                        // 多边形与手绘路径的交点
                        for (LineGeo pathEdge : path.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, pathEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    }
                }
            } else if (newObject instanceof PathGeo path) {
                // 手绘路径与其他图形的交点
                for (LineGeo edge : path.getEdges()) {
                    if (obj instanceof LineGeo line) {
                        List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, line);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof CircleGeo circle) {
                        List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections(edge, circle);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PolygonGeo polygon) {
                        for (LineGeo polyEdge : polygon.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, polyEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (obj instanceof PathGeo otherPath) {
                        // 手绘路径与手绘路径的交点
                        for (LineGeo otherEdge : otherPath.getEdges()) {
                            List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(edge, otherEdge);
                            for (Point2D point : intersections) {
                                PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                                intersectionPoint.setColor(Color.PURPLE);
                                intersectionPoints.add(intersectionPoint);
                            }
                        }
                    }
                }
            } else if (obj instanceof PolygonGeo polygon) {
                // 其他图形与多边形的交点
                for (LineGeo edge : polygon.getEdges()) {
                    if (newObject instanceof LineGeo line) {
                        List<Point2D> intersections = IntersectionUtils.getLineLineIntersections(line, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (newObject instanceof CircleGeo circle) {
                        List<Point2D> intersections = IntersectionUtils.getLineCircleIntersections(edge, circle);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    } else if (newObject instanceof InfiniteLineGeo infiniteLine) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    }
                }
            } else if (obj instanceof InfiniteLineGeo infiniteLine) {
                // 其他图形与无限直线的交点
                if (newObject instanceof LineGeo line) {
                    List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, line);
                    for (Point2D point : intersections) {
                        PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                        intersectionPoint.setColor(Color.PURPLE);
                        intersectionPoints.add(intersectionPoint);
                    }
                } else if (newObject instanceof CircleGeo circle) {
                    List<Point2D> intersections = IntersectionUtils.getInfiniteLineCircleIntersections(infiniteLine, circle);
                    for (Point2D point : intersections) {
                        PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                        intersectionPoint.setColor(Color.PURPLE);
                        intersectionPoints.add(intersectionPoint);
                    }
                }
            } else if (newObject instanceof InfiniteLineGeo infiniteLine) {
                // 无限直线与多边形的交点
                if (obj instanceof PolygonGeo polygon) {
                    for (LineGeo edge : polygon.getEdges()) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    }
                } else if (obj instanceof PathGeo path) {
                    // 无限直线与手绘路径的交点
                    for (LineGeo edge : path.getEdges()) {
                        List<Point2D> intersections = IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge);
                        for (Point2D point : intersections) {
                            PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                            intersectionPoint.setColor(Color.PURPLE);
                            intersectionPoints.add(intersectionPoint);
                        }
                    }
                }
            }
        }

        return intersectionPoints;
    }

    /**
     * 重新计算所有图形之间的交点
     * <p>
     * 用于拖动后更新交点位置
     *
     * @param context 绘制上下文
     */
    public void recalculateAllIntersections(DrawingContext context) {
        // 1. 删除所有旧的交点（紫色的PointGeo）
        List<WorldObject> allObjects = new ArrayList<>(context.getObjects());
        for (WorldObject obj : allObjects) {
            if (obj instanceof PointGeo point) {
                // 检查是否为紫色交点
                if (isIntersectionPoint(point)) {
                    context.removeObject(obj);
                }
            }
        }

        // 2. 重新计算所有图形之间的交点
        List<WorldObject> objects = new ArrayList<>(context.getObjects());
        List<PointGeo> newIntersectionPoints = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            WorldObject obj1 = objects.get(i);
            // 跳过点对象
            if (obj1 instanceof PointGeo) continue;

            for (int j = i + 1; j < objects.size(); j++) {
                WorldObject obj2 = objects.get(j);
                // 跳过点对象
                if (obj2 instanceof PointGeo) continue;

                // 计算交点
                List<Point2D> intersections = calculateIntersections(obj1, obj2);
                for (Point2D point : intersections) {
                    PointGeo intersectionPoint = new PointGeo(point.getX(), point.getY(), false);
                    intersectionPoint.setColor(Color.PURPLE);
                    newIntersectionPoints.add(intersectionPoint);
                }
            }
        }

        // 3. 添加新的交点
        for (PointGeo point : newIntersectionPoints) {
            context.addObject(point);
        }

        // 4. 重绘
        context.redraw();
    }

    /**
     * 计算两个几何对象之间的交点
     *
     * @param obj1 几何对象1
     * @param obj2 几何对象2
     * @return 交点列表
     */
    public List<Point2D> calculateIntersections(WorldObject obj1, WorldObject obj2) {
        List<Point2D> intersections = new ArrayList<>();

        if (obj1 instanceof LineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineLineIntersections((LineGeo) obj1, (LineGeo) obj2));
        } else if (obj1 instanceof LineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getLineCircleIntersections((LineGeo) obj2, (CircleGeo) obj1));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getCircleCircleIntersections((CircleGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof LineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj1, (LineGeo) obj2));
        } else if (obj1 instanceof LineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections((InfiniteLineGeo) obj2, (LineGeo) obj1));
        } else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof CircleGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj1, (CircleGeo) obj2));
        } else if (obj1 instanceof CircleGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineCircleIntersections((InfiniteLineGeo) obj2, (CircleGeo) obj1));
        } else if (obj1 instanceof InfiniteLineGeo && obj2 instanceof InfiniteLineGeo) {
            intersections.addAll(IntersectionUtils.getInfiniteLineInfiniteLineIntersections((InfiniteLineGeo) obj1, (InfiniteLineGeo) obj2));
        } else if (obj1 instanceof PolygonGeo polygon) {
            // 多边形与其他图形的交点
            for (LineGeo edge : polygon.getEdges()) {
                if (obj2 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, line));
                } else if (obj2 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj2 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                } else if (obj2 instanceof PolygonGeo otherPolygon) {
                    for (LineGeo otherEdge : otherPolygon.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, otherEdge));
                    }
                }
            }
        } else if (obj2 instanceof PolygonGeo polygon) {
            // 其他图形与多边形的交点
            for (LineGeo edge : polygon.getEdges()) {
                if (obj1 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(line, edge));
                } else if (obj1 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj1 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        } else if (obj1 instanceof InfiniteLineGeo infiniteLine) {
            // 无限直线与其他图形的交点
            if (obj2 instanceof PolygonGeo polygon) {
                for (LineGeo edge : polygon.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            } else if (obj2 instanceof PathGeo path) {
                for (LineGeo edge : path.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        } else if (obj2 instanceof InfiniteLineGeo infiniteLine) {
            // 其他图形与无限直线的交点
            if (obj1 instanceof PolygonGeo polygon) {
                for (LineGeo edge : polygon.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            } else if (obj1 instanceof PathGeo path) {
                for (LineGeo edge : path.getEdges()) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        } else if (obj1 instanceof PathGeo path) {
            // 手绘路径与其他图形的交点
            for (LineGeo edge : path.getEdges()) {
                if (obj2 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, line));
                } else if (obj2 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj2 instanceof PolygonGeo polygon) {
                    for (LineGeo polyEdge : polygon.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, polyEdge));
                    }
                } else if (obj2 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                } else if (obj2 instanceof PathGeo otherPath) {
                    for (LineGeo otherEdge : otherPath.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(edge, otherEdge));
                    }
                }
            }
        } else if (obj2 instanceof PathGeo path) {
            // 其他图形与手绘路径的交点
            for (LineGeo edge : path.getEdges()) {
                if (obj1 instanceof LineGeo line) {
                    intersections.addAll(IntersectionUtils.getLineLineIntersections(line, edge));
                } else if (obj1 instanceof CircleGeo circle) {
                    intersections.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
                } else if (obj1 instanceof PolygonGeo polygon) {
                    for (LineGeo polyEdge : polygon.getEdges()) {
                        intersections.addAll(IntersectionUtils.getLineLineIntersections(polyEdge, edge));
                    }
                } else if (obj1 instanceof InfiniteLineGeo infiniteLine) {
                    intersections.addAll(IntersectionUtils.getInfiniteLineLineIntersections(infiniteLine, edge));
                }
            }
        }

        return intersections;
    }

    /**
     * 判断点是否为交点（通过颜色判断）
     *
     * @param point 点对象
     * @return true 表示是交点，false 表示不是
     */
    public boolean isIntersectionPoint(PointGeo point) {
        // 使用新增的getColor方法
        Color color = point.getColor();
        return Color.PURPLE.equals(color);
    }
}
