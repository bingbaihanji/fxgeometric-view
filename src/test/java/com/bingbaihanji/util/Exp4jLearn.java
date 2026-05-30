//package com.bingbaihanji.util;
//
//import net.objecthunter.exp4j.Expression;
//import net.objecthunter.exp4j.ExpressionBuilder;
//
//import net.objecthunter.exp4j.function.Function;
//
///**
// *
// * @author bingbaihanji
// */
//public class Exp4jLearn {
//
//    public static void main(String[] args) {
//        demo2();
//    }
//
//    private static void demo1() {
//        Expression exp = new ExpressionBuilder("3 * sin(x) - 2 / (x - 2)")
//                .variables("x")          // 声明变量名
//                .build()
//                .setVariable("x", 0);  // 赋值
//
//        double result = exp.evaluate();
//        System.out.println(result);  // 输出 3 * sin(π/2) - 2/(π/2 - 2)}
//
//    }
//
//    private static void demo2() {
//        Function logb = new Function("logb", 1) {
//            @Override
//            public double apply(double... args) {
//                // 换底公式
//                return  (Math.log(args[0]) / Math.log(2));
//            }
//        };
//
//        double result = new ExpressionBuilder("logb(8)")
//                .function(logb)
//                .build()
//                .evaluate();
//
//        System.out.println(result);
//
//
//
//
//    }
//
//
//}