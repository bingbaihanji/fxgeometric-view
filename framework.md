


## 几何图形绘制底层原理详解

### 一、整体架构概览

项目采用了**分层架构**，核心层次从下到上为：

```
GridChartView (视图容器)
    ↓
Canvas + GraphicsContext (JavaFX绘制层)
    ↓
WorldTransform (坐标系统转换)
    ↓
WorldPainter / WorldObject (绘制接口)
    ↓
具体实现：PointGeo, LineGeo, CircleGeo等
```


---

### 二、坐标变换系统（`WorldTransform`）

这是整个系统的**核心基础**。它维护两个坐标系统间的双向转换：

#### 关键参数：
- **`scale`**：缩放因子（世界单位 → 像素），初始值为 50（表示 1 个世界单位 = 50 像素）
- **`offsetX`、`offsetY`**：世界原点在屏幕上的像素位置

#### 转换公式：
```
// 世界坐标 → 屏幕坐标
screenX = offsetX + worldX × scale
screenY = offsetY - worldY × scale  （注意Y轴反向）

// 屏幕坐标 → 世界坐标
worldX = (screenX - offsetX) / scale
worldY = (offsetY - screenY) / scale
```


**Y轴反向**是重要细节：屏幕坐标系中Y向下为正，而数学坐标系中Y向上为正。因此转换时使用减法实现翻转。

---

### 三、图形绘制流程GridChartView.redraw()

绘制是一个**分层、递归的过程**：

```java
public void redraw() {
    // 1. 清空画布
    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    
    // 2. 绘制背景绘制器（网格、坐标轴）
    for (WorldPainter painter : painters) {
        painter.paint(gc, transform, w, h);  // GridPainter, AxesPainter
    }
    
    // 3. 绘制几何图形对象
    for (WorldObject obj : objects) {
        obj.paint(gc, transform, w, h);      // PointGeo, LineGeo, CircleGeo等
    }
    
    // 4. 绘制交互预览（正在绘制的图形）
    if (previewPainter != null) {
        previewPainter.accept(gc, transform);
    }
    
    // 5. 绘制特殊点吸附提示（高亮圈）
    if (nearbySpecialPoint != null) {
        drawSpecialPointHint(gc);
    }
}
```


每次调用都会：
- 清空整个画布
- 使用 `WorldTransform` 进行坐标转换
- 调用每个图形的 `paint()` 方法

例如，`PointGeo.paint()` 的绘制过程：

```java
public void paint(GraphicsContext gc, WorldTransform t, double w, double h) {
    // 1. 世界坐标 → 屏幕坐标转换
    double sx = t.worldToScreenX(x);
    double sy = t.worldToScreenY(y);
    
    // 2. 选择颜色（hover时高亮）
    gc.setFill(hover ? Color.ORANGE : color);
    
    // 3. 在屏幕坐标上绘制
    double r = hover ? 6 : 4;
    gc.fillOval(sx - r, sy - r, r * 2, r * 2);
    
    // 4. 绘制点的标签
    gc.fillText(name, sx + 8, sy - 8);
}
```


---

### 四、缩放实现原理

#### 滚轮缩放（`handleZoom()`）：

关键是**保持鼠标位置不变**（视觉上鼠标指向的世界点不动）：

```java
private void handleZoom(ScrollEvent e) {
    double newScale = transform.getScale();
    
    // 1. 根据滚轮方向改变缩放因子
    if (e.getDeltaY() > 0) {
        newScale *= 1.1;  // 放大10%
    } else {
        newScale *= 0.9;  // 缩小10%
    }
    
    // 2. 获取鼠标在世界中的位置
    double mouseX = e.getX();
    double mouseY = e.getY();
    double worldX = transform.screenToWorldX(mouseX);
    double worldY = transform.screenToWorldY(mouseY);
    
    // 3. 设置新的缩放因子
    transform.setScale(newScale);
    
    // 4. 调整偏移量，使鼠标位置的世界坐标保持不变
    double newOffsetX = mouseX - worldX * newScale;
    double newOffsetY = mouseY + worldY * newScale;
    transform.setOffset(newOffsetX, newOffsetY);
    
    redraw();
}
```


**核心逻辑**：缩放前后，鼠标位置的世界坐标必须相同：
- 旧：`worldX = (mouseX - offsetX) / oldScale`
- 新：`mouseX = newOffsetX + worldX × newScale`
- 因此：`newOffsetX = mouseX - worldX × newScale`

---

### 五、拖动实现原理（平移）

#### 中键拖拽 initMousePan()：

```java
private void initMousePan() {
    addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
        if (e.isMiddleButtonDown()) {
            panning = true;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        }
    });
    
    addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
        if (!panning) return;
        
        // 计算鼠标移动距离（屏幕坐标）
        double dx = e.getX() - lastMouseX;
        double dy = e.getY() - lastMouseY;
        
        // 直接将屏幕移动量加到偏移量上
        transform.setOffset(
            transform.getOffsetX() + dx,
            transform.getOffsetY() + dy
        );
        
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        
        redraw();
    });
}
```

**原理简单**：拖动就是直接改变 offsetX和 offsetY，使世界点在屏幕上移动对应像素距离。

---

### 六、几何图形交点计算 IntersectionUtils

这个工具类提供了**完整的交点计算算法**。以下是主要类型：

#### 1. **线段与线段的交点**（参数方程法）

