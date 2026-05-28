package com.bingbaihanji.controller;

import com.bingbaihanji.config.GeometryConfig;
import com.bingbaihanji.view.layout.core.WorldTransform;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 预览管理器
 * <p>
 * 统一管理所有预览对象的更新和绘制,提供一致的预览体验
 * 参考 GeoGebra 的 DrawableND 和 Previewable 接口设计
 *
 * @author bingbaihanji
 * @date 2026-01-05
 */
public class PreviewManager {

    /**
     * 当前活动的预览对象
     */
    private final List<Previewable> previewables = new ArrayList<>();

    /**
     * 添加预览对象
     */
    public void addPreviewable(Previewable previewable) {
        if (!previewables.contains(previewable)) {
            previewables.add(previewable);
        }
    }

    /**
     * 移除预览对象
     */
    public void removePreviewable(Previewable previewable) {
        previewables.remove(previewable);
    }

    /**
     * 清除所有预览对象
     */
    public void clearAll() {
        for (Previewable previewable : previewables) {
            previewable.reset();
        }
        previewables.clear();
    }

    /**
     * 更新所有预览对象
     */
    public void updateAll(double mouseX, double mouseY) {
        for (Previewable previewable : previewables) {
            if (previewable.isActive()) {
                previewable.updatePreview(mouseX, mouseY);
            }
        }
    }

    /**
     * 绘制所有预览对象
     */
    public void paintAll(GraphicsContext gc, WorldTransform transform) {
        for (Previewable previewable : previewables) {
            if (previewable.isActive()) {
                previewable.paintPreview(gc, transform);
            }
        }
    }

    // ========== 管理方法 ==========

    /**
     * 重置所有预览对象
     */
    public void resetAll() {
        for (Previewable previewable : previewables) {
            previewable.reset();
        }
    }

    /**
     * 是否有活动的预览对象
     */
    public boolean hasActivePreview() {
        return previewables.stream().anyMatch(Previewable::isActive);
    }

    /**
     * 预览接口
     * <p>
     * 所有可预览的对象都应实现此接口
     */
    public interface Previewable {
        /**
         * 更新预览状态
         *
         * @param mouseX 当前鼠标X坐标(世界坐标)
         * @param mouseY 当前鼠标Y坐标(世界坐标)
         */
        void updatePreview(double mouseX, double mouseY);

        /**
         * 绘制预览
         *
         * @param gc        绘制上下文
         * @param transform 坐标变换
         */
        void paintPreview(GraphicsContext gc, WorldTransform transform);

        /**
         * 是否有效(是否应该显示)
         *
         * @return true表示应该显示预览
         */
        boolean isActive();

        /**
         * 重置预览状态
         */
        void reset();
    }

    /**
     * 线段预览对象
     */
    public static class LinePreview implements Previewable {
        private final boolean infinite; // 是否为无限直线
        private double x1, y1; // 起点(固定)
        private double x2, y2; // 终点(跟随鼠标)
        private boolean active = false;

        public LinePreview(boolean infinite) {
            this.infinite = infinite;
        }

        public void setStartPoint(double x, double y) {
            this.x1 = x;
            this.y1 = y;
            this.active = true;
        }

        @Override
        public void updatePreview(double mouseX, double mouseY) {
            this.x2 = mouseX;
            this.y2 = mouseY;
        }

        @Override
        public void paintPreview(GraphicsContext gc, WorldTransform transform) {
            if (!active) return;

            double sx1 = transform.worldToScreenX(x1);
            double sy1 = transform.worldToScreenY(y1);
            double sx2 = transform.worldToScreenX(x2);
            double sy2 = transform.worldToScreenY(y2);

            gc.save();
            gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
            gc.setLineWidth(1.5);
            gc.setLineDashes(6);

            if (infinite) {
                // 扩展直线到画布边缘
                double dx = sx2 - sx1;
                double dy = sy2 - sy1;
                double length = Math.sqrt(dx * dx + dy * dy);
                if (length > 1e-6) {
                    double scale = 10000;
                    double t = scale / length;
                    sx1 = sx1 - t * dx;
                    sy1 = sy1 - t * dy;
                    sx2 = sx1 + 2 * t * dx;
                    sy2 = sy1 + 2 * t * dy;
                }
            }

            gc.strokeLine(sx1, sy1, sx2, sy2);
            gc.restore();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void reset() {
            active = false;
        }
    }

