package com.bingbaihanji;




/**
 *
 * @author bingbaihanji
 * @date 2025-12-20 15:19:38
 * @description 程序入口
 */
public class FXGeometricApplication {
    public static void main(String[] args) {
        // 设置英文环境
//         java.util.Locale.setDefault(java.util.Locale.US);
        // d3d  Direct3D 仅限 Windows, Windows平台性能最优的硬件加速后端
        // es2  OpenGL ES 2.0	 跨平台（Windows/macOS/Linux等）
        // metal Metal macOS平台专用的高性能硬件加速后端
        // sw	Software Rendering	 纯CPU软件渲染的兜底方案
//        System.setProperty("prism.order", "metal,es2,sw"); // 优先硬件加速，失败后回退到软件渲染（推荐）
        FXGeometricView.launch(FXGeometricView.class, args);

    }
}