```
设线段1：P = P1 + t(P2-P1)，t ∈ [0,1]
设线段2：Q = P3 + u(P4-P3)，u ∈ [0,1]

求解：P = Q 的参数t、u
检查：两个参数都在[0,1]范围内时有交点
```


#### 2. **线段与圆的交点**（二次方程法）

```
圆心为C，半径为r的圆：(x-cx)² + (y-cy)² = r²
线段参数方程：P(t) = P1 + t(P2-P1)

代入圆的方程得：at² + bt + c = 0
判别式 Δ = b² - 4ac：
  - Δ < 0：无交点
  - Δ = 0：相切（1个交点）
  - Δ > 0：相交（2个交点）
```


关键代码：
```java
double a = dx * dx + dy * dy;
double b = 2 * (dx * (x1 - cx) + dy * (y1 - cy));
double c = (x1 - cx) * (x1 - cx) + (y1 - cy) * (y1 - cy) - r * r;
double discriminant = b * b - 4 * a * c;

if (discriminant < 0) return intersections;  // 无解
// 求解：t1 = (-b ± √Δ) / 2a
```


#### 3. **圆与圆的交点**（几何法）

```
设两圆心距为d，半径分别为r1、r2

特殊情况检查：
  - d > r1 + r2：相离（无交点）
  - d < |r1 - r2|：一圆包含另一圆（无交点）
  - d = 0 且 r1 = r2：重合（无穷多交点）

一般情况：
  计算连接线上的投影点P2，再沿垂直方向计算两个交点
```


关键代码：
```java
double a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);  // 投影距离
double h = Math.sqrt(r1 * r1 - a * a);              // 垂直距离

// 计算投影点
double x3 = x1 + a * (x2 - x1) / d;
double y3 = y1 + a * (y2 - y1) / d;

// 计算两个交点
double ix1 = x3 + h * (y2 - y1) / d;
double iy1 = y3 - h * (x2 - x1) / d;
```


---

### 七、特殊点吸附机制

鼠标操作时有**三级吸附优先级**：

```java
// 优先级1：吸附到特殊点（交点等，距离10像素以内）
SpecialPoint nearestSpecialPoint = findNearestSpecialPoint(worldX, worldY);
if (nearestSpecialPoint != null) {
    worldX = nearestSpecialPoint.getX();
    worldY = nearestSpecialPoint.getY();
} else {
    // 优先级2：吸附到整数点（5像素以内）
    worldX = snapToInteger(worldX);
    
    // 优先级3：吸附到网格点（基于轴刻度，5像素以内）
    worldX = snapToGrid(worldX, step);
}
```


这给用户带来了**精确且流畅的交互体验**。

---

### 八、命中测试（拾取）

每个图形都实现了 [hitTest()](file://D:\javaProject\bingbaihanji\FXGeometricView\src\main\java\com\binbaihanji\view\layout\draw\geometry\WorldObject.java#L13-L13) 方法，用于检测点击是否在图形上：

```java
// PointGeo: 圆形判定
public boolean hitTest(double wx, double wy, double tol) {
    return Math.hypot(wx - x, wy - y) < tol;
}

// LineGeo: 点到线段距离判定
public boolean hitTest(double x, double y, double tolerance) {
    double distance = Math.abs(dy * x - dx * y + ...) / length;
    return distance <= tolerance;
}

// CircleGeo: 圆周判定（到圆周的距离）
public boolean hitTest(double x, double y, double tolerance) {
    double d = Math.hypot(x - cx, y - cy);
    return Math.abs(d - r) <= tolerance;  // 到圆周的距离
}
```


---

### 九、整合流程示例：用户缩放+拖动+点击

```
用户滚轮缩放（1.1倍）
  ↓
GridChartView.handleZoom()
  ├─ 计算新的scale
  ├─ 保持鼠标位置的世界坐标不变
  ├─ 调整offsetX/offsetY
  └─ 调用redraw()
  ↓
GridChartView.redraw()
  ├─ 清空canvas
  ├─ 遍历painters（网格、坐标轴）
  │   └─ 使用新的transform进行坐标变换和绘制
  ├─ 遍历objects（几何图形）
  │   └─ 每个图形的paint()都使用新的transform
  └─ 绘制完成

用户中键拖动
  ↓
GridChartView.initMousePan()
  ├─ 计算鼠标移动距离
  ├─ 更新offsetX/offsetY
  └─ 调用redraw()

用户点击图形
  ↓
GridChartView.initMouseClickOutput()
  ├─ 屏幕坐标 → 世界坐标
  ├─ 应用吸附机制
  ├─ 遍历objects（从后往前，堆叠顺序）
  │   ├─ 调用hitTest()检测是否命中
  │   └─ 若命中，调用onClick()
  └─ 输出或处理结果
```


---

### 十、关键设计要点总结

| 方面         | 实现方式                 | 优势               |
| ------------ | ------------------------ | ------------------ |
| **坐标变换** | 参数化的 WorldTransform  | 统一管理，易于调试 |
| **绘制**     | 分层Painter + Object模式 | 易于扩展新图形类型 |
| **缩放**     | 保持鼠标点不动           | 直观的放大体验     |
| **拖动**     | 直接修改offset           | 实现简洁高效       |
| **交点**     | 纯数学算法（无近似）     | 精度高，可靠性强   |
| **吸附**     | 多级优先级机制           | 兼顾精度和易用性   |

---

这就是项目几何图形绘制的完整原理。核心就是：**通过 WorldTransform统一管理坐标变换，使所有绘制和交互操作都在统一的坐标系下进行，配合精确的数学算法计算交点**。