    /**
     * 圆形预览对象
     */
    public static class CirclePreview implements Previewable {
        private double cx, cy;    // 圆心(固定)
        private double radius;    // 半径(跟随鼠标)
        private double mouseX, mouseY; // 当前鼠标位置(世界坐标)
        private boolean active = false;

        public void setCenterPoint(double x, double y) {
            this.cx = x;
            this.cy = y;
            this.active = true;
        }

        @Override
        public void updatePreview(double mouseX, double mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.radius = Math.sqrt(Math.pow(mouseX - cx, 2) + Math.pow(mouseY - cy, 2));
        }

        @Override
        public void paintPreview(GraphicsContext gc, WorldTransform transform) {
            if (!active || radius < 1e-6) return;

            double screenCx = transform.worldToScreenX(cx);
            double screenCy = transform.worldToScreenY(cy);
            double screenRadius = radius * transform.getScale();

            gc.save();
            gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
            gc.setLineWidth(1.5);
            gc.setLineDashes(6);
            gc.strokeOval(screenCx - screenRadius, screenCy - screenRadius,
                    screenRadius * 2, screenRadius * 2);

            // 绘制半径线：从圆心指向鼠标位置
            gc.setLineDashes(2);
            double mouseScreenX = transform.worldToScreenX(mouseX);
            double mouseScreenY = transform.worldToScreenY(mouseY);
            gc.strokeLine(screenCx, screenCy, mouseScreenX, mouseScreenY);

            gc.restore();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void reset() {
            active = false;
            radius = 0;
        }
    }

    /**
     * 多边形预览对象
     */
    public static class PolygonPreview implements Previewable {
        private final List<double[]> points = new ArrayList<>();
        private double currentX, currentY; // 当前鼠标位置
        private boolean active = false;

        public void addPoint(double x, double y) {
            points.add(new double[]{x, y});
            active = true;
        }

        public void removeLastPoint() {
            if (!points.isEmpty()) {
                points.remove(points.size() - 1);
            }
            if (points.isEmpty()) {
                active = false;
            }
        }

        @Override
        public void updatePreview(double mouseX, double mouseY) {
            this.currentX = mouseX;
            this.currentY = mouseY;
        }

        @Override
        public void paintPreview(GraphicsContext gc, WorldTransform transform) {
            if (!active || points.isEmpty()) return;

            gc.save();
            gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
            gc.setLineWidth(1.5);
            gc.setLineDashes(6);

            // 绘制已有的边
            for (int i = 0; i < points.size(); i++) {
                double[] p1 = points.get(i);
                double[] p2 = (i < points.size() - 1) ? points.get(i + 1)
                        : new double[]{currentX, currentY};

                double sx1 = transform.worldToScreenX(p1[0]);
                double sy1 = transform.worldToScreenY(p1[1]);
                double sx2 = transform.worldToScreenX(p2[0]);
                double sy2 = transform.worldToScreenY(p2[1]);

                gc.strokeLine(sx1, sy1, sx2, sy2);
            }

            // 如果有3个点以上,绘制闭合线
            if (points.size() >= 2) {
                double[] first = points.get(0);
                double sx1 = transform.worldToScreenX(currentX);
                double sy1 = transform.worldToScreenY(currentY);
                double sx2 = transform.worldToScreenX(first[0]);
                double sy2 = transform.worldToScreenY(first[1]);

                gc.setLineDashes(2);
                gc.setStroke(GeometryConfig.Colors.PREVIEW_LIGHT_TRANSPARENT);
                gc.strokeLine(sx1, sy1, sx2, sy2);
            }

            gc.restore();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void reset() {
            points.clear();
            active = false;
        }

        public int getPointCount() {
            return points.size();
        }
    }

    /**
     * 选择框预览对象
     */
    public static class SelectionRectanglePreview implements Previewable {
        private double x1, y1; // 起点
        private double x2, y2; // 当前鼠标位置
        private boolean active = false;

        public void setStartPoint(double x, double y) {
            this.x1 = x;
            this.y1 = y;
            this.active = true;
        }

