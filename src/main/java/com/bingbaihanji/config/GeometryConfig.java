package com.bingbaihanji.config;

/**
 * 几何绘图常量配置类
 * <p>
 * 统一管理所有魔数，提高代码可维护性
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public final class GeometryConfig {

    private GeometryConfig() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * 吸附相关常量
     */
    public static class Snapping {
        /**
         * 点吸附阈值（像素）
         */
        public static final double POINT_SNAP_THRESHOLD_PIXELS = 15.0;

        /**
         * 边吸附阈值（像素）
         */
        public static final double EDGE_SNAP_THRESHOLD_PIXELS = 10.0;

        /**
         * 网格吸附阈值（像素）
         */
        public static final double GRID_SNAP_THRESHOLD_PIXELS = 12.0;

        /**
         * 圆相切吸附阈值（像素）
         */
        public static final double CIRCLE_TANGENT_THRESHOLD_PIXELS = 15.0;

        private Snapping() {
        }
    }

    /**
     * 点渲染相关常量
     */
    public static class PointRendering {
        /**
         * 默认点半径（像素）
         */
        public static final double DEFAULT_POINT_RADIUS = 4.0;

        /**
         * 悬停状态点半径（像素）
         */
        public static final double HOVER_POINT_RADIUS = 6.0;

        /**
         * 选中状态点半径（像素）
         */
        public static final double SELECTED_POINT_RADIUS = 6.0;

        /**
         * 小点半径（像素）- 用于多边形顶点等
         */
        public static final double SMALL_POINT_RADIUS = 3.0;

        /**
         * 预览点半径（像素）
         */
        public static final double PREVIEW_POINT_RADIUS = 4.0;

        /**
         * 点的边界框边距（世界坐标）
         */
        public static final double BOUNDING_BOX_MARGIN = 0.1;

        private PointRendering() {
        }
    }

    /**
     * 容差和检测相关常量
     */
    public static class Tolerance {
        /**
         * 点复用检测阈值（像素）
         */
        public static final double POINT_REUSE_THRESHOLD_PIXELS = 10.0;

        /**
         * 顶点命中测试容差（像素）
         */
        public static final double VERTEX_HIT_TEST_PIXELS = 10.0;

        /**
         * 对象命中测试容差（像素）
         */
        public static final double OBJECT_HIT_TEST_PIXELS = 5.0;

        /**
         * 多边形闭合检测阈值（像素）
         */
        public static final double POLYGON_CLOSE_THRESHOLD_PIXELS = 15.0;

        /**
         * 约束图形检测阈值（像素）
         */
        public static final double CONSTRAINT_SNAP_DISTANCE_PIXELS = 15.0;

        private Tolerance() {
        }
    }

    /**
     * 线条和样式相关常量
     */
    public static class LineStyle {
        /**
         * 默认线宽（像素）
         */
        public static final double DEFAULT_LINE_WIDTH = 2.0;

        /**
         * 悬停状态线宽（像素）
         */
        public static final double HOVER_LINE_WIDTH = 3.0;

        /**
         * 预览线虚线长度（像素）
         */
        public static final double PREVIEW_DASH_LENGTH = 6.0;

        /**
         * 多边形预览虚线长度（像素）
         */
        public static final double POLYGON_PREVIEW_DASH_LENGTH = 4.0;

        /**
         * 无限直线扩展系数
         */
        public static final double INFINITE_LINE_EXTENSION_FACTOR = 10000.0;

        private LineStyle() {
        }
    }

    /**
     * 预览和临时对象相关常量
     */
    public static class Preview {
        /**
         * 预览颜色代码
         */
        public static final String PREVIEW_COLOR = "#759eb2";

        /**
         * 闭合高亮颜色
         */
        public static final String CLOSE_HIGHLIGHT_COLOR = "LIGHTGREEN";

        /**
         * 预览线虚线模式
         */
        public static final double[] PREVIEW_DASH_PATTERN = {6.0};

        /**
         * 闭合线虚线模式
         */
        public static final double[] CLOSE_DASH_PATTERN = {4.0};

        private Preview() {
        }
    }

    /**
     * 函数采样相关常量
     */
    public static class FunctionSampling {
        /**
         * 默认每单位世界坐标的采样点数
         */
        public static final int SAMPLES_PER_UNIT = 5;

        /**
         * 最小采样点数
         */
        public static final int MIN_SAMPLES = 100;

        /**
         * 最大采样点数
         */
        public static final int MAX_SAMPLES = 10000;

        private FunctionSampling() {
        }
    }

    /**
     * 标签和文字相关常量
     */
    public static class Label {
        /**
         * 默认字体大小
         */
        public static final double DEFAULT_FONT_SIZE = 12.0;

        /**
         * 标签偏移量（像素）
         */
        public static final double LABEL_OFFSET_X = 8.0;
        public static final double LABEL_OFFSET_Y = -8.0;

        private Label() {
        }
    }

    /**
     * 性能和优化相关常量
     */
    public static class Performance {
        /**
         * 浮点数比较精度
         */
        public static final double EPSILON = 0.0001;

        /**
         * 圆心重合判定阈值
         */
        public static final double CIRCLE_CENTER_COINCIDE_THRESHOLD = 0.0001;

        /**
         * 最小有效距离
         */
        public static final double MIN_VALID_DISTANCE = 1e-10;

        private Performance() {
        }
    }

    /**
     * 旋转和变换相关常量
     */
    public static class Transform {
        /**
         * 旋转中心点半径（像素）
         */
        public static final double ROTATION_CENTER_RADIUS = 5.0;

        /**
         * 旋转圆圈半径（像素）
         */
        public static final double ROTATION_CIRCLE_RADIUS = 30.0;

        /**
         * 旋转步长（弧度）- 15度
         */
        public static final double ROTATION_STEP = Math.PI / 12;

        private Transform() {
        }
    }
}
