package com.bingbaihanji.view.layout.core;

import com.bingbaihanji.geometry.WorldObject;
import com.bingbaihanji.geometry.impl.TrigonometricFunctionGeo;

import java.util.Collections;
import java.util.List;

/**
 * 视图对象集合
 * <p>
 * 管理几何对象列表及其派生状态: 三角函数计数与对象变化监听器.
 * 列表本身可与其他窗口共享, 因此这里只负责在共享列表上做增删,
 * 不涉及重绘与吸附缓存等视图/控制器关注点.
 */
public class ObjectCollection {

    /**
     * 对象列表本体(可与其它窗口共享)
     */
    private final List<WorldObject> objects;

    /**
     * 只读视图, 构造时创建一次
     */
    private final List<WorldObject> readOnlyView;

    /**
     * 当前三角函数对象数量, 用于自动切换坐标轴单位标签
     */
    private int trigFunctionCount = 0;

    /**
     * 构造函数
     *
     * @param sharedObjects 对象列表, 可与其他视图共享
     */
    public ObjectCollection(List<WorldObject> sharedObjects) {
        this.objects = sharedObjects;
        this.readOnlyView = Collections.unmodifiableList(sharedObjects);
        // 共享列表可能已有内容, 初始化三角函数计数
        for (WorldObject obj : sharedObjects) {
            if (obj instanceof TrigonometricFunctionGeo) {
                trigFunctionCount++;
            }
        }
    }

    /**
     * 获取只读对象视图(零分配)
     *
     * @return 只读列表
     */
    public List<WorldObject> get() {
        return readOnlyView;
    }

    /**
     * 获取列表本体, 仅供同包的多窗口构造共享使用
     * <p>
     * 调用方不得直接修改结构, 增删必须走 {@link #add} / {@link #remove} / {@link #clear},
     * 否则三角函数计数与变化通知会失同步.
     *
     * @return 可变的共享列表
     */
    List<WorldObject> sharedList() {
        return objects;
    }

    /**
     * 添加对象并更新三角函数计数
     *
     * @param obj 几何对象
     */
    public void add(WorldObject obj) {
        objects.add(obj);
        if (obj instanceof TrigonometricFunctionGeo) {
            trigFunctionCount++;
        }
    }

    /**
     * 移除对象并更新三角函数计数
     *
     * @param obj 几何对象
     */
    public void remove(WorldObject obj) {
        objects.remove(obj);
        if (obj instanceof TrigonometricFunctionGeo) {
            trigFunctionCount--;
        }
    }

    /**
     * 清空所有对象
     */
    public void clear() {
        objects.clear();
        trigFunctionCount = 0;
    }

    /**
     * 当前是否存在三角函数对象
     *
     * @return true 表示存在
     */
    public boolean hasTrigonometricFunction() {
        return trigFunctionCount > 0;
    }
}