        @Override
        public void updatePreview(double mouseX, double mouseY) {
            this.x2 = mouseX;
            this.y2 = mouseY;
        }

        @Override
        public void paintPreview(GraphicsContext gc, WorldTransform transform) {
            if (!active) return;

            double minX = Math.min(x1, x2);
            double minY = Math.min(y1, y2);
            double width = Math.abs(x2 - x1);
            double height = Math.abs(y2 - y1);

            double sx = transform.worldToScreenX(minX);
            double sy = transform.worldToScreenY(minY + height);
            double sw = width * transform.getScale();
            double sh = height * transform.getScale();

            gc.save();
            // 填充半透明背景
            gc.setFill(GeometryConfig.Colors.PREVIEW_FILL);
            gc.fillRect(sx, sy, sw, sh);

            // 绘制边框
            gc.setStroke(GeometryConfig.Colors.PREVIEW_STROKE);
            gc.setLineWidth(1);
            gc.setLineDashes(4);
            gc.strokeRect(sx, sy, sw, sh);
            gc.restore();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void reset() {
            active = false;
        }

        /**
         * 获取选择框的世界坐标范围
         */
        public double[] getBounds() {
            double minX = Math.min(x1, x2);
            double maxX = Math.max(x1, x2);
            double minY = Math.min(y1, y2);
            double maxY = Math.max(y1, y2);
            return new double[]{minX, maxX, minY, maxY};
        }
    }

    /**
     * 椭圆预览对象
     * <p>
     * 三阶段：F1 固定 → F2 固定 → 鼠标确定椭圆上一点 P，2a = |PF1| + |PF2|
     */
    public static class EllipsePreview implements Previewable {
        private double f1x, f1y;      // 焦点1（固定）
        private double f2x, f2y;      // 焦点2（固定，第二阶段设置）
        private double mouseX, mouseY; // 当前鼠标位置
        private boolean active = false;
        private boolean f2Set = false; // F2 是否已设置

        /** 设置焦点1 */
        public void setFocus1(double x, double y) {
            this.f1x = x;
            this.f1y = y;
            this.active = true;
            this.f2Set = false;
        }

        /** 设置焦点2，进入第三阶段 */
        public void setFocus2(double x, double y) {
            this.f2x = x;
            this.f2y = y;
            this.f2Set = true;
        }

        /** 当前 2a 值（鼠标位置到两焦点距离之和） */
        public double getTwoA() {
            if (!f2Set) return 0;
            return Math.hypot(mouseX - f1x, mouseY - f1y)
                    + Math.hypot(mouseX - f2x, mouseY - f2y);
        }

