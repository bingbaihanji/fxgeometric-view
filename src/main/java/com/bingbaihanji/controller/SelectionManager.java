package com.bingbaihanji.controller;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.MathCalculationUtils;
import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 选择管理器
 *
 * @author bingbaihanji
 * @date 2025-12-31
 * @description 管理几何对象的选择状态，参考 GeoGebra 的 SelectionManager 设计
 * 支持单选、多选、框选、批量操作等功能
 */
public class SelectionManager {

    /**
     * 主选择列表（所有选中的对象）
     */
    private final List<WorldObject> selectedObjects = new ArrayList<>();

    // 已禁用：边界框（用于显示选中对象的边界和句柄）
    // private final BoundingBox boundingBox = new BoundingBox();

    /**
     * 选择变化监听器列表
     */
    private final List<SelectionChangeListener> listeners = new ArrayList<>();

    //   选中对象列表  
    /**
     * 框选区域（用于框选模式）
     */
    private SelectionRectangle selectionRectangle = null;

    //   选择变化监听器  
    /**
     * 是否启用框选模式
     */
    private boolean rectangleSelectionEnabled = true;

    //   选择操作  

    /**
     * 添加对象到选择集
     *
     * @param object 要选中的对象
     * @param notify 是否通知监听器
     * @return 如果对象被成功添加返回 true
     */
    public boolean addSelectedObject(WorldObject object, boolean notify) {
        if (object == null || selectedObjects.contains(object)) {
            return false;
        }

        selectedObjects.add(object);
        object.setSelected(true);

        // 已禁用：更新边界框
        // updateBoundingBox();

        if (notify) {
            notifySelectionChanged();
        }

        return true;
    }

    /**
     * 添加对象到选择集（默认通知）
     */
    public boolean addSelectedObject(WorldObject object) {
        return addSelectedObject(object, true);
    }

    /**
     * 从选择集移除对象
     *
     * @param object 要移除的对象
     * @param notify 是否通知监听器
     * @return 如果对象被成功移除返回 true
     */
    public boolean removeSelectedObject(WorldObject object, boolean notify) {
        if (object == null || !selectedObjects.contains(object)) {
            return false;
        }

        selectedObjects.remove(object);
        object.setSelected(false);

        // 已禁用：更新边界框
        // updateBoundingBox();

        if (notify) {
            notifySelectionChanged();
        }

        return true;
    }

    /**
     * 从选择集移除对象（默认通知）
     */
    public boolean removeSelectedObject(WorldObject object) {
        return removeSelectedObject(object, true);
    }

    /**
     * 切换对象的选择状态
     *
     * @param object 要切换的对象
     * @return 切换后的选中状态
     */
    public boolean toggleSelection(WorldObject object) {
        if (object == null) {
            return false;
        }

        if (selectedObjects.contains(object)) {
            removeSelectedObject(object);
            return false;
        } else {
            addSelectedObject(object);
            return true;
        }
    }

    /**
     * 清空所有选择
     *
     * @param notify 是否通知监听器
     */
    public void clearSelection(boolean notify) {
        if (selectedObjects.isEmpty()) {
            return;
        }

        // 取消所有对象的选中状态
        for (WorldObject obj : selectedObjects) {
            obj.setSelected(false);
        }

        selectedObjects.clear();

        // 已禁用：清空边界框
        // boundingBox.clear();

        if (notify) {
            notifySelectionChanged();
        }
    }

    /**
     * 清空所有选择（默认通知）
     */
    public void clearSelection() {
        clearSelection(true);
    }

    /**
     * 设置唯一选中对象（清空其他选择）
     *
     * @param object 要选中的对象
     */
    public void selectOnly(WorldObject object) {
        clearSelection(false);
        addSelectedObject(object, true);
    }

    /**
     * 选择多个对象
     *
     * @param objects 要选中的对象列表
     * @param append  是否追加到现有选择（true）或替换（false）
     */
    public void selectMultiple(List<WorldObject> objects, boolean append) {
        if (!append) {
            clearSelection(false);
        }

        for (WorldObject obj : objects) {
            addSelectedObject(obj, false);
        }

        notifySelectionChanged();
    }

    //   查询方法  

    /**
     * 获取所有选中对象（不可修改）
     */
    public List<WorldObject> getSelectedObjects() {
        return Collections.unmodifiableList(selectedObjects);
    }

    /**
     * 获取选中对象数量
     */
    public int getSelectedCount() {
        return selectedObjects.size();
    }

    /**
     * 是否有选中的对象
     */
    public boolean hasSelection() {
        return !selectedObjects.isEmpty();
    }

    /**
     * 检查对象是否被选中
     */
    public boolean isSelected(WorldObject object) {
        return selectedObjects.contains(object);
    }

    /**
     * 获取第一个选中的对象（如果有）
     */
    public WorldObject getFirstSelected() {
        return selectedObjects.isEmpty() ? null : selectedObjects.get(0);
    }

    /**
     * 获取最后一个选中的对象（如果有）
     */
    public WorldObject getLastSelected() {
        return selectedObjects.isEmpty() ? null : selectedObjects.get(selectedObjects.size() - 1);
    }

