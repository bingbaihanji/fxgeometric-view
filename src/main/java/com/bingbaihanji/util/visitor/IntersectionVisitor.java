package com.bingbaihanji.util.visitor;

import com.bingbaihanji.util.IntersectionUtils;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * 交点计算访问者
 * <p>
 * 使用访问者模式替代 IntersectionHandler 中的 instanceof 树,
 * 将双分派简化为: obj1.accept(new IntersectionVisitor(obj2))
 *
 * @author bingbaihanji
 * @date 2026-05-22
 */
public class IntersectionVisitor implements GeometryVisitor<List<Point2D>> {

    private final WorldObject other;

    public IntersectionVisitor(WorldObject other) {
        this.other = other;
    }

    @Override
    public List<Point2D> visitPoint(PointGeo point) {
        return List.of();
    }

    @Override
    public List<Point2D> visitLine(LineGeo line) {
        if (other instanceof LineGeo l2) {
            return IntersectionUtils.getLineLineIntersections(line, l2);
        }
        if (other instanceof CircleGeo circle) {
            return IntersectionUtils.getLineCircleIntersections(line, circle);
        }
        if (other instanceof InfiniteLineGeo il) {
            return IntersectionUtils.getInfiniteLineLineIntersections(il, line);
        }
        if (other instanceof FunctionGeo func) {
            return IntersectionUtils.getLineFunctionIntersections(line, func);
        }
        if (other instanceof PathGeo path) {
            return intersectLineWithEdges(line, path.getEdges());
        }
        if (other instanceof PolygonGeo polygon) {
            return intersectLineWithEdges(line, polygon.getEdges());
        }
        if (other instanceof RegularPolygonGeo rp) {
            return intersectLineWithEdges(line, rp.getEdges());
        }
        if (other instanceof EllipseGeo ellipse) {
            return IntersectionUtils.getEllipseLineIntersections(ellipse, line);
        }
        return List.of();
    }

    @Override
    public List<Point2D> visitInfiniteLine(InfiniteLineGeo il) {
        if (other instanceof LineGeo line) {
            return IntersectionUtils.getInfiniteLineLineIntersections(il, line);
        }
        if (other instanceof CircleGeo circle) {
            return IntersectionUtils.getInfiniteLineCircleIntersections(il, circle);
        }
        if (other instanceof InfiniteLineGeo il2) {
            return IntersectionUtils.getInfiniteLineInfiniteLineIntersections(il, il2);
        }
        if (other instanceof FunctionGeo func) {
            return IntersectionUtils.getInfiniteLineFunctionIntersections(il, func);
        }
        if (other instanceof PathGeo path) {
            return intersectInfiniteLineWithEdges(il, path.getEdges());
        }
        if (other instanceof PolygonGeo polygon) {
            return intersectInfiniteLineWithEdges(il, polygon.getEdges());
        }
        if (other instanceof RegularPolygonGeo rp) {
            return intersectInfiniteLineWithEdges(il, rp.getEdges());
        }
        if (other instanceof EllipseGeo ellipse) {
            return IntersectionUtils.getEllipseLineIntersections(ellipse,
                    new LineGeo(il.getPoint1X(), il.getPoint1Y(), il.getPoint2X(), il.getPoint2Y()));
        }
        return List.of();
    }

    @Override
    public List<Point2D> visitCircle(CircleGeo circle) {
        if (other instanceof LineGeo line) {
            return IntersectionUtils.getLineCircleIntersections(line, circle);
        }
        if (other instanceof CircleGeo c2) {
            return IntersectionUtils.getCircleCircleIntersections(circle, c2);
        }
        if (other instanceof InfiniteLineGeo il) {
            return IntersectionUtils.getInfiniteLineCircleIntersections(il, circle);
        }
        if (other instanceof FunctionGeo func) {
            return IntersectionUtils.getCircleFunctionIntersections(circle, func);
        }
        if (other instanceof PathGeo path) {
            return intersectCircleWithEdges(circle, path.getEdges());
        }
        if (other instanceof PolygonGeo polygon) {
            return intersectCircleWithEdges(circle, polygon.getEdges());
        }
        if (other instanceof RegularPolygonGeo rp) {
            return intersectCircleWithEdges(circle, rp.getEdges());
        }
        if (other instanceof EllipseGeo ellipse) {
            return IntersectionUtils.getEllipseCircleIntersections(ellipse, circle);
        }
        return List.of();
    }

    @Override
    public List<Point2D> visitPolygon(PolygonGeo polygon) {
        return intersectEdgesWithOther(polygon.getEdges());
    }

    @Override
    public List<Point2D> visitRegularPolygon(RegularPolygonGeo rp) {
        return intersectEdgesWithOther(rp.getEdges());
    }

    @Override
    public List<Point2D> visitPath(PathGeo path) {
        if (other instanceof PathGeo) {
            return List.of(); // 手绘路径之间不显示交点
        }
        return intersectEdgesWithOther(path.getEdges());
    }

