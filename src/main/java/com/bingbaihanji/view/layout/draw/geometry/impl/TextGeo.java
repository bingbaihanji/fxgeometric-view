package com.bingbaihanji.view.layout.draw.geometry.impl;

import java.util.List;

import com.bingbaihanji.constant.ObjectType;
import com.bingbaihanji.util.FillRenderer;
import com.bingbaihanji.util.LineStyleUtil;
import com.bingbaihanji.util.StyleManager;
import com.bingbaihanji.view.layout.core.WorldTransform;
import com.bingbaihanji.view.layout.draw.geometry.GeometryVisitor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * 自由文本标注
 * <p>
 * 单击定位后输入文本内容，支持背景填充、边框和旋转。
 * </p>
 *
 * @author bingbaihanji
 * @date 2026-06-23
 */
public class TextGeo extends AbstractWorldObject {

    private double x;
    private double y;
    private double rotation = 0; // 旋转角度（度）

    public TextGeo(double x, double y, String text) {
        super(ObjectType.TEXT);
        this.x = x;
        this.y = y;
        this.label = text;
        this.labelVisible = true;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }

    @Override
    public void paint(GraphicsContext gc, WorldTransform transform, double w, double h) {
        if (label == null || label.isEmpty()) return;

        double sx = transform.worldToScreenX(x);
        double sy = transform.worldToScreenY(y);

        Font font = Font.font("Arial", labelFontSize > 0 ? labelFontSize : 16);
        gc.save();
        gc.translate(sx, sy);
        if (rotation != 0) {
            gc.rotate(rotation);
        }

        // 纯文本（无背景框、无边框）
        gc.setFill(labelColor != null ? labelColor : getEffectiveColor());
        gc.setFont(font);
        gc.fillText(label, 0, 0);

        gc.restore();
    }

    @Override
    public boolean hitTest(double wx, double wy, double tolerance) {
        double dx = wx - x;
        double dy = wy - y;
        // 基于字体大小的近似边界（世界坐标），加上容差
        double fs = labelFontSize > 0 ? labelFontSize : 16;
        double textW = (label != null ? label.length() : 1) * fs * 0.6 / 50.0;
        double textH = fs / 50.0;
        return Math.abs(dx) < textW + tolerance && Math.abs(dy) < textH + tolerance;
    }

    @Override
    public List<DraggablePoint> getDraggablePoints() {
        return List.of(new DraggablePoint(x, y, (nx, ny) -> {
            x = nx;
            y = ny;
        }));
    }

    @Override
    public void rotateAroundPoint(double cx, double cy, double angle) {
        double dx = x - cx, dy = y - cy;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        x = cx + dx * cos - dy * sin;
        y = cy + dx * sin + dy * cos;
        rotation += Math.toDegrees(angle);
    }

    @Override
    public double[] getBoundingBox() {
        return new double[]{x - 0.5, y - 0.2, x + 0.5, y + 0.2};
    }

    @Override
    public <T> T accept(GeometryVisitor<T> visitor) {
        return visitor.visitText(this);
    }
}
