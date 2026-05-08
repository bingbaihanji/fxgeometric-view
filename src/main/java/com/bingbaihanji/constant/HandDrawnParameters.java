package com.bingbaihanji.constant;

/**
 * 手绘风格绘制参数默认值枚举
 * <p>提供曲线简化、平滑、张力、最小间距等核心算法的默认配置</p>
 */
public enum HandDrawnParameters {

    /**
     * 简化容差：控制曲线简化的敏感度，值越小保留的点越多，曲线越精细
     */
    DEFAULT_SIMPLIFY_EPSILON("简化容差 (epsilon)", 0.1d,
            "道格拉斯-普克简化算法的容差参数。值越小，简化后保留的点越多，曲线越接近原始路径；值越大，平坦部分被简化成直线的程度越高，曲线越粗糙"),

    /**
     * 平滑细分数：每两个控制点之间生成的中间点数量
     */
    DEFAULT_SMOOTH_SEGMENTS("平滑细分数 (segments)", 26,
            "曲线插值时，每两个相邻控制点之间生成的中间点数量。值越大，曲线越平滑细腻，但计算开销也越大。推荐范围 10~50"),

    /**
     * 张力：控制曲线的紧绷程度
     */
    DEFAULT_TENSION("张力 (tension)", 0.85d,
            "影响 Catmull-Rom 样条曲线的弯曲特性。值越小（接近0），曲线越紧绷，线段越直；值越大（接近1），曲线越松弛，弯折越明显。推荐范围 0.3~0.9"),

    /**
     * 最小点间距：过滤过密采样点的阈值
     */
    DEFAULT_MIN_POINT_DISTANCE("最小点间距 (minDistance)", 0.02d,
            "当鼠标或触摸移动时，若相邻两个采样点的欧氏距离小于该阈值，则丢弃后一个点，避免产生过密冗余点。单位与坐标系相关，通常为世界单位或像素");

    private final String name;      // 参数显示名称（含英文标识）
    private final Number value;     // 默认数值
    private final String desc;      // 详细说明

    HandDrawnParameters(String name, Number value, String desc) {
        this.name = name;
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据显示名称查找枚举（支持中文名或"中文 (英文)"格式）
     *
     * @param displayName 显示名称，如 "简化容差 (epsilon)" 或 "简化容差"
     * @return 匹配的枚举，未找到返回 null
     */
    public static HandDrawnParameters fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (HandDrawnParameters p : values()) {
            if (p.name.equals(displayName) || p.name.startsWith(displayName + " (")) {
                return p;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public Number getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return String.format("HandDrawnParameter{name='%s', value=%s, desc='%s'}", name, value, desc);
    }
}