        @Override
        public void updatePreview(double mouseX, double mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        @Override
        public void paintPreview(GraphicsContext gc, WorldTransform transform) {
            if (!active) return;

            gc.save();
            gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
            gc.setLineWidth(1.5);

            // 绘制 F1
            drawFocusDot(gc, transform, f1x, f1y, true);

            if (f2Set) {
                // 绘制 F2
                drawFocusDot(gc, transform, f2x, f2y, true);
                // 绘制 F1-F2 连线
                gc.setLineDashes(2);
                gc.strokeLine(
                        transform.worldToScreenX(f1x), transform.worldToScreenY(f1y),
                        transform.worldToScreenX(f2x), transform.worldToScreenY(f2y));

                // 计算椭圆参数并绘制预览椭圆
                double twoA = getTwoA();
                double a = twoA / 2.0;
                double c = Math.hypot(f2x - f1x, f2y - f1y) / 2.0;
                if (a > c && a > 1e-9) {
                    double b = Math.sqrt(a * a - c * c);
                    double cx = (f1x + f2x) / 2.0;
                    double cy = (f1y + f2y) / 2.0;
                    double angle = Math.atan2(f2y - f1y, f2x - f1x);
                    double cos = Math.cos(angle);
                    double sin = Math.sin(angle);

                    gc.setLineDashes(6);
                    int n = 128;
                    double[] sx = new double[n];
                    double[] sy = new double[n];
                    for (int i = 0; i < n; i++) {
                        double t = 2 * Math.PI * i / n;
                        double xt = a * Math.cos(t);
                        double yt = b * Math.sin(t);
                        double wx = cx + xt * cos - yt * sin;
                        double wy = cy + xt * sin + yt * cos;
                        sx[i] = transform.worldToScreenX(wx);
                        sy[i] = transform.worldToScreenY(wy);
                    }
                    gc.strokePolygon(sx, sy, n);
                }
            }

            // 绘制鼠标跟随点（半透明）
            double mouseSx = transform.worldToScreenX(mouseX);
            double mouseSy = transform.worldToScreenY(mouseY);
            gc.setStroke(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(1.5);
            gc.setLineDashes(null);
            gc.strokeOval(mouseSx - 6, mouseSy - 6, 12, 12);
            gc.setFill(GeometryConfig.Colors.PREVIEW.deriveColor(0, 1, 1, 0.6));
            gc.fillOval(mouseSx - 4, mouseSy - 4, 8, 8);

            gc.restore();
        }

        private void drawFocusDot(GraphicsContext gc, WorldTransform transform,
                                  double wx, double wy, boolean filled) {
            double sx = transform.worldToScreenX(wx);
            double sy = transform.worldToScreenY(wy);
            gc.setFill(GeometryConfig.Colors.PREVIEW);
            gc.fillOval(sx - 4, sy - 4, 8, 8);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void reset() {
            active = false;
            f2Set = false;
        }

        public boolean isF2Set() {
            return f2Set;
        }
    }

    /**
     * 正多边形预览对象
     */
    public static class RegularPolygonPreview implements Previewable {
        private double cx, cy;        // 中心(固定)
        private double radius;        // 半径(跟随鼠标)
        private int sideCount = 6;    // 边数
        private double mouseX, mouseY; // 当前鼠标位置(世界坐标)
        private boolean active = false;

        public void setCenterPoint(double x, double y) {
            this.cx = x;
            this.cy = y;
            this.active = true;
        }

        public void setSideCount(int sideCount) {
            this.sideCount = Math.max(3, Math.min(10, sideCount));
        }

        /**
         * 更新预览(带半径参数)
         */
        public void updatePreview(double mouseX, double mouseY, double radius) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.radius = radius;
        }

        /**
         * 更新预览(鼠标位置作为顶点位置)
         */
        public void updatePreviewWithVertex(double vertexX, double vertexY) {
            this.mouseX = vertexX;
            this.mouseY = vertexY;
            // 计算半径：从中心到顶点的距离
            this.radius = Math.sqrt(Math.pow(vertexX - cx, 2) + Math.pow(vertexY - cy, 2));
        }

        @Override
        public void updatePreview(double mouseX, double mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.radius = Math.sqrt(Math.pow(mouseX - cx, 2) + Math.pow(mouseY - cy, 2));
        }

        @Override
        public void paintPreview(GraphicsContext gc, WorldTransform transform) {
            if (!active || radius < 1e-6) return;

            gc.save();
            gc.setStroke(GeometryConfig.Colors.PREVIEW_TRANSPARENT);
            gc.setLineWidth(1.5);
            gc.setLineDashes(6);

            // 计算第一个顶点指向鼠标的角度
            double angleToMouse = Math.atan2(mouseY - cy, mouseX - cx);

            // 计算顶点坐标(第一个顶点在鼠标位置方向)
            double[] xPoints = new double[sideCount];
            double[] yPoints = new double[sideCount];

            double angleStep = 2 * Math.PI / sideCount;

            for (int i = 0; i < sideCount; i++) {
                double angle = angleToMouse + i * angleStep;
                double worldX = cx + radius * Math.cos(angle);
                double worldY = cy + radius * Math.sin(angle);

                xPoints[i] = transform.worldToScreenX(worldX);
                yPoints[i] = transform.worldToScreenY(worldY);
            }

            // 绘制正多边形
            gc.strokePolygon(xPoints, yPoints, sideCount);

            // 绘制半径线：从中心指向第一个顶点(鼠标位置)
            gc.setLineDashes(2);
            double screenCx = transform.worldToScreenX(cx);
            double screenCy = transform.worldToScreenY(cy);
            double mouseScreenX = transform.worldToScreenX(mouseX);
            double mouseScreenY = transform.worldToScreenY(mouseY);
            gc.strokeLine(screenCx, screenCy, mouseScreenX, mouseScreenY);

            gc.restore();
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void reset() {
            active = false;
            radius = 0;
        }
    }
}
