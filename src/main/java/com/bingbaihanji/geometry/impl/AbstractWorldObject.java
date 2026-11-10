package com.bingbaihanji.geometry.impl;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.LabelPosition;
import com.bingbaihanji.constant.LineType;
import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.ObjectIdGenerator;
import com.bingbaihanji.geometry.WorldObject;
import com.bingbaihanji.geometry.style.LabelStyle;
import com.bingbaihanji.geometry.style.ShapeStyle;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * 抽象世界对象基类
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 实现 WorldObject 接口的所有通用属性,简化子类实现
 */
public abstract class AbstractWorldObject implements WorldObject {

    //   基本信息  

    /**
     * 对象唯一 ID
     */
    protected final long id;

    /**
     * 对象类型
     */
    protected final ObjectType objectType;

    /**
     * 标签样式(文本/可见性/位置/字号/颜色)
     */
    protected final LabelStyle labelStyle = new LabelStyle();

    /**
     * 图形样式(描边/线型/线宽/透明度/填充)
     */
    protected final ShapeStyle shapeStyle = new ShapeStyle();

    //   交互状态

    /**
     * 悬停状态
     */
    protected boolean hover = false;

    /**
     * 选中状态
     */
    protected boolean selected = false;

    //   视觉属性

    /**
     * 图层 (0-9)
     */
    protected int layer = 0;

    //   可见性和锁定

    /**
     * 是否可见
     */
    protected boolean visible = true;

    /**
     * 是否锁定(不可编辑属性)
     */
    protected boolean locked = false;

    /**
     * 是否固定(不可移动)
     */
    protected boolean fixed = false;

    /**
     * 是否可选择
     */
    protected boolean selectable = true;

    //   边缓存(子类 getEdges() 使用)
    protected List<LineGeo> cachedEdges;
    protected boolean edgesDirty = true;

    //   构造函数  

    /**
     * 构造函数
     *
     * @param objectType 对象类型
     */
    protected AbstractWorldObject(ObjectType objectType) {
        this.id = ObjectIdGenerator.nextId();
        this.objectType = objectType;
    }

    //   基本信息实现  

    /**
     * 计算点列表的边界框
     *
     * @param points 点列表
     * @param getX   X坐标提取函数
     * @param getY   Y坐标提取函数
     * @return [minX, maxX, minY, maxY], 空列表返回null
     */
    protected static <T> double[] computeBoundingBox(List<T> points,
                                                     ToDoubleFunction<T> getX,
                                                     ToDoubleFunction<T> getY) {
        if (points.isEmpty()) return null;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (T p : points) {
            minX = Math.min(minX, getX.applyAsDouble(p));
            maxX = Math.max(maxX, getX.applyAsDouble(p));
            minY = Math.min(minY, getY.applyAsDouble(p));
            maxY = Math.max(maxY, getY.applyAsDouble(p));
        }
        return new double[]{minX, maxX, minY, maxY};
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public ObjectType getObjectType() {
        return objectType;
    }

    @Override
    public String getLabel() {
        return labelStyle.getText();
    }

    @Override
    public void setLabel(String label) {
        labelStyle.setText(label);
    }

    @Override
    public boolean isLabelVisible() {
        return labelStyle.isVisible();
    }

    @Override
    public void setLabelVisible(boolean visible) {
        labelStyle.setVisible(visible);
    }

    @Override
    public LabelPosition getLabelPosition() {
        return labelStyle.getPosition();
    }

    @Override
    public void setLabelPosition(LabelPosition position) {
        labelStyle.setPosition(position);
    }

    @Override
    public double getLabelFontSize() {
        return labelStyle.getFontSize();
    }

    @Override
    public void setLabelFontSize(double fontSize) {
        labelStyle.setFontSize(fontSize);
    }

    @Override
    public Color getLabelColor() {
        return labelStyle.getColor();
    }

    //   交互状态实现

    @Override
    public void setLabelColor(Color color) {
        labelStyle.setColor(color);
    }

    @Override
    public boolean isHover() {
        return hover;
    }

    @Override
    public void setHover(boolean hover) {
        this.hover = hover;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    //   视觉属性实现  

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public Color getColor() {
        return shapeStyle.getColor();
    }

    @Override
    public void setColor(Color color) {
        shapeStyle.setColor(color);
    }

    @Override
    public LineType getLineType() {
        return shapeStyle.getLineType();
    }

    @Override
    public void setLineType(LineType lineType) {
        shapeStyle.setLineType(lineType);
    }

    @Override
    public double getLineWidth() {
        return shapeStyle.getLineWidth();
    }

    @Override
    public void setLineWidth(double lineWidth) {
        shapeStyle.setLineWidth(lineWidth);
    }

    @Override
    public double getOpacity() {
        return shapeStyle.getOpacity();
    }

    @Override
    public void setOpacity(double opacity) {
        shapeStyle.setOpacity(opacity);
    }

    @Override
    public int getLayer() {
        return layer;
    }

    //   填充属性实现  

    @Override
    public void setLayer(int layer) {
        this.layer = Math.max(0, Math.min(layer, 9)); // 限制范围 0-9
    }

    @Override
    public FillType getFillType() {
        return shapeStyle.getFillType();
    }

    @Override
    public void setFillType(FillType fillType) {
        shapeStyle.setFillType(fillType);
    }

    @Override
    public Color getFillColor() {
        return shapeStyle.getFillColor();
    }

    @Override
    public void setFillColor(Color fillColor) {
        shapeStyle.setFillColor(fillColor);
    }

    @Override
    public double getFillOpacity() {
        return shapeStyle.getFillOpacity();
    }

    @Override
    public void setFillOpacity(double fillOpacity) {
        shapeStyle.setFillOpacity(fillOpacity);
    }

    @Override
    public int getHatchAngle() {
        return shapeStyle.getHatchAngle();
    }

    @Override
    public void setHatchAngle(int angle) {
        shapeStyle.setHatchAngle(angle);
    }

    @Override
    public int getHatchDistance() {
        return shapeStyle.getHatchDistance();
    }

    //   可见性和锁定实现

    @Override
    public void setHatchDistance(int distance) {
        shapeStyle.setHatchDistance(distance);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isFixed() {
        return fixed;
    }

    @Override
    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    @Override
    public boolean isSelectable() {
        return selectable;
    }

    //   工具方法  

    @Override
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    /**
     * 获取有效的描边颜色(考虑透明度和选中/悬停状态)
     */
    protected Color getEffectiveColor() {
        Color base = getColor();
        if (selected) {
            return GeometryConfig.Colors.CONSTRUCTION_HIGHLIGHT; // 选中时高亮颜色
        } else if (hover) {
            return base.brighter(); // 悬停时变亮
        } else {
            return new Color(
                    base.getRed(),
                    base.getGreen(),
                    base.getBlue(),
                    getOpacity()
            );
        }
    }

    /**
     * 获取有效的填充颜色(考虑填充透明度)
     */
    protected Color getEffectiveFillColor() {
        Color base = getFillColor();
        return new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                getFillOpacity() * getOpacity() // 综合透明度
        );
    }

    /**
     * 获取有效的线宽(考虑选中/悬停状态)
     */
    protected double getEffectiveLineWidth() {
        if (selected || hover) {
            return getLineWidth() + 1.0; // 选中或悬停时加粗
        }
        return getLineWidth();
    }

    /**
     * 使边缓存失效
     */
    protected final void invalidateEdgeCache() {
        this.edgesDirty = true;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%d, label='%s']",
                objectType.getDisplayName(), id, getLabel());
    }

