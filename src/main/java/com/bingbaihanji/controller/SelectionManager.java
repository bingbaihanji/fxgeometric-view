package com.bingbaihanji.controller;

import com.bingbaihanji.constant.ObjectType;
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
 */
public class SelectionManager {

    // ========== 选中对象列表 ==========

    /**
     * 主选择列表（所有选中的对象）
     */
    private final List<WorldObject> selectedObjects = new ArrayList<>();

    // ========== 选择变化监听器 ==========

    /**
     * 选择变化监听器列表
     */
    private final List<SelectionChangeListener> listeners = new ArrayList<>();

    // ========== 选择操作 ==========

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

    // ========== 查询方法 ==========

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

    // ========== 监听器管理 ==========

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

    // ========== 监听器接口 ==========

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
}