    /**
     * 按类型过滤选中的对象
     *
     * @param type 对象类型
     * @return 指定类型的选中对象列表
     */
    public List<WorldObject> getSelectedByType(ObjectType type) {
        return selectedObjects.stream()
                .filter(obj -> obj.getObjectType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有选中的点对象
     */
    public List<WorldObject> getSelectedPoints() {
        return selectedObjects.stream()
                .filter(obj -> obj.getObjectType().isPoint())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有选中的线对象
     */
    public List<WorldObject> getSelectedLines() {
        return selectedObjects.stream()
                .filter(obj -> obj.getObjectType().isLine())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有选中的多边形对象
     */
    public List<WorldObject> getSelectedPolygons() {
        return selectedObjects.stream()
                .filter(obj -> obj.getObjectType().isPolygon())
                .collect(Collectors.toList());
    }

    //   框选功能  

    /**
     * 开始框选（记录起始点）
     *
     * @param worldX 起始点X坐标（世界坐标）
     * @param worldY 起始点Y坐标（世界坐标）
     */
    public void startRectangleSelection(double worldX, double worldY) {
        if (!rectangleSelectionEnabled) {
            return;
        }
        selectionRectangle = new SelectionRectangle(worldX, worldY);
    }

    /**
     * 更新框选区域（更新结束点）
     *
     * @param worldX 当前点X坐标（世界坐标）
     * @param worldY 当前点Y坐标（世界坐标）
     */
    public void updateRectangleSelection(double worldX, double worldY) {
        if (selectionRectangle != null) {
            selectionRectangle.updateEnd(worldX, worldY);
        }
    }

    /**
     * 完成框选（根据区域选中对象）
     *
     * @param allObjects 所有对象列表
     * @param append     是否追加到现有选择（true）或替换（false）
     */
    public void finishRectangleSelection(List<WorldObject> allObjects, boolean append) {
        if (selectionRectangle == null) {
            return;
        }

        List<WorldObject> objectsInRect = selectionRectangle.getObjectsInRectangle(allObjects);

        if (!append) {
            clearSelection(false);
        }

        for (WorldObject obj : objectsInRect) {
            addSelectedObject(obj, false);
        }

        notifySelectionChanged();
        selectionRectangle = null;
    }

    /**
     * 取消框选
     */
    public void cancelRectangleSelection() {
        selectionRectangle = null;
    }

    /**
     * 获取当前框选区域（用于绘制）
     */
    public SelectionRectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    /**
     * 是否正在进行框选
     */
    public boolean isRectangleSelecting() {
        return selectionRectangle != null;
    }

    /**
     * 设置是否启用框选功能
     */
    public void setRectangleSelectionEnabled(boolean enabled) {
        this.rectangleSelectionEnabled = enabled;
        if (!enabled) {
            selectionRectangle = null;
        }
    }

    //   批量操作  

    /**
     * 删除所有选中的对象
     *
     * @param removeAction 删除动作（接收对象列表）
     */
    public void deleteSelectedObjects(java.util.function.Consumer<List<WorldObject>> removeAction) {
        if (selectedObjects.isEmpty()) {
            return;
        }

        List<WorldObject> toDelete = new ArrayList<>(selectedObjects);
        removeAction.accept(toDelete);
        clearSelection();
    }

    /**
     * 对所有选中对象执行操作
     */
    public void forEachSelected(java.util.function.Consumer<WorldObject> action) {
        selectedObjects.forEach(action);
    }

    //   监听器管理  

    /**
     * 添加选择变化监听器
     */
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 移除选择变化监听器
     */
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器选择已变化
     */
    private void notifySelectionChanged() {
        for (SelectionChangeListener listener : listeners) {
            listener.onSelectionChanged(selectedObjects);
        }
    }

    // ========== BoundingBox 相关方法（已禁用） ==========

    /**
     * 更新边界框（已禁用）
     */
    /*
    private void updateBoundingBox() {
        if (selectedObjects.isEmpty()) {
            boundingBox.clear();
        } else {
            boundingBox.setObjects(selectedObjects);
        }
    }
    */

    /**
     * 获取边界框（已禁用）
     */
    /*
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
    */

    /**
     * 是否有边界框（已禁用）
     */
    /*
    public boolean hasBoundingBox() {
        return !boundingBox.isEmpty();
    }
    */

    //   监听器接口  

    /**
     * 选择变化监听器接口
     */
    @FunctionalInterface
    public interface SelectionChangeListener {
        /**
         * 选择发生变化时调用
         *
         * @param selectedObjects 当前选中的对象列表
         */
        void onSelectionChanged(List<WorldObject> selectedObjects);
    }

    /**
     * 框选区域类
     */
    public static class SelectionRectangle {
        private final double startX;
        private final double startY;
        private double endX;
        private double endY;

        public SelectionRectangle(double startX, double startY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = startX;
            this.endY = startY;
        }

        public void updateEnd(double x, double y) {
            this.endX = x;
            this.endY = y;
        }

        /**
         * 获取区域内的所有对象
         */
        public List<WorldObject> getObjectsInRectangle(List<WorldObject> allObjects) {
            double minX = MathCalculationUtils.min(startX, endX);
            double maxX = MathCalculationUtils.max(startX, endX);
            double minY = MathCalculationUtils.min(startY, endY);
            double maxY = MathCalculationUtils.max(startY, endY);

            return allObjects.stream()
                    .filter(obj -> {
                        double[] bbox = obj.getBoundingBox();
                        if (bbox == null) {
                            return false;
                        }
                        // 检查边界框是否与选择区域相交
                        return bbox[0] <= maxX && bbox[1] >= minX &&
                                bbox[2] <= maxY && bbox[3] >= minY;
                    })
                    .collect(Collectors.toList());
        }

        public double getMinX() {
            return MathCalculationUtils.min(startX, endX);
        }

        public double getMaxX() {
            return MathCalculationUtils.max(startX, endX);
        }

        public double getMinY() {
            return MathCalculationUtils.min(startY, endY);
        }

        public double getMaxY() {
            return MathCalculationUtils.max(startY, endY);
        }

        public double getWidth() {
            return MathCalculationUtils.abs(endX - startX);
        }

        public double getHeight() {
            return MathCalculationUtils.abs(endY - startY);
        }
    }
}
