package com.bingbaihanji.util;

import com.bingbaihanji.controller.IDrawingContext;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
import com.bingbaihanji.view.layout.draw.geometry.impl.PointGeo;

import java.util.List;

/**
 * 几何图形命令
 * <p>
 * 封装"添加对象 + 计算交点 + 撤销/重做"模式,
 * 消除各 Handler 中重复的匿名 CommandHistory.Command 代码
 *
 * @author bingbaihanji
 * @date 2026-05-22
 */
public class GeometryCommand implements CommandHistory.Command {

    private final IDrawingContext context;
    private final WorldObject primaryObject;
    private final List<? extends WorldObject> additionalObjects;
    private final List<PointGeo> intersectionPoints;

    /**
     * 标准构造函数(对象 + 自动计算交点)
     */
    public GeometryCommand(IDrawingContext context, WorldObject primaryObject) {
        this(context, primaryObject, List.of());
    }

    /**
     * 带附加对象的构造函数(如多边形的内部顶点)
     */
    public GeometryCommand(IDrawingContext context, WorldObject primaryObject,
                           List<? extends WorldObject> additionalObjects) {
        this.context = context;
        this.primaryObject = primaryObject;
        this.additionalObjects = additionalObjects;
        this.intersectionPoints = context.getIntersectionHandler()
                .checkIntersections(primaryObject, context);
    }

    @Override
    public void execute() {
        context.addObject(primaryObject);
        for (PointGeo point : intersectionPoints) {
            context.addObject(point);
        }
        for (WorldObject obj : additionalObjects) {
            context.addObject(obj);
        }
    }

    @Override
    public void undo() {
        context.removeObject(primaryObject);
        for (PointGeo point : intersectionPoints) {
            context.removeObject(point);
        }
        for (WorldObject obj : additionalObjects) {
            context.removeObject(obj);
        }
    }
}
