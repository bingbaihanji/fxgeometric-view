package com.bingbaihanji.factory;

import com.bingbaihanji.constant.FunctionType;
import com.bingbaihanji.model.FunctionInputResult;
import com.bingbaihanji.util.Logger;
import com.bingbaihanji.view.layout.draw.geometry.impl.*;

import java.util.Map;

/**
 * 函数几何对象工厂
 * <p>
 * 使用工厂模式封装函数对象的创建逻辑
 * 消除DrawingController中的switch-case代码
 *
 * @author bingbaihanji
 * @date 2025-01-04
 */
public class FunctionFactory {

    private static final Logger logger = Logger.getLogger(FunctionFactory.class);

    /**
     * 根据输入结果创建函数对象
     *
     * @param input 函数输入结果
     * @return 创建的函数对象,如果失败则返回null
     */
    public static FunctionGeo createFunction(FunctionInputResult input) {
        if (input == null) {
            logger.warn("函数输入结果为null");
            return null;
        }

        try {
            Map<String, Double> params = input.getParameters();
            FunctionType type = input.getType();

            FunctionGeo function = createByType(input, type, params);

            // 设置定义域
            if (function != null && !input.isAutoRange()) {
                function.setDomainRange(input.getDomainMin(), input.getDomainMax());
            }

            return function;
        } catch (Exception e) {
            logger.error("创建函数对象时发生错误: " + input.getType(), e);
            return null;
        }
    }

    /**
     * 根据类型和参数创建函数对象
     */
    private static FunctionGeo createByType(FunctionInputResult input, FunctionType type, Map<String, Double> params) {
        return switch (type) {
            case LINEAR -> createLinearFunction(params);
            case QUADRATIC -> createQuadraticFunction(params);
            case RECIPROCAL -> createReciprocalFunction(params);
            case SINE, COSINE, TANGENT -> createTrigonometricFunction(type, params);
            case EXPONENTIAL -> createExponentialFunction(params);
            case LOGARITHMIC -> createLogarithmicFunction(params);
            case ELLIPSE -> createEllipseFunction(params);
            case HYPERBOLA -> createHyperbolaFunction(params);
            case PARABOLA_CONIC -> createParabolaConicFunction(params);
            case CUSTOM -> createCustomFunction(input);
            default -> {
                logger.error("不支持的函数类型: " + type);
                yield null;
            }
        };
    }

    /**
     * 创建一次函数 y = kx + b
     */
    private static LinearFunctionGeo createLinearFunction(Map<String, Double> params) {
        return new LinearFunctionGeo(
                getParam(params, "k", 1.0),
                getParam(params, "b", 0.0)
        );
    }

    /**
     * 创建二次函数 y = ax² + bx + c
     */
    private static QuadraticFunctionGeo createQuadraticFunction(Map<String, Double> params) {
        return new QuadraticFunctionGeo(
                getParam(params, "a", 1.0),
                getParam(params, "b", 0.0),
                getParam(params, "c", 0.0)
        );
    }

    /**
     * 创建反比例函数 y = k/x
     */
    private static ReciprocalFunctionGeo createReciprocalFunction(Map<String, Double> params) {
        return new ReciprocalFunctionGeo(
                getParam(params, "k", 1.0)
        );
    }

    /**
     * 创建三角函数 y = A·sin(ωx + φ) + k
     */
    private static TrigonometricFunctionGeo createTrigonometricFunction(FunctionType type, Map<String, Double> params) {
        return new TrigonometricFunctionGeo(
                type,
                getParam(params, "A", 1.0),
                getParam(params, "omega", 1.0),
                getParam(params, "phi", 0.0),
                getParam(params, "k", 0.0)
        );
    }

    /**
     * 创建指数函数 y = a^x
     */
    private static ExponentialFunctionGeo createExponentialFunction(Map<String, Double> params) {
        return new ExponentialFunctionGeo(
                getParam(params, "a", Math.E)
        );
    }

    /**
     * 创建对数函数 y = log_a(x)
     */
    private static LogarithmicFunctionGeo createLogarithmicFunction(Map<String, Double> params) {
        return new LogarithmicFunctionGeo(
                getParam(params, "a", Math.E)
        );
    }

    /**
     * 创建椭圆函数 (x-cx)²/a² + (y-cy)²/b² = 1
     */
    private static EllipseFunctionGeo createEllipseFunction(Map<String, Double> params) {
        return new EllipseFunctionGeo(
                getParam(params, "cx", 0.0),
                getParam(params, "cy", 0.0),
                getParam(params, "a", 1.0),
                getParam(params, "b", 1.0)
        );
    }

    /**
     * 创建双曲线函数 (x-cx)²/a² - (y-cy)²/b² = 1
     */
    private static HyperbolaFunctionGeo createHyperbolaFunction(Map<String, Double> params) {
        return new HyperbolaFunctionGeo(
                getParam(params, "cx", 0.0),
                getParam(params, "cy", 0.0),
                getParam(params, "a", 1.0),
                getParam(params, "b", 1.0)
        );
    }

    /**
     * 创建抛物线(圆锥曲线)函数 y² = 2px
     */
    private static ParabolaConicFunctionGeo createParabolaConicFunction(Map<String, Double> params) {
        return new ParabolaConicFunctionGeo(
                getParam(params, "p", 1.0)
        );
    }

    /**
     * 创建自定义表达式函数 y = f(x)
     */
    private static CustomFunctionGeo createCustomFunction(FunctionInputResult input) {
        String expr = input.getCustomExpression();
        if (expr == null || expr.isBlank()) {
            logger.error("自定义函数表达式为空");
            return null;
        }
        return new CustomFunctionGeo(expr);
    }

    /**
     * 从参数Map中获取参数值,如果不存在则使用默认值
     */
    private static double getParam(Map<String, Double> params, String key, double defaultValue) {
        Double value = params.get(key);
        return value != null ? value : defaultValue;
    }
}
