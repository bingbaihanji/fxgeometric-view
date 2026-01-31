package com.bingbaihanji.util;

import com.bingbaihanji.constant.DrawMode;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 光标管理器
 * <p>
 * 统一管理不同模式和状态下的光标样式
 * 参考 GeoGebra 的光标管理设计
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public class CursorManager {

    /**
     * 模式对应的光标缓存
     */
    private final Map<DrawMode, Cursor> modeCursors = new HashMap<>();
    /**
     * 特殊状态光标缓存
     */
    private final Map<String, Cursor> stateCursors = new HashMap<>();
    /**
     * 默认光标
     */
    private Cursor defaultCursor = Cursor.DEFAULT;

    /**
     * 初始化光标管理器
     */
    public CursorManager() {
        initializeModeCursors();
        initializeStateCursors();
    }

    /**
     * 初始化各模式的光标
     */
    private void initializeModeCursors() {
        // 点模式 - 十字光标
        modeCursors.put(DrawMode.POINT, Cursor.CROSSHAIR);

        // 线段模式 - 十字光标
        modeCursors.put(DrawMode.LINE, Cursor.CROSSHAIR);

        // 无限直线模式 - 十字光标
        modeCursors.put(DrawMode.INFINITE_LINE, Cursor.CROSSHAIR);

        // 圆形模式 - 十字光标
        modeCursors.put(DrawMode.CIRCLE, Cursor.CROSSHAIR);

        // 多边形模式 - 十字光标
        modeCursors.put(DrawMode.POLYGON, Cursor.CROSSHAIR);

        // 手绘模式 - 画笔光标(如果有自定义图标)
        modeCursors.put(DrawMode.FREEHAND, Cursor.HAND);

        // 旋转模式 - 特殊旋转光标(可以自定义)
        modeCursors.put(DrawMode.ROTATE, Cursor.MOVE);

        // 选择模式 - 默认光标
        modeCursors.put(DrawMode.NONE, Cursor.DEFAULT);
    }

    /**
     * 初始化特殊状态的光标
     */
    private void initializeStateCursors() {
        // 拖动状态
        stateCursors.put("dragging", Cursor.CLOSED_HAND);

        // 移动状态
        stateCursors.put("moving", Cursor.MOVE);

        // 调整大小(横向)
        stateCursors.put("resize_h", Cursor.H_RESIZE);

        // 调整大小(纵向)
        stateCursors.put("resize_v", Cursor.V_RESIZE);

        // 调整大小(对角线)
        stateCursors.put("resize_nw_se", Cursor.NW_RESIZE);
        stateCursors.put("resize_ne_sw", Cursor.NE_RESIZE);

        // 等待状态
        stateCursors.put("wait", Cursor.WAIT);

        // 禁止操作
        stateCursors.put("not_allowed", Cursor.NONE);

        // 文本选择
        stateCursors.put("text", Cursor.TEXT);

        // 手形光标(可点击)
        stateCursors.put("hand", Cursor.HAND);

        // 十字光标(精确定位)
        stateCursors.put("crosshair", Cursor.CROSSHAIR);
    }

    /**
     * 获取模式对应的光标
     *
     * @param mode 绘制模式
     * @return 光标
     */
    public Cursor getModeBasedCursor(DrawMode mode) {
        return modeCursors.getOrDefault(mode, defaultCursor);
    }

    /**
     * 获取特殊状态的光标
     *
     * @param state 状态名称
     * @return 光标
     */
    public Cursor getStateCursor(String state) {
        return stateCursors.getOrDefault(state, defaultCursor);
    }

    /**
     * 获取默认光标
     */
    public Cursor getDefaultCursor() {
        return defaultCursor;
    }

    /**
     * 设置默认光标
     *
     * @param cursor 默认光标
     */
    public void setDefaultCursor(Cursor cursor) {
        this.defaultCursor = cursor;
    }

    /**
     * 加载自定义图标光标
     *
     * @param imagePath 图标路径(相对于resources目录)
     * @param hotspotX  热点X坐标
     * @param hotspotY  热点Y坐标
     * @return 图标光标,如果加载失败则返回默认光标
     */
    public Cursor loadCustomCursor(String imagePath, double hotspotX, double hotspotY) {
        try {
            URL url = getClass().getResource(imagePath);
            if (url != null) {
                Image image = new Image(url.toExternalForm());
                return new ImageCursor(image, hotspotX, hotspotY);
            }
        } catch (Exception e) {
            // 加载失败,返回默认光标
            return defaultCursor;
        }
        return defaultCursor;
    }

    /**
     * 注册自定义模式光标
     *
     * @param mode   绘制模式
     * @param cursor 光标
     */
    public void registerModeCursor(DrawMode mode, Cursor cursor) {
        modeCursors.put(mode, cursor);
    }

    /**
     * 注册自定义状态光标
     *
     * @param state  状态名称
     * @param cursor 光标
     */
    public void registerStateCursor(String state, Cursor cursor) {
        stateCursors.put(state, cursor);
    }

    // ========== 快捷方法 ==========

    /**
     * 获取拖动光标
     */
    public Cursor getDraggingCursor() {
        return getStateCursor("dragging");
    }

    /**
     * 获取移动光标
     */
    public Cursor getMoveCursor() {
        return getStateCursor("moving");
    }

    /**
     * 获取手形光标
     */
    public Cursor getHandCursor() {
        return getStateCursor("hand");
    }

    /**
     * 获取十字光标
     */
    public Cursor getCrosshairCursor() {
        return getStateCursor("crosshair");
    }

    /**
     * 获取等待光标
     */
    public Cursor getWaitCursor() {
        return getStateCursor("wait");
    }

    /**
     * 获取禁止光标
     */
    public Cursor getNotAllowedCursor() {
        return getStateCursor("not_allowed");
    }

    // ========== 智能光标选择 ==========

    /**
     * 根据上下文智能选择光标
     *
     * @param mode       当前绘制模式
     * @param isDragging 是否正在拖动
     * @param nearVertex 是否靠近顶点
     * @param nearEdge   是否靠近边缘
     * @return 合适的光标
     */
    public Cursor getSmartCursor(DrawMode mode, boolean isDragging, boolean nearVertex, boolean nearEdge) {
        // 拖动状态优先
        if (isDragging) {
            return getDraggingCursor();
        }

        // 非绘制模式下的特殊情况
        if (mode == DrawMode.NONE) {
            if (nearVertex) {
                return getCrosshairCursor();
            }
            if (nearEdge) {
                return getHandCursor();
            }
            return defaultCursor;
        }

        // 绘制模式下使用模式光标
        return getModeBasedCursor(mode);
    }

    /**
     * 预定义的光标状态枚举
     */
    public enum CursorState {
        DEFAULT,          // 默认
        MODE_BASED,       // 基于模式
        DRAGGING,         // 拖动中
        MOVING,           // 移动中
        HAND,             // 手形(可点击)
        CROSSHAIR,        // 十字(精确定位)
        RESIZE_HORIZONTAL,// 横向调整大小
        RESIZE_VERTICAL,  // 纵向调整大小
        WAIT,             // 等待
        NOT_ALLOWED       // 禁止操作
    }
}
