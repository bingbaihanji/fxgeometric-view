package com.bingbaihanji.io;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 工程数据载体
 * <p>
 * 包含所有需要持久化的场景数据：几何对象、视图状态、设置等
 */
public class ProjectData implements Serializable {

    public static final String FILE_EXTENSION = ".fxgeo";
    public static final byte[] MAGIC = {'F', 'X', 'G', 'E'};
    public static final byte VERSION = 1;
    @Serial
    private static final long serialVersionUID = 1L;
    // 视图状态
    private double scaleX;
    private double scaleY;
    private double offsetX;
    private double offsetY;
    private int backgroundColor;
    private int unitLabelTypeOrdinal;

    // 对象列表
    private List<ObjectData> objects;

    // 点命名管理器状态
    private int nextPointNameIndex;

    public double getScaleX() {
        return scaleX;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getUnitLabelTypeOrdinal() {
        return unitLabelTypeOrdinal;
    }

    public void setUnitLabelTypeOrdinal(int unitLabelTypeOrdinal) {
        this.unitLabelTypeOrdinal = unitLabelTypeOrdinal;
    }

    public List<ObjectData> getObjects() {
        return objects;
    }

    public void setObjects(List<ObjectData> objects) {
        this.objects = objects;
    }

    public int getNextPointNameIndex() {
        return nextPointNameIndex;
    }

    public void setNextPointNameIndex(int nextPointNameIndex) {
        this.nextPointNameIndex = nextPointNameIndex;
    }
}
