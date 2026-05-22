package com.bingbaihanji.view.layout.draw.geometry.impl;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.constant.FillType;
import com.bingbaihanji.constant.LabelPosition;
import com.bingbaihanji.constant.LineType;
import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.ObjectIdGenerator;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;
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
     * 对象标签/名称
     */
    protected String label = "";

    /**
     * 标签是否可见
     */
    protected boolean labelVisible = true;

    /**
     * 标签位置
     */
    protected LabelPosition labelPosition = LabelPosition.AUTO;

    /**
     * 标签字体大小
     */
    protected double labelFontSize = 12.0;

    /**
     * 标签颜色
     */
    protected Color labelColor = GeometryConfig.Colors.LABEL_TEXT;

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
     * 对象颜色(描边颜色)
     */
    protected Color color = GeometryConfig.Colors.LABEL_TEXT;

    /**
     * 线型
     */
    protected LineType lineType = LineType.FULL;

    /**
     * 线宽
     */
    protected double lineWidth = 2.0;

    /**
     * 透明度 (0.0-1.0)
     */
    protected double opacity = 1.0;

    /**
     * 图层 (0-9)
     */
    protected int layer = 0;

    //   填充属性  

    /**
     * 填充类型
     */
    protected FillType fillType = FillType.NONE;

    /**
     * 填充颜色
     */
    protected Color fillColor = GeometryConfig.Colors.SELECTION_FILL;

    /**
     * 填充透明度 (0.0-1.0)
     */
    protected double fillOpacity = 0.3;

    /**
     * 填充角度(度,用于线条填充)
     */
    protected int hatchAngle = 45;

    /**
     * 填充间距(像素,用于线条填充)
     */
    protected int hatchDistance = 10;

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
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

    @Override
    public boolean isLabelVisible() {
        return labelVisible;
    }

    @Override
    public void setLabelVisible(boolean visible) {
        this.labelVisible = visible;
    }

    @Override
    public LabelPosition getLabelPosition() {
        return labelPosition;
    }

    @Override
    public void setLabelPosition(LabelPosition position) {
        this.labelPosition = position != null ? position : LabelPosition.AUTO;
    }

    @Override
    public double getLabelFontSize() {
        return labelFontSize;
    }

    @Override
    public void setLabelFontSize(double fontSize) {
        this.labelFontSize = Math.max(6, Math.min(fontSize, 72)); // 限制范围 6-72
    }

    @Override
    public Color getLabelColor() {
        return labelColor;
    }

    //   交互状态实现  

    @Override
    public void setLabelColor(Color color) {
        this.labelColor = color != null ? color : GeometryConfig.Colors.LABEL_TEXT;
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
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color != null ? color : GeometryConfig.Colors.LABEL_TEXT;
    }

    @Override
    public LineType getLineType() {
        return lineType;
    }

    @Override
    public void setLineType(LineType lineType) {
        this.lineType = lineType != null ? lineType : LineType.FULL;
    }

    @Override
    public double getLineWidth() {
        return lineWidth;
    }

    @Override
    public void setLineWidth(double lineWidth) {
        this.lineWidth = Math.max(0.5, Math.min(lineWidth, 20.0)); // 限制范围 0.5-20
    }

    @Override
    public double getOpacity() {
        return opacity;
    }

    @Override
    public void setOpacity(double opacity) {
        this.opacity = Math.max(0.0, Math.min(opacity, 1.0)); // 限制范围 0.0-1.0
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
        return fillType;
    }

    @Override
    public void setFillType(FillType fillType) {
        this.fillType = fillType != null ? fillType : FillType.NONE;
    }

    @Override
    public Color getFillColor() {
        return fillColor;
    }

    @Override
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor != null ? fillColor : color;
    }

    @Override
    public double getFillOpacity() {
        return fillOpacity;
    }

    @Override
    public void setFillOpacity(double fillOpacity) {
        this.fillOpacity = Math.max(0.0, Math.min(fillOpacity, 1.0));
    }

    @Override
    public int getHatchAngle() {
        return hatchAngle;
    }

    @Override
    public void setHatchAngle(int angle) {
        this.hatchAngle = angle % 360; // 角度归一化
    }

    @Override
    public int getHatchDistance() {
        return hatchDistance;
    }

    //   可见性和锁定实现  

    @Override
    public void setHatchDistance(int distance) {
        this.hatchDistance = Math.max(1, Math.min(distance, 50)); // 限制范围 1-50
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
        if (selected) {
            return GeometryConfig.Colors.CONSTRUCTION_HIGHLIGHT; // 选中时高亮颜色
        } else if (hover) {
            return color.brighter(); // 悬停时变亮
        } else {
            return new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    opacity
            );
        }
    }

    /**
     * 获取有效的填充颜色(考虑填充透明度)
     */
    protected Color getEffectiveFillColor() {
        return new Color(
                fillColor.getRed(),
                fillColor.getGreen(),
                fillColor.getBlue(),
                fillOpacity * opacity // 综合透明度
        );
    }

    /**
     * 获取有效的线宽(考虑选中/悬停状态)
     */
    protected double getEffectiveLineWidth() {
        if (selected || hover) {
            return lineWidth + 1.0; // 选中或悬停时加粗
        }
        return lineWidth;
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
                objectType.getDisplayName(), id, label);
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