    /**
     * 可复用坐标点 — 统一管理 PointGeo 引用和内部坐标回退
     * <p>
     * 当一个几何对象需要存储坐标(端点/圆心等)时,既可以直接引用外部 PointGeo
     * 实现点复用,也可以使用独立的内部坐标。此类封装了这两种情况的差异,
     * 提供统一的 getter/setter/旋转/拖动接口。
     */
    protected static class ReusableCoordinate {
        private PointGeo pointRef;
        private double x, y;
        private String name;
        private boolean internal;

        /**
         * 内部坐标构造(不自动命名)
         */
        public ReusableCoordinate(double x, double y) {
            this(x, y, null);
        }

        /**
         * 内部坐标构造(指定名称)
         */
        public ReusableCoordinate(double x, double y, String name) {
            this.pointRef = null;
            this.x = x;
            this.y = y;
            this.name = name;
            this.internal = true;
        }

        /**
         * 引用外部点构造
         */
        public ReusableCoordinate(PointGeo ref, double x, double y) {
            this.pointRef = ref;
            this.x = x;
            this.y = y;
            if (ref != null) {
                this.name = ref.getName();
                this.internal = false;
            } else {
                this.name = null;
                this.internal = true;
            }
        }

        public double getX() {
            return pointRef != null ? pointRef.getX() : x;
        }

        public double getY() {
            return pointRef != null ? pointRef.getY() : y;
        }

        /**
         * 获取内部坐标(忽略引用,用于序列化)
         */
        public double getRawX() {
            return x;
        }

        /**
         * 获取内部坐标(忽略引用,用于序列化)
         */
        public double getRawY() {
            return y;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isInternal() {
            return internal;
        }

        public PointGeo getRef() {
            return pointRef;
        }

        public boolean hasRef() {
            return pointRef != null;
        }

        public boolean isConstrained() {
            return pointRef != null && pointRef.isConstrained();
        }

        /**
         * 更新位置(引用存在时委托给 PointGeo,否则更新内部坐标)
         */
        public void updatePosition(double newX, double newY) {
            if (pointRef != null) {
                pointRef.updatePosition(newX, newY);
            } else {
                x = newX;
                y = newY;
            }
        }

        /**
         * 直接设置内部坐标(不触发 PointNameManager 更新)
         */
        public void setInternalPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * 绕指定中心旋转
         *
         * @param cx  旋转中心X
         * @param cy  旋转中心Y
         * @param cos cos(angle)
         * @param sin sin(angle)
         */
        public void rotateAround(double cx, double cy, double cos, double sin) {
            if (pointRef != null && !pointRef.isConstrained()) {
                double dx = pointRef.getX() - cx;
                double dy = pointRef.getY() - cy;
                pointRef.updatePosition(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos);
            } else if (pointRef == null) {
                double dx = x - cx;
                double dy = y - cy;
                x = cx + dx * cos - dy * sin;
                y = cy + dx * sin + dy * cos;
            }
        }
    }
}