    @Override
    public List<Point2D> visitEllipse(EllipseGeo ellipse) {
        if (other instanceof LineGeo line) {
            return intersectEllipseWithEdges(ellipse, List.of(line));
        }
        if (other instanceof InfiniteLineGeo il) {
            return intersectEllipseWithEdges(ellipse, List.of(new LineGeo(il.getPoint1X(), il.getPoint1Y(),
                    il.getPoint2X(), il.getPoint2Y())));
        }
        if (other instanceof CircleGeo circle) {
            return IntersectionUtils.getEllipseCircleIntersections(ellipse, circle);
        }
        if (other instanceof FunctionGeo func) {
            return intersectEllipseWithFunction(ellipse, func);
        }
        if (other instanceof PolygonGeo polygon) {
            return intersectEllipseWithEdges(ellipse, polygon.getEdges());
        }
        if (other instanceof RegularPolygonGeo rp) {
            return intersectEllipseWithEdges(ellipse, rp.getEdges());
        }
        if (other instanceof PathGeo path) {
            return intersectEllipseWithEdges(ellipse, path.getEdges());
        }
        return List.of();
    }

    @Override
    public List<Point2D> visitFunction(FunctionGeo func) {
        if (other instanceof FunctionGeo f2) {
            return IntersectionUtils.getFunctionFunctionIntersections(func, f2);
        }
        if (other instanceof LineGeo line) {
            return IntersectionUtils.getLineFunctionIntersections(line, func);
        }
        if (other instanceof InfiniteLineGeo il) {
            return IntersectionUtils.getInfiniteLineFunctionIntersections(il, func);
        }
        if (other instanceof CircleGeo circle) {
            return IntersectionUtils.getCircleFunctionIntersections(circle, func);
        }
        if (other instanceof PolygonGeo polygon) {
            return intersectFunctionWithEdges(func, polygon.getEdges());
        }
        if (other instanceof RegularPolygonGeo rp) {
            return intersectFunctionWithEdges(func, rp.getEdges());
        }
        if (other instanceof PathGeo path) {
            return intersectFunctionWithEdges(func, path.getEdges());
        }
        return List.of();
    }

    // === 复合类型分解的私有辅助方法 ===

    /**
     * 将边集合与 "其他" 对象求交
     */
    private List<Point2D> intersectEdgesWithOther(List<LineGeo> edges) {
        List<Point2D> results = new ArrayList<>();
        for (LineGeo edge : edges) {
            IntersectionVisitor edgeVisitor = new IntersectionVisitor(other);
            results.addAll(edge.accept(edgeVisitor));
        }
        return results;
    }

    private List<Point2D> intersectLineWithEdges(LineGeo line, List<LineGeo> edges) {
        List<Point2D> results = new ArrayList<>();
        for (LineGeo edge : edges) {
            results.addAll(IntersectionUtils.getLineLineIntersections(line, edge));
        }
        return results;
    }

    private List<Point2D> intersectInfiniteLineWithEdges(InfiniteLineGeo il, List<LineGeo> edges) {
        List<Point2D> results = new ArrayList<>();
        for (LineGeo edge : edges) {
            results.addAll(IntersectionUtils.getInfiniteLineLineIntersections(il, edge));
        }
        return results;
    }

    private List<Point2D> intersectCircleWithEdges(CircleGeo circle, List<LineGeo> edges) {
        List<Point2D> results = new ArrayList<>();
        for (LineGeo edge : edges) {
            results.addAll(IntersectionUtils.getLineCircleIntersections(edge, circle));
        }
        return results;
    }

    private List<Point2D> intersectEllipseWithEdges(EllipseGeo ellipse, List<LineGeo> edges) {
        List<Point2D> results = new ArrayList<>();
        for (LineGeo edge : edges) {
            results.addAll(IntersectionUtils.getEllipseLineIntersections(ellipse, edge));
        }
        return results;
    }

    private List<Point2D> intersectEllipseWithFunction(EllipseGeo ellipse, FunctionGeo func) {
        List<Point2D> results = new ArrayList<>();
        List<List<javafx.geometry.Point2D>> segments = func.getSampledSegments();
        if (segments == null || segments.isEmpty()) return results;
        for (List<javafx.geometry.Point2D> points : segments) {
            if (points == null || points.size() < 2) continue;
            for (int i = 0; i < points.size() - 1; i++) {
                javafx.geometry.Point2D p1 = points.get(i);
                javafx.geometry.Point2D p2 = points.get(i + 1);
                if (!Double.isFinite(p1.getX()) || !Double.isFinite(p1.getY())
                        || !Double.isFinite(p2.getX()) || !Double.isFinite(p2.getY())) continue;
                LineGeo seg = new LineGeo(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                results.addAll(IntersectionUtils.getEllipseLineIntersections(ellipse, seg));
            }
        }
        return results;
    }

    private List<Point2D> intersectFunctionWithEdges(FunctionGeo func, List<LineGeo> edges) {
        List<Point2D> results = new ArrayList<>();
        for (LineGeo edge : edges) {
            results.addAll(IntersectionUtils.getLineFunctionIntersections(edge, func));
        }
        return results;
    }
}
