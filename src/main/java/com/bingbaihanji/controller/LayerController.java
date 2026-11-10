package com.bingbaihanji.controller;

import com.bingbaihanji.geometry.WorldObject;
import com.bingbaihanji.util.CommandHistory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 层级控制器
 * <p>
 * 负责单个图形对象的层级管理: 前移, 后移, 置顶, 置底。
 * 绘制顺序由 {@link WorldObject#getLayer()} 决定, 因此这里调整的是 layer 值,
 * 而不是对象列表顺序 (列表是只读视图, 且排序后对绘制顺序没有影响)。
 * 每次调整都记入命令历史, 可撤销。
 */
public class LayerController {

    private final IDrawingContext context;

    public LayerController(IDrawingContext context) {
        this.context = context;
    }

    /**
     * 将对象上移一层
     *
     * @param target 目标对象
     */
    public void bringForward(WorldObject target) {
        shift(List.of(target), 1);
    }

    /**
     * 将对象下移一层
     *
     * @param target 目标对象
     */
    public void sendBackward(WorldObject target) {
        shift(List.of(target), -1);
    }

    /**
     * 将对象置于顶层
     *
     * @param target 目标对象
     */
    public void bringToFront(WorldObject target) {
        moveToEdge(List.of(target), true);
    }

    /**
     * 将对象置于底层
     *
     * @param target 目标对象
     */
    public void sendToBack(WorldObject target) {
        moveToEdge(List.of(target), false);
    }

    /**
     * 相邻移动一层
     *
     * @param selected  要移动的对象
     * @param direction 正数上移, 负数下移
     */
    private void shift(List<WorldObject> selected, int direction) {
        if (selected.isEmpty()) {
            return;
        }

        List<WorldObject> reordered = visualOrder();
        if (direction > 0) {
            // 从后往前扫, 避免选中块内部连锁交换
            for (int i = reordered.size() - 2; i >= 0; i--) {
                if (selected.contains(reordered.get(i)) && !selected.contains(reordered.get(i + 1))) {
                    swap(reordered, i, i + 1);
                }
            }
        } else {
            for (int i = 1; i < reordered.size(); i++) {
                if (selected.contains(reordered.get(i)) && !selected.contains(reordered.get(i - 1))) {
                    swap(reordered, i, i - 1);
                }
            }
        }
        applyNewOrder(reordered);
    }

    /**
     * 移到最前或最后
     *
     * @param selected 要移动的对象
     * @param toFront  true 置顶, false 置底
     */
    private void moveToEdge(List<WorldObject> selected, boolean toFront) {
        if (selected.isEmpty()) {
            return;
        }

        List<WorldObject> remaining = visualOrder();
        remaining.removeAll(selected);

        List<WorldObject> reordered = new ArrayList<>(remaining.size() + selected.size());
        if (toFront) {
            reordered.addAll(remaining);
            reordered.addAll(selected);
        } else {
            reordered.addAll(selected);
            reordered.addAll(remaining);
        }
        applyNewOrder(reordered);
    }

    /**
     * 按当前可见顺序排列对象: 先按 layer, 相同 layer 保持列表顺序
     *
     * @return 新列表, 不修改原列表
     */
    private List<WorldObject> visualOrder() {
        List<WorldObject> ordered = new ArrayList<>(context.getObjects());
        ordered.sort(Comparator.comparingInt(WorldObject::getLayer));
        return ordered;
    }

    private static void swap(List<WorldObject> list, int i, int j) {
        WorldObject tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    /**
     * 把新的可见顺序写回 layer 值, 并记录为可撤销命令
     *
     * @param reordered 新的可见顺序
     */
    private void applyNewOrder(List<WorldObject> reordered) {
        Map<WorldObject, Integer> oldLayers = new LinkedHashMap<>();
        for (WorldObject obj : context.getObjects()) {
            oldLayers.put(obj, obj.getLayer());
        }

        Map<WorldObject, Integer> newLayers = new LinkedHashMap<>();
        for (int i = 0; i < reordered.size(); i++) {
            newLayers.put(reordered.get(i), i);
        }

        context.executeCommand(new CommandHistory.Command() {
            @Override
            public void execute() {
                applyLayers(newLayers);
            }

            @Override
            public void undo() {
                applyLayers(oldLayers);
            }
        });
    }

    /**
     * 批量写回图层值并重绘
     */
    private void applyLayers(Map<WorldObject, Integer> layers) {
        layers.forEach(WorldObject::setLayer);
        context.redraw();
    }
}
