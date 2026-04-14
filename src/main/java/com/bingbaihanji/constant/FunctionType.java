package com.bingbaihanji.constant;

import com.bingbaihanji.model.FunctionParameter;

/**
 * 函数类型枚举
 * <p>
 * 定义所有支持的函数类型及其参数
 *
 * @author bingbaihanji
 * @date 2026-01-04
 */
public enum FunctionType {
    /**
     * 一次函数：y = kx + b
     */
    LINEAR("一次函数",
            new FunctionParameter[]{
                    new FunctionParameter("k", "斜率 k", "1.0", "直线的斜率"),
                    new FunctionParameter("b", "截距 b", "0.0", "y轴截距")
            }),

    /**
     * 二次函数：y = ax² + bx + c
     */
    QUADRATIC("二次函数",
            new FunctionParameter[]{
                    new FunctionParameter("a", "二次项系数 a", "1.0", "不能为0"),
                    new FunctionParameter("b", "一次项系数 b", "0.0", "一次项系数"),
                    new FunctionParameter("c", "常数项 c", "0.0", "常数项")
            }),

    /**
     * 反比例函数：y = k/x
     */
    RECIPROCAL("反比例函数",
            new FunctionParameter[]{
                    new FunctionParameter("k", "系数 k", "1.0", "反比例系数,k≠0")
            }),

    /**
     * 正弦函数：y = A·sin(ωx + φ) + k
     */
    SINE("正弦函数",
            new FunctionParameter[]{
                    new FunctionParameter("A", "振幅 A", "1.0", "振幅"),
                    new FunctionParameter("omega", "角频率 ω", "1.0", "角频率"),
                    new FunctionParameter("phi", "初相 φ", "0.0", "初相位(弧度)"),
                    new FunctionParameter("k", "垂直偏移 k", "0.0", "垂直偏移")
            }),

    /**
     * 余弦函数：y = A·cos(ωx + φ) + k
     */
    COSINE("余弦函数",
            new FunctionParameter[]{
                    new FunctionParameter("A", "振幅 A", "1.0", "振幅"),
                    new FunctionParameter("omega", "角频率 ω", "1.0", "角频率"),
                    new FunctionParameter("phi", "初相 φ", "0.0", "初相位(弧度)"),
                    new FunctionParameter("k", "垂直偏移 k", "0.0", "垂直偏移")
            }),

    /**
     * 正切函数：y = A·tan(ωx + φ) + k
     */
    TANGENT("正切函数",
            new FunctionParameter[]{
                    new FunctionParameter("A", "系数 A", "1.0", "系数"),
                    new FunctionParameter("omega", "角频率 ω", "1.0", "角频率"),
                    new FunctionParameter("phi", "初相 φ", "0.0", "初相位(弧度)"),
                    new FunctionParameter("k", "垂直偏移 k", "0.0", "垂直偏移")
            }),

    /**
     * 指数函数：y = a^x
     */
    EXPONENTIAL("指数函数",
            new FunctionParameter[]{
                    new FunctionParameter("a", "底数 a", "2.0", "底数,a>0且a≠1")
            }),

    /**
     * 对数函数：y = log_a(x)
     */
    LOGARITHMIC("对数函数",
            new FunctionParameter[]{
                    new FunctionParameter("a", "底数 a", "2.0", "底数,a>0且a≠1")
            }),

    /**
     * 椭圆：(x-cx)²/a² + (y-cy)²/b² = 1
     */
    ELLIPSE("椭圆",
            new FunctionParameter[]{
                    new FunctionParameter("cx", "中心 x", "0.0", "椭圆中心x坐标"),
                    new FunctionParameter("cy", "中心 y", "0.0", "椭圆中心y坐标"),
                    new FunctionParameter("a", "长半轴 a", "2.0", "长半轴长度"),
                    new FunctionParameter("b", "短半轴 b", "1.0", "短半轴长度")
            }),

    /**
     * 双曲线：(x-cx)²/a² - (y-cy)²/b² = 1
     */
    HYPERBOLA("双曲线",
            new FunctionParameter[]{
                    new FunctionParameter("cx", "中心 x", "0.0", "双曲线中心x坐标"),
                    new FunctionParameter("cy", "中心 y", "0.0", "双曲线中心y坐标"),
                    new FunctionParameter("a", "实半轴 a", "2.0", "实半轴长度"),
                    new FunctionParameter("b", "虚半轴 b", "1.0", "虚半轴长度")
            }),

    /**
     * 抛物线(圆锥曲线)：y² = 2px
     */
    PARABOLA_CONIC("抛物线",
            new FunctionParameter[]{
                    new FunctionParameter("p", "焦参数 p", "1.0", "焦点到准线距离的一半")
            }),

    /**
     * 自定义表达式函数：y = f(x)
     * <p>
     * 用户直接输入数学表达式,例如 sin(x) + x^2
     */
    CUSTOM("自定义函数", new FunctionParameter[0]);

    private final String displayName;
    private final FunctionParameter[] parameters;

    FunctionType(String displayName, FunctionParameter[] parameters) {
        this.displayName = displayName;
        this.parameters = parameters;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FunctionParameter[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
