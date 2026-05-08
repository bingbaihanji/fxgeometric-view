package com.bingbaihanji.controller;

import com.bingbaihanji.view.layout.draw.geometry.WorldObject;

import java.util.List;

/**
 * 层级控制器
 * <p>
 * 负责图形对象的层级管理：前移、后移、置顶、置底
 */
public class LayerController {

    private final DrawingContext context;

    public LayerController(DrawingContext context) {
        this.context = context;
    }

    /**
     * 将选中对象上移一层
     */
    public void bringSelectionForward() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        for (WorldObject obj : selectedObjects) {
            int index = allObjects.indexOf(obj);
            if (index < allObjects.size() - 1) {
                allObjects.remove(index);
                allObjects.add(index + 1, obj);
            }
        }
        context.redraw();
    }

    /**
     * 将选中对象下移一层
     */
    public void sendSelectionBackward() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        for (WorldObject obj : selectedObjects) {
            int index = allObjects.indexOf(obj);
            if (index > 0) {
                allObjects.remove(index);
                allObjects.add(index - 1, obj);
            }
        }
        context.redraw();
    }

    /**
     * 将选中对象置于顶层
     */
    public void bringSelectionToFront() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        allObjects.removeAll(selectedObjects);
        allObjects.addAll(selectedObjects);
        context.redraw();
    }

    /**
     * 将选中对象置于底层
     */
    public void sendSelectionToBack() {
        List<WorldObject> selectedObjects = context.getSelectionManager().getSelectedObjects();
        if (selectedObjects.isEmpty()) return;

        List<WorldObject> allObjects = context.getObjects();
        allObjects.removeAll(selectedObjects);
        allObjects.addAll(0, selectedObjects);
        context.redraw();
    }